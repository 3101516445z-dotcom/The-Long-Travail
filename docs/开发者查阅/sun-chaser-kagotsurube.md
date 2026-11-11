# 逐日与籠釣瓶接入

物品 ID 为 `the_long_travail:sun_chaser` 和 `the_long_travail:kagotsurube`。逐日通过携带资格生效，籠釣瓶使用腰带资格；基础能力独立于苦旅，阶段通过 [TravailStateApi](state-api.md) 判定。多件同物品不叠加。

## 伤害来源与追加链

`combat/SunBladeActors` 查询效果主体与阶段；`farreach/SunChaser` 保存伤害前燃烧状态和目标最大生命快照；`underworld/Kagotsurube` 提供凋零延长与直击转换资格。

`DamageIdentity` 保留原来源的对象身份，将转换后的来源交给原版及外部事件。两者分别用于派生链认领和有效类型防御；创建新 DamageSource 代替原实例可能改变内部认领结果。

逐日见证默认按目标最大生命快照的 1% 追加直击伤害，排除 TLT 自身输出增幅与必杀，继续经过适用防御和外部事件。每个效果主体在同一同步派生链中至多追加一次。逐日追加可延长籠釣瓶凋零，但不进入无穷花普通入队及旧饰品普通成功攻击分派。

无穷花重放按已有金额入队，FIFO，每服务器刻一笔，容量 4096。一笔重放可触发一次逐日，逐日追加金额不写回原队列。详见[伤害结算](伤害结算与追加效果.md#追加与重放)。

## 治疗、冷却与弹射物

逐日抑制截止时间使用世界游戏时间，重复命中刷新。正常治疗通过 LivingHealEvent 取消，直接 setHealth 保留自己的写入路径。抑制期间清除受伤冷却；其他机制恢复冷却时应协调该状态。

`combat/ProjectileReflection` 在 Forge 弹射物命中事件反转速度、归属和火球推进方向，并重置离开拥有者的状态。自定义弹射物需沿用对应命中事件及速度、归属字段，反弹后碰撞重新生成伤害。

## 凋零与合成

籠釣瓶每次确认伤害默认增加 100 tick 凋零。已有实例通过 NBT 副本更新时长，保留等级、隐藏链与显示标记；无限时长不变。没有凋零时走原版效果添加流程，遵守效果适用性。延长不再次经过完整时长放大流程。

[kagotsurube.json](../../src/main/resources/data/the_long_travail/recipes/kagotsurube.json) 使用 `minecraft:crafting_shaped`。材料 `the_long_travail:cursed_book` 由 `underworld/CursedBookIngredient` 匹配：附魔书包含至少一种已注册诅咒即可，也允许混合其他附魔。

该 Ingredient 的 `isSimple=false`，同时提供 JSON 和网络序列化。展示候选是已注册诅咒的一级书，自动合成应调用材料谓词判断实际 NBT。结果为全新物品，不继承输入剑的 NBT。旧 `the_long_travail:kagotsurube` 配方类型已移除，外部数据包应改为当前有序配方格式。

## 女仆接入

女仆仅在原生饰品栏取得这两件物品的效果。阶段读取在线主人苦旅，基础免火和凋零独立生效；效果、属性、冷却与伤害归属仍属于女仆。

`MaidAccessories` 的安全门面将第三方类型隔离在适配实现中。`SunBladeActors` 复用女仆弹射物归属解析；同步逐日认领按女仆自身 UUID 区分，攻击玩家时遵守 `MaidAccessories.canHarm` 的主人与玩家限制。

`MaidPeriodicEffects` 在时停返回前校正籠釣瓶属性，返回后推进逐日回血；时停不推进恢复。卸装直接撤销临时属性，后续周期按最终槽位校正；实体离开时由 `MaidRuntimeState.forget` 清理。已施加的凋零和禁疗持续到各自期限，已反弹弹射物保持新归属。

参数共用 `SunBladeConfig`，逐日保存于 `aspects/far_reach.toml`，籠釣瓶保存于 `aspects/underworld.toml`。女仆没有独立倍率。
