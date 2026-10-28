import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { root, readInputs, snapshot, compare, validate, inspect } from './check.mjs';

const inputs = readInputs();
const catalog = JSON.parse(fs.readFileSync(path.join(root, 'tools/damage-contracts/catalog.json'), 'utf8'));

test('当前目录与基线一致', () => {
    const result = inspect();
    assert.deepEqual(result.errors, []);
    assert.deepEqual(result.changed, []);
});
test('新增 hurt 入口必须分类', () => {
    const changed = { ...inputs, 'src/main/java/NewProc.java': 'target.hurt(source, amount);' };
    assert.ok(validate(catalog, changed).some(error => error.includes('未分类')));
    assert.deepEqual(compare(snapshot(inputs), snapshot(changed)), ['src/main/java/NewProc.java']);
});
test('移除同步链保护而不增加 hurt 也会要求复查', () => {
    const file = 'src/main/java/com/thelongtravail/combat/DamageScope.java';
    const changed = { ...inputs, [file]: inputs[file].replace('chain.add(attacker)', 'true') };
    assert.notEqual(changed[file], inputs[file]);
    assert.deepEqual(compare(snapshot(inputs), snapshot(changed)), [file]);
});
test('删除入口、修改类型标签及非核心辅助类均会要求复查', () => {
    const removed = { ...inputs };
    const file = catalog.effects[0].sources[0];
    delete removed[file];
    assert.ok(validate(catalog, removed).some(error => error.includes('不存在')));
    assert.deepEqual(compare(snapshot(inputs), snapshot(removed)), [file]);
    for (const target of [
        'src/main/resources/data/the_long_travail/tags/damage_type/retaliation.json',
        'src/main/java/com/thelongtravail/common/AttributeModifierSet.java'
    ]) {
        assert.ok(target in inputs);
        assert.deepEqual(compare(snapshot(inputs), snapshot({ ...inputs, [target]: inputs[target] + '\n// changed' })), [target]);
    }
});
test('错误后继、重复 ID、失效源码引用不能通过', () => {
    for (const mutate of [
        copy => copy.effects.push(copy.effects[0]),
        copy => copy.effects[0].successors.push('missing_effect'),
        copy => copy.effects[0].sources.push('missing.java'),
        copy => { copy.effects[0].guard = ''; }
    ]) {
        const changed = structuredClone(catalog);
        mutate(changed);
        assert.ok(validate(changed, inputs).length > 0);
    }
});
test('仅 CRLF 与 LF 转换不导致源码变化', () => {
    assert.deepEqual(snapshot({ a: 'x\r\ny\r\n' }), snapshot({ a: 'x\ny\n' }));
});
