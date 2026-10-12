# 状态查询与旅程写入接口

通过 `com.thelongtravail.api.TravailStateApi` 可以查询玩家的恶意与见证状态，用于物品或模组联动，无需自行读取 NBT 或扫描饰品栏。

## 十二个便捷接口

参数均为 `Player`，返回值均为 `boolean`。

| 类别 | 检测恶意 | 检测见证 |
|---|---|---|
| 繁茂 | `hasFlourishingMalice(player)` | `hasFlourishingWitness(player)` |
| 归墟 | `hasAbyssMalice(player)` | `hasAbyssWitness(player)` |
| 穷遐 | `hasFarReachMalice(player)` | `hasFarReachWitness(player)` |
| 幽谷 | `hasDeepValleyMalice(player)` | `hasDeepValleyWitness(player)` |
| 冥府 | `hasUnderworldMalice(player)` | `hasUnderworldWitness(player)` |
| 无垠 | `hasBoundlessMalice(player)` | `hasBoundlessWitness(player)` |

```java
import com.thelongtravail.api.TravailStateApi;

if (!player.level().isClientSide && TravailStateApi.hasAbyssWitness(player)) {
    // 在这里执行新物品与归墟之见证的联动。
}
```

## 通用接口与一次查询多个状态

```java
import com.thelongtravail.TravailAspect;
import com.thelongtravail.api.TravailStateApi;

boolean malice = TravailStateApi.hasMalice(player, TravailAspect.BOUNDLESS);
boolean witness = TravailStateApi.hasWitness(player, TravailAspect.BOUNDLESS);
TravailStateApi.AspectState state = TravailStateApi.state(player, TravailAspect.BOUNDLESS);

// 同时判断多个状态时，使用一次查询得到的快照。
var current = TravailStateApi.snapshot(player);
if (current.hasWitness(TravailAspect.ABYSS) && current.hasMalice(TravailAspect.DEEP_VALLEY)) {
    // 组合条件。
}
```

## 状态约定

- `INACTIVE`：没有装备苦旅，或传入空玩家。六个恶意与六个见证查询全部返回 false。因此不能用 `!hasWitness(...)` 代替 `hasMalice(...)`。
- `MALICE` / `WITNESS`：以 Curios 槽位中第一件已装备的苦旅为准；背包、主手中的苦旅不激活玩家状态。异常情况下装备多件时也不合并状态。
- 装备尚未初始化的苦旅时，没有见证标记的类别属于恶意。接口不会生成旅程或改写存档；可用快照的 `initialized()` 额外要求旅程已经初始化。
- 支持自然完成与调试命令的强制状态；优先级为强制恶意 > 强制见证 > 自然见证。
- 查询的是类别状态，不代表其附带条件当前正在触发。例如处于无垠恶意，并不代表玩家此刻一定在高空减速；处于幽谷恶意，也不代表当前一定有视觉剥夺。
- 查询只读取状态，不会触发效果或修改物品数据。快照不持有玩家或物品引用，状态变化后需要重新获取。
- 在所在逻辑端的游戏线程调用。服务端结果用于伤害、消耗和奖励等正式判定；客户端查询用于显示，取决于当前同步到的装备状态。
- `TravailAspect` 参数不得为 null；玩家可以为 null。

## 检查物品自身保存的状态

`TravailStateApi.inspectDiary(stack)` 返回同一种快照，仅检查这件物品，并不表示任何玩家已经装备它。适合预览与配方逻辑。空物品、`null` 和非苦旅物品均返回 `INACTIVE`。

快照的 `diaryPresent()` 表示查询来源包含苦旅：对 `snapshot(player)` 是已装备；对 `inspectDiary(stack)` 仅代表物品类型正确。运行前应完成模组注册；作为外部模组集成时应声明依赖或做好可选依赖隔离。

## 服务端旅程写入

外部只读查询继续使用 `com.thelongtravail.api.TravailStateApi`。服务端写入使用 `TravailJourneyApi`，提供 `completeRequirement`、`completeRandomRequirement`、`grantWitness`、`resetAspect`、`resetJourney`、`setOverride`、`setAllOverrides`、`clearOverride`、`clearAllOverrides`。接口只操作首个已装备日记，须在服务端线程调用，不自动消耗调用方物品。返回值通过 `changed()` 判定是否修改、通过 `accepted()` 判定操作是否有效，重复覆盖和清除属于有效操作。

`RequirementRef` 区分群系和结构；`AspectJourneyKey` 包含整本身份、主题和轮次。延迟授予见证须保存原身份，不能执行时重新取身份。死者之书账本使用同样的身份，旧账本仅匹配旧主题轮次，不会恢复精粹开启的新轮次。未知或损坏的主题身份不会回退到旧格式。新数据不支持降级到旧版模组；客户端与服务端须使用相同模组版本。

结果对象提供变更与重置主题、前后有效状态位、前后身份、探索与展示变化。自然到访保留原有多主题完成和受控查询续接；外部写入进度会取消旧查询与路引任务。来源或覆盖变化只更新状态修订，不取消探索确认。底层 `JourneyOperations` 和 NBT 入口供模组内部集成使用，外部集成应使用上述 API。

`TravailJourneyApi.identity(player, aspect)` 获取当前主题身份，未装备有效且已初始化的苦旅或身份数据异常时可能返回 `null`。使用 `grantWitness` 前应检查身份，并保存发起操作时的身份供延迟回调使用。
