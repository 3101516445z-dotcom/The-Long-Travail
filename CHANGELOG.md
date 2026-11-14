# 兼容迁移说明

本页汇总接入当前源码时需要调整的接口、数据与协议。具体约定见[开发文档](docs/README.md)。

| 接入位置 | 当前要求 |
| --- | --- |
| 联动预设记录 | 主题文件使用独立的 `presets` 一级分组。旧 `compatInitialization` 记录自动迁入，新旧位置同时有值时采用新位置；脚本读写路径须同步更新，见[预设路径](docs/开发者查阅/runtime-integration.md#联动预设写入)。 |
| 网络与物品注册 | 协议为 25-items- 加物品注册签名，两端精确匹配。读取可选 RegistryObject 前检查 isPresent()，注册配置修改后重启两端。 |
| 效果植入 | EffectInsertion.addOrGet 的四参数重载将实例调整与同步放入完成回调，失败时恢复本效果及标准实体状态；消耗放在成功返回之后。三参数入口保留，返回后的操作由调用方负责，见[恢复范围](docs/开发者查阅/runtime-integration.md#可复用的效果植入工具)。 |
| 携带区域 | CarryingApi.allowed 的区域名限于六种内置来源，未知名称或 null 抛出 IllegalArgumentException；合法区域的开关含义不变，见[携带 API](docs/开发者查阅/carrying-api.md)。 |
| 旅程写入 | 使用 TravailJourneyApi；延迟授予见证携带发起时的 AspectJourneyKey。主题重置会改变轮次，旧身份不能写入新轮次。 |
| 直击伤害 | 新来源使用 direct_strike。don_quixote_strike、rose_retaliation 旧键保留供既有数据包引用，TLT 新伤害使用统一来源工厂。 |
| 籠釣瓶配方 | 使用 minecraft:crafting_shaped 与 the_long_travail:cursed_book 材料；旧自定义配方类型已移除。 |
| 六主题物品 | 新增六项注册签名；圣愚共享战利品表的旧默认配置自动升级，自定义表保持原值。详见[六主题物品接入](docs/开发者查阅/六主题物品.md)。 |
| 磷叶石治疗 | 触发模式、冷却与排除伤害类型独立位于 `aspects/deep_valley.toml` 的 `items.phosphophyllite`；日记受击配置仅控制日记。两项提示分别同步各自的服务端触发模式。 |
| 磷叶石交易 | 大师级石匠以 9 个回响碎片和 1 个紫水晶簇兑换 1 个磷叶石。已有交易自动更新材料，保留交易次数与售罄状态。 |
| 液化丰饶数据 | 当前合成产出六份，使用紫水晶碎片、牛奶桶、下界疣和六个玻璃瓶；酿造使用龙息与紫水晶碎片。 |
| RSI 统一磁盘 | 按 1.5.3 的直接 API 和字段桥接接入；物品与流体数量使用 long，准备及提交均检查备份状态。 |
| 战斗属性显示 | Apothic Attributes 百分比属性通过隔离的直接 API 构造，缺席或失败时使用 RangedAttribute。 |

迁移涉及物品 NBT、主题身份或外部存储时，保留原物品身份与各自生命周期。旅程保存格式不支持降级，客户端和服务端使用相同构建及注册设置。
