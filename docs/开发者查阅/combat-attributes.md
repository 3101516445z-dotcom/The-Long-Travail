# 战斗属性接入

注册入口为 `registry/ModAttributes`，读取入口为 `combat/CombatAttributes`。属性通过 Forge `EntityAttributeModificationEvent` 挂载到有属性供应器的实体，并同步到客户端。

| 属性 | 注册 ID | 范围 | 含义 |
| --- | --- | --- | --- |
| 见切 | `the_long_travail:insight` | 0 至 Float.MAX_VALUE | 固定减伤与反击点数 |
| 必杀 | `the_long_travail:critical_chance` | 0 至 1 | 独立双倍伤害概率 |
| 霸主 | `the_long_travail:overlord` | 0 至 Float.MAX_VALUE | 相对于最大生命的整笔伤害免疫阈值 |

## 读取与修饰

```java
double insight = CombatAttributes.insight(entity);
double chance = CombatAttributes.criticalChance(entity);
double overlord = CombatAttributes.overlord(entity);
```

参数为非空 `LivingEntity`。读取修饰符生效后的值，缺少属性或出现非有限值时返回 0。

装备加成使用固定来源 UUID 的临时属性修饰符，卸装时移除本来源；基础值设置使用 `setBaseValue`。属性属于实体自身，宠物不会自动继承主人属性。

```mcfunction
/attribute @s the_long_travail:insight base set 5
/attribute @s the_long_travail:critical_chance base set 0.25
/attribute @s the_long_travail:overlord base set 0.1
```

## 结算约定

- 必杀在有效 Hurt 结算中抽取一次，使用独立乘数 2；不追加第二次 `hurt`。自伤不触发。
- 见切在百分比减伤之后扣除固定值。责任实体有效且不是自身时可产生反击，第三方反伤通过 `the_long_travail:retaliation` 标签参与反伤链识别。
- 霸主在吸收生命之前比较当前伤害与 `getMaxHealth() * overlord`；不超过阈值时整笔免疫，超过时不减额。最大生命不包含吸收生命。
- 霸主阈值以 double 计算，再转为与伤害一致的 float 精度比较。最终 Damage 事件修改金额后不重新判定霸主。
- 直接生命写入不属于普通伤害结算；专用来源可具有独立处理规则。

完整次序与来源身份见[伤害结算](伤害结算与追加效果.md)。
