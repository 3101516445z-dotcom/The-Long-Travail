import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';
import { guardProcess } from './process-guard.mjs';

const root = process.cwd();
const variant = process.argv[2] || 'core';
const variants = ['core', 'core-boundaries', 'ftb', 'ftb-boundaries', 'chunks', 'storage', 'lootr'];
if (!variants.includes(variant)) throw Error('Unknown variant');
const runId = process.argv[3] || Date.now().toString();
if (!/^[A-Za-z0-9_-]+$/.test(runId)) {
    throw Error('runId must contain only letters, digits, underscores or hyphens');
}
const java = process.env.TREMOR_JAVA
    || (process.env.JAVA_HOME ? path.join(process.env.JAVA_HOME, 'bin', 'java.exe') : 'java');
const libraries = path.resolve(process.env.TREMOR_LIBRARIES
    || path.join(root, 'build/tremor-release-verification/libraries'));
const configSeed = process.argv[4] ? path.resolve(root, process.argv[4], 'config') : null;
const dir = path.join(root, 'build', 'tremor-release-' + variant + '-' + runId);
fs.mkdirSync(path.join(dir, 'mods'), { recursive: true });
if (configSeed) fs.cpSync(configSeed, path.join(dir, 'config'), { recursive: true });
// 共用只读 Forge 运行库，世界、配置和日志均写入独立运行目录。
fs.symlinkSync(libraries, path.join(dir, 'libraries'), 'junction');
const installed = path.resolve(process.env.TREMOR_MODS || path.join(root, '../../mods'));
const wanted = ['curios-forge-'];
if (!variant.startsWith('core')) wanted.push('ftb-ultimine', 'ftb-library', 'ftb-teams', 'architectury');
if (variant === 'chunks') wanted.push('ftb-chunks');
if (variant === 'storage') wanted.push('sophisticatedcore', 'sophisticatedstorage', 'sophisticatedbackpacks');
if (variant === 'lootr') wanted.push('lootr');
const inputs = [
    path.join(root, 'build/libs/The_Long_Travail-0.1.3.jar'),
    path.join(root, 'build/libs/travail-tremor-test-1.jar'),
];
for (const pattern of wanted) {
    const matches = fs.readdirSync(installed).filter(name => name.includes(pattern) && name.endsWith('.jar'));
    if (matches.length !== 1) throw Error('Ambiguous dependency ' + pattern + ': ' + matches);
    inputs.push(path.join(installed, matches[0]));
}
const hashes = [];
for (const file of inputs) {
    fs.copyFileSync(file, path.join(dir, 'mods', path.basename(file)));
    hashes.push({ file, sha256: crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex') });
}
fs.copyFileSync(path.join(root, 'run/eula.txt'), path.join(dir, 'eula.txt'));
fs.writeFileSync(path.join(dir, 'server.properties'),
    'online-mode=false\nserver-ip=127.0.0.1\nserver-port=25749\nlevel-name=tremor-test\n'
    + 'level-type=minecraft\\:flat\nlevel-seed=20261027\ngenerate-structures=false\n'
    + 'view-distance=2\nsimulation-distance=2\nmax-tick-time=60000\n');
fs.writeFileSync(path.join(dir, 'environment.json'),
    JSON.stringify({ variant, runId, hashes, configSeed, java, libraries }, null, 2));

async function run(label, properties) {
    fs.writeFileSync(path.join(dir, 'tremor-result.txt'), 'NOT_RUN');
    const fd = fs.openSync(path.join(dir, label + '.log'), 'w');
    const child = spawn(java,
        ['-Xmx2G', ...properties, '@libraries/net/minecraftforge/forge/1.20.1-47.4.10/win_args.txt', '--nogui'],
        { cwd: dir, windowsHide: true, stdio: ['ignore', fd, fd] });
    const guard = guardProcess(child, {
        timeoutMs: 900000,
        onStop: reason => console.error(label, reason),
    });
    let code;
    try {
        code = await new Promise((resolve, reject) => {
            child.once('error', reject);
            child.once('close', resolve);
        });
    } finally {
        fs.closeSync(fd);
    }
    const result = fs.readFileSync(path.join(dir, 'tremor-result.txt'), 'utf8');
    fs.writeFileSync(path.join(dir, label + '-result.txt'), result);
    console.log(label, code, result);
    if (code !== 0 || guard.reason || !result.startsWith('PASS ')) {
        throw Error(label + ' failed: ' + result + ' log=' + dir);
    }
}

const properties = ['-Dtravail.tremor.pressure=true'];
if (variant === 'ftb') properties.push('-Dtravail.tremor.chainOnly=true');
else if (variant !== 'core') properties.push('-Dtravail.tremor.targeted=true');
if (variant === 'core-boundaries') properties.push('-Dtravail.tremor.interleavedRetest=true');
console.log('RUN_DIRECTORY', dir);
await run('effects', properties);
if (variant === 'core') {
    for (let n = 1; n <= 5; n++) await run('restart-' + n, ['-Dtravail.tremor.restart=true']);
}
console.log('COMPLETED', dir);
