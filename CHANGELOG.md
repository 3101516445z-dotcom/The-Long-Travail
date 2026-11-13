# 兼容迁移说明

本页汇总接入当前源码时需要调整的接口、数据与协议。具体约定见[开发文档](docs/README.md)。

| 接入位置 | 当前要求 |
| --- | --- |
| 网络与物品注册 | 协议为 25-items- 加物品注册签名，两端精确匹配。读取可选 RegistryObject 前检查 isPresent()，注册配置修改后重启两端。 |
| 效果植入 | 显式强制植入使用 EffectInsertion.addOrGet，完成业务更新后调用 synchronize；叠层及专用客户端同步由调用方负责，见运行时接入文档。 |
| 旅程写入 | 使用 TravailJourneyApi；延迟授予见证携带发起时的 AspectJourneyKey。主题重置会改变轮次，旧身份不能写入新轮次。 |
| 直击伤害 | 新来源使用 direct_strike。don_quixote_strike、rose_retaliation 旧键保留供既有数据包引用，TLT 新伤害使用统一来源工厂。 |
| 籠釣瓶配方 | 使用 minecraft:crafting_shaped 与 the_long_travail:cursed_book 材料；旧自定义配方类型已移除。 |
| 六主题物品 | 新增六项注册签名；圣愚共享战利品表的旧默认配置自动升级，自定义表保持原值。详见[六主题物品接入](docs/开发者查阅/六主题物品.md)。 |
| 磷叶石治疗 | 触发模式、冷却与排除伤害类型独立位于 `aspects/deep_valley.toml` 的 `items.phosphophyllite`；日记受击配置仅控制日记。两项提示分别同步各自的服务端触发模式。 |
| 液化丰饶数据 | 当前合成产出六份，使用紫水晶碎片、牛奶桶、下界疣和六个玻璃瓶；酿造使用龙息与紫水晶碎片。 |
| RSI 统一磁盘 | 按 1.5.3 的直接 API 和字段桥接接入；物品与流体数量使用 long，准备及提交均检查备份状态。 |
| 战斗属性显示 | Apothic Attributes 百分比属性通过隔离的直接 API 构造，缺席或失败时使用 RangedAttribute。 |

迁移涉及物品 NBT、主题身份或外部存储时，保留原物品身份与各自生命周期。旅程保存格式不支持降级，客户端和服务端使用相同构建及注册设置。
