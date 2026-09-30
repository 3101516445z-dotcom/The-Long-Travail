# 规则、画质与奖励预算

本次调整保留默认的强制流体伤害与“受到攻击即触发恶意”。不添加独立 HUD 图标。探索要求使用版本号缓存；自动效果提供默认关闭的频率上限。

## 服主配置

文件：`config/the_long_travail-common.toml`。新键会在新版启动时补齐；已有配置值保留。

```toml
[general]
receivedMaliceTrigger = "ATTACK_ATTEMPT"
receivedMaliceCooldownTicks = 0
receivedMaliceExcludedDamageTypes = []
maxAutomaticEffectActionsPerPlayerPerSecond = 0

[abyss]
fluidErosionUnavoidable = true
fluidErosionProtectionMode = "ENFORCED"

[boundless]
phantomMaxKillsPerScan = 16

[rewards]
rewardMaxItemEntitiesPerTick = 32
rewardQueueCapacity = 1024
rewardOverflowPolicy = "DEFER"
rewardMaxEntitiesPerReward = 64
```

将键添加到原有同名段落，不要在一个 TOML 文件中重复声明同名段落。

### 流体伤害

| 模式 | 事件取消 | 事件把伤害归零或减伤 | 外部增伤 |
|---|---|---|---|
| ENFORCED（默认） | 绕过 | 保护本模组计算后的伤害下限 | 保留 |
| RESPECT_CANCELLATION | 尊重 | 未取消时保护伤害下限 | 保留 |
| STANDARD | 尊重 | 接受 | 保留，再按原有普通路径应用本模组倍率 |

这里的事件指 Attack、Hurt、Damage 边界。RESPECT_CANCELLATION 区分真正取消事件与仅将数值改成 0。

旧开关 `fluidErosionUnavoidable=false` 优先，等同于 STANDARD，避免旧配置升级后重新启用强制伤害。三种模式均保留流体伤害原有的伤害类型标签与吸收生命穿透；不承诺取消所有真实伤害特性。内部免伤、输出限制、图腾与死亡事件仍沿用原有规则。

### 受击恶意

- ATTACK_ATTEMPT：保持原有行为；外部最终取消伤害，仍可能触发受击恶意。
- HEALTH_LOSS：仅正常完成伤害调用且实际生命值下降后触发；只损失吸收生命不触发。致死命中发生过实际扣血，也属于该类别。
- 冷却是玩家级别的共享冷却，单位游戏刻，20 刻约一秒；在触发前占用，嵌套命中共享。0 保持无限制。冷却限制恶意尝试频率，不保证每次尝试都抽中效果。
- 伤害类型排除表填写完整 ID，例如 `["minecraft:generic", "minecraft:fall"]`。只排除受击恶意，不改变伤害和攻击成功奖励。
- 登出、死亡、重生、换维度与日记刷新会清除运行时冷却。

### 幻翼与额外奖励

- 每名玩家每轮最多尝试处理 16 只幻翼；被取消的伤害也计入尝试，避免无敌幻翼导致事件风暴。扫描范围和间隔继续使用原配置。
- 钓鱼和幻翼的本模组额外物品奖励共用全服队列，每刻最多尝试生成 32 个物品实体；不限制原版或其他模组的掉落。
- 可堆叠物品按最大堆叠生成；同一位置、同一维度、相同物品数据的相邻奖励可合并。不可堆叠物品分批生成。
- DEFER：正常情况下保留全部奖励，分批发放。
- LIMIT：每次奖励只保留至多 `rewardMaxEntitiesPerReward` 个堆叠，多余数量不发放；保留的部分仍分批处理。该模式有意削减极端配置奖励。
- 队列满时先暂停自动击杀，避免杀死幻翼后没有队列空间。钓鱼仍正常进行，但本模组额外物品奖励跳过；日志最多每分钟提示一次。
- 每次自动击杀前预留奖励记录，死亡事件中的嵌套逻辑不能抢占该位置。
- 待发记录随主世界 SavedData 保存到 `data/the_long_travail_rewards.dat`，正常保存、关闭和重启后继续。它不是崩溃事务日志，不提供断电时跨区块与 SavedData 的原子提交保证。
- 目标区块未加载、维度暂时不存在时保留记录，不强制加载区块；轮转到其他记录继续尝试。重新可用后继续发放。
- 掉落实体生成被事件取消时保留奖励，也消耗本刻尝试预算。长期被取消或永远无法加载的目标会占用队列空间，最终使自动击杀暂停；不会无限扩张内存。
- 调低容量只阻止新增记录，不删除已保存奖励。

## 探索缓存与自动效果上限

探索要求新增 `RequirementsRevision`。初次生成写入版本 1，每次实际完成目标后递增；重复访问同一目标不会递增。服务端探索缓存与客户端要求排版比较物品、NBT 根/要求对象身份、旅程 UUID 和版本号，不再深比较或复制整个要求列表。语言、资源与配置重载等原有页面失效条件仍保留。

旧日记缺失版本号时按 0 读取，不重抽任务、不清空进度、不因读取而修改存档。换物品、网络替换 NBT、刷新旅程会使缓存失效。当前结构查询仍一次完成，没有引入分批查询或延迟判定。

其他代码若直接原地修改要求 NBT，完成后必须调用 `LongTravailData.requirementsChanged(stack)`；正常完成目标请使用 `visitBiome` / `visitStructure`。任意原地 NBT 编辑若不通知版本变化，不保证马上被已有缓存识别。

`general.maxAutomaticEffectActionsPerPlayerPerSecond` 默认 0，保持不限制；例如设为 100，表示每名玩家任意连续 20 游戏刻内最多执行 100 次自动效果尝试（20 TPS 时约一秒，低 TPS 下按游戏时间计算）。

- 远望见证的负面效果移除尝试、随机正面效果添加，以及幽谷见证成功攻击后的迅捷和随机增益，共享上限。
- 即使移除失败、添加被拒绝或事件被取消，也占一次额度；进入事件之前扣除，嵌套触发不能抢用同一额度。
- 超额尝试直接跳过，不排队、不补发，空闲时不累计额外额度；跨整数秒边界也不能连续获得双倍额度。
- 不限制受击恶意、视觉剥夺、僵硬惩罚、幽谷黑暗/失明免疫，以及其他模组自主添加的效果。
- 调低上限立即生效；登出、死亡、重生、换维度和日记刷新会清除玩家运行时计数。关闭限流后恢复原操作数量。
- 启动或重载时记录已启用上限；远望配置的理论尝试峰值可能超过额度时警告。多个来源共享额度，没有为各来源单独保留份额。
- 保持 0 可完全沿用旧配置频率；希望限制极端配置时可从 100 开始调整。该配置会限制游戏效果的实际尝试次数，而不只是内部优化。

## 客户端画质

文件：`config/the_long_travail-client.toml`。

```toml
nameHaloQuality = "HIGH"
```

HIGH 保持 16 次外围采样；MEDIUM 为 8 次；LOW 为 4 次；OFF 关闭外围光晕。四档都保留正文材质效果。低档按透明度叠加关系调整单次采样透明度，使亮度尽量接近，但空间分布和柔和度会有区别。修改后重启客户端可确保生效。

## 同步与视觉状态

配置同步解码限制：128 个数值键、32 个池、每池 2048 条、合计 8192 条；键长 128 字符、条目长 1024 字符，整包预算 1 MiB。拒绝负数长度、重复键、非有限数字、截断与多余数据。完整校验后才更新客户端快照。

服务端发送前使用相同约束；超限时日志指出具体池或值，跳过该次同步，不发送半份配置。请修正配置并重新加载；在此之前客户端可能仍使用旧值或本地回退值。

视觉剥夺的清理统一经过 `clear(player, reason)`：命令、牛奶、到期、死亡、重生、换维度、日记刷新等路径携带原因并执行相同清理与客户端同步。不添加 HUD，不通过反复补加药水图标与其他模组争夺状态。

## 验证命令

```powershell
.\gradlew.bat --offline -I tools/review.init.gradle reviewTimeline reviewPerformance reviewConfigSafety reviewShader
.\gradlew.bat --offline -I tools/review.init.gradle runServer
.\gradlew.bat --offline build jarJar
```

`runServer` 使用 `build/review-server` 隔离测试世界，测试结束自动停止。必须检查日志中的 `TRAVAIL_CONFIG_COMBAT_PASS`、`TRAVAIL_REWARDS_PASS` 与 `TRAVAIL_SMOKE_PASS`；旧测试入口会捕获断言，因此不能只看 Gradle 退出码。

## 正面效果获得提示音

`config/the_long_travail-common.toml` 的 `[general]` 中，`muteBeneficialEffectGainSounds = true` 默认开启。

佩戴苦旅并拥有穷遐或幽谷任意见证时，静默 Sounds 播放的所有正面效果获得提示音，包括其他饰品、药水、指令或模组给予的正面效果，不要求来自苦旅的效果池。按效果的 `isBeneficial()` 分类，负面/中性效果及效果消失不受这条新增规则影响；原有精确静音规则仍然生效。仅放在背包中的苦旅无效，强制恶意/见证遵循现有状态优先级。

配置由服务端同步；关闭后恢复原有精确音效策略。播放前读取当前佩戴状态和配置，避免短暂排队期间摘下饰品或修改配置后仍沿用旧状态。

此规则接入当前整合包 Sounds 2.2.1 的效果通知，不拦截饮用/装备操作声，也不拦截其他模组独立播放、无法关联到效果类别的自定义声音。未安装 Sounds 或兼容注入结构不匹配时不启用该适配。
