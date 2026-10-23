const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

// 解析前记录所有键，防止重复键被后面的译文静默覆盖。
function readLocale(source, label) {
    const values = JSON.parse(source);
    assert(values && !Array.isArray(values) && typeof values === 'object', label);
    for (const [key, value] of Object.entries(values)) {
        assert.equal(typeof value, 'string', `${label}: ${key} must contain text`);
        assert(value.trim().length > 0, `${label}: ${key} must not be empty`);
    }
    const keys = [];
    const seen = new Set();
    const pairs = /("(?:[^"\\]|\\.)*")\s*:\s*("(?:[^"\\]|\\.)*")/g;
    for (const match of source.matchAll(pairs)) {
        const key = JSON.parse(match[1]);
        assert(!seen.has(key), `${label}: duplicate key ${key}`);
        seen.add(key);
        keys.push(key);
    }
    assert.equal(keys.length, Object.keys(values).length, `${label}: expected flat string pairs`);
    return { values, keys };
}

// 编号参数允许换序或重复；无编号参数独立递增，%% 不占用参数。
function argumentSlots(text) {
    const slots = new Set();
    let next = 1;
    for (const match of text.matchAll(/%%|%(?:(\d+)\$)?s/g)) {
        if (match[0] === '%%') continue;
        const slot = match[1] === undefined ? next++ : Number(match[1]);
        assert(slot > 0, 'Translation argument indexes start at 1');
        slots.add(slot);
    }
    return [...slots].sort((a, b) => a - b);
}

// 第一个 locale 为参照，其余 locale 的键、顺序与占位参数槽位都必须与之一致。
function verifyLocales(...localized) {
    assert(localized.length >= 2, 'at least two locales are required');
    const [reference, ...others] = localized;
    for (const other of others) {
        assert.deepEqual(reference.keys, other.keys, 'Locale keys and grouping order must match');
        for (const key of reference.keys) {
            assert.deepEqual(
                argumentSlots(reference.values[key]),
                argumentSlots(other.values[key]),
                `${key}: localized argument slots must match`
            );
        }
    }
}

function selfTest() {
    assert.throws(() => readLocale('{"key":"first","key":"last"}', 'fixture'), /duplicate/);
    assert.throws(() => readLocale('{"key":{}}', 'fixture'), /must contain text/);
    assert.throws(() => readLocale('{"key":"text",}', 'fixture'), SyntaxError);
    assert.deepEqual(argumentSlots('%2$s %1$s %2$s'), [1, 2]);
    assert.deepEqual(argumentSlots('%s%% %s'), [1, 2]);
    assert.deepEqual(argumentSlots('%%s'), []);
    assert.throws(() => argumentSlots('%0$s'), /start at 1/);
    const chinese = readLocale('{"key":"%s"}', 'fixture');
    assert.throws(
        () => verifyLocales(chinese, readLocale('{"key":"missing"}', 'fixture')),
        /argument slots/
    );
    assert.throws(
        () => verifyLocales(chinese, readLocale('{"other":"%s"}', 'fixture')),
        /grouping order/
    );
}

function verify() {
    selfTest();
    const { readGroups } = require('./generate-language-files.cjs');
    const merged = readGroups(path.resolve(__dirname, '../src/main/lang'));
    console.log(`PASS: ${Object.keys(merged.zh_cn).length} language keys, groups and arguments.`);
}

module.exports = { verify, readLocale, verifyLocales, argumentSlots };
if (require.main === module) verify();
