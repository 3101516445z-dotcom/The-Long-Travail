# 配置说明

服务端与单人游戏的主要配置文件为：

```text
config/the_long_travail-common.toml
```

修改后请保存文件，并重新启动世界或服务器以确保配置生效。配置文件中的每个选项都带有中英文注释。

## 通用设置

`[general]` 包含首次进入世界时是否赠送“苦旅”、每组旅程抽取的群系与结构数量，以及自动效果频率限制等设置。

```toml
[general]
receivedMaliceTrigger = "ATTACK_ATTEMPT"
receivedMaliceCooldownTicks = 0
receivedMaliceExcludedDamageTypes = []
maxAutomaticEffectActionsPerPlayerPerSecond = 0
```

- `receivedMaliceTrigger`：受击恶意的触发方式。`ATTACK_ATTEMPT` 按攻击尝试触发，`HEALTH_LOSS` 仅在生命值实际下降时触发。
- `receivedMaliceCooldownTicks`：受击恶意的共享冷却，20 游戏刻约为一秒；`0` 表示不限制。
- `receivedMaliceExcludedDamageTypes`：不会触发受击恶意的伤害类型 ID 列表。
- `maxAutomaticEffectActionsPerPlayerPerSecond`：每名玩家每秒允许的自动效果尝试次数；`0` 表示不限制。

## 六组旅程设置

以下分组分别控制六组恶意与见证：

- `[flourishing]`：繁茂
- `[abyss]`：归墟
- `[farReach]`：穷遐
- `[deepValley]`：幽谷
- `[underworld]`：冥府
- `[boundless]`：无垠

每组可以调整效果数值、群系池、结构池和权重。修改基础池只影响之后首次初始化的苦旅，已经生成的旅程会继续保留原有条件。

常见配置格式：

```text
群系：群系ID|显示名称|权重
结构：结构ID|显示名称|权重
正面效果：效果ID|最高等级|权重
普通权重实体：实体ID|权重
奖励物品：物品ID|权重|最小数量|最大数量
```

概率通常使用 `0` 到 `1` 的小数，例如 `0.5` 表示 50%。

## 流体伤害模式

`[abyss]` 中的 `fluidErosionProtectionMode` 支持：

- `ENFORCED`：采用苦旅计算的流体伤害下限。
- `RESPECT_CANCELLATION`：尊重被其他机制取消的伤害事件。
- `STANDARD`：采用普通伤害事件处理方式。

## 奖励预算

`[rewards]` 可以限制额外奖励实体的生成速度和队列容量，避免极端配置一次生成过多物品。

```toml
[rewards]
rewardMaxItemEntitiesPerTick = 32
rewardQueueCapacity = 1024
rewardOverflowPolicy = "DEFER"
rewardMaxEntitiesPerReward = 64
```

- `DEFER`：将奖励分批发放。
- `LIMIT`：限制单次奖励产生的物品实体数量。

## 客户端画质

客户端配置文件为：

```text
config/the_long_travail-client.toml
```

```toml
nameHaloQuality = "HIGH"
```

可选值为 `HIGH`、`MEDIUM`、`LOW` 和 `OFF`。降低该选项可以减少名称光晕的绘制开销；`OFF` 只关闭外围光晕，不影响正文显示。

## 正面效果提示音

`[general]` 中的 `muteBeneficialEffectGainSounds` 用于控制兼容 Sounds 模组时的正面效果获得提示音：

```toml
muteBeneficialEffectGainSounds = true
```

未安装 Sounds 时，此选项不会影响游戏。
