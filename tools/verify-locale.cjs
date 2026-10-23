// 单个 locale 的局部自检：比对指定分组与 zh_cn 的键、顺序、占位符、颜色码。
// 用法: node tools/verify-locale.cjs <locale> <group> [<group>...]
// 例:   node tools/verify-locale.cjs zh_tw common diary catalog
const fs = require('node:fs');
const path = require('node:path');
const { readLocale, verifyLocales, verifyColors } = require('./verify-language-files.cjs');
const { locales } = require('./generate-language-files.cjs');
const root = path.resolve(__dirname, '../src/main/lang');
const [locale, ...requestedGroups] = process.argv.slice(2);
const registeredGroups = JSON.parse(fs.readFileSync(path.join(root, 'groups.json'), 'utf8'));
if (!locales.includes(locale) || requestedGroups.some(group => !registeredGroups.includes(group))) {
    console.error(`用法: node tools/verify-locale.cjs <${locales.join('|')}> [group ...]`);
    process.exit(2);
}
const groups = requestedGroups.length ? [...new Set(requestedGroups)] : registeredGroups;
let failures = 0;
let totalKeys = 0;
for (const group of groups) {
    try {
        const read = language => readLocale(fs.readFileSync(path.join(root, language, `${group}.json`), 'utf8'), `${language}/${group}`);
        const reference = read('zh_cn');
        const target = read(locale);
        verifyLocales(reference, target);
        if (locale !== 'en_us') verifyColors(reference, target);
        totalKeys += reference.keys.length;
        console.log(`PASS ${locale}/${group}: ${reference.keys.length} 键`);
    } catch (error) {
        failures++;
        console.error(`FAIL ${locale}/${group}: ${error.message}`);
    }
}
console.log(`${groups.length} 组 / ${totalKeys} 键通过 / ${failures} 组失败`);
process.exitCode = failures ? 1 : 0;
