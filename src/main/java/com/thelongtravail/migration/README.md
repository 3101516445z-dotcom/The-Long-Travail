# TLT 版本兼容

本目录用于 TLT 新版本读取或迁移旧版本数据。跨模组联动位于同级的 `compat/`。

## 配置迁移

入口为 `config/ConfigUpgrade.java`，按配置生命周期调用。

| 入口 | 调用时机 | 已有规则 |
| --- | --- | --- |
| `witnessRewards` | 主题配置加载前 | 在主题文件之间迁移钓鱼奖励和通用饰品栏配置 |
| `upgradeGeneral` | 恢复流程读取 general.toml 后 | 更新旧版默认战利品表，保留自定义条目 |
| `beforeValidation` | 按现行配置规范校验前 | 迁移旧预设分组，将旧环境总峰值换算为额外增量 |

配置读写、校验和备份由 `config/storage/` 负责。

## 其他已有兼容读取

旧旅程 NBT 格式由 `journey/JourneyState.java` 识别，旧群系名称由 `journey/RequirementNames.java` 解析，旧见证账本由 `underworld/UnderworldLedger.java` 读取。这些逻辑在读取对应数据时执行。
