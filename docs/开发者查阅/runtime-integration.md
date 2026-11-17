# 运行时接入

## 注册与网络匹配

物品注册由 [ItemRegistrationConfig](../../src/main/java/com/thelongtravail/config/ItemRegistrationConfig.java) 在启动时确定。配置关闭的物品没有注册实例；读取对应 `RegistryObject` 前先检查 `isPresent()`。配方可使用 Forge 条件检查物品是否存在。

[TravailNetwork](../../src/main/java/com/thelongtravail/network/TravailNetwork.java) 的协议标识为 `27-items-` 加物品注册签名，两端精确匹配。变更注册配置后须重启客户端和服务端。关闭注册会影响旧存档中对应的物品数据。

执行消耗、奖励、旅程写入及伤害时使用逻辑服务端游戏线程；客户端读取同步状态显示。网络处理器排队进入游戏线程后，再校验发送者、当前菜单及操作身份。

## 配置加载与重载

配置位于 `config/the_long_travail/`，由 [TravailConfig](../../src/main/java/com/thelongtravail/config/TravailConfig.java) 装配 Forge Spec。通用设置位于 `general.toml`，主题设置位于 `aspects/`。字段格式见[随包配置说明](../../src/main/resources/assets/the_long_travail/config/configuration-guide.zh_cn.txt)。

`config/storage/ManagedConfig` 先读取候选配置，再校验并发布。热重载时，非法普通值沿用上一有效值或默认值，其他合法修改仍可发布；无法解析的文件保留旧活动配置。有非法输入时保留磁盘原文并阻止自动覆盖。监听 Forge 配置重载事件后，应重新取得有效值及相关缓存。

配置文件的磁盘内容与当前活动值可能不同；读取运行值时，以已完成校验的 Forge Spec 为准。

## 联动预设写入

主题文件中的 `presets` 与 `travail`、`items` 同级，集中保存联动预设处理记录。普通效果参数及目标列表仍位于各自原来的位置。

| 旧记录前缀 | 当前记录前缀 |
| --- | --- |
| `travail.requirements.compatInitialization` | `presets.exploration` |
| `travail.witness.compatInitialization` | `presets.witness` |
| `items.golden_age.compatInitialization` | `presets.golden_age` |
| `items.perfect_fluid.compatInitialization` | `presets.perfect_fluid` |

前缀后的模组分组和字段名保持不变。[ConfigPresetMigration](../../src/main/java/com/thelongtravail/config/storage/ConfigPresetMigration.java) 在配置校验前迁移旧记录，仅处理当前 Spec 定义的预设组。缺少的新记录沿用旧值；新旧位置同时有值时采用新位置，包括显式设置的 `false`。迁移保留已处理状态，不触发已完成预设重新补入。通过当前路径读写预设记录。

预设按来源、主题和目标池分别记录初始化。来源缺席时不标记；已处理但没有新增条目时仍标记；已有条目优先。标记随配置跨世界共用。重新补入预设时只处理对应标记，保留其他来源的用户配置。

## 延迟操作与生命周期

延迟旅程写入保存发起时的主题身份，通过 [TravailJourneyApi](state-api.md#服务端旅程写入) 校验；装备恢复使用[饰品接入](accessory-equipment.md)提供的资格检查和返还入口。

第三方自主 tick、机器传输和脚本循环可在执行前查询 [TimeStopApi](state-api.md#时停查询)，由调用方实现暂停与恢复。

## 可复用的效果植入工具

`runtime/effects/EffectInsertion.addOrGet(target, incoming, source)` 是服务端显式调用入口：正常添加后读取实际实例；首次拒收时通过真实效果表写入并校验，失败恢复原键，成功补执行原版 `onEffectAdded`，使普通效果的属性修饰生效。死亡目标或客户端调用返回 `null`。实体自行覆写的效果查询、回调和后续清除仍参与执行。

返回值为目标实际持有的效果实例，失败为 `null`。已有实例优先遵循正常 `addEffect` 的合并行为；目标拒绝更新时返回既有实例，不自动覆盖等级、累计时长或消耗物品。这些业务策略由调用方决定。直接改带属性修饰效果的等级还需要正确的属性更新回调，不能只改 amplifier。

```java
var actual = EffectInsertion.addOrGet(target, incoming, source, inserted -> {
    // 专用叠层、时长与同步放在此处，异常时一并恢复。
    EffectInsertion.synchronize(target, inserted);
});
if (actual == null) return; // 由业务方决定失败处理
// 成功返回后再消耗物品或其他资源。
```

四参数重载将正常添加、强制写入、添加回调和调用方的实例调整／同步纳入同一失败恢复范围。发生 `RuntimeException` 或未取得实际效果时，恢复本次效果的原实例、标准详情与隐藏链，以及原有属性的基值、永久和瞬时修饰符、生命值、吸收值和本效果的追踪标记；回调首次实例化的属性恢复为实体默认值。成功时不恢复状态，保留原版合并规则。三参数重载仍可用，但返回后的业务操作不在其恢复范围内。

恢复直接操作状态，不重试效果的添加、更新或移除回调。完成恢复后补发原状态的效果更新或移除包，并限频记录异常；恢复本身失败时保留异常链向上传播，不返回普通失败。严重 `Error` 不会被吞掉。旧隐藏链有循环或超过已有的 128 节上限时，在添加前拒绝操作。

恢复范围限于本次效果和上述标准实体状态。世界修改、掉落物、其他实体／效果、能力数据、自定义效果字段及动态渲染因子等状态由回调所属实现管理；回调只执行一次。

`synchronize` 刷新效果显示状态，并在服务端向当前追踪者广播原版效果包。

调用方需要客户端效果写入及后加入追踪者补包时，应显式接入相应同步处理；服务端写入成功与客户端同步分别处理。

效果须具有有效注册 ID 才能同步；未注册项跳过并限频记录，其他合法项继续同步。跳过同步不会删除目标上的实际效果。
