# 逐日与籠釣瓶女仆联动

两件物品仅在女仆原生饰品栏生效，普通背包不算装备。基础能力独立于主人苦旅；恶意与见证读取在线主人所佩戴苦旅的对应主题状态，互不继承。女仆不继承归墟恶意的周期性流体诅咒，因此没有新增豁免入口。

## 查询与可选加载

MaidAccessories.items 注册两件物品；TravailMaidPlugin 循环绑定 IMaidBauble 并沿用客户端同步。共用玩法只接触原版类型：equipped 查询原生饰品，phase 返回 AspectState，damageActor 解析女仆伤害归属。门面先判断模组加载状态，内部桥接的返回签名为 LivingEntity，避免在缺少女仆依赖时验证 EntityMaid 的继承关系。

SunBladeActors.attacker 保持玩家原来源判断；女仆复用 MaidCombatCalculations 的来源及 Projectile.owner 查询和循环保护。phase 将玩家 TravailStateApi 与女仆 MaidAccessoryRules 接到同一效果入口。查询不修改 DamageSource，也不把女仆伤害算到主人名下。

## 共用伤害链

DamageScope → Kagotsurube.convert → CombatContext 使用原始来源判断输出、有效来源判断防御。确认普通生命或吸收生命损失后延长凋零、写入逐日抑制；收尾按主体 UUID 在同步派生链内认领一次逐日追加。不同女仆独立认领。

SunStrike 保留固定比例与禁止本模组增伤、必杀的标记；能够延长籠釣瓶凋零，但不进入无穷花普通入队分派。无穷花保留每服务器刻一笔、FIFO、4096容量及既有溢出策略。逐日派生伤害对玩家目标沿用 MaidAccessories.canHarm 的主人与玩家伤害限制。

## 周期与生命周期

MaidPeriodicEffects 在时停返回前对账籠釣瓶属性，返回后调用 SunChaser.maintain；回血沿用实体每20刻间隔，时停不重复恢复。主人离线或无苦旅时停止分支，基础免火与凋零仍有效。

TravailMaidPlugin.onTakeOff 直接清除籠釣瓶临时属性，不重读回调期间的旧槽。周期维护按最终槽内容校正；MaidRuntimeState.forget 在实体离开或清理时撤除属性。目标已有凋零及禁疗按各自持续时间保留，已经反弹的弹射物不追溯改写。

## 配置与文本

女仆复用 SunBladeConfig，无专属倍率。MaidAccessoryTooltips 根据物品名单追加两行深灰色原生饰品与主人阶段说明。拾遗正文、分行及风味描述沿用玩家物品的展示资源。

共用机制与获取规则见 [逐日与籠釣瓶](sun-chaser-kagotsurube.md)。
