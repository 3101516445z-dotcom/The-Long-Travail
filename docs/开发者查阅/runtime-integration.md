# 运行时接入

## 注册与网络匹配

物品注册由 [ItemRegistrationConfig](../../src/main/java/com/thelongtravail/config/ItemRegistrationConfig.java) 在启动时确定。配置关闭的物品没有注册实例；读取对应 `RegistryObject` 前先检查 `isPresent()`。配方可使用 Forge 条件检查物品是否存在。

[TravailNetwork](../../src/main/java/com/thelongtravail/network/TravailNetwork.java) 的协议标识为 `24-items-` 加物品注册签名，两端精确匹配。变更注册配置后须重启客户端和服务端。关闭注册会影响旧存档中对应的物品数据。

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
