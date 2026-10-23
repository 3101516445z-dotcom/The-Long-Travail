# 语言文件维护

这里是全部语言翻译的唯一维护源。不要编辑 build 下的生成文件，也不要在 src/main/resources/assets/the_long_travail/lang 中新增同名文件。

现有 5 种语言：`zh_cn`（简体中文）、`zh_tw`（台湾繁体）、`zh_hk`（香港繁体）、`lzh`（文言）、`en_us`（英文）。每种语言各有 11 个分组；groups.json 决定合并顺序。**`zh_cn` 是键名与顺序的基准**，其余 4 种语言必须与它逐键、逐行对齐。增加已有功能的文本时，同时修改 5 种语言的对应文件；新增分组时，先添加到 groups.json，再为每种语言建立文件。

> `zh_tw` / `zh_hk` / `lzh` 的用词规范见仓库根目录的 `lang-spec-zh_tw-zh_hk-lzh.md`：Minecraft 官方繁中译名、台港用词差异、文言体例、以及 `remap` 式硬性规则（键序 / 占位符 / 颜色码）。

| 分组 | 内容 |
| --- | --- |
| common | 共用属性、状态、提示、按键类别等 |
| diary | 苦旅界面、绑定、探索与反转规则 |
| catalog | 拾遗界面、通用图鉴说明 |
| utility_items | 归乡、全修、路引等通用物品 |
| compatibility | 女仆、JEI、Jade、Curios 等专用兼容文本 |
| aspects/flourishing | 繁茂及关联物品、花朵、生长效果 |
| aspects/abyss | 归墟及关联物品、降雨、流体效果 |
| aspects/far_reach | 穷遐及关联物品 |
| aspects/deep_valley | 幽谷及关联物品、视觉剥夺 |
| aspects/underworld | 冥府及关联物品、复活 |
| aspects/boundless | 无垠及关联物品、时停 |

同主题的物品名、提示、消息、死亡文本与主题页面文案放在同一个主题文件。通用说明留在公共分组；专用于外部模组的文本放兼容分组。每个键只允许出现一次。

## 构建与验证

需要 Node.js 18 或更新版本（无需 npm 安装依赖）。Gradle 默认调用 PATH 中的 node；可用 -PnodeExecutable=绝对路径 指定程序。

- `node tools/verify-language-files.cjs`：检查源分组、重复键、中英文键及顺序、占位参数。
- `node tools/test-language-generation.cjs`：运行错误输入与确定性生成测试。
- `./gradlew.bat generateLanguageFiles --offline`：生成标准语言文件。
- 常规 `processResources`、`classes`、`jarJar` 和 `check` 自动执行生成步骤；IDE 应使用 Gradle 构建资源。

生成位置为 build/generated/resources/language/assets/the_long_travail/lang/，打包后为标准的 `zh_cn.json`、`zh_tw.json`、`zh_hk.json`、`lzh.json` 与 `en_us.json`。构建不会携带分组清单和维护说明。

非法 JSON、重复键、非文本或空译文、缺少任一语言文件、键或顺序不一致、占位参数不一致、未列入清单的源文件都会导致生成失败。新增占位参数时同步检查全部 5 种语言；译文的颜色代码和换行须按实际显示需求保留。
