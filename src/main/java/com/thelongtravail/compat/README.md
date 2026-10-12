# 可选模组联动入口

所有针对可选模组的 Java 实现集中在本目录。按对方模组划分子包，反射调用放在对应子包中，Mixin 统一放在 `mixin/<模组名>`。Mixin 包根必须与普通适配类分开，避免 Mixin 的类加载隔离阻止适配类加载。

| 子目录 | 内容 |
| --- | --- |
| `jei`、`mixin/jei` | 花饰配方与紫水晶母岩精粹来源展示、阅读界面遮挡适配 |
| `jade` | 掉落物说明精简 |
| `mixin/embeddium` | 方块模型顶点动态光照 |
| `caelus`、`mixin/caelus` | 伊卡洛斯飞行 API 协调与 Mixin |
| `ftbteams` | 剑与灯队友判断、时停队伍快照 |
| `gravestone`、`mixin/gravestone` | 墓碑 API 契约、物品校正、加载队列与 Mixin |
| `sounds`、`mixin/sounds` | 效果通知音去重、静音策略与 Sounds Mixin |
| `maidspell` | 万法皆通生命值保护下的饰品上限移除校正 |
| `maid`、`mixin/maid` | 女仆原生饰品、只读主人苦旅资格、独立冷却、游泳适配与性格收纳牡丹快照校正 |
| `apotheosis` | 神化铁砧等级限制的字节码检查与重复注入协调 |

## 核心与联动的边界

- Minecraft、Forge、Curios 是必需依赖，不归入此目录。MixinExtras 是随包提供的实现基础设施。
- 原版动态光源计算、原版箱子光照查询、物品机制、真实配方、复活账本和存档格式保留在核心包中。
- `EffectChanges` 负责实际药水效果的添加、移除和所有权，保留在核心；音效通知消息作为该状态流的输出保留原有网络注册，客户端音效处理及见证静音规则集中在 `sounds`。
- 核心只调用本目录中使用 Minecraft/Java 参数的适配入口；第三方类、类名和 API 签名留在这里。
- `IcarusCaelusCompat` 先检测 Caelus，再加载内部 API 调用类，不能把可选 API 引用放进核心初始化。
- FTB Teams 保留原有不同失败策略：剑与灯无法确认关系时保守排除玩家，时停保留已有队伍信息并记录警告。
- 墓碑账本数据仍属于核心存档；`UnderworldStorage` 保留委托入口，墓碑具体操作位于 `GravestoneStorage`。
- JEI/Jade 由各自插件发现机制加载。不要从核心直接实例化插件类。
- 神化专用检查位于 `apotheosis/AnvilCompatPlugin.java`；苦旅自身的铁砧等级限制、费用封顶和免费取出逻辑仍属于核心 Mixin，未安装神化时同样需要。

## 配置和构建入口

- 核心 Mixin：`src/main/resources/mixins.the_long_travail.json`。
- 可选 Mixin：`src/main/resources/mixins.the_long_travail.compat.json`，包根为 `com.thelongtravail.compat.mixin`。
- 可选目标的字节码检查：本目录的 `OptionalCompatPlugin.java`；保留原有缺失跳过、版本不支持拒绝加载等安全策略。
- 核心铁砧 Mixin 的神化协调入口：`apotheosis/AnvilCompatPlugin.java`，由核心 Mixin 配置注册；确认神化已提供无限等级阈值时，仅跳过重复的等级限制注入，保留费用封顶和免费取出。
- 可选编译依赖：`gradle/optional-compat.gradle`，由主 `build.gradle` 引入，均为 `compileOnly`。
- 两份 Mixin 配置均由 Gradle 开发运行与成品 JAR Manifest 注册，移动类时必须同步配置和 refmap。
- 语言文件仍在 Minecraft 标准 `assets/the_long_travail/lang` 路径，以保证资源加载和语言切换。

## 升级 Minecraft 版本时

1. 先移植必需依赖、核心注册、玩法、网络协议和核心 Mixin。
2. 对暂未移植的联动，停用对应可选 Mixin/插件注册和编译依赖，并让核心调用的适配入口保留安全默认实现。仅删除整个 `compat` 目录仍会破坏这些调用，不等同于一个独立构建开关。
3. 按子目录逐个对照新版本第三方 API，恢复适配和 `OptionalCompatPlugin` 的契约检查。
4. 分别验证缺少可选模组的基础客户端/服务端，以及安装相关模组的联动场景。
5. 复活账本及墓碑校正涉及物品一致性，不应在未确认存档处理策略时随意关闭。

移动联动实现时，应保持注册 ID、配置键、网络消息顺序及存档契约一致；功能更新涉及这些内容时，须同步维护资源、文档与升级说明。
