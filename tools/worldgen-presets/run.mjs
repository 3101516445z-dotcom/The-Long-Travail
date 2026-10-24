import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';
import { guardProcess } from '../structure-regression/process-guard.mjs';

const mods = path.resolve('../../mods');
const runRoot = path.resolve(
    process.env.WORLDGEN_TEST_ROOT || 'build/worldgen-presets-' + Date.now()
);
const libraryRoot = path.resolve('build/release-validation');
const targets = JSON.parse(
    fs.readFileSync('tools/worldgen-presets/targets.json', 'utf8')
);
const reloadOnly = process.env.WORLDGEN_RELOAD_ONLY === '1';
if (reloadOnly && !process.env.WORLDGEN_TEST_ROOT)
    throw Error('Reload requires WORLDGEN_TEST_ROOT');
const sets = {
    ars_nouveau: [/ars_nouveau-.*\.jar$/],
    cataclysm: [/L_Enders_Cataclysm-.*\.jar$/, /lionfishapi-.*\.jar$/],
    irons_spellbooks: [
        /irons_spellbooks-.*\.jar$/,
        /^irons_lib-.*\.jar$/,
        /^player-animation-lib-.*\.jar$/
    ],
    goety: [/\] goety-.*\.jar$/],
    touhou_little_maid_spell: [
        /touhou_little_maid_spell-.*\.jar$/,
        /\] touhoulittlemaid-.*\.jar$/
    ],
    legendary_monsters: [/legendarymonsters-.*\.jar$/],
    eeeabsmobs: [/eeeabsmobs-.*\.jar$/],
    mowziesmobs: [/mowziesmobs-.*\.jar$/],
    alexscaves: [/alexscaves-.*\.jar$/, /^citadel-.*\.jar$/]
};
if (
    process.env.WORLDGEN_ONLY &&
    !Object.hasOwn(sets, process.env.WORLDGEN_ONLY)
) {
    throw Error('Unknown WORLDGEN_ONLY: ' + process.env.WORLDGEN_ONLY);
}
const hash = (p) =>
    crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
fs.mkdirSync(runRoot, { recursive: true });
const summary = [];
console.log('RUN_ROOT=' + runRoot);
for (const [ns, patterns] of Object.entries(sets)) {
    if (process.env.WORLDGEN_ONLY && ns !== process.env.WORLDGEN_ONLY) continue;
    const dir = path.join(runRoot, ns);
    fs.mkdirSync(path.join(dir, 'mods'), { recursive: true });
    if (reloadOnly && !fs.existsSync(path.join(dir, 'structures.properties')))
        throw Error('Missing saved locations for ' + ns);
    const manifest = [];
    for (const pattern of [
        /^curios-forge-.*\.jar$/,
        /^geckolib-forge-.*\.jar$/,
        ...patterns
    ]) {
        const matches = fs.readdirSync(mods).filter((n) => pattern.test(n));
        if (matches.length !== 1)
            throw Error('Ambiguous mod ' + pattern + ' ' + matches);
        const source = path.join(mods, matches[0]),
            copy = path.join(dir, 'mods', matches[0]);
        fs.copyFileSync(source, copy);
        manifest.push({ source, copy, sha256: hash(source) });
    }
    for (const name of [
        'The_Long_Travail-0.1.2-fix.jar',
        'travail-worldgen-presets-smoke-1.jar'
    ]) {
        const source = path.resolve('build/libs', name),
            copy = path.join(dir, 'mods', name);
        fs.copyFileSync(source, copy);
        manifest.push({ source, copy, sha256: hash(source) });
    }
    fs.writeFileSync(
        path.join(dir, reloadOnly ? 'final-inputs.json' : 'inputs.json'),
        JSON.stringify(manifest, null, 2)
    );
    fs.copyFileSync('run/eula.txt', path.join(dir, 'eula.txt'));
    fs.writeFileSync(
        path.join(dir, 'targets.json'),
        JSON.stringify(
            targets.filter((t) => t.id.startsWith(ns + ':')),
            null,
            2
        )
    );
    fs.writeFileSync(
        path.join(dir, 'server.properties'),
        'online-mode=false\nserver-ip=127.0.0.1\nserver-port=25668\nlevel-name=world\nlevel-seed=12345\nview-distance=2\nsimulation-distance=2\nmax-tick-time=0\ngenerate-structures=true\n'
    );
    // Forge 参数文件使用相对路径，目录联接仅共享运行库。
    if (!fs.existsSync(path.join(dir, 'libraries')))
        fs.symlinkSync(
            path.join(libraryRoot, 'libraries'),
            path.join(dir, 'libraries'),
            'junction'
        );
    const runs = [];
    for (const stage of reloadOnly ? ['after'] : ['before', 'after']) {
        fs.writeFileSync(path.join(dir, 'structures-result.txt'), 'NOT_RUN');
        console.log('START ' + ns + ' ' + stage);
        const prefix = (reloadOnly ? 'final-' : '') + stage;
        const log = fs.createWriteStream(path.join(dir, prefix + '.log'));
        const child = spawn(
            process.env.JAVA_HOME
                ? path.join(process.env.JAVA_HOME, 'bin/java.exe')
                : 'C:/Program Files/Java/jdk-21/bin/java.exe',
            [
                '-Xmx4G',
                '-Dtravail.structures.stage=' + stage,
                '@libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt',
                '--nogui'
            ],
            { cwd: dir, windowsHide: true }
        );
        const guard = guardProcess(child, { timeoutMs: 1200000 });
        child.stdout.pipe(log, { end: false });
        child.stderr.pipe(log, { end: false });
        const code = await new Promise((resolve, reject) => {
            child.once('error', reject);
            child.once('close', resolve);
        });
        await new Promise((resolve) => log.end(resolve));
        const result = fs.readFileSync(
            path.join(dir, 'structures-result.txt'),
            'utf8'
        );
        fs.writeFileSync(path.join(dir, prefix + '-result.txt'), result);
        const ok = code === 0 && !guard.reason && result.startsWith('PASS');
        runs.push({
            stage,
            code,
            interruption: guard.reason,
            status: ok ? 'PASS' : 'FAIL',
            result
        });
        console.log(ns + ' ' + stage + ' ' + (ok ? 'PASS' : 'FAIL'));
        if (!ok) break;
    }
    const unchanged = manifest.every(
        (m) => hash(m.source) === m.sha256 && hash(m.copy) === m.sha256
    );
    summary.push({
        namespace: ns,
        status:
            runs.length === (reloadOnly ? 1 : 2) &&
            runs.every((r) => r.status === 'PASS') &&
            unchanged
                ? 'PASS'
                : 'FAIL',
        unchanged,
        runs
    });
    fs.writeFileSync(
        path.join(runRoot, reloadOnly ? 'final-report.json' : 'report.json'),
        JSON.stringify(summary, null, 2)
    );
}
console.log(
    JSON.stringify(
        summary.map((r) => ({ namespace: r.namespace, status: r.status })),
        null,
        2
    )
);
process.exitCode = summary.every((r) => r.status === 'PASS') ? 0 : 1;
