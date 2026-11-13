# 运行时接入

## 注册与网络匹配

物品注册由 [ItemRegistrationConfig](../../src/main/java/com/thelongtravail/config/ItemRegistrationConfig.java) 在启动时确定。配置关闭的物品没有注册实例；读取对应 `RegistryObject` 前先检查 `isPresent()`。配方可使用 Forge 条件检查物品是否存在。

[TravailNetwork](../../src/main/java/com/thelongtravail/network/TravailNetwork.java) 的协议标识为 `25-items-` 加物品注册签名，两端精确匹配。变更注册配置后须重启客户端和服务端。关闭注册会影响旧存档中对应的物品数据。

附属执行消耗、奖励、旅程写入及伤害时使用逻辑服务端游戏线程；客户端读取同步状态显示。网络处理器排队进入游戏线程后，再校验发送者、当前菜单及操作身份。

## 配置加载与重载

配置位于 `config/the_long_travail/`，由 [TravailConfig](../../src/main/java/com/thelongtravail/config/TravailConfig.java) 装配 Forge Spec。通用设置位于 `general.toml`，主题设置位于 `aspects/`。字段格式见[随包配置说明](../../src/main/resources/assets/the_long_travail/config/configuration-guide.zh_cn.txt)。

`config/storage/ManagedConfig` 先读取候选配置，再校验并发布。热重载时，非法普通值沿用上一有效值或默认值，其他合法修改仍可发布；无法解析的文件保留旧活动配置。有非法输入时保留磁盘原文并阻止自动覆盖。附属监听 Forge 配置重载事件后，应重新取得有效值及相关缓存。

直接对活动文件调用 `load()` 会绕开候选校验。常规保存使用 `ManagedConfig.save()`，预设更新使用 `ManagedConfig.update(edit)`，保留对尚未处理的磁盘变更的检查。

## 联动预设写入

`update` 在候选读取和写盘前检查磁盘变化，将列表及处理标记一次保存，再更新活动配置与 Spec 缓存。`edit` 返回 true 表示需要保存。该路径不分发 Forge Reloading 事件；启动预设后的运行池刷新由后续服务端启动事件完成。

预设按来源、主题和目标池分别记录初始化。来源缺席时不标记；已处理但没有新增条目时仍标记；已有条目优先。标记随配置跨世界共用。重新补入预设时只处理对应标记，保留其他来源的用户配置。数据入口见[可选联动预设](optional-compat.md#数据预设)。

## 共享状态与生命周期

| 入口 | 调用约定 |
| --- | --- |
| `common/AttributeModifierSet` | 各来源使用独立 ID；reconcile 幂等，clear 只移除该来源。 |
| `runtime/effects/OverflowHealing` | 显式加入溢出治疗；正常 heal 返回且目标存活后才增加吸收生命，嵌套普通 heal 不继承资格。 |
| `combat/ReceivedHitGate` | 每个实例代表冷却分组，按实体计时，退出生命周期时清理。 |
| `common/ItemDelivery` | 按合法堆叠入包，剩余掉落；取消掉落实体会阻止该部分交付。 |

延迟旅程操作保存发起时的主题身份，通过 [TravailJourneyApi](state-api.md#服务端旅程写入) 校验；装备恢复通过[饰品接入](accessory-equipment.md)处理。清理只撤销本来源的属性、队列和临时状态。

队列失败项可能已产生副作用，未开始的尾部保留原顺序；已发生的死亡、奖励和消耗不得重放。实体清理按对象身份匹配，避免影响复用同一 ID 或 UUID 的新实例。

第三方自主 tick、机器传输和脚本循环可在执行前查询 [TimeStopApi](state-api.md#时停查询)，由各自实现暂停与恢复。

## 飞行能力共存

小王子通过 `boundless/LittlePrinceFlight` 维护 `mayfly`，携带查询 UNKNOWN 时仅保留已有授权。客户端按实体 ID、UUID 和维度接收权限，活跃授权每 20 游戏刻重同步。

撤权时会清除非创造、非旁观玩家的 `mayfly` 与 `flying`，字段没有按外部来源分别计数。其他飞行模组需要协调双方更新与撤权时序。伊卡洛斯见[Caelus 接入](optional-compat.md#飞行与属性)。

## 村民交易展示

槲寄生、巴别塔、堂吉诃德、磷叶石及考古物品使用 Forge `VillagerTradesEvent` 注册。展示模组可通过 `getOffer(null, null)` 枚举，每次返回独立实例。旧村民补齐按产物去重并保留使用次数，反复读取不刷新库存。

## 槲寄生赋予入口

`flourishing/Mistletoe.apply` 通过 `runtime/effects/EffectInsertion.addOrGet` 在正常 `addEffect` 后检查实际效果；目标拒绝首次赋予时直接写入活动效果表，拒绝更新时仍按本次使用计算的等级和时长更新现有实例。该兜底仅用于槲寄生物品使用，不改变其他效果的免疫规则。兜底通过 `LivingEffectsAccessor` 访问原版真实效果表，兼容目标对外返回只读或伪装视图的情况；写入后校验实际可见实例，校验失败则恢复该键且不消耗物品。

等级、时长和隐藏效果链统一在赋予后处理，随后标记效果状态更新、更新专属移速修饰并广播效果包，最后消耗物品。槲寄生不依赖 `MobEffect` 的通用属性修饰回调。`MistletoeExpiryMixin` 在实际移除回调后立即更新移速并广播移除包，避免观察客户端残留旧状态。主动清除后不自动补回；实际伤害与移动效果沿各自结算入口处理。

`MistletoeClientEffectsMixin` 在原版效果同步包调用 `forceAddEffect` 后校验槲寄生实例；客户端实体拒收时，将服务端下发的实例写入真实效果表，覆盖首次追踪和后续更新。其它效果沿用原版处理。

`PlayerEvent.StartTracking` 为新追踪玩家补发目标当前的槲寄生状态，与使用物品时的即时广播共同覆盖已有观察者和后来加入的观察者。

## 可复用的效果植入工具

`runtime/effects/EffectInsertion.addOrGet(target, incoming, source)` 是服务端显式调用入口：正常添加后读取实际实例；首次拒收时通过真实效果表写入并校验，失败恢复原键，成功补执行原版 `onEffectAdded`，使普通效果的属性修饰生效。死亡目标或客户端调用返回 `null`。实体自行覆写的效果查询、回调和后续清除仍参与执行。

返回值为目标实际持有的效果实例，失败为 `null`。已有实例优先遵循正常 `addEffect` 的合并行为；目标拒绝更新时返回既有实例，不自动覆盖等级、累计时长或消耗物品。这些业务策略由调用方决定。直接改带属性修饰效果的等级还需要正确的属性更新回调，不能只改 amplifier。

```java
var actual = EffectInsertion.addOrGet(target, incoming, source);
if (actual == null) return; // 由业务方决定失败处理
// 如有专用叠层、时长或资源消耗规则，在此处理。
EffectInsertion.synchronize(target, actual);
```

`synchronize` 刷新效果显示状态，并在服务端向当前追踪者广播原版效果包。`writeVerified` 是底层写入及回滚工具，供已有生命周期入口复用，不执行添加回调。

客户端拒收兜底、首次追踪补包及移除补包只接入槲寄生。其它效果需要这些非玩家客户端显示行为时，由业务方显式接入相应同步处理。
