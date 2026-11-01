# 拾遗术语解释

拾遗详情页支持战斗属性及主题专有术语的悬浮解释，包括直击伤害、处决、伤害加成、主花与配花、催生、随机药水、淘金、佳肴、魂数、对阵亡灵及高空。正文中每种术语首次出现的位置带下划线，鼠标移到该词上即可查看解释；移开后关闭。

同页后续出现不重复标记，段落及滚动不会重置首次位置。“直击伤害”使用统一词条，不再识别简称。小窗采用灰褐底、低饱和铜色细边框与米灰色文字。

实现：CatalogGlossary 在现有正文染色之后保留样式并添加首次术语标记，CatalogDetailScreen 按最终换行后的文字进行命中检测。术语名、别名和解释维护于五语言 catalog.json 及六份 catalog_主题分组中的 glossary 条目。动态参数由 CatalogGlossary 和 CatalogFarReachCards 读取 TooltipConfigSync，保持与正文使用同一服务端快照。返回、退出、正文滚动保持原有行为。

下划线独立于字体绘制：关闭原版逐字下划线，每段连续术语只画一次，按最终GUI变换保持一个帧缓冲像素厚度，使用70%透明度的低亮铜色。线条与字底留出间距，悬浮命中仍依据文字样式标记。

## 正文分组与样式

CatalogEntries 将物品映射到对应主题的详情构建器：CatalogFlourishingDetails、CatalogAbyssDetails、CatalogFarReachDetails、CatalogValleyDetails、CatalogUnderworldDetails 和 CatalogBoundlessDetails。CatalogUtilityDetails 处理旅程助力、主题精粹及解密之石。构建器只组织展示内容与读取配置，不改变服务端机制。

正文先由 CatalogTextStyle 统一语义色，再由 CatalogGlossary 按物品范围标记术语。主题条件采用独立紫色标题，段内过渡强调使用金色，数值与操作使用绿色，列表符号使用灰色。未佩戴苦旅的红色提示独立于正文，不隐藏仍可生效的基础能力。卡片正文使用统一米灰色。

CatalogDetailScreen 根据字体宽度为列表续行保留正文缩进，并避免中文句末标点独占末行。悬停命中和卡片定位均使用当前页面的实际缩放与滚动坐标。

## 悬浮简述

ThemeTooltips 从各主题语言分组读取简述，保留基础能力与苦旅状态能力的区别。仅允许佩戴苦旅后装备的饰品使用装备限制提示，万魂幡与洛伦兹幻想使用状态效果未生效提示。死者之书的复活仍要求佩戴苦旅。女仆万魂幡提示由 compatibility 分组说明收魂独立于主人苦旅状态，属性加成依赖对应状态。
