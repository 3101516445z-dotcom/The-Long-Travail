# 可选模组联动入口

可选联动位于 `src/main/java/com/thelongtravail/compat/`，按模组分包，包含对应反射调用。Mixin 放在 `mixin/<模组名>`，与普通适配类分开，避免类加载隔离冲突。

| 子目录 | 内容 |
| --- | --- |
| `jei`、`mixin/jei` | 花饰配方与紫水晶母岩精粹来源展示、札记及拾遗目录/详情页的 JEI 渲染遮挡适配 |
| `jade` | 掉落物说明精简 |
| `mixin/embeddium` | 方块模型顶点动态光照 |
| `caelus`、`mixin/caelus` | 伊卡洛斯飞行 API 协调与 Mixin |
| `ftbteams` | 剑与灯队友判断、时停队伍快照 |
| `gravestone`、`mixin/gravestone` | 墓碑 API 契约、物品校正、加载队列与 Mixin |
| `sounds`、`mixin/sounds` | 效果通知音去重、静音策略与 Sounds Mixin |
| `maidspell` | 万法皆通生命值保护下的饰品上限移除校正 |
| `maid`、`mixin/maid` | 女仆原生饰品、只读主人苦旅资格、独立冷却、游泳适配与性格收纳牡丹快照校正 |
| `apotheosis`、`mixin/apotheosis` | 神化铁砧等级限制的字节码检查、重复注入协调及名称样式适配 |
| `attributeslib` | Apothic Attributes 百分比属性构造与缺失回退 |
| `quark`、`mixin/quark` | 日记符文及附魔光效适配 |
| `maid/client` | 女仆专用提示识别与追加文案 |
| `mixin/dummmmmmy` | 试验假人的有效伤害统计适配 |
| `fluid` | 完美流体复制精妙背包、RS 系列磁盘与 AE2 存储元件 |

## 核心与联动的边界

- Minecraft、Forge、Curios 是必需依赖，不归入此目录。MixinExtras 是随包提供的实现基础设施。
- 原版动态光源计算、原版箱子光照查询、物品机制、真实配方、复活账本和存档格式保留在核心包中。
- `EffectChanges` 负责实际药水效果的添加、移除和所有权，保留在核心；音效通知消息作为该状态流的输出保留原有网络注册，客户端音效处理及见证静音规则集中在 `sounds`。
- 核心只调用 compat 中使用 Minecraft/Java 参数的适配入口；第三方类、类名和 API 签名留在这里。
- `IcarusCaelusCompat` 先检测 Caelus，再加载内部 API 调用类，不能把可选 API 引用放进核心初始化。
- FTB Teams 查询失败时，剑与灯保守排除关系不明的玩家；时停取消本次释放，不使用不完整的队友名单创建区域，也不产生成功释放后的冷却。查询故障会限频记录，后续调用仍可恢复。
- 墓碑账本数据仍属于核心存档；`UnderworldStorage` 保留委托入口，墓碑具体操作位于 `GravestoneStorage`。
- JEI/Jade 由各自插件发现机制加载。不要从核心直接实例化插件类。
- 神化专用检查位于 `apotheosis/AnvilCompatPlugin.java`；苦旅自身的铁砧等级限制、费用封顶和免费取出逻辑仍属于核心 Mixin，未安装神化时同样需要。

## 注册与构建入口

- 核心 Mixin：`src/main/resources/mixins.the_long_travail.json`。
- 可选 Mixin：`src/main/resources/mixins.the_long_travail.compat.json`，包根为 `com.thelongtravail.compat.mixin`。
- 可选目标的字节码检查：`compat/OptionalCompatPlugin.java`，缺失目标时跳过；Gravestone、Caelus 的契约不匹配仍会抛出异常并阻止启动。神化铁砧的契约检查由下述独立插件负责，不匹配时同样抛出异常。
- 核心铁砧 Mixin 的神化协调入口：`apotheosis/AnvilCompatPlugin.java`，由核心 Mixin 配置注册；确认神化已提供无限等级阈值时，仅跳过重复的等级限制注入，保留费用封顶和免费取出。
- 可选编译依赖：`gradle/optional-compat.gradle`，由主 `build.gradle` 引入，均为 `compileOnly`。
- 两份 Mixin 配置均由 Gradle 开发运行与成品 JAR Manifest 注册，移动类时必须同步配置和 refmap。

## 女仆饰品

使用 `compat/maid/MaidAccessories` 作为不含女仆 API 参数的安全入口；白名单以 `MaidAccessories.items()` 为准，由插件注册和运行时共同使用。新增物品不会自动兼容，需先审查效果，再接入资格、事件和生命周期维护。只读取女仆原生饰品栏，不把手持、普通背包或 Curios 栏当作生效位置。

主人通过真实 UUID 查询当前在线服务端玩家；主人主题状态只读，不能复制苦旅、写入旅程或把玩家本体惩罚套到女仆。分支资格在实际事件中复核；女仆自己的属性、生命值、位置和冷却不能替换成主人的值。失去主人或分支资格时只撤除相应分支，保留不依赖苦旅的基础效果。

卸装、换主人、离开世界时清理短期状态；需要跨照片/胶片恢复的冷却使用现有兼容身份及账本，不能改写女仆原生 UUID 或借卸装重置。新增生命上限修饰符时，同时审查 `MaidPersonalityStorage` 的收纳快照及 `maidspell` 的生命保护适配：只校正可确认属于本模组的来源，不猜测清除旧存档中的未知补偿，也不能把保护写入 API 当作正常伤害/治疗的捷径。

女仆专用提示识别与追加位于 `compat/maid/client/MaidAccessoryTooltips`；默认复制限制名单由 `compat/maid/MaidFluidDefaults` 提供，配置加载、保存和一次性初始化标记仍由 `config/PerfectFluidCompatDefaults` 管理。

### 槽位增量同步

本模组原生女仆饰品统一由 `MaidBaubleSync` 维护展示状态。只比较本模组饰品占用过或正在占用的槽位；每刻末尾发送变化槽位的当前真实物品副本，移动同时更新旧槽与新槽，替换时不得以空栈覆盖新物品。禁止把筛选后的物品列表传给 `fullSync`：女仆客户端会先清空整个饰品栏。首次追踪补发本模组当前槽位的增量信息，其他槽位继续由原生流程管理。未变化时不发包，服务端冷却不为展示而写入物品。

第六天魔王、万魂幡在原生女仆槽位入口检查主人苦旅资格与同种唯一性，运行时再次校验。主人离线或卸下苦旅后保留物品与魂数，清除对应属性并停止收魂。普通背包、手持、女仆 Curios均不生效。玩家、女仆各自的魂数写入自己的实物，不将女仆击杀再转给主人。

## 试验假人

`compat/mixin/dummmmmmy/DummyDamageMixin` 仅在目标累计有效伤害后调用 `CombatContext.recordDummyDamage`；不修改对方血量、统计、伤害公式或返回值。必须有匹配目标和 DamageSource 的当前伤害上下文，不能以适配为名重新施加伤害或改变 HEALTH_LOSS 语义。

契约检查应覆盖目标类、完整方法签名及唯一注入位置。目标缺失时跳过，接口不匹配时警告并停用，不能退回粗略命中判定。假人发行 JAR 中的 SRG 字符串没有 refmap；在开发映射环境验证时，需要单独准备匹配当前映射的依赖副本，不能直接据此修改玩家安装的原 JAR。普通源码构建不要求加载假人模组。

## 外部库存复制

通用 ItemStack/NBT 副本不能自动复制由 UUID 引用的外部库存。`compat/fluid/FluidCopyGraph` 统一管理精妙背包、RS 物品/流体盘、RS Integration 共振存储盘及万界归墟盘。每次复制使用独立的“库存系统 + 原 UUID → 新 UUID”映射，保留复制品内部的重复引用/循环关系；通过队列处理外部库存，避免交替嵌套递归溢出。

预览仅处理物品数据、预留身份，不读写外部库存。原版背包合成和工作台在领取/quickMoveStack 交付前准备并提交副本；失败（含 Shift 无法移入背包）时回滚新增库存，并尝试恢复材料、菜单和玩家物品栏；单项恢复失败会记录到原异常并继续其余恢复，不保证第三方存储异常时全部恢复成功。每次连续合成使用独立上下文，不依赖领取后的 ItemCraftedEvent，不关闭精妙背包 UUID 去重。

RS 用整数 Quantity 保存条目数量；复制物品身份时以 Count=1 序列化，再恢复真实数量。RS 标准盘通过原库存的工厂重建，共振盘保留能力数据；万界归墟盘单独重建 UnifiedDiskCore、文件记录及 RS 代理，不能只复制其引用 NBT。缺失库存、未知 RS 库存实现、统一盘错误世界/格式、隔离条目、备份进行中、容量截断或写入失败均拒绝复制，不降级为共享或空库存。

净化遍历复制品的内嵌数据及所有受支持外部库存，清除各层《苦旅》、完美流体及当前配置黑名单中的物品，原库存不变。判断入口是 `abyss/PerfectFluidCopies.isProtected`，不能只按最外层物品或只按完美流体 ID 过滤。自动合成及其他未接入领取事务的菜单不接受含已初始化外部库存引用的配方，普通物品复制不受此限制。

保护边界：每次最多 4096 个独立外部库存，NBT 遍历深度最多 128、节点最多 4000000；外部库存链深度不累加到 NBT 深度。超限整次拒绝，不截断复制。回滚覆盖本次新增库存及菜单物品，不宣称进程崩溃/断电下跨全部存档文件的原子提交，也不自动修复此前已经共享 UUID 的磁盘。

AE2 存储适配：`FluidAeStore` 识别标准及便携式物品/流体元件，按 AEKey 与 long 数量处理内嵌库存。物品样本以 Count=1 接入同一复制图，数量单独保留；净化后相同 key 合并，严格检查数量溢出、字段与总量，并通过 AE2 读取接口复核。分区过滤不作为实物库存遍历，名称、电量和升级保留。无需 AE2 编译/运行硬依赖，未安装时不加载 AE2 类。

AE2 元件统一要求原版背包合成或工作台领取事务；AE2 合成终端与自动合成暂不支持该复制。已绑定空间区域的元件（包括嵌套）拒绝复制；创造元件若配置了完美流体、含完美流体容器或受保护库存引用，拒绝复制。未知元件实现不宣称兼容。
