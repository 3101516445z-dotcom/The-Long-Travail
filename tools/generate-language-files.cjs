const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { readLocale, verifyLocales } = require('./verify-language-files.cjs');

// 首个 locale（zh_cn）作为键与顺序的参照；其余 locale 必须与它完全对齐。
const locales = ['zh_cn', 'en_us', 'zh_tw', 'zh_hk', 'lzh'];

function filesUnder(directory) {
    return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
        assert(!entry.isSymbolicLink(), `${directory}: symbolic links are not supported`);
        const file = path.join(directory, entry.name);
        return entry.isDirectory() ? filesUnder(file) : [file];
    });
}

function readGroups(root) {
    const groups = JSON.parse(fs.readFileSync(path.join(root, 'groups.json'), 'utf8'));
    assert(Array.isArray(groups) && groups.length > 0, 'groups.json must be a non-empty array');
    assert.equal(new Set(groups).size, groups.length, 'groups.json contains duplicate groups');
    for (const group of groups) {
        assert(typeof group === 'string' && /^[a-z][a-z0-9_]*(?:\/[a-z][a-z0-9_]*)*$/.test(group),
            `Invalid group path: ${group}`);
    }
    const expected = new Set(['groups.json', 'README.md']);
    for (const locale of locales) for (const group of groups) expected.add(`${locale}/${group}.json`);
    for (const file of filesUnder(root)) {
        const relative = path.relative(root, file).split(path.sep).join('/');
        assert(expected.has(relative), `Unlisted language source: ${relative}`);
    }
    const merged = Object.fromEntries(locales.map(locale => [locale, Object.create(null)]));
    const owners = new Map();
    for (const group of groups) {
        const pair = locales.map(locale => {
            const label = `${locale}/${group}.json`;
            return readLocale(fs.readFileSync(path.join(root, label), 'utf8'), label);
        });
        try { verifyLocales(...pair); } catch (error) {
            throw new Error(`${group}: ${error.message}`, { cause: error });
        }
        for (const key of pair[0].keys) {
            assert(!owners.has(key), `${group}: duplicate key ${key}, already in ${owners.get(key)}`);
            owners.set(key, group);
        }
        locales.forEach((locale, index) => Object.assign(merged[locale], pair[index].values));
    }
    return merged;
}

function generate(root, output) {
    // 所有分组通过校验后才写入，避免留下部分更新的译文。
    const merged = readGroups(root);
    const directory = path.join(output, 'assets/the_long_travail/lang');
    fs.mkdirSync(directory, { recursive: true });
    for (const locale of locales) {
        const file = path.join(directory, `${locale}.json`);
        const text = JSON.stringify(merged[locale], null, 2) + '\n';
        if (!fs.existsSync(file) || fs.readFileSync(file, 'utf8') !== text) fs.writeFileSync(file, text);
    }
    console.log(`Generated ${locales.length} locales, ${Object.keys(merged.zh_cn).length} keys each.`);
    return merged;
}

module.exports = { readGroups, generate, locales };
if (require.main === module) {
    const root = process.argv[2] || path.resolve(__dirname, '../src/main/lang');
    const output = process.argv[3] || path.resolve(__dirname, '../build/generated/resources/language');
    generate(root, output);
}
