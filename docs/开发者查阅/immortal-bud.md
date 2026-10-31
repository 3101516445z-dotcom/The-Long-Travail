# 永生芽：赋予与保护

## 入口和数据

- 物品 ID：`the_long_travail:immortal_bud`，普通可堆叠材料，不是饰品。
- 实例标记：物品 NBT 布尔键 `the_long_travail:immortal_bud`。
- `ImmortalBud` 负责判定、赋予及掉落物保护资格；判定不会创建 NBT。
- `ImmortalBudRecipe` 使用自定义无序配方，只接受一个芽槽位、一个有耐久容量、未标记且不在 itemBlacklist 内的目标槽位。每次复制目标并输出一件，所有剩余物槽位为空，避免目标自身的合成返还重复生成。
- 赋予使用 `ItemStack.copy()`，写入独立标记，并固定设置 `Damage=0`。直接归零仅限赋予流程，以支持禁止普通修复或只允许损伤增加的物品；普通修复仍遵循物品自身规则。
- 获得配方使用 `#minecraft:saplings`、骨粉、无穷花，各一份；两种配方支持 2×2。

## 耐久调用关系

`ImmortalBudHurtMixin` 在 `ItemStack.hurt()` 入口对带标记物品的正数损耗返回 false，阻止计算破损以及依据返回值的删除。

`ImmortalBudDamageMixin` 在 `Item` 上提供 `IForgeItem.setDamage()` 的转发实现：带标记且请求损伤大于当前损伤时返回，否则调用原接口默认实现。这样同时覆盖标准栈 setter、直接物品 setter，以及重写 setter 后调用 `super.setDamage()` 的路径。

Forge 47.4.10 的 `ItemStack.hurt()` 使用局部计算的预计损伤判断破损，而不是 setter 返回后重新读取损伤。因此只阻止写入不足以阻止超额损伤破坏，必须同时保护 `hurt()` 入口。Mixin 0.8.5 不支持接口内注入，故通过 `Item` 的接口转发实现第二层，不替换 Forge 默认写入规则。

完全重写 setter 且不调用父实现、显式绕过此转发的接口默认调用、直接 NBT 写入、自定义资源系统、无条件删除等不保证覆盖。不会全局拦截 `shrink()` 或任意 NBT `Damage` 写入。不依赖原版 `Unbreakable`，已有原版标记保持原样。

## 掉落物与显示

`ImmortalBudDropMixin` 在 `ItemEntity.hurt()` 拦截带火焰或爆炸伤害标签的来源；在 tick 开头重置受保护掉落物的非负年龄，使正常自然到期失效。保留正常 tick、拾取延迟与物品合并行为。标记从物品栈读取，支持保存重载；其他伤害及虚空流程照常。

`ThemeTooltips.finish()` 在名称之后添加 `AspectTheme.FLOURISHING.name()` 组件，复用现有字体材质。永生芽自身接入繁茂创造栏、拾遗图鉴、五种语言和 JEI 信息页。动态赋予配方不是固定输出配方，通过物品提示和 JEI 信息说明用法。

## 相关文档

职责分工和调用链路见 [永生芽赋予与物品保护](架构/链路/永生芽赋予与物品保护.md)。验证工具和验收范围见 [当前状态与验收边界](架构/维护/当前状态与验收边界.md)。

[返回开发文档](README.md)
