import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';
import { spawn } from 'node:child_process';
import {guardProcess} from './process-guard.mjs';

const [round, phase, ...options] = process.argv.slice(2);
if (!['01', '02', '03', '04', '05', '06', '07', '08', '09', '10', '11', '12', '13', '14', '15', '16', '17'].includes(round)
        || !['before', 'after'].includes(phase)
        || options.some(option => option !== '--build') || options.length > 1) {
    console.error('Usage: node tools/structure-regression/run.mjs 01|02|03|04|05|06|07|08|09|10|11|12|13|14|15|16|17 before|after [--build]');
    process.exit(2);
}
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const stamp = new Date().toISOString().replace(/[:.]/g, '-');
const reportDir = path.join(root, 'build/structure-reports', `${stamp}-round-${round}-${phase}-${process.pid}`);
fs.mkdirSync(reportDir, { recursive: true });
function inputSnapshot() {
    const files = [];
    function walk(relative) {
        const full = path.join(root, relative);
        const stat = fs.lstatSync(full);
        if (stat.isSymbolicLink()) throw new Error(`Unexpected symbolic link: ${relative}`);
        if (stat.isDirectory()) {
            for (const name of fs.readdirSync(full).sort()) walk(`${relative}/${name}`);
        } else files.push({ path: relative, sha256: crypto.createHash('sha256').update(fs.readFileSync(full)).digest('hex') });
    }
    for (const relative of ['src/main', 'gradle', 'tools/structure-regression', 'build.gradle', 'settings.gradle', 'gradle.properties']) walk(relative);
    return files;
}
const inputs = inputSnapshot();
const argumentsList = [...(options.includes('--build') ? ['build'] : []), '-I',
    `tools/structure-regression/round-${round}.init.gradle`, `-PstructurePhase=${phase}`, 'runServer', '--offline', '--no-daemon'];
const resultNames = { '01': 'result.txt', '02': 'journey-api-result.txt', '03': 'accessory-restore-result.txt', '04': 'maid-compat-result.txt', '05': 'maid-compat-result.txt', '06': 'maid-compat-result.txt', '07': 'maid-compat-result.txt', '08': 'maid-compat-result.txt', '09': 'maid-compat-result.txt', '10': 'config-result.txt', '11': 'config-result.txt', '12': 'combat-attributes-result.txt', '13': 'maid-compat-result.txt', '14': 'restart-result.txt', '15': 'maid-compat-result.txt', '16': 'soak-result.txt', '17': 'structures-result.txt' };
const serverDir = path.join(root, round === '17' ? 'build/structure-defaults-server' : round === '14' ? 'build/structure-restart-server' : `build/structure-round-${round}-${round === '01' ? 'server' : phase}`);
const report = { round, phase, startedAt: new Date().toISOString(), root, arguments: argumentsList,
    inputs, nodeVersion: process.version, status: 'NOT_RUN' };
const reportPath = path.join(reportDir, 'report.json');
fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
const log = fs.createWriteStream(path.join(reportDir, 'gradle.log'), { flags: 'wx' });
console.log(`Running round ${round} (${phase}); evidence: ${reportDir}`);
// All command tokens are fixed or validated above; no user shell fragments are accepted.
const child = process.platform === 'win32'
    ? spawn('cmd.exe', ['/d', '/s', '/c', `gradlew.bat ${argumentsList.join(' ')}`], { cwd: root, windowsHide: true })
    : spawn('./gradlew', argumentsList, { cwd: root });
guardProcess(child, {timeoutMs: 15 * 60_000, onStop: reason => {
    report.interruption = reason;
    report.status = 'FAIL';
    fs.writeFileSync(reportPath, JSON.stringify(report, null, 2) + '\n');
}});
child.stdout.pipe(log, { end: false });
child.stderr.pipe(log, { end: false });
child.on('error', error => { report.launchError = String(error); });
child.on('close', (code, signal) => {
    log.end(() => {
        report.exitCode = code;
        report.signal = signal;
        report.finishedAt = new Date().toISOString();
        try {
            report.result = fs.readFileSync(path.join(serverDir, resultNames[round]), 'utf8').trim();
            report.inputsUnchanged = JSON.stringify(inputs) === JSON.stringify(inputSnapshot());
            for (const name of [resultNames[round], 'checks.txt', 'maid-compat-checks.txt', 'config-transitions.txt', 'addon-inputs.json', 'structures.properties']) {
                const file = path.join(serverDir, name);
                if (fs.existsSync(file)) fs.copyFileSync(file, path.join(reportDir, name));
            }
        } catch (error) { report.evidenceError = String(error); }
        report.status = !report.interruption && code === 0 && /^PASS(?:\s|$)/.test(report.result) && report.inputsUnchanged ? 'PASS' : 'FAIL';
        fs.writeFileSync(reportPath, JSON.stringify(report, null, 2) + '\n');
        console.log(`${report.status}: ${reportPath}`);
        process.exitCode = report.status === 'PASS' ? 0 : 1;
    });
});
