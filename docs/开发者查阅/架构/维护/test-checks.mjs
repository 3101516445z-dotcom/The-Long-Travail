import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import {spawnSync} from 'node:child_process';
import {fileURLToPath} from 'node:url';
import {headingAnchors, inputSnapshot, validateLinks} from './checks.mjs';
const here = path.dirname(fileURLToPath(import.meta.url));
const root = fs.mkdtempSync(path.join(os.tmpdir(), 'tlt-architecture-'));
const doc = path.join(root, 'docs/开发者查阅/架构');
let passed = 0;
function test(name, fn) {fn(); passed++; console.log('PASS ' + name);}
function put(relative, text) {
  const p = path.join(root, relative); fs.mkdirSync(path.dirname(p), {recursive:true}); fs.writeFileSync(p, text);
}
function run(check = false) {
  const result = spawnSync(process.execPath, [path.join(doc, '维护/generate.mjs'), ...(check ? ['--check'] : [])],
    {cwd: os.tmpdir(), encoding:'utf8'});
  if (result.error) throw result.error;
  return result;
}
function ok(result) {assert.equal(result.status, 0, result.stderr);}
try {
  test('Unicode headings, punctuation, repeated headings and fenced examples', () => {
    const set = headingAnchors('# 中文 标题！\n## Repeat\n## Repeat\n~~~md\n# Fake\n~~~\nSetext\n---\n');
    assert.deepEqual([...set], ['中文-标题','repeat','repeat-1','setext']);
  });
  put('src/main/java/com/thelongtravail/Demo.java', 'package com.thelongtravail; class Demo {}');
  put('src/main/resources/demo.json', '{}'); put('src/main/lang/groups.json', '[]');
  put('build.gradle', '// fixture');
  put('docs/开发者查阅/架构/README.md', '# 中文 标题\n## Repeat\n## Repeat\n[local](#repeat-1)\n[encoded](#%E4%B8%AD%E6%96%87-%E6%A0%87%E9%A2%98)\n[external](https://example.invalid/no-network)\n');
  for (const file of ['generate.mjs','checks.mjs']) put('docs/开发者查阅/架构/维护/' + file, fs.readFileSync(path.join(here,file)));
  test('Generate and check from unrelated cwd', () => {ok(run()); ok(run(true));});
  const javaBefore = JSON.parse(fs.readFileSync(path.join(doc,'索引/引用候选.json'))).sourceFingerprint;
  for (const [name, mutate] of [
    ['resource edit', () => put('src/main/resources/demo.json', '{"changed":true}')],
    ['resource addition', () => put('src/main/resources/new.json', '{}')],
    ['resource deletion', () => fs.unlinkSync(path.join(root,'src/main/resources/new.json'))],
    ['language edit', () => put('src/main/lang/groups.json', '["common"]')],
    ['build edit', () => put('build.gradle', '// changed')],
    ['build addition', () => put('gradle/extra.gradle', '// extra')],
    ['build deletion', () => fs.unlinkSync(path.join(root,'gradle/extra.gradle'))],
    ['tool addition', () => put('tools/verify-locale.cjs', '// fixture')]
  ]) test(name + ' invalidates manifest without changing Java fingerprint', () => {
    const before = inputSnapshot(root).fingerprint; mutate();
    assert.notEqual(inputSnapshot(root).fingerprint, before);
    const failed = run(true); assert.notEqual(failed.status, 0); assert.match(failed.stderr,/Index stale: 资源与构建指纹/);
    ok(run()); ok(run(true));
    assert.equal(JSON.parse(fs.readFileSync(path.join(doc,'索引/引用候选.json'))).sourceFingerprint,javaBefore);
  });
  test('Broken heading rejected even when target file exists', () => {
    put('docs/开发者查阅/架构/bad.md', '[bad](README.md#missing)');
    assert.throws(() => validateLinks(doc), /Broken anchor/);
    assert.match(run(true).stderr,/Broken anchor/);
  });
  test('Missing file rejected', () => {
    put('docs/开发者查阅/架构/bad.md', '[bad](missing.md)');
    assert.throws(() => validateLinks(doc), /Broken link/);
  });
  test('Fenced fake links ignored and angle paths supported', () => {
    put('docs/开发者查阅/架构/space name.md', '# Target');
    put('docs/开发者查阅/架构/bad.md', '```md\n[x](missing.md)\n```\n[ok](<space name.md#target>)');
    ok(run(true));
  });
  test('Java edit still invalidates original index', () => {
    put('src/main/java/com/thelongtravail/Demo.java','package com.thelongtravail; class Demo { int x; }');
    const failed = run(true); assert.notEqual(failed.status,0); assert.match(failed.stderr,/Index stale/);
    ok(run()); ok(run(true));
  });
  console.log(`PASS: ${passed} architecture-check cases.`);
} finally {
  assert.equal(path.dirname(root), os.tmpdir());
  assert(path.basename(root).startsWith('tlt-architecture-'));
  fs.rmSync(root, {recursive:true, force:true});
}
