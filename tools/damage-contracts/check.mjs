import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { fileURLToPath } from 'node:url';

export const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const directory = 'tools/damage-contracts';
const normalized = text => text.replace(/\r\n/g, '\n');
const digest = text => crypto.createHash('sha256').update(normalized(text)).digest('hex');

function walk(base) {
    return fs.readdirSync(base, { withFileTypes: true }).flatMap(entry => {
        const name = path.join(base, entry.name);
        return entry.isDirectory() ? walk(name) : [name];
    });
}

export function readInputs(base = root) {
    // 全量 Java 指纹覆盖通过回调、反射或新增辅助类引入的结算变化。
    const files = walk(path.join(base, 'src/main')).filter(file =>
        file.endsWith('.java') || /[/\\]damage_type[/\\]/.test(file) ||
        /[/\\]mixins\.[^/\\]+\.json$/.test(file));
    return Object.fromEntries(files.sort().map(file => [
        path.relative(base, file).replaceAll('\\', '/'), fs.readFileSync(file, 'utf8')
    ]));
}

export function snapshot(inputs) {
    return { version: 1, files: Object.fromEntries(Object.entries(inputs)
        .sort(([a], [b]) => a.localeCompare(b, 'en'))
        .map(([file, content]) => [file, digest(content)])) };
}

export function compare(expected, actual) {
    return [...new Set([...Object.keys(expected.files), ...Object.keys(actual.files)])]
        .sort().filter(file => expected.files[file] !== actual.files[file]);
}

export function validate(catalog, inputs) {
    const errors = [];
    if (catalog.version !== 1 || !Array.isArray(catalog.effects)) return ['目录格式错误'];
    const ids = new Set();
    for (const effect of catalog.effects) {
        if (!effect.id || ids.has(effect.id)) errors.push(`重复或缺少 ID: ${effect.id}`);
        ids.add(effect.id);
        for (const field of ['name', 'stage', 'amount', 'ownership', 'guard'])
            if (typeof effect[field] !== 'string' || !effect[field].trim())
                errors.push(`${effect.id}: 缺少 ${field}`);
        if (!Array.isArray(effect.sources) || !effect.sources.length)
            errors.push(`${effect.id}: 缺少源码入口`);
        for (const file of effect.sources ?? [])
            if (!(file in inputs)) errors.push(`${effect.id}: 源码不存在 ${file}`);
        for (const successor of effect.successors ?? [])
            if (!catalog.effects.some(other => other.id === successor))
                errors.push(`${effect.id}: 后继不存在 ${successor}`);
    }
    for (const [file, owner] of Object.entries(catalog.entryOwners ?? {})) {
        if (!(file in inputs)) errors.push(`入口文件不存在: ${file}`);
        if (!ids.has(owner)) errors.push(`入口分类不存在: ${owner}`);
        if (!catalog.effects.find(effect => effect.id === owner)?.sources.includes(file))
            errors.push(`入口未列入所属效果源码: ${file}`);
    }
    // 候选点扫描用于定位；完整性由上面的全量源码指纹复查保证。
    for (const [file, content] of Object.entries(inputs)) {
        if (!file.endsWith('.java')) continue;
        if (/(?:\.\s*(?:hurt|setHealth|setAbsorptionAmount|die|kill)|travail\$commitHealth)\s*\(/.test(content)
            && !catalog.entryOwners?.[file]) errors.push(`未分类的伤害或生命写入入口: ${file}`);
    }
    return errors;
}

export function inspect(base = root) {
    const inputs = readInputs(base);
    const catalog = JSON.parse(fs.readFileSync(path.join(base, directory, 'catalog.json'), 'utf8'));
    const expected = JSON.parse(fs.readFileSync(path.join(base, directory, 'baseline.json'), 'utf8'));
    if (expected.version !== 1 || !expected.files || !Object.keys(expected.files).length)
        throw new Error('源码基线格式错误或为空');
    const current = snapshot(inputs);
    return { errors: validate(catalog, inputs), changed: compare(expected, current), current };
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    try {
        const args = process.argv.slice(2);
        if (args.some(arg => arg !== '--candidate')) throw new Error('用法: node tools/damage-contracts/check.mjs [--candidate]');
        const result = inspect();
        if (args.includes('--candidate')) {
            const output = path.join(root, 'build/damage-contracts/candidate.json');
            fs.mkdirSync(path.dirname(output), { recursive: true });
            fs.writeFileSync(output, JSON.stringify(result.current, null, 2) + '\n');
            console.log('候选指纹已写入 build/damage-contracts/candidate.json；基线保持原样。');
        }
        for (const error of result.errors) console.error(error);
        for (const file of result.changed) console.error(`需要复查: ${file}`);
        if (result.errors.length || result.changed.length) process.exitCode = 1;
        else console.log('伤害目录结构、入口分类及源码基线一致。');
    } catch (error) {
        console.error(error.message);
        process.exitCode = 1;
    }
}
