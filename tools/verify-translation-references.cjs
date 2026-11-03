const fs = require('node:fs');
const path = require('node:path');

function files(directory, suffix) {
    return fs.readdirSync(directory, { withFileTypes: true }).flatMap(entry => {
        const filename = path.join(directory, entry.name);
        if (entry.isDirectory()) return files(filename, suffix);
        return entry.name.endsWith(suffix) ? [filename] : [];
    });
}

const locales = fs.readdirSync('src/main/lang')
    .filter(name => fs.statSync(path.join('src/main/lang', name)).isDirectory());
const dictionaries = locales.map(locale => ({
    locale,
    values: Object.assign({}, ...files(path.join('src/main/lang', locale), '.json')
        .map(filename => JSON.parse(fs.readFileSync(filename, 'utf8')))),
}));
const references = new Map();
for (const filename of files('src/main/java', '.java')) {
    const source = fs.readFileSync(filename, 'utf8');
    // 只检查完整字符串参数，不把动态前缀或外部模组的键当成完整键。
    const calls = /(?:Component\.translatable(?:WithFallback)?|TooltipTranslations\.text)\(\s*"([^"\n]+)"\s*[,)]/g;
    for (const match of source.matchAll(calls)) {
        if (match[1].includes('.the_long_travail.')) references.set(match[1], filename);
    }
}

const glossary = fs.readFileSync(
    'src/main/java/com/thelongtravail/client/catalog/CatalogGlossary.java', 'utf8');
const ids = glossary.slice(
    glossary.indexOf('private static final List<String> IDS'),
    glossary.indexOf('private record Match'));
const dynamicBodies = ['lantern_enemies', 'lantern_combat', 'gold_panning', 'delicacy', 'growth'];
for (const match of ids.matchAll(/"([a-z_]+)"/g)) {
    for (const suffix of ['title', 'aliases', 'body']) {
        // 这些正文由专门的动态卡片生成器提供。
        if (suffix === 'body' && dynamicBodies.includes(match[1])) continue;
        references.set(
            `gui.the_long_travail.catalog.glossary.${match[1]}.${suffix}`, 'CatalogGlossary.IDS');
    }
}

const errors = [];
for (const { locale, values } of dictionaries) {
    for (const [key, filename] of references) {
        if (!(key in values)) errors.push(`${locale}: ${key} (${filename})`);
    }
}
if (errors.length) throw new Error('Missing referenced translations:\n' + errors.join('\n'));
console.log(`TRANSLATION_REFERENCES_PASS keys=${references.size} locales=${locales.length}; dynamic expressions require client regression`);
