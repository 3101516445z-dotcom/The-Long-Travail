# 可选模组联动入口

适用版本：The Long Travail 0.1.2，Minecraft 1.20.1。

针对可选模组的 Java 实现集中在 `src/main/java/com/thelongtravail/compat/`。按对方模组划分子包，反射调用放在对应子包中，Mixin 统一放在 `mixin/<模组名>`。Mixin 包根必须与普通适配类分开，避免 Mixin 的类加载隔离阻止适配类加载。

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
| `apotheosis` | 神化铁砧等级限制的字节码检查与重复注入协调 |
| `mixin/dummmmmmy` | 试验假人的有效伤害统计适配 |
| `PerfectFluidBackpacks` | 完美流体复制精妙背包的外部库存 |

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
- 可选目标的字节码检查：`compat/OptionalCompatPlugin.java`；保留原有缺失跳过、版本不支持拒绝加载等安全策略。
- 核心铁砧 Mixin 的神化协调入口：`apotheosis/AnvilCompatPlugin.java`，由核心 Mixin 配置注册；确认神化已提供无限等级阈值时，仅跳过重复的等级限制注入，保留费用封顶和免费取出。
- 可选编译依赖：`gradle/optional-compat.gradle`，由主 `build.gradle` 引入，均为 `compileOnly`。
- 两份 Mixin 配置均由 Gradle 开发运行与成品 JAR Manifest 注册，移动类时必须同步配置和 refmap。

## 女仆饰品

使用 `compat/maid/MaidAccessories` 作为不含女仆 API 参数的安全入口；白名单以 `MaidAccessories.items()` 为准，由插件注册和运行时共同使用。新增物品不会自动兼容，需先审查效果，再接入资格、事件和生命周期维护。只读取女仆原生饰品栏，不把手持、普通背包或 Curios 栏当作生效位置。

主人通过真实 UUID 查询当前在线服务端玩家；主人主题状态只读，不能复制苦旅、写入旅程或把玩家本体惩罚套到女仆。分支资格在实际事件中复核；女仆自己的属性、生命值、位置和冷却不能替换成主人的值。失去主人或分支资格时只撤除相应分支，保留不依赖苦旅的基础效果。

卸装、换主人、离开世界时清理短期状态；需要跨照片/胶片恢复的冷却使用现有兼容身份及账本，不能改写女仆原生 UUID 或借卸装重置。新增生命上限修饰符时，同时审查 `MaidPersonalityStorage` 的收纳快照及 `maidspell` 的生命保护适配：只校正可确认属于本模组的来源，不猜测清除旧存档中的未知补偿，也不能把保护写入 API 当作正常伤害/治疗的捷径。

### 槽位增量同步

本模组原生女仆饰品统一由 `MaidBaubleSync` 维护展示状态。只比较本模组饰品占用过或正在占用的槽位；每刻末尾发送变化槽位的当前真实物品副本，移动同时更新旧槽与新槽，替换时不得以空栈覆盖新物品。禁止把筛选后的物品列表传给 `fullSync`：女仆客户端会先清空整个饰品栏。首次追踪补发本模组当前槽位的增量信息，其他槽位继续由原生流程管理。未变化时不发包，服务端冷却不为展示而写入物品。

第六天魔王、万魂幡在原生女仆槽位入口检查主人苦旅资格与同种唯一性，运行时再次校验。主人离线或卸下苦旅后保留物品与魂数，清除对应属性并停止收魂。普通背包、手持、女仆 Curios均不生效。玩家、女仆各自的魂数写入自己的实物，不将女仆击杀再转给主人。

## 试验假人

`compat/mixin/dummmmmmy/DummyDamageMixin` 仅在目标累计有效伤害后调用 `CombatContext.recordDummyDamage`；不修改对方血量、统计、伤害公式或返回值。必须有匹配目标和 DamageSource 的当前伤害上下文，不能以适配为名重新施加伤害或改变 HEALTH_LOSS 语义。

契约检查应覆盖目标类、完整方法签名及唯一注入位置。目标缺失时跳过，接口不匹配时警告并停用，不能退回粗略命中判定。假人发行 JAR 中的 SRG 字符串没有 refmap；在开发映射环境验证时，需要单独准备匹配当前映射的依赖副本，不能直接据此修改玩家安装的原 JAR。普通源码构建不要求加载假人模组。

## 外部库存复制

通用 ItemStack/NBT 副本不能自动复制由 UUID 引用的外部库存。`compat/PerfectFluidBackpacks` 负责精妙背包的专用适配：预览只预留新身份，实际领取时复制、净化外部数据并标记 SavedData 已修改；原件的身份及库存保持不变。

匹配、预览、getRemainingItems 和失败的 Shift 领取不得提前写入外部库存。快捷领取可能先移动输出再触发合成事件，因此领取时必须使用预留身份。嵌套容器引用需要旧身份到新身份映射来处理循环；序列化空槽被省略时按 Slot 配对，不能只按列表位置对应。不要关闭对方自己的 UUID 去重机制。

现有实现依赖标准 Forge 玩家合成事件；绕过事件的自动合成机器、其他模组外部存储或非标准编码需分别适配。净化只操作复制品中的标准物品记录，不按任意字符串替换物品名称、说明或原件数据。
