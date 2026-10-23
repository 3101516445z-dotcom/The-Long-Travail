const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { readGroups, generate, locales } = require('./generate-language-files.cjs');
const temporaryRoot = path.resolve(__dirname, '../build/tmp/language-tests');
fs.mkdirSync(temporaryRoot, { recursive: true });
const root = fs.mkdtempSync(path.join(temporaryRoot, 'case-'));
const source = path.join(root, 'source');
const output = path.join(root, 'output');
let passed = 0;
function reset() {
    fs.mkdirSync(source, { recursive: true });
    fs.writeFileSync(path.join(source, 'groups.json'), '["common","aspects/abyss"]');
    for (const locale of locales) {
        fs.mkdirSync(path.join(source, locale, 'aspects'), { recursive: true });
        fs.writeFileSync(path.join(source, locale, 'common.json'), '{"key":"%1$s %%"}');
        fs.writeFileSync(path.join(source, locale, 'aspects/abyss.json'), '{"other":"text"}');
    }
}
function reject(change, pattern) {
    reset();
    change();
    assert.throws(() => readGroups(source), pattern);
    passed++;
}
const put = (file, content) => fs.writeFileSync(path.join(source, file), content);
try {
    reset();
    generate(source, output);
    const destination = path.join(output, 'assets/the_long_travail/lang/zh_cn.json');
    const first = fs.readFileSync(destination);
    const modified = fs.statSync(destination).mtimeMs;
    generate(source, output);
    assert.deepEqual(fs.readFileSync(destination), first);
    assert.equal(fs.statSync(destination).mtimeMs, modified);
    passed++;
    reject(() => put('zh_cn/common.json', '{"key":"a","key":"b"}'), /duplicate/);
    reject(() => {
        for (const l of locales) put(`${l}/aspects/abyss.json`, '{"key":"text"}');
    }, /duplicate key/);
    reject(() => put('zh_cn/common.json', '{"key":"text",}'), /JSON|property/i);
    reject(() => put('zh_cn/common.json', '{"key":3}'), /must contain text/);
    reject(() => put('zh_cn/common.json', '{"key":" "}'), /empty/);
    reject(() => put('en_us/common.json', '{}'), /grouping order/);
    reject(() => put('en_us/common.json', '{"key":"%2$s"}'), /argument slots/);
    reject(() => {
        put('zh_cn/common.json', '{"a":"a","b":"b"}');
        put('en_us/common.json', '{"b":"b","a":"a"}');
    }, /grouping order/);
    for (const locale of locales.slice(2)) {
        reject(() => put(`${locale}/common.json`, '{}'), /grouping order/);
        reject(() => put(`${locale}/common.json`, '{"key":"%2$s"}'), /argument slots/);
        reject(() => put(`${locale}/common.json`, '{"key":"§a%1$s %%§r"}'), /color codes/);
    }
    reject(() => put('groups.json', '["common","common"]'), /duplicate groups/);
    reject(() => put('groups.json', '["../escape"]'), /Invalid group/);
    reject(() => fs.unlinkSync(path.join(source, 'en_us/common.json')), /ENOENT/);
    reject(() => put('zh_cn/unlisted.json', '{}'), /Unlisted/);
    fs.unlinkSync(path.join(source, 'zh_cn/unlisted.json'));
    reset();
    put('en_us/common.json', '{"key":"broken"}');
    assert.throws(() => generate(source, output), /argument slots/);
    assert.deepEqual(fs.readFileSync(destination), first);
    passed++;
    console.log(`PASS: ${passed} language-generation cases.`);
} finally {
    assert(path.dirname(root) === temporaryRoot);
    fs.rmSync(root, { recursive: true, force: true });
}
