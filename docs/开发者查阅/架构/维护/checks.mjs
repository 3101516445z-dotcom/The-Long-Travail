// Static documentation checks only. No game or build execution.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const slash = p => p.split(path.sep).join('/');
export function filesUnder(dir) {
  if (!fs.existsSync(dir)) return [];
  return fs.readdirSync(dir, {withFileTypes: true}).flatMap(entry => {
    if (entry.isSymbolicLink()) throw Error('Symbolic link outside audit contract: ' + path.join(dir, entry.name));
    const p = path.join(dir, entry.name);
    return entry.isDirectory() ? filesUnder(p) : [p];
  }).sort();
}
export function inputSnapshot(root) {
  const directories = ['src/main/resources', 'src/main/lang', 'gradle'];
  const fixed = ['build.gradle', 'settings.gradle', 'gradle.properties', 'gradlew', 'gradlew.bat',
    'tools/generate-language-files.cjs', 'tools/verify-language-files.cjs',
    'tools/test-language-generation.cjs', 'tools/verify-locale.cjs'];
  const files = [...directories.flatMap(p => filesUnder(path.join(root, p))),
    ...fixed.map(p => path.join(root, p)).filter(p => fs.existsSync(p))]
    .map(p => ({path: slash(path.relative(root, p)), sha256: hash(fs.readFileSync(p))}))
    .sort((a, b) => a.path < b.path ? -1 : a.path > b.path ? 1 : 0);
  return {schema: 1, scope: {directories, fixed},
    fingerprint: hash(files.map(f => f.path + '\0' + f.sha256).join('\n')), files};
}
function withoutFences(text) {
  let fence = null;
  return text.split(/\r?\n/).map(line => {
    const match = line.match(/^ {0,3}(`{3,}|~{3,})/);
    if (fence) {
      if (match && match[1][0] === fence[0] && match[1].length >= fence.length &&
          line.slice(match[0].length).trim() === '') fence = null;
      return '';
    }
    if (match) { fence = match[1]; return ''; }
    return line;
  }).join('\n');
}
// Repository heading convention: ATX/Setext, Unicode letters/numbers, duplicate -N suffix.
// Not a full CommonMark/GitHub renderer (HTML headings and entities are out of scope).
export function headingAnchors(text) {
  const lines = withoutFences(text).split('\n'), used = new Set();
  for (let i = 0; i < lines.length; i++) {
    const atx = lines[i].match(/^ {0,3}#{1,6}\s+(.+?)\s*#*\s*$/);
    let title = atx?.[1];
    if (!title && i + 1 < lines.length && lines[i].trim() && /^ {0,3}(?:=+|-+)\s*$/.test(lines[i + 1])) {
      title = lines[i++].trim();
    }
    if (!title) continue;
    const base = title.replace(/\[([^\]]+)\]\([^)]*\)/g, '$1')
      .replace(/<[^>]+>/g, '').replace(/[`*_~]/g, '').toLowerCase()
      .replace(/[^\p{L}\p{N}\p{M}\s_-]/gu, '').replace(/\s/g, '-');
    let anchor = base, suffix = 0;
    while (used.has(anchor)) anchor = base + '-' + (++suffix);
    used.add(anchor);
  }
  return used;
}
export function validateLinks(doc) {
  let links = 0, anchors = 0;
  const cache = new Map();
  for (const file of filesUnder(doc).filter(p => p.endsWith('.md'))) {
    const text = withoutFences(fs.readFileSync(file, 'utf8'));
    // Inline Markdown links, optionally angle-wrapped. Reference links/HTML aren't parsed.
    for (const m of text.matchAll(/\]\((<[^>]+>|[^)\n]+)\)/g)) {
      let target = m[1].trim();
      if (target.startsWith('<')) target = target.slice(1, -1);
      if (/^[a-z][\w+.-]*:/i.test(target) || target.startsWith('//')) continue;
      const split = target.indexOf('#');
      const location = split < 0 ? target : target.slice(0, split);
      const fragment = split < 0 ? '' : decodeURIComponent(target.slice(split + 1));
      const destination = location ? path.resolve(path.dirname(file), decodeURIComponent(location)) : file;
      if (!fs.existsSync(destination)) throw Error('Broken link ' + file + ' -> ' + target);
      links++;
      if (fragment && destination.toLowerCase().endsWith('.md')) {
        if (!cache.has(destination)) cache.set(destination, headingAnchors(fs.readFileSync(destination, 'utf8')));
        if (!cache.get(destination).has(fragment)) throw Error('Broken anchor ' + file + ' -> ' + target);
        anchors++;
      }
    }
  }
  return {links, anchors};
}
