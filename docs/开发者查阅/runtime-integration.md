# 运行时接入与公共模块

以下为源码内部协作入口，非稳定外部 API；修改时须核对方法签名、Mixin 和映射。

## 伤害上下文

`helper/CombatContext.java` 为每次受伤调用维护独立上下文。实际伤害确认包含普通生命损失和伤害吸收量损失，同次调用只确认一次；攻击尝试、事件未取消或 `hurt` 返回 `true` 均不能单独证明实际伤害。受击惩罚的 `HEALTH_LOSS` 仍只计算普通生命损失。

新增伤害后效果应复用现有确认与收尾节点，保留来源、自伤、反伤、处决、重放、嵌套和异常退出限制。伤害归属以实际 `DamageSource` 为准，不能自动将无来源伤害或伙伴攻击归给玩家。三项战斗属性的结算顺序及例外见[战斗属性](combat-attributes.md#结算约定)。

仅记录伤害统计、不扣原版生命值的实体需要专用确认适配，现有接入见[试验假人联动](optional-compat.md#试验假人)。

## 公共模块选择

以下路径相对 `src/main/java/com/thelongtravail/`；通用模块不应混入物品专用分支。

| 模块 | 接入时需要保留的契约 |
| --- | --- |
| `common/AttributeModifierSet` | 按 ResourceLocation 来源管理临时属性；重复 reconcile 幂等，clear 只移除本来源，不给实体补造缺失属性。不同来源不可共用标识。 |
| `common/OverflowHealing` | 显式选择加入溢出治疗；遵守正常 heal 及 Forge 事件，只在正常返回且目标仍存活时增加吸收。嵌套普通 heal 不继承外层资格，需要配套 Mixin。 |
| `common/ReceivedHitGate` | 一个实例代表一个冷却分组，按实体独立计时；调用者提供触发条件、排除和时长，并负责退出生命周期时清理。 |
| `common/TagItemSampler` | 查询实时标签，按物品 ID 取样；额外 ID 与标签去重，不应改写全局标签来实现单个物品的候选池。 |
| `common/ItemDelivery` | 拆分合法堆叠、插入背包、剩余掉落；外部取消掉落实体时不能据此保证投递成功或持久化。 |

通用属性和治疗模块不应引用主题状态、具体物品或女仆 API。战利品抽样与注入入口集中见[战利品接入](loot-integration.md#抽样与注入)。借用它们时也需要保留对应字段桥接、Mixin 注册和生产映射，不能只复制一个 Java 文件。

## 终结与实体生命周期

入口见 `valley/AzraelExecution`、`FinalExecution`、`ExecutionWatch` 及相关原版类桥接。它们属于内部生命周期实现，不作为外部稳定处决 API。新增管理员命令或内部调用前需核对现有玩法豁免和指令权限的区别。

- 清理旧实例必须按对象身份匹配；复用相同 ID 或 UUID 的新实例不是原目标。玩家继续走正常死亡、PlayerList.respawn 和状态复制路径，不能套用非玩家底层清理。
- 强制终结可能绕过可取消死亡入口、自定义 die 或部分第三方事件。成功脱离服务器生命周期不等于完成 Boss 状态、任务、经验、成就或专属奖励结算。
- 清理重试不能重复执行死亡、发奖或已发生副作用的回调。补偿只是一套受限的物品条目识别规则，见 [战利品接入](loot-integration.md)。
- 主动观察有期限；实体实例的被动终结标记与主动轮询是不同层次。到期不能因为保存或再次 track 无条件重启监测，也不能宣称会取消第三方保活线程或永久禁止新实例生成。
- 原版实体存储操作仍要求游戏线程；volatile 或复制迭代集合不能保证任意第三方异步修改安全。修改网络身份或期限字段时，须同步更新协议及客户端处理。

## 调度、重入和存档

时停只覆盖已有原版/Forge 更新入口，第三方自主 tick、全局脚本、机器传输或系统时钟逻辑应显式查询 [TimeStopApi](state-api.md#时停查询)，不能仅凭世界表现判断是否冻结。

恢复队列遇到异常时，失败项可能已产生副作用，不能盲目重试整个批次；保留尚未开始的尾部及原顺序，并继续传播原异常。保存重载、取消与实体卸载需要保持同一约定。

异步区块准备和安全落点搜索应取消过期请求、释放票据，并在执行回调时复核玩家当前模式、维度和请求身份。协作时间预算不能强行中断第三方单次碰撞回调。

延迟写入旅程使用 [TravailJourneyApi 的身份契约](state-api.md#服务端旅程写入)；装备恢复、前置失效和物品返还使用[饰品接入](accessory-equipment.md)。集成验证应覆盖正常、嵌套、异常退出、保存重载及卸载重进等路径；构建成功不代表这些运行时场景均已通过。

## 村民交易注册与展示

槲寄生、巴别塔、堂吉诃德和磷叶石通过 Forge `VillagerTradesEvent` 分别注册到农民、皮匠、牧羊人和石匠的 5 级交易表，新增考古物品交易入口为 `loot/ArchaeologyTrades`。交易工厂不依赖实体或随机源，允许展示模组通过 `getOffer(null, null)` 枚举双材料与产物；每次调用返回独立交易实例。

`AbstractVillager.getOffers()` 的补齐桥接继续服务旧存档及未抽中该模板的符合条件村民：按产物去重，不重置交易次数。槲寄生旧价格迁移会更新对应材料；新增考古物品交易保留已有首条并去掉重复项。固定材料价格的拦截保持不变。新增同类交易应同时检查标准模板可发现、旧交易补齐、重复读取不增项，以及已耗尽库存不刷新。

## 箭矢命中与飞行能力

洛伦兹幻想仅初始化非三叉戟 `AbstractArrow`，通过原版 `AbstractArrow.onHitEntity` 中实际的 `Entity.hurt` 调用建立爆头和见证结算边界。沿用此调用链的模组箭矢可获得对应功能；完全覆盖命中方法且不调用原版链的箭矢需要专用适配，不能仅凭继承关系承诺支持。

多部件目标使用主体取样生命值、去重及记录冷却，但追加伤害必须调用实际命中部件的伤害入口，保留部位修正与阶段保护，不能统一改走主体的默认受伤入口。

`lorentz_void` 不带弹射物标签，保留实际射手，绕过常规护甲防御及普通受伤冷却，保留吸收生命、见切、霸主、事件取消和阶段无敌。它不复用堂吉诃德跳过吸收生命的分支，不再次触发本模组必杀和攻击后追加链。

### 飞行能力共存

小王子入口：`boundless/LittlePrinceFlight.update(ServerPlayer, CarryingResult)`。确认携带时维护 `mayfly`；`UNKNOWN` 仅延续内存中的已有授权。起停由原版输入决定，不强制起飞。

授权变化、修复或强制重同步时发送 `PrinceFlightPacket`，活跃授权还会每 20 游戏刻重同步；同一游戏刻的重复更新合并发送。客户端 `PrinceFlightPermission` 校验玩家实体 ID、UUID 和维度，活跃时补回 `mayfly`，不把 `flying` 强制设为 true。

撤权时，非创造/旁观玩家的 `mayfly`、`flying` 均被清除；未区分外部授权来源，联动须验证更新和撤权时序。客户端更换玩家实例、死亡或换维度会清理授权；服务端保留重同步与生命周期清理。跨步修饰由 `LittlePrince` 独立维护。

## 运行诊断

`diagnostics/RuntimeDiagnostics` 由 JVM 参数 `-Dthelongtravail.diagnostics=true` 启用，通过 `snapshot()` 返回各指标的 `count`、`total`、`max`、`last`。`*_NS` 为纳秒，`REPLAY_WAIT_TICKS` 为游戏刻，节点、库存和队列指标按对应计数解释；此接口不自动输出停服日志。

`data/HotPathMetrics` 使用独立参数 `-Dtravail.hotpath.metrics=true`，在服务端停止时输出累计统计与峰值。两套开关互不替代，均须在启动前设置。

## 配置加载与重载

### 加载与重载流程

配置仍由 `ForgeConfigSpec` 定义并注册到 Forge。`config/ManagedConfig` 通过独立 `ConfigFileTypeHandler` 管理本模组文件，不改写其他模组的处理器。`ConfigRecovery` 先解析文件，解析失败时改用空候选；仅解析成功的 general.toml 尝试特定迁移，随后按 spec 校验、必要时备份并写回；`ConfigValidation` 校验普通配置项并转换合法数字字符串；`ConfigWatcher` 监听创建、修改和删除事件，等待保存事件短暂停顿后再读取。

文件热重载先进入独立候选配置，校验后发布新配置、清理 spec 缓存并发送 Forge 重载事件，使现有运行时缓存及客户端说明同步继续生效。运行中非法普通值沿用旧有效值或默认值，其他有效修改仍可发布，同时保留磁盘输入并禁止自动写回；无法解析的文件整体拒绝，保留旧活动配置。此流程不同于下文的预设 update 路径。不要直接对正在使用的文件配置调用 `load()`，也不要通过保存动作覆盖尚未处理的磁盘修改。

### 排版与保存

以下入口均位于 `config` 包，新增配置或调整输出时按职责维护：

| 入口 | 职责 |
| --- | --- |
| `ConfigLayout` | 稳定排列配置节；主题文件先列探索条件池，再列恶意、见证和物品设置。 |
| `ConfigTomlWriter` | 输出 TOML 排版，将预设处理记录移到末尾并按类型分组；群系、结构记录在前。 |
| `ConfigFileIO.write` | 比较输出与磁盘内容；有变化时写入临时文件，再替换目标文件。优先使用原子移动，不支持时使用普通替换。 |

同类型预设用配置键区分来源和目标池。例如 `[travail.witness.compatInitialization]` 下的 `goety.positiveEffectsAdded`，仍对应 `travail.witness.compatInitialization.goety.positiveEffectsAdded`；点号写法不改变层级，也不合并独立记录。

排版调整须保留键名、值及类型、列表顺序与重复项，以及未知字段和注释。经过读取、保存后再次保存，应得到相同文本；内容未变时不重写文件。常规保存使用 `ManagedConfig.save()`，保留其磁盘变更检查。上述未知内容保留是排版层契约；加载校验中的 `ForgeConfigSpec.correct` 仍可能删除未定义键。

### 联动预设写入

群系、结构、奖励、生物、增益效果及黄金时代黑名单预设，通过配置包内的 `ManagedConfig.update(edit)` 修改候选配置。调用方在同一次修改中补充列表并设置处理标记；`edit` 返回 `true` 表示需要保存，返回 `false` 表示没有修改。候选条目的注册与合法性筛选由各预设实现负责。

`update` 在读取候选与写盘前分别核对磁盘是否变化，保存后更新活动配置并刷新 spec 缓存，但不显式分发 Forge Reloading 事件，也不复用完整 validate 流程；运行时刷新需核对调用方或启动阶段的既有安排。不要拆成“先保存列表、再保存标记”两次写入。各类来源检测、标记与运行时池更新方式见[联动预设接入](optional-compat.md#联动群系预设)。完美流体黑名单沿用 `PerfectFluidCompatDefaults` 的启动初始化与保存流程。

### 注释与说明书维护

- **配置字段**：在对应 spec 定义处维护用途、单位和特殊规则；配置节的共用说明由 `ConfigFilesDescription.describe` 管理。
- **预设记录**：各预设定义处标明模组名称；分组标题、记录含义和说明书章节指引由 `ConfigTomlWriter` 统一输出。
- **中英文说明书**：维护资源中的 [configuration-guide.zh_cn.txt](../../src/main/resources/assets/the_long_travail/config/configuration-guide.zh_cn.txt) 与 [configuration-guide.en_us.txt](../../src/main/resources/assets/the_long_travail/config/configuration-guide.en_us.txt)。

说明书按“配置写法 → 预设 → 其他机制”排列。新增或调整章节时，中英文标题、内容和章节编号须对应，并同步配置注释中的指引。复杂机制放入说明书，字段注释保留简述与定位编号，避免逐项重复整段规则。

`ConfigFilesDescription` 读取这两份资源，`ConfigFiles.writeGuides` 在启动时生成或更新配置目录中的 `配置说明.txt` 与 `Configuration Guide.txt`。修改生成后的说明书不会更新源码，下次启动会按资源内容更新。

### 错误恢复与诊断

自动纠正已有值或恢复损坏文件前，将原始字节与处理说明保存到 `config/the_long_travail/invalid/<时间与唯一编号>/`；备份成功且原文件未被再次修改后才替换。保留主题相对路径，不自动清理备份。启动时仅恢复损坏文件；有效配置历史仅在本次运行内保留。

配置诊断统一通过 `ConfigProblems.report` 输出。`ConfigDiagnostics` 最多记住 1,024 条不同错误，按写入顺序淘汰；详细警告使用滚动 60 秒窗口，最多 20 条。去重记录不按时间清空，与配置有效值和磁盘备份独立。

限流部分从首条被省略的警告起约每分钟汇总一次，由配置监听线程在空闲时继续刷新，停服时输出剩余汇总。汇总最多列出 16 个文件，超出时提示另有文件；计数为被省略的报告次数，已淘汰错误再次出现也会计入。

新增配置优先通过既有 spec 注册；动态嵌套结构须独立校验，不能只用整组类型判断代替内部字段校验。共享战利品的逐表处理见[战利品接入](loot-integration.md#配置错误处理)。

### 检查入口

| 命令 | 检查范围 |
| --- | --- |
| `gradlew.bat testConfigLayout` | 排版读写一致性、重复保存、预设分组、未知内容保留，以及中英文说明书章节对应。 |
| `gradlew.bat testConfigRecovery testConfigDiagnostics` | 解析与文件恢复，以及诊断容量、限流和并发行为。 |
| `gradlew.bat check` | 包含上述检查，以及各类联动预设和组合检查。 |

实际配置监听、模组注册表及完整配置生成流程需在 Forge 环境验证；独立检查通过不等于完整整合包验收。
