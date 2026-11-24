# 源码索引

| 查找内容 | 源码入口 | 调用约定 |
|---|---|---|
| 模组初始化、物品注册 | [TheLongTravail](../src/main/java/com/thelongtravail/TheLongTravail.java) · [ModRegistry](../src/main/java/com/thelongtravail/registry/ModRegistry.java) | 主题物品按注册配置启用。 |
| 苦旅状态、旅程进度 | [TravailStateApi](../src/main/java/com/thelongtravail/api/TravailStateApi.java) · [TravailJourneyApi](../src/main/java/com/thelongtravail/api/TravailJourneyApi.java) | 玩家快照读取已装备的苦旅。检查单个物品栈用 `inspectDiary`。旅程写入在服务端线程执行。 |
| 携带判定、外部库存 | [CarryingApi](../src/main/java/com/thelongtravail/api/CarryingApi.java) | 服务端查询返回 `PRESENT`、`ABSENT` 或 `UNKNOWN`。外部来源检测失败且没有来源确认携带时返回 `UNKNOWN`，此时保留已有保护。 |
| 饰品佩戴、前置失效 | [AccessoryEquipRules](../src/main/java/com/thelongtravail/equipment/AccessoryEquipRules.java) · [DependentAccessories](../src/main/java/com/thelongtravail/equipment/DependentAccessories.java) | `canEquipUnique` 带声明槽位时接受该槽位和通用饰品槽，省略槽位时接受任意功能槽。自定义条件先于唯一性检查。 |
| 女仆饰品 | [MaidAccessoryRules](../src/main/java/com/thelongtravail/compat/maid/MaidAccessoryRules.java) | 查询女仆的饰品资格与主题状态。 |
| 伤害确认、诊断 | [DamageHealthLossEvent](../src/main/java/com/thelongtravail/combat/DamageHealthLossEvent.java) · [DamageTrace](../src/main/java/com/thelongtravail/diagnostics/DamageTrace.java) | 服务端实际扣除普通生命值时触发事件。仅消耗伤害吸收时不触发。 |
| 网络与旧配置升级 | [TravailNetwork](../src/main/java/com/thelongtravail/network/TravailNetwork.java) · [ConfigUpgrade](../src/main/java/com/thelongtravail/migration/config/ConfigUpgrade.java) | 分别查找消息注册和旧配置键迁移。 |
| 战利品配置 | [UnifiedLootConfig](../src/main/java/com/thelongtravail/loot/UnifiedLootConfig.java) | 按战利品表定位配置入口。 |
| 条件提示、拾遗与翻译 | [CarryingConditions](../src/main/java/com/thelongtravail/client/CarryingConditions.java) · [CatalogEntries](../src/main/java/com/thelongtravail/client/catalog/CatalogEntries.java) · [语言源](../src/main/lang/README.md) | 条件提示与拾遗共用翻译。五种语言按分组生成。 |
