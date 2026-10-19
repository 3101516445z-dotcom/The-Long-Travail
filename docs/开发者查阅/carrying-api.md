# 统一携带 API

适用版本：The Long Travail 0.1.2，Minecraft 1.20.1。

入口：`com.thelongtravail.api.CarryingApi`。用于駅、巴别塔、小王子，也允许外部模组查询任意物品。只在逻辑服务端的游戏线程调用；不接受客户端上报的携带事实。

## 查询与前置条件

```java
boolean carried = CarryingApi.isCarrying(player, item);
CarryingApi.CarryingResult result = CarryingApi.query(player, item);
```

`query` 返回 `PRESENT`（确认携带）、`ABSENT`（确认未携带）、`UNKNOWN`（没有来源确认携带，但至少一个外部检测器查询失败）。任意正常来源确认携带时，优先返回 `PRESENT`。兼容布尔入口 `isCarrying` 只对 `PRESENT` 返回 true；不能将其 false 用作失败时清退装备的依据。

未知结果下：巴别塔保守保留保护，小王子只保留此前由本物品授予的飞行，不新增授权；駅不凭未知结果新增携带收益。外部调用若会移除装备或撤销保护，应显式处理 `UNKNOWN`，暂缓破坏性操作。

只判断玩家是否在启用位置有效携带物品；不会授予效果、修改 NBT 或替代苦旅佩戴资格。对死亡或观察者玩家，`query` 返回 `ABSENT`，`isCarrying` 返回 `false`。

默认扫描主物品栏（含快捷栏和主手）、副手、盔甲栏、末影箱、Curios 功能槽；排除外观槽，不递归打开背包和潜影盒，不读取普通方块容器。仅缓存已确认携带的位置，每次查询重新校验；不缓存未携带的结果。

位置策略在 `config/the_long_travail/general.toml` 的 `carrying` 下。駅与巴别塔的原位置开关作为兼容限制保留：全局开关和旧开关均启用时，该物品才使用此来源。新接入物品仅受全局开关控制。检测盔甲或 Curios 槽不自动赋予物品装备资格。

駅继续自行检查实际佩戴苦旅、天气和归墟阶段；巴别塔基础保护独立，交易强化检查穷遐恶意；小王子不依赖苦旅。不要缓存这些物品的最终效果状态。

## 外部库存检测器

初始化时注册一次具有唯一 ID的检测器。只检查本模组拥有并实际允许生效的库存，不扫描末影箱等已有来源。

```java
CarryingApi.registerProvider(
    ResourceLocation.fromNamespaceAndPath("your_mod", "accessory_inventory"),
    (player, item) -> yourInventoryContains(player, item)
);
```

`yourInventoryContains` 是兼容方自己的实现。检测器返回当前真实状态，不保留玩家或物品栈引用，不修改库存。重复 ID抛错。单个检测器抛出的运行时异常会按来源 ID限频记录（每分钟最多一次），随后继续检查其他检测器；没有确认来源时返回 `UNKNOWN`。不永久禁用检测器，后续查询允许恢复。禁止检测器递归查询同一玩家；递归异常由外层检测器边界隔离为未知结果。初始化注册错误、错误线程调用仍明确抛错，不属于运行时来源故障降级。

检测器受 `carrying.externalInventoryTick` 统一开关控制。该键保留旧名称，覆盖外部检测器和上报信号两种接入方式。

## 短期携带信号

适合已有逐刻更新机制的容器；仅在该库存确实携带物品时上报：

```java
CarryingApi.reportCarried(player, stack.getItem(),
    ResourceLocation.fromNamespaceAndPath("your_mod", "inventory_tick"));
```

按玩家、物品和来源分别记录。信号在上报时刻至其后 2 游戏刻内有效；停止上报后自动失效，不写入存档。不要在打开容器时只上报一次来模拟持续携带。

旧 `EkiItem` / `BabelItem` 的负槽位 `inventoryTick` 调用继续桥接到统一服务。新兼容优先使用命名检测器或上报接口，不依赖物品类名。

## 生命周期与兼容边界

玩家退出和服务端结束时清理临时记录。外部库存无需复制进玩家背包，也不应调用苦旅状态写入接口。

苦旅本身仍必须有效佩戴；将它放进支持的外部库存或末影箱，不代表装备了苦旅。女仆沿用原生饰品栏及在线主人状态，不由此玩家库存接口扩展成女仆背包生效。
