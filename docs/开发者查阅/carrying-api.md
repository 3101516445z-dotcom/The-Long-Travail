# 统一携带 API

入口：`com.thelongtravail.api.CarryingApi`，可查询任意物品。查询和上报要求非空 `ServerPlayer`、`Item`，在逻辑服务端游戏线程调用，不接受客户端携带上报。

## 查询与前置条件

```java
boolean carried = CarryingApi.isCarrying(player, item);
CarryingApi.CarryingResult result = CarryingApi.query(player, item);
```

`query` 返回 `PRESENT`（已确认携带）、`ABSENT`（未携带）或 `UNKNOWN`（无确认来源且至少一个外部检测器失败）；任一来源确认携带即优先返回 `PRESENT`。`isCarrying` 仅对 `PRESENT` 返回 true，false 不能作为故障时清退装备的依据。

未知结果下：巴别塔保守保留保护，小王子只保留此前由本物品授予的飞行，不新增授权；駅与黄金时代不凭未知结果新增携带收益。外部调用若会移除装备或撤销保护，应显式处理 `UNKNOWN`，暂缓破坏性操作。

查询不授予效果、修改 NBT 或替代佩戴资格。`query` 对死亡或观察者玩家返回 `ABSENT`。

默认扫描主物品栏（含快捷栏和主手）、副手、盔甲栏、末影箱、Curios 功能槽；排除外观槽，不递归打开背包和潜影盒，不读取普通方块容器。仅缓存已确认携带的位置，每次查询重新校验；不缓存未携带的结果。

位置策略在 `config/the_long_travail/general.toml` 的 `carrying` 下。駅与巴别塔的原位置开关作为兼容限制保留：全局开关和旧开关均启用时，该物品才使用此来源。新接入物品仅受全局开关控制。检测盔甲或 Curios 槽不自动赋予物品装备资格。

駅继续自行检查实际佩戴苦旅、天气和归墟阶段；巴别塔基础保护独立，交易强化检查穷遐恶意；小王子不依赖苦旅。不要缓存这些物品的最终效果状态。

## 读取可操作的物品栈

`CarryingApi.findStack(player, item, filter)` 按主物品栏、副手、盔甲栏、末影箱、Curios 功能槽的顺序返回首件通过筛选的真实物品栈，遵循各位置开关并排除外观槽；没有匹配则返回 `ItemStack.EMPTY`。调用方可据此标记或消耗原栈。

该接口不检查存活或旁观状态，供死亡入口读取物品；不使用位置缓存、外部 Provider 或短期上报，因为后两者只证明携带，不能提供可操作的物品栈。死者之书由 `UnderworldItems.equipped(player, false)` 在日记存在时调用，并过滤已被账本消耗的书。黄金时代使用 `isCarrying`，可接收外部携带信号。

## 外部库存检测器

初始化时单线程注册唯一 ID 的检测器；入口不校验服务端线程，也不支持并发注册。仅检查接入方拥有且允许生效的库存，避免重复扫描已有来源。

```java
CarryingApi.registerProvider(
    ResourceLocation.fromNamespaceAndPath("your_mod", "accessory_inventory"),
    (player, item) -> yourInventoryContains(player, item)
);
```

`yourInventoryContains` 是兼容方自己的实现。检测器返回当前真实状态，不保留玩家或物品栈引用，不修改库存。重复 ID 抛错。单个检测器抛出的运行时异常会按来源 ID 限频记录（每分钟最多一次），随后继续检查其他检测器；没有确认来源时返回 `UNKNOWN`。不永久禁用检测器，后续查询允许恢复。回调中新注册的检测器从下一次查询开始参与。禁止检测器递归查询同一玩家；递归异常由外层检测器边界隔离为未知结果。初始化注册错误、错误线程调用仍明确抛错，不属于运行时来源故障降级。

检测器受 `carrying.externalInventoryTick` 统一开关控制。该键保留旧名称，覆盖外部检测器和上报信号两种接入方式。

## 短期携带信号

适合已有逐刻更新机制的容器；仅在该库存确实携带物品时上报：

```java
CarryingApi.reportCarried(player, stack.getItem(),
    ResourceLocation.fromNamespaceAndPath("your_mod", "inventory_tick"));
```

信号按玩家、物品和来源记录，上报时刻及其后 2 游戏刻有效，不写入存档。持续携带须持续上报，不能只在打开容器时上报一次。

旧 `EkiItem` / `BabelItem` 的负槽位 `inventoryTick` 调用继续桥接到统一服务。新兼容优先使用命名检测器或上报接口，不依赖物品类名。

## 生命周期与兼容边界

玩家退出和服务端结束时清理临时记录。外部库存无需复制进玩家背包，也不应调用苦旅状态写入接口。

苦旅本身仍必须有效佩戴；将它放进支持的外部库存或末影箱，不代表装备了苦旅。女仆沿用原生饰品栏及在线主人状态，不由此玩家库存接口扩展成女仆背包生效。
