# 施色与名称显示

## 持久数据

物品根标签的字符串 `the_long_travail:name_style` 接受 `travail`、`flourishing`、`abyss`、`far_reach`、`deep_valley`、`underworld`、`boundless`、`prose`、`flavor`、`clear`。

缺失、未知或类型错误按未施色处理。`clear` 显式关闭 TLT 名称样式；删除字段则恢复物品自身默认渲染。读取名称不创建或修改物品数据。

字段独立于 `display`，保留自定义名称、Lore、神化词缀及外部库存身份。不同样式按原版 NBT 规则区分堆叠，材料合成后的继承由配方决定。

## 名称转换顺序

[NameColoring](../../src/main/java/com/thelongtravail/item/NameColoring.java) 在两端处理组件。顺序为物品或自定义名称、神化完整词缀与品质色、TLT 神化适配、施色。

[ColoringNameMixin](../../src/main/java/com/thelongtravail/mixin/presentation/ColoringNameMixin.java) 使用优先级 300，包裹神化 500 与词缀适配 400。转换复制组件、翻译参数和子组件，保留翻译键；显示副本的颜色码按选定样式处理。还原样式移除 TLT 标记及附加颜色，存在神化品质时使用品质色。

`prose`、`flavor` 对应镀金与陶染，渲染标记以 `:coloring:auto` 结束。字体或名称替换模组应保留组件结构及样式，使语言切换和动态材质继续工作。

## 容器与界面扩展

`menu/ColoringMenu` 使用标准菜单按钮请求，服务端校验当前菜单及选项范围。结果槽只展示预览，领取时按实际移动数量提交并扣除输入。左键、右键、Shift 支持领取；数字键交换和丢弃不领取预览。

关闭时返还输入，满包掉落。预览使用普通物品栈副本，保留原物品的外部库存身份，不能将预览当作独立库存交付。

`client/ColoringScreen` 依次调用 `renderBackground → super.render → renderTooltip`，界面扩展可使用标准容器背景绘制事件。
