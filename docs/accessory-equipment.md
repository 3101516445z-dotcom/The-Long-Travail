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
- `unique(context, item, true)`：沿用春日的游戏、情愫对查询结果不排除外观槽的原有检查方式。

没有苦旅前置条件的饰品只调用 `unique`，不要额外添加 `hasDiary`。槽位列表、主人绑定、消耗状态、外观槽限制、死亡掉落规则仍由原来的物品逻辑负责。

`CuriosRestoreMixin` 为 `readTag` 建立 `AccessoryRestore` 作用域，结束或异常时自动清理，支持嵌套并按实体隔离。一般饰品不直接创建作用域。`DependentAccessories.unique` 保留为转发入口，现有调用自动使用统一规则。

这项处理不会将物品改成死亡保留：死亡掉落仍服从 Curios 和 `keepInventory` 的原有规则。

## 恢复检查要点

新增饰品时，应检查重生复制、同玩家数据重载、前置条件、物品 NBT、单件限制及恢复作用域隔离。死者之书与墓碑恢复还需单独检查第三方存储中的物品一致性。
