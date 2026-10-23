// 单个 locale 的局部自检：比对指定分组与 zh_cn 的键、顺序、占位符、颜色码。
// 用法: node tools/verify-locale.cjs <locale> <group> [<group>...]
// 例:   node tools/verify-locale.cjs zh_tw common diary catalog
const fs = require('node:fs');
const path = require('node:path');
const { readLocale, argumentSlots } = require('./verify-language-files.cjs');

const root = path.resolve(__dirname, '../src/main/lang');
const [locale, ...groups] = process.argv.slice(2);

if (!locale || groups.length === 0) {
    console.error('用法: node tools/verify-locale.cjs <locale> <group> [<group>...]');
    console.error('locale: zh_tw | zh_hk | lzh');
    process.exit(2);
}

const colorCodes = text => (text.match(/§[0-9a-fk-or]/g) || []).sort().join('');

let failures = 0;
let totalKeys = 0;

for (const group of groups) {
    const refPath = path.join(root, 'zh_cn', `${group}.json`);
    const targetPath = path.join(root, locale, `${group}.json`);
    const problems = [];

    let ref;
    let target;
    try {
        ref = readLocale(fs.readFileSync(refPath, 'utf8'), `zh_cn/${group}`);
        target = readLocale(fs.readFileSync(targetPath, 'utf8'), `${locale}/${group}`);
    } catch (error) {
        console.log(`FAIL ${locale}/${group}: ${error.message}`);
        failures++;
        continue;
    }

    if (ref.keys.length !== target.keys.length) {
        problems.push(`键数 ${target.keys.length} != zh_cn ${ref.keys.length}`);
    }
    const refKeySet = new Set(ref.keys);
    const targetKeySet = new Set(target.keys);
    for (const key of target.keys) if (!refKeySet.has(key)) problems.push(`多出的键: ${key}`);
    for (const key of ref.keys) if (!targetKeySet.has(key)) problems.push(`缺少的键: ${key}`);

    if (problems.length === 0) {
        const orderDiff = ref.keys.findIndex((key, index) => key !== target.keys[index]);
        if (orderDiff !== -1) problems.push(`键顺序在第 ${orderDiff + 1} 个键起不一致: ${ref.keys[orderDiff]} vs ${target.keys[orderDiff]}`);
    }

    if (problems.length === 0) {
        for (const key of ref.keys) {
            const expected = argumentSlots(ref.values[key]);
            const actual = argumentSlots(target.values[key]);
            if (JSON.stringify(expected) !== JSON.stringify(actual)) {
                problems.push(`${key}: 占位符 ${JSON.stringify(actual)} != ${JSON.stringify(expected)}`);
            }
            const expectedColors = colorCodes(ref.values[key]);
            const actualColors = colorCodes(target.values[key]);
            if (expectedColors !== actualColors) {
                problems.push(`${key}: 颜色码 "${actualColors}" != "${expectedColors}"`);
            }
        }
    }

    totalKeys += Math.min(ref.keys.length, target.keys.length);
    if (problems.length === 0) {
        console.log(`PASS ${locale}/${group}: ${ref.keys.length} 键`);
    } else {
        failures++;
        console.log(`FAIL ${locale}/${group}: ${problems.length} 处问题`);
        for (const problem of problems.slice(0, 12)) console.log(`  - ${problem}`);
        if (problems.length > 12) console.log(`  ... 另有 ${problems.length - 12} 处`);
    }
}

console.log(failures === 0
    ? `\n全部通过（${groups.length} 组 / ${totalKeys} 键 / ${locale}）`
    : `\n${failures} 组未通过`);
process.exit(failures === 0 ? 0 : 1);
