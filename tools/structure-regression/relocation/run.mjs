import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {spawn} from 'node:child_process';
import {guardProcess} from '../process-guard.mjs';

const root = process.cwd();
const plans = {
    reading: ['tools/structure-regression/relocation/reading-client.init.gradle', 'build/reading-client/reading-result.txt'],
    'rain-client': ['tools/structure-regression/relocation/rain-client.init.gradle', 'build/rain-client/rain-client-result.txt'],
    boundless: ['tools/structure-regression/relocation/boundless-client.init.gradle', 'build/boundless-client/boundless-client-result.txt'],
    'lantern-client': ['tools/structure-regression/relocation/lantern-client.init.gradle', 'build/lantern-client/lantern-client-result.txt'],
    'abundance-client': ['tools/structure-regression/abundance-client.init.gradle', 'build/abundance-client/client-result.txt'],
    'tales-client': ['tools/structure-regression/tales-preview.init.gradle', 'build/tales-preview/result.txt'],
    hotpath: ['tools/structure-regression/relocation/hotpath.init.gradle', 'build/hotpath-review-server/hotpath-result.txt'],
    'hotpath-relocation': ['tools/structure-regression/relocation/hotpath.init.gradle', 'build/hotpath-review-server/hotpath-result.txt', '-PhotpathRelocation'],
    dream: ['tools/structure-regression/relocation/dream-request.init.gradle', 'build/dream-request-server/dream-request-result.txt'],
    abundance: ['tools/structure-regression/abundance.init.gradle', 'build/abundance-server/abundance-result.txt'],
    tales: ['tools/structure-regression/tales.init.gradle', 'build/tales-server/tales-result.txt'],
    fluid: ['tools/structure-regression/fluid-core.init.gradle', 'build/fluid-core-server/fluid-core-result.txt'],
};

function fingerprint() {
    const hash = crypto.createHash('sha256');
    function walk(p) {
        for (const e of fs.readdirSync(p, {withFileTypes: true}).sort((a, b) => a.name.localeCompare(b.name))) {
            const f = path.join(p, e.name);
            if (e.isDirectory()) walk(f);
            else if (e.isFile()) {
                hash.update(path.relative(root, f));
                hash.update(fs.readFileSync(f));
            }
        }
    }
    for (const d of ['src/main', 'gradle', 'tools']) walk(path.join(root, d));
    for (const p of ['build.gradle', 'settings.gradle', 'gradle.properties']) hash.update(fs.readFileSync(p));
    return hash.digest('hex');
}

const clients = new Set(['reading', 'rain-client', 'boundless', 'lantern-client', 'abundance-client', 'tales-client']);
const names = process.argv.slice(2);
if (names.includes('--list')) {
    console.log(Object.keys(plans).join('\n'));
    process.exit(0);
}
if (!names.length || names.some(n => !Object.hasOwn(plans, n))) throw Error('Select known checks');
for (const name of names)
    if (!fs.existsSync(plans[name][0])) throw Error('Missing fixture: ' + plans[name][0]);

const dir = path.join(root, 'build/relocation-evidence', new Date().toISOString().replace(/[:.]/g, '-'));
fs.mkdirSync(dir, {recursive: true});
const inputs = fingerprint();
for (const name of names) {
    const [init, result, option = ''] = plans[name];
    const report = {name, init, result, inputs, status: 'NOT_RUN', started: new Date().toISOString()};
    const rp = path.join(dir, name + '.json');
    const save = () => fs.writeFileSync(rp, JSON.stringify(report, null, 2));
    save();
    console.log('Running ' + name + '; evidence: ' + rp);
    if (fs.existsSync(result)) fs.copyFileSync(result, path.join(dir, name + '-previous-result.txt'));
    fs.mkdirSync(path.dirname(result), {recursive: true});
    fs.writeFileSync(result, 'NOT_RUN');
    const startedMs = Date.now();
    const log = fs.openSync(path.join(dir, name + '.log'), 'w');
    const code = await new Promise((resolve, reject) => {
        const child = spawn(
            'cmd.exe',
            ['/d', '/s', '/c', `gradlew.bat -I ${init} ${option} ${clients.has(name) ? 'runClient' : 'runServer'} --offline --no-daemon`],
            {cwd: root, windowsHide: true, stdio: ['ignore', log, log]}
        );
        guardProcess(child, {
            timeoutMs: 15 * 60_000,
            onStop: reason => {
                report.interruption = reason;
                save();
            }
        });
        child.on('error', reject);
        child.on('close', resolve);
    });
    fs.closeSync(log);
    report.exitCode = code;
    report.finished = new Date().toISOString();
    report.resultText = fs.existsSync(result) ? fs.readFileSync(result, 'utf8') : 'MISSING';
    report.inputsUnchanged = fingerprint() === inputs;
    report.status = code === 0 && /^PASS(?:\s|$)/.test(report.resultText)
        && report.inputsUnchanged && !report.interruption ? 'PASS' : 'FAIL';
    if (fs.existsSync(result)) fs.copyFileSync(result, path.join(dir, name + '-result.txt'));
    for (const extra of ['logs/latest.log', 'logs/debug.log']) {
        const p = path.join(path.dirname(result), extra);
        if (fs.existsSync(p) && fs.statSync(p).mtimeMs >= startedMs)
            fs.copyFileSync(p, path.join(dir, name + '-' + path.basename(extra)));
    }
    const shots = path.join(path.dirname(result), 'screenshots');
    if (fs.existsSync(shots)) {
        for (const f of fs.readdirSync(shots)) {
            const p = path.join(shots, f);
            if (fs.statSync(p).mtimeMs >= startedMs) fs.copyFileSync(p, path.join(dir, name + '-' + f));
        }
    }
    save();
    console.log(report.status + ': ' + rp);
    if (report.status !== 'PASS') {
        process.exitCode = 1;
        break;
    }
}
