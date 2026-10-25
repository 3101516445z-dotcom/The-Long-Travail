# 施色与名称显示

物品 ID：`the_long_travail:coloring`。八个原版萤石粉围绕一个原版刷子合成。无消耗和耐久，右键打开容器菜单，支持整组处理；右键结果取一个，左键取可容纳数量，Shift 转移至背包。

## 入口与数据

- `item/ColoringItem`：物品、名称、说明及服务端开窗。
- `item/NameColoring`：固定十选项和名称转换；公共端不引用客户端类。
- `menu/ColoringMenu`：输入与输出预览、选项同步、领取和关闭返还。
- `client/ColoringScreen`：三类选项、预览、Esc/关闭按钮。
- `mixin/ColoringNameMixin`：在完整 `ItemStack.getHoverName()` 返回后处理名称。
- `compat/apotheosis/AffixNameStyle`：已施色时让位；清空时保留神化品质色。

物品根标签下的字符串 `the_long_travail:name_style` 取值为 `travail`、`flourishing`、`abyss`、`far_reach`、`deep_valley`、`underworld`、`boundless`、`prose`、`flavor`、`clear`。缺失、未知或类型错误按未施色处理，不在读取时修复或修改物品。

`clear` 是显式禁用 TLT 名称效果，不能用删除字段替代。删除字段恢复物品自身默认规则；GUI 不提供额外的第十一个选项。

独立记录不放入 `display`，不覆盖自定义名称、Lore、神化词缀数据和外部库存身份。不同记录的物品按原版 NBT 规则区分堆叠。普通数据复制可以携带记录，消耗材料生成其他物品不额外添加全局继承规则。

## 名称与渲染

顺序：物品/自定义名称 → 神化完整词缀名称与品质色 → TLT 既有神化适配 → 施色。

`ColoringNameMixin` 使用优先级 300，位于神化 500 与现有适配 400 外层。必须在真实加载 Mixin 的环境中验证，不能仅凭优先级数值或纯函数测试断言最终顺序。

转换复制组件、翻译参数和子组件，保留翻译键及原始数据，不写回染色后的文本。选择效果后只在显示副本中处理嵌入颜色码，保留原始名称；清空移除已知 TLT 标记及其附加颜色。神化品质存在时清空采用该品质颜色。未施色物品继续原路径。

两种正文标记以 `:coloring:auto` 结束，客户端根据当前字体与文字计算宽度；不会把语言或旧字体宽度写进 NBT。复用 `DiaryFontEffects` 及现有 Shader，不增加新的 Shader 材质编号。

## 容器语义

选择通过标准菜单按钮请求发送，服务端验证玩家当前菜单和固定选项范围。输出槽是不可普通取出的预览；领取只在服务端提交，按实际移动数量扣除输入。数字键交换、丢弃等绕过结果提交的操作不能取走预览。输入和玩家背包使用标准容器操作。

菜单打开后不锁定施色所在槽；允许移动工具，菜单持续到主动关闭或正常容器生命周期结束。关闭返还未领取输入，满包按原版规则掉落；不返还输出预览。输入物品不执行独立库存克隆，不能调用完美流体复制适配器。

## 验证入口

```
gradlew.bat build jarJar --offline
gradlew.bat -I tools/structure-regression/coloring.init.gradle runServer --offline
gradlew.bat -I tools/structure-regression/coloring.init.gradle -PcoloringApotheosis runServer --offline
gradlew.bat -I tools/structure-regression/coloring.init.gradle runClient --offline
```

神化环境使用 7.4.8 与 Placebo 8.6.3，另需在 `build/coloring-deps` 准备匹配的 Apothic Attributes；客户端测试依赖先运行最小服务端生成的隔离世界。脚本仅绑定本机测试端口，不读取整合包正式存档。测试目录保留在 build 下供检查，证据归档后按项目约定可恢复地清理。

服务端覆盖真实名称注入、铁砧、数据持久及菜单领取；神化夹具额外覆盖品质变化和真实重铸台预览/领取。客户端测试单独检查开窗、选择同步、材质识别、截图和关闭返还。通过范围以每次结果文件为准，不能等同于完整整合包、远程联机或所有第三方界面已兼容。

整合包副本验收、依赖版本及可复现命令见[施色验证说明](../../tools/structure-regression/coloring-README.md)。完整包验收保留脚本、配置和资源组合，另行记录结果与加载 JAR 的哈希；不使用正式玩家存档。

## 施色面板

逻辑尺寸 284×254，菜单输入物品坐标 (81,47)，输出 (187,47)，玩家背包首槽 (62,163)，快捷栏首槽 (62,221)。视觉框与槽内 16×16 物品分开，外槽框 28×28。修改坐标时必须同步 ColoringMenu 和客户端回归鼠标坐标。

ColoringScreen 使用自绘 Button 保留标准焦点与键盘激活；十项按钮用粗体及既有名称材质，清空按钮以窄宽换行为两行。显示名为“镀金”“陶染”“还原样式”，NBT ID 仍为 prose/flavor/clear。右侧分区标题仍为“清空”。外框与关闭按钮复用 CatalogFrame；只开放绘制入口，拾遗自身绘制行为不变。

绘制顺序保留标准 `renderBackground → super.render → renderTooltip`，让 JEI 等界面扩展收到容器背景绘制事件，不以面板自身底色替代该流程。

客户端回归加 -PcoloringQuick 可跳过十分钟持续阶段（仅约一秒），仍执行十项实际按钮点击、资源重载、输出槽点击和关闭按钮点击。快速回归不能当作重新完成十分钟压力测试。

## 源码职责与目录

- `item/ColoringItem`：右键入口与提示；`item/NameColoring`：物品栈样式 ID、名称组件变换，不读取客户端类或直接依赖神化。
- `menu/ColoringMenu`：两端共享的菜单与槽位布局，服务端独占预览生成、领取与数量提交。不放入 client 包，避免专用服务器加载客户端类。
- `client/ColoringScreen`：按钮输入、字体与面板绘制；复用 `client/catalog/CatalogFrame` 的无状态绘制方法，不复用拾遗会话或菜单状态。
- `mixin/ColoringNameMixin`：原版名称入口的薄桥接；神化品质处理留在 `compat/apotheosis/AffixNameStyle`，不从通用名称处理反向加载神化类。
- `registry/ModRegistry` 注册菜单与物品，`TravailClientEvents` 在客户端装配 Screen；使用标准菜单包，不新增自定义协议。

该分工对应项目书的物品入口、平台接入、逻辑端状态及客户端显示边界。新增 menu 包用于正式容器职责，不将状态事务并入纯显示的拾遗目录。

## 拾遗展示与文案

施色位于旅程助力中，顺序为归乡、全修、路引、施色。助力展座复用下方六列的列距，四件物品对应中间四列；不改变解密之石、精粹和主题物品的列位置。

物品提示保留确认的两行短正文及统一拾遗入口。拾遗详情独立维护为四句、两段，中间通过空组件分段，不复用物品提示。CatalogTextStyle 对施色和还原样式使用既有亮金色机制强调，数量及整组范围使用碧玉绿，其他说明保留正文色；不改写语言资源中的原文。共享详情换行只处理末行全为中文句末标点的情况，将前一汉字连同原样式移至末行，避免孤立标点；不扩大正文区或改变字号。

客户端回归结束前额外检查四个助力展座与中间四列对齐，并保存首页、施色详情及物品提示截图。渲染与断行效果仍应检查截图，不以坐标断言代替视觉检查。
