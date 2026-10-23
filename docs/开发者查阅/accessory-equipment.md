# 饰品佩戴与恢复校验

公共入口：`com.thelongtravail.helper.AccessoryEquipRules`。

Curios 的 `readTag` 在提交新槽位表前调用 `canEquip`。重生、登录及其他整套饰品数据恢复期间，不应直接读取当前装备来判断前置条件或重复物品。

## 新饰品接入

```java
@Override
public boolean canEquip(SlotContext context, ItemStack stack) {
    if (context.cosmetic() || !context.identifier().equals("head")
            || !(context.entity() instanceof Player player)) return false;
    // 主人绑定、已消耗等物品自身校验也放在 unique 之前。
    if (!AccessoryEquipRules.hasDiary(player)) return false;
    return AccessoryEquipRules.unique(context, this);
}
```

- `hasDiary(player)`：正常穿戴时检查旅行日记槽；恢复时检查待恢复数据中的旅行日记槽。
- `hasEquipped(player, slot, item)`：指定功能槽中的前置物品检查。恢复期检查的是输入快照，不递归验证其他饰品的依赖；应使用本身可独立恢复的前置物品，避免循环依赖。
- `unique(context, item)`：正常穿戴允许同槽位替换，并拒绝其他功能槽已有同种物品；恢复时只允许首个通过本物品校验的位置，同一位置重复校验不影响结果。必须在其他条件均通过后最后调用。
- `unique(context, item, true)`：正常穿戴时，沿用春日一隅、喜欢的心情将外观槽计入唯一性查询的方式；恢复期间仍委托同一个 `AccessoryRestore` 首次接受记录，不另按此参数扫描外观槽。它不代表允许把物品装备到外观槽。

无苦旅前置的饰品只调用 `unique`。槽位、绑定、消耗、外观槽和死亡掉落限制由物品自身逻辑负责；死亡掉落仍遵守 Curios 与 `keepInventory`。

`CuriosRestoreMixin` 为 `readTag` 建立 `AccessoryRestore` 作用域，结束或异常时自动清理，支持嵌套并按实体隔离。一般饰品不直接创建作用域。`DependentAccessories.unique` 保留为转发入口，现有调用自动使用统一规则。

## 恢复检查要点

新增饰品时，应检查重生复制、同玩家数据重载、前置条件、物品 NBT、单件限制及恢复作用域隔离。死者之书与墓碑恢复还需单独检查第三方存储中的物品一致性。

## 前置失效与卸装

`DependentAccessories.register(Supplier<Item>, Predicate<ServerPlayer>, Consumer<ServerPlayer>)` 登记依赖关系，初始化时注册一次，避免重复登记。cleanup 应可重复调用，且只撤销自己的属性、托管模式或短期状态。实际槽位修改完成后可调用 `reconcile(player)`；不要在单次 onUnequip 回调或普通 NBT 更新中推断前置已经失去。

`reconcile` 核对前置、槽位及物品，处理重入，并将移除物品返还背包或掉落；不要另建扫描/返还流程。主题分支失效时，应保留无苦旅前置的基础能力。

第三方死亡储物、墓碑和外部库存可能在实体之外保存物品副本。消耗或迁移特定物品时需要稳定身份和专用存储适配，不能只处理玩家当前背包，更不能按物品 ID 清除所有同类物品。涉及旅程轮次的恢复另见 [状态与旅程 API](state-api.md)。
