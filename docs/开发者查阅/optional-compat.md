# 可选模组联动

## 加载与依赖

第三方专用实现位于 `src/main/java/com/thelongtravail/compat/`，按模组分包；注入位于 `compat/mixin/`，由 [可选 Mixin 配置](../../src/main/resources/mixins.the_long_travail.compat.json) 注册。

公共门面使用 Minecraft 或 Java 类型，确认目标模组存在后才加载第三方 API 实现。编译依赖由 [optional-compat.gradle](../../gradle/optional-compat.gradle) 及[固定 API 准备说明](../../tools/dependencies/README.md)管理；`compileOnly` 依赖需在开发运行环境另行提供。

`OptionalCompatPlugin` 检查目标类与成员布局。`OptionalCompatChecks`、`OptionalCompatState` 维护 Gravestone、Caelus 和神化铁砧的契约及停用状态，判断依据为实际签名和字节码。接口不匹配时停用相应适配并记录警告，重启后重新检查。

## 飞行与属性

`compat/caelus/IcarusCaelusCompat` 协调伊卡洛斯的 Caelus 飞行判定。检查公共静态入口、`canFallFly` 完整签名、可注入方法体和 TriState 字段。适配停用或运行时链接失败时，回到未接入 Caelus 的判定。

`compat/attributeslib/PercentAttributes` 通过隔离内部类直接构造 Apothic Attributes 的 `PercentBasedAttribute`，用于必杀和霸主的百分比显示；缺少依赖或调用失败时使用 `RangedAttribute`。实际属性范围和计算见[战斗属性](combat-attributes.md)。

## 队伍与女仆

FTB Teams 查询失败时，剑与灯排除关系不明的玩家；时停取消本次释放，也不产生成功释放后的冷却。后续查询可重新尝试。

女仆的安全门面为 `compat/maid/MaidAccessories`，支持名单由 `items()` 提供。只读取女仆原生饰品栏；阶段资格查询 UUID 对应的在线主人，但效果主体、位置、属性及冷却属于女仆自身。读取主人状态使用 [TravailStateApi](state-api.md)，不复制日记或写入主人旅程。

新增女仆饰品须同时接入注册、资格、事件与生命周期。卸装、换主人和离开世界时清理本物品状态；跨照片或胶片恢复的冷却使用现有兼容身份和账本。生命上限修饰还需协调 `MaidPersonalityStorage` 的收纳快照与 `maidspell/MaidSpellHealth` 的生命写入保护，只处理可确认归属的修饰。

`MaidBaubleSync` 在每刻末尾增量同步本模组占用过或正在占用的槽位，移动同时更新旧槽和新槽。向女仆 `fullSync` 传入筛选后的部分物品会清空其余客户端槽位，应使用对应的增量入口。

第六天魔王和万魂幡在装备时检查主人苦旅与同种唯一性；主人离线或卸下苦旅后，保留物品与魂数，清除对应属性并停止收魂。逐日与籠釣瓶的基础效果及阶段规则见[专项接入](sun-chaser-kagotsurube.md#女仆接入)。

## 墓碑恢复

`compat/gravestone` 通过 `getDeath`、`setDeath` 和 `getAllItems` 读取及校正死亡库存。停用状态同时控制两个 Mixin、加载队列、读取和账本触发扫描；停用后停止新校正并清理待处理队列，保留账本及已完成的校正。

条目类型在整批校验后才开始校正。第三方方法本体抛出的异常继续传播；已出队条目不自动重新入队。死亡存储应保留物品稳定身份，按消耗账本校正对应副本，避免按物品 ID 删除所有同类物品。

## 神化铁砧与名称

`compat/apotheosis/AnvilCompatPlugin` 检查 `apoth_removeLevelCap(int)` 是否直接返回 `Integer.MAX_VALUE`，客户端同时检查界面实现。安装神化时由神化处理等级限制；TLT 的费用封顶、免费取出和名称样式分别处理。

名称适配位于 `AffixNameStyle`，调用顺序和组件规则见[名称显示](name-coloring.md)。JEI/Jade 通过各自插件发现机制加载，核心初始化不直接实例化插件类。

## 试验假人

`compat/mixin/dummmmmmy/DummyDamageMixin` 在目标累计有效伤害后调用 `CombatContext.recordDummyDamage`，要求当前上下文的目标和 DamageSource 匹配。适配只提供实际损失确认，不再次施加伤害，也不把统计伤害记作普通生命损失。

目标类、完整方法签名及唯一注入位置不匹配时停用。开发映射环境需要与当前映射匹配的依赖副本；发行 JAR 中无 refmap 的 SRG 字符串不能直接视为开发环境方法名。

## 外部库存复制

`abyss/copy/FluidCopyGraph` 管理精妙背包、RS 物品与流体盘、RS Integration 共振盘及统一磁盘。`compat/fluid/FluidCopyGraph` 保留委托门面。每次复制维护独立的“存储系统 + 原 UUID → 新 UUID”映射，保留副本内部别名和环路；外部库存使用队列遍历。

预览只处理物品数据及预留身份。原版背包与工作台在交付前准备并提交副本，失败时回滚新库存，并尝试恢复材料、菜单和玩家库存；恢复失败会保留异常。每次连续合成独立建图。自定义菜单接入时须覆盖准备、提交和失败回滚，不能在领取后的 ItemCraftedEvent 才开始复制。

`abyss/copy/PerfectFluidCopies.isProtected` 用于净化各层苦旅、完美流体及配置黑名单物品。净化覆盖内嵌数据及已支持的外部库存。未接入交付事务的自动合成和菜单拒绝含已初始化外部库存引用的配方。

| 存储 | 数据约定 |
| --- | --- |
| RS 标准盘 | 以原库存工厂重建；物品身份用 Count=1 序列化，实际数量由整数 Quantity 保存。 |
| RSI 统一磁盘 | 按 1.5.3 接口重建 UnifiedDiskCore、文件及 RS 代理；数量使用 long，Mana、Source、Soul 各复制一次。准备和提交阶段都检查备份状态。 |
| 精妙背包 | 保留库存、升级和设置。Mob Catcher 的 capturedMobs 非空或类型异常时拒绝整个复制，含嵌套背包；预览不读取外部库存。 |
| AE2 | 标准及便携式物品/流体元件按 AEKey 和 long 数量处理。物品样本进入同一复制图，分区过滤不是库存；已绑定空间区域的元件拒绝复制。 |
| 超越维度 | FluidBeyondStore 识别 matter_compress_ball 的实物 stack_list；保持条目顺序、重复项及 long 数量，资源实现须属于适配器支持集合。 |

未知库存实现、缺失数据、容量截断、非法数量或写入失败时拒绝复制，保持原库存。每次最多 4096 个独立外部库存，NBT 深度上限 128、节点上限 4000000；超限拒绝整次操作，外部库存链深度不计入 NBT 深度。

AE2 元件需要原版背包或工作台领取事务；AE2 终端与自动合成未接入这一事务。物质球仅复制实体资源条目，不克隆超维网络或机器绑定。

## 数据预设

预设按已注册 ID 筛选，数据位于对应 `compat/<来源>/`，入口位于 `config/defaults/`。

| 入口 | 来源检测与配置 |
| --- | --- |
| BiomeCompatDefaults / StructureCompatDefaults | 世界群系或结构注册表的命名空间；探索条目按主题独立，新增权重 1。 |
| RewardCompatDefaults | 物品注册表；钓鱼和幻翼奖励分别保存权重与数量。 |
| FishingEntityCompatDefaults | 实体类型注册表；冥府 fishingEntities 使用“实体ID\|权重”。 |
| EffectCompatDefaults | 效果注册表中已注册的 BENEFICIAL 效果；穷遐和幽谷各自提供“效果ID\|最高等级\|权重”。 |
| GoldenAgeCompatDefaults | ModList 加物品/附魔注册表；食物与附魔黑名单分别记录。 |

已有同 ID 配置优先，列表和初始化标记一次保存。来源缺席不标记，零新增仍记为已处理；标记随配置跨世界共用。`youkaishomecoming` 与 `youkaisfeasts` 使用各自完整 ID 和独立标记。保存及刷新时序见[运行时接入](runtime-integration.md#联动预设写入)。
