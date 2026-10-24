# 可选模组联动入口

可选联动位于 `src/main/java/com/thelongtravail/compat/`，按模组分包，包含对应反射调用。Mixin 放在 `mixin/<模组名>`，与普通适配类分开，避免类加载隔离冲突。

| 子目录 | 内容 |
| --- | --- |
| `jei`、`mixin/jei` | 花饰配方与紫水晶母岩精粹来源展示、札记及拾遗目录/详情页的 JEI 渲染遮挡适配 |
| `jade` | 掉落物说明精简 |
| `youkaisfeasts`、`youkaishomecoming`、`arselemental` | 钓出生物预设；新生魔艺与车万女仆的生物预设分别位于 `arsnouveau`、`maid` |
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
| `terralith`、`incendium`、`alexscaves` | 按主题提供联动群系与结构预设 |
| `goetyawaken`、`townsandtowers`、`goety`、`ironsspellbooks`、`arsnouveau`、`aquamirae`、`mowziesmobs`、`traveloptics` | 按主题提供结构预设；万法皆通结构预设位于 `maidspell` |
| `l2complements`、`curseofpandora`、`celestialcore`、`apprenticecodex`、`enigmaticlegacy`、`enigmaticaddons`、`enigmaticdelicacy` | 钓鱼与幻翼物品奖励预设；Iron 奖励预设位于 `ironsspellbooks` |
| `enigmaticlegacy`、`enigmaticdelicacy`、`l2complements`、`celestialenchantments` | 黄金时代食物与附魔黑名单预设 |

## 联动群系预设

`compat/terralith/TerralithBiomeDefaults` 提供六个主题的 65 个候选 ID；`compat/incendium/IncendiumBiomeDefaults` 提供冥府 8 个；`compat/alexscaves/AlexsCavesBiomeDefaults` 提供幽谷 2 个。`config/BiomeCompatDefaults` 集中登记命名空间、标记与主题条目，在 `ServerStartingEvent` 读取世界的 `Registries.BIOME`，按实际命名空间识别联动并逐项检查注册状态，不依赖模组列表。名称采用 `@biome.<namespace>.<path>`（路径中的 `/` 保留），新增权重固定为 1，不提供额外译文。

按主题池内 ID 去重并保留已有条目。`travail.requirements.compatInitialization` 下的 `terralithBiomesAdded`、`incendiumBiomesAdded`、`alexsCavesBiomesAdded` 按联动、主题独立记录本次处理，包括零新增；无对应预设的主题不生成该标记，未检测到对应命名空间时不标记。每份配置的池与标记通过 `ManagedConfig.update` 一次保存，成功后发布配置并清理 spec 缓存；尚未处理的磁盘编辑不会被覆盖。初始化后清理探索池缓存。

标记跨存档共用，不随预设更新或玩家删除自动重置。新增联动的数据仍放入对应 `compat` 子目录，只登记有条目的主题；结构预设应使用独立标记。`gradlew.bat testBiomeCompatDefaults` 检查条目归属、去重、权重与独立初始化语义；世界注册表与文件保存需隔离 Forge 检查。

## 联动结构预设

`config/StructureCompatDefaults` 登记 12 个模组的结构预设，通过世界的 `Registries.STRUCTURE` 检测命名空间与实际 ID；不依赖模组 ID，因此 `towns_and_towers` 等命名空间与模组 ID 不一致的来源也可识别。候选数据放在各 `compat/<模组>/...StructureDefaults`，保持六个主题的归属。

主题范围、条目数量和标记名称见[配置说明](../玩家查阅/configuration-controls.md#联动结构预设)。结构标记使用 `*StructuresAdded`，独立于 `*BiomesAdded`；无对应候选的主题不定义标记。群系和结构共用 `config/ExplorationPresetEntries` 按池内 ID 合并，新结构名称为 `@structure.<namespace>.<path>`，保留路径中的 `/`，权重为 1。未选择接入的模组和原版条目不会自动补入，也不会被删除。

保存沿用 `ManagedConfig.update`，每个主题的结构池与本次处理标记同文件提交，保存成功后发布配置并清理缓存；缺席时不标记，处理后即使零新增也标记。结构抽取数量与已有旅程不变。`gradlew.bat testStructureCompatDefaults testBiomeCompatDefaults` 检查范围、独立标记、重试与合并规则。

## 联动物品奖励预设

`config/RewardCompatDefaults` 在 `ServerStartingEvent` 按 `ForgeRegistries.ITEMS` 的实际命名空间检测来源，逐项检查物品是否注册。8 个模组的 49 条候选分别位于对应 `compat` 子目录中的 `...RewardDefaults`，返回 `物品ID|权重|最小数量|最大数量`；各类分别提供独立的 `fishingItems()`、`phantomDrops()` 列表，目前内容相同，允许今后分别调整或只提供其中一类。

`RewardCompatDefaults.Preset` 分别持有两类列表，按目标主题选择条目；该类候选为空时不定义对应初始化标记。仅冥府、无垠定义奖励标记：`travail.witness.compatInitialization.<namespace>.fishingItemsAdded` 与 `phantomDropsAdded`。同池按 ID 去重，已有条目优先；保留预设权重与数量，不使用探索条目的 `@翻译键` 或固定权重 1。ROR 不在候选中，不删除已有手动条目。来源缺席不标记；处理后零新增也标记。标记重置与共享范围见[玩家配置说明](../玩家查阅/configuration-controls.md#见证奖励池与联动预设)。

通过 `ManagedConfig.update` 同次保存池与标记，写入失败不发布初始化结果；随后已有的 `ServerStartedEvent` 调用 `RuntimePools.reload`，更新按权重编译的物品池。无需更改发奖事件和队列逻辑，也不改钓出生物配置。

`TravailConfig.defaultFishingItems()`、`defaultPhantomDrops()` 分别提供原版默认值，目前各 12 项；合法已有列表不做迁移覆盖。`gradlew.bat testRewardCompatDefaults` 检查来源范围、权重数量、合并和标记规则；隔离 Forge 测试另行验证注册表、文件持久化、spec 缓存与运行时奖励表。

## 钓出生物联动预设

`config/FishingEntityCompatDefaults` 仅处理冥府的 `fishingEntities`。5 个来源的 15 条候选位于对应 `compat` 子目录中的 `...FishingDefaults.entities()`，格式为 `实体ID|权重`；亚小妹不在自动候选中，已有手动配置不删除。

在 `ServerStartingEvent` 使用 `ForgeRegistries.ENTITY_TYPES` 检测来源命名空间并逐项检查 ID。`travail.witness.compatInitialization.<namespace>.fishingEntitiesAdded` 按来源独立记录；与奖励、探索标记分开，来源缺席不标记，零新增仍记录。合并使用 `WeightedPresetEntries` 保留已有同 ID 权重和顺序，保存沿用 `ManagedConfig.update`；已有的 `ServerStartedEvent` 随后通过 `RuntimePools.reload` 更新运行时加权表。

`TravailConfig.defaultFishingEntities()` 单独提供 9 项原版默认配置，不经过联动注入，也没有初始化标记。候选默认值、触发概率和实际实体生成逻辑分别管理，本次只调整默认池和联动候选。

`gradlew.bat testFishingEntityCompatDefaults testRewardCompatDefaults` 验证实体名单、权重、独立标记及共用合并方法的奖励回归。隔离注册表测试验证文件持久化、缓存与运行时实体表；真实第三方生物行为仍需整合包验收。

## 增益效果联动预设

`config/EffectCompatDefaults` 在 `ServerStartingEvent` 按 `ForgeRegistries.MOB_EFFECTS` 的命名空间识别来源，仅补入已注册且分类为 `BENEFICIAL` 的预设效果。来源名单包括 12 个命名空间，每池 36 项；AD、`esoteric_spell`、`projectile_damage`、`gtbcs_geomancy_plus` 不在名单中。

数据位于对应 `compat` 子目录的效果预设类；`farReach()` 与 `deepValley()` 分别定义 `效果ID|最高等级|权重` 列表，不互相转调。新增来源时登记 `EffectCompatDefaults.PRESETS`，按主题提供候选，空列表表示该主题不参与。原版默认值由 `TravailConfig.defaultFarReachEffects()` 与 `defaultDeepValleyEffects()` 分别维护。

每个主题文件独立保存 `travail.witness.compatInitialization.<namespace>.positiveEffectsAdded`。使用 `WeightedPresetEntries` 按 ID 保留手动配置，由 `ManagedConfig.update` 同次保存列表和标记；来源缺席不标记，存在但零新增也标记。随后 `ServerStartedEvent` 的既有 `RuntimePools.reload` 更新运行时表。两个抽取池独立；既有穷遐等级加成上限仍按两池同 ID 的较高等级合并。

`gradlew.bat testEffectCompatDefaults` 验证候选范围、手动优先、一次性初始化、筛选及主题数据独立性；隔离 Forge 检查另行验证实际注册表分类、持久化和运行时池。

## 黄金时代联动黑名单

`config/GoldenAgeCompatDefaults` 在 `ServerStartingEvent` 按 `ModList` 检测安装状态，分别通过物品和附魔注册表检查候选 ID。四个来源的数据位于 `compat/enigmaticlegacy`、`enigmaticdelicacy`、`l2complements`、`celestialenchantments` 的 `...GoldenAgeDefaults` 类。

新增来源时在 `PRESETS` 登记模组 ID、目标 `Pool` 和候选列表；同一来源可分别提供食物与附魔黑名单，两类标记独立。基础默认列表为空，联动补充不写入原版默认值。列表与 `items.golden_age.compatInitialization.<modId>.<foodBlacklistAdded|enchantmentBlacklistAdded>` 通过 `ManagedConfig.update` 同次保存；保留手动值，来源存在时零新增也记录，来源缺席保持待处理。

沿用 `GoldenAgePools` 的黑名单筛选与按列表内容校验的缓存，不修改注册表或其他获取途径。`gradlew.bat testGoldenAgeCompatDefaults` 检查名单、独立标记、注册表分流、手动优先及一次性补充。

## 妖怪们的归家与飨宴

`youkaishomecoming` 与 `youkaisfeasts` 是独立注册命名空间。归家预设位于 `compat/youkaishomecoming`，分别提供 5 项钓出生物与穷遐、幽谷各 5 项增益效果；三份列表独立维护，沿用对应飨宴预设的权重与等级上限。

归家候选已按上游 [实体注册](https://github.com/Minecraft-LightLand/Youkai-Homecoming/blob/main/src/main/java/dev/xkmc/youkaishomecoming/init/registrate/YHEntities.java) 与 [效果注册](https://github.com/Minecraft-LightLand/Youkai-Homecoming/blob/main/src/main/java/dev/xkmc/youkaishomecoming/init/registrate/YHEffects.java) 核对。按完整 ID 去重，来源初始化标记分别保存在各命名空间下，不将两个来源互相映射或迁移；只注册其中一个时不会填入另一个的 ID。后续差异应修改各自的显式列表。

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
- 可选目标的字节码检查：`compat/OptionalCompatPlugin.java`；Gravestone、Caelus 和神化铁砧共用 `compat/OptionalCompatChecks` 契约检查与 `compat/OptionalCompatState` 状态。未安装时跳过，接口不匹配或字节码不可读时警告并停用对应适配，单项适配每次启动最多警告一次。
- 核心铁砧 Mixin 的神化协调入口：`apotheosis/AnvilCompatPlugin.java`，由核心 Mixin 配置注册；未安装神化时保留苦旅的等级限制注入；安装后检查服务端实现，客户端另检查界面实现。确认支持时跳过重复注入；不支持时同样跳过这两处阈值注入，交由神化处理等级限制。费用封顶、免费取出与名称样式保持独立。
- 可选编译依赖：`gradle/optional-compat.gradle`，由主 `build.gradle` 引入，均为 `compileOnly`。
- 两份 Mixin 配置均由 Gradle 开发运行与成品 JAR Manifest 注册，移动类时必须同步配置和 refmap。

### 兼容判断依据

运行时按实际接口契约判断，不使用模组版本号白名单。`gradle/optional-compat.gradle` 中固定的版本用于编译；测试记录中的版本只标明受测 JAR。

| 适配 | 检查内容 |
| --- | --- |
| Gravestone | `getDeath`、`setDeath`、`getAllItems` 的完整签名及公共实例方法资格；`setDeath` 须有可注入的方法体。 |
| Caelus | `getInstance` 的公共静态入口、API 与实现类的 `canFallFly` 签名，以及 `TriState.DEFAULT/ALLOW/DENY` 字段；实现方法须可注入。 |
| Apotheosis 铁砧 | `apoth_removeLevelCap(int)` 的字节码是否直接返回 `Integer.MAX_VALUE`。客户端同时核对界面实现。 |

墓碑和 Caelus 的注入选择器使用完整方法描述符，避免同名重载误匹配。神化的检查只接受当前已识别的字节码形式；行为等价但结构不同的实现也可能停用协调，需要核对后扩展契约检查。

### 停用后的运行时入口

Gravestone 的两个 Mixin、加载队列、物品读取及账本触发的索引扫描共用停用状态。停用后不再开始新的校正，加载队列在处理入口发现停用时清理（条目类型不符时立即清理），不删除墓碑账本，也不撤销已完成的库存校正；恢复兼容版本并重启后重新检查。运行时发现反射签名、访问权限或返回条目类型不符时也停用，条目类型先整批验证，再开始物品校正。第三方方法本体抛出的异常会向上传播，不属于签名不兼容的停用分支；加载条目在处理前已出队，没有自动重新入队保证。扫描触发及保存边界见 [墓碑校正与账本生命周期](架构/链路/墓碑校正与账本生命周期.md)。

Caelus 的 Mixin 回调与 `IcarusCaelusCompat` 共用状态。停用时不再调用可选 API，伊卡洛斯沿用没有 Caelus 适配时的判定；运行时链接错误也会关闭此适配。普通玩法异常和严重虚拟机错误不作为接口不匹配吞掉。

`./gradlew.bat testOptionalCompatibility` 运行状态及字节码契约回归，常规 `check` 也包含该任务；可用 `-PcompatTestJars="路径1;路径2"` 追加实际 Gravestone、Caelus 或 Apotheosis JAR 的契约检查。

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

超越维度 1.20.1 / 0.7.30 的压缩物质球由 `FluidBeyondStore` 处理，仅识别 `beyonddimensions:matter_compress_ball` 的实物 `stack_list`。物品资源样本以数量 1 接入同一复制图，真实数量独立保留为 long；条目顺序和重复条目不合并，不对不同资源求总量。支持物品、流体、FE，以及已安装并注册的 Botania Mana、Ars Source 和 Mekanism 四种化学资源实现；未知实现、非法数量、不能无损回读的数据整次拒绝。当前资源键格式以及带外层整数数量的旧 `Type/Stack` 物品格式可处理，其他旧格式保守拒绝。只规范化副本，原件不变。

物质球统一使用原版背包/工作台领取事务，预览不创建外部库存；嵌套禁用物品净化、背包生物拒绝及回滚沿用现有规则。此适配不克隆超维网络，不修改 NetId、绑定回调或机器过滤配置。扩展资源的代码支持与实际运行验证范围须分别记录。


精妙背包的 Mob Catcher 生物存储属于硬性联动限制，不写入配置黑名单。领取前读取背包外部库存，`capturedMobs` 非空或字段类型异常时拒绝整次复制，包括受支持容器中嵌套的此类背包；原件、材料和实体数据不变。不含该字段的旧版背包、空列表及仅安装捕捉升级但未捕捉生物的背包保持原有行为。预览仍不读取外部库存，可能显示结果，但服务端领取时会拒绝。

通用 ItemStack/NBT 副本不能自动复制由 UUID 引用的外部库存。`compat/fluid/FluidCopyGraph` 统一管理精妙背包、RS 物品/流体盘、RS Integration 共振存储盘及万界归墟盘。每次复制使用独立的“库存系统 + 原 UUID → 新 UUID”映射，保留复制品内部的重复引用/循环关系；通过队列处理外部库存，避免交替嵌套递归溢出。

预览仅处理物品数据、预留身份，不读写外部库存。原版背包合成和工作台在领取/quickMoveStack 交付前准备并提交副本；失败（含 Shift 无法移入背包）时回滚新增库存，并尝试恢复材料、菜单和玩家物品栏；单项恢复失败会记录到原异常并继续其余恢复，不保证第三方存储异常时全部恢复成功。每次连续合成使用独立上下文，不依赖领取后的 ItemCraftedEvent，不关闭精妙背包 UUID 去重。

RS 用整数 Quantity 保存条目数量；复制物品身份时以 Count=1 序列化，再恢复真实数量。RS 标准盘通过原库存的工厂重建，共振盘保留能力数据；万界归墟盘单独重建 UnifiedDiskCore、文件记录及 RS 代理，不能只复制其引用 NBT。缺失库存、未知 RS 库存实现、统一盘错误世界/格式、隔离条目、备份进行中、容量截断或写入失败均拒绝复制，不降级为共享或空库存。

净化遍历复制品的内嵌数据及所有受支持外部库存，清除各层《苦旅》、完美流体及当前配置黑名单中的物品，原库存不变。判断入口是 `abyss/PerfectFluidCopies.isProtected`，不能只按最外层物品或只按完美流体 ID 过滤。自动合成及其他未接入领取事务的菜单不接受含已初始化外部库存引用的配方，普通物品复制不受此限制。

保护边界：每次最多 4096 个独立外部库存，NBT 遍历深度最多 128、节点最多 4000000；外部库存链深度不累加到 NBT 深度。超限整次拒绝，不截断复制。回滚覆盖本次新增库存及菜单物品，不宣称进程崩溃/断电下跨全部存档文件的原子提交，也不自动修复此前已经共享 UUID 的磁盘。

AE2 存储适配：`FluidAeStore` 识别标准及便携式物品/流体元件，按 AEKey 与 long 数量处理内嵌库存。物品样本以 Count=1 接入同一复制图，数量单独保留；净化后相同 key 合并，严格检查数量溢出、字段与总量，并通过 AE2 读取接口复核。分区过滤不作为实物库存遍历，名称、电量和升级保留。无需 AE2 编译/运行硬依赖，未安装时不加载 AE2 类。

AE2 元件统一要求原版背包合成或工作台领取事务；AE2 合成终端与自动合成暂不支持该复制。已绑定空间区域的元件（包括嵌套）拒绝复制；创造元件若配置了完美流体、含完美流体容器或受保护库存引用，拒绝复制。未知元件实现不宣称兼容。
