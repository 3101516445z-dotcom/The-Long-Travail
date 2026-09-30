const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../src/main/resources/assets/the_long_travail');
const translations = ['zh_cn', 'en_us'].map(locale => JSON.parse(fs.readFileSync(path.join(root, 'lang', locale + '.json'), 'utf8')));
assert.deepEqual(Object.keys(translations[0]).sort(), Object.keys(translations[1]).sort(), 'Locale keys must match');
assert.equal(translations[0]['tooltip.the_long_travail.open_diary'], '\u957f\u6309[%1$s]\u9605\u8bfb\u8be6\u60c5', 'Chinese hold hint must survive file encoding');
assert.equal(translations[0]['gui.the_long_travail.diary.bookmark_hint'], '%1$s\u4e4b%2$s', 'Bookmark HUD uses the localized possessive separator');
const arities = { flourishing: [3, 1], abyss: [4, 1], far_reach: [3, 4], deep_valley: [4, 5], underworld: [2, 5], boundless: [2, 3] };
for (const locale of translations) {
    assert.match(locale['tooltip.the_long_travail.open_diary'], /\[%1\$s\]/);
    assert(!('gui.the_long_travail.diary.close_hint' in locale), 'Exit shortcuts are deliberately not displayed');
    for (const [aspect, counts] of Object.entries(arities)) {
        ['malice', 'witness'].forEach((state, index) => {
            const prefix = `tooltip.the_long_travail.${aspect}.${state}`;
            const effect = locale[prefix + '.effect'];
            assert.equal(typeof effect, 'string', prefix + ' needs the effect text used by the diary');
            assert.match(effect, /^\u2022[ \u00a0]\S/, prefix + ' starts with a spaced bullet');
            const placeholders = [...new Set([...effect.matchAll(/%(\d+)\$s/g)].map(match => +match[1]))].sort((a, b) => a - b);
            assert.deepEqual(placeholders, Array.from({length: counts[index]}, (_, i) => i + 1), prefix + ' must preserve every configured value');
        });
    }
}
assert(!fs.existsSync(path.join(root, 'font/diary.json')), 'GUI uses the default font');
assert(!fs.existsSync(path.join(root, 'font/zhimangxing.ttf')), 'Special handwriting font removed');
for (const [file, width, height] of [['diary_base', 1448, 1086], ['diary_bookmark', 2172, 724]]) {
    const data = fs.readFileSync(path.join(root, 'textures/gui', file + '.png'));
    assert.equal(data.toString('ascii', 1, 4), 'PNG');
    assert.equal(data.readUInt32BE(16), width);
    assert.equal(data.readUInt32BE(20), height);
    assert.equal(data[25], 6, 'Textures require RGBA transparency');
}
console.log('PASS: both locales, effect arguments, key hint, default GUI font, transparent textures.');
