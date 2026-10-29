# 运行时异常保护

## 古今语怪附魔转移

`EnchantmentTransfer.evaluate` 在副本上计算完整结果，捕获 `isCurse`、`canEnchant`、`isCompatibleWith` 调用中的 `RuntimeException`。任何一次失败都会取消整个转移，不返回部分结果；日志包含附魔注册名、阶段与堆栈。目标适用性检查收到物品副本。

`EnchantmentTransferRecipe` 按合成格输入缓存计划，让匹配、预览和返还使用同一次计算；输入变化或标签更新后重新计算。蜂蜜瓶的返还固定为玻璃瓶。

`TransferNotices` 在服务端以快捷栏提示说明失败。同一菜单中的同一失败输入只提示一次。原版背包和工作台的结果点击、快捷转移会在消耗前拦截失败预览并清空结果。第三方容器直接扣减输入时，需要在自身领取入口处理失败与返还。

## 液化丰饶

`AbundanceEffectPool` 分别隔离药水读取失败、空样本、空效果和效果回调失败。普通效果缺少有效样本时仍使用配置的默认时长；自身回调异常的效果被排除。构建完成后一次发布不可变效果池及其诊断状态，避免读取半成品。

部分条目失效但仍有可用效果时不提示玩家。有异常记录的空池与无异常记录的空池使用不同提示；无法确定原因的整体重建失败使用中性提示。抽样及药水准备发生异常时，在交付与扣除物品前取消操作。有效数据的抽样层级、随机调用次数和正常使用时序保持不变。

## 伊卡洛斯

`IcarusWingTexture` 在读取像素前验证 64×32 RGBA 格式。资源包贴图无法使用时，从模组自身读取默认 PNG，并注册独立动态贴图，使外部资源包无法再次覆盖回退内容。内置 PNG 也无法读取时使用缺失贴图与矩形轮廓兜底。

缓存和日志以资源重载为周期；重载释放动态贴图并使所有翅膀模型在下一次绘制时重建。该视觉降级只记日志，不打断玩家操作。

## 验证入口

- `tools/structure-regression/tales.init.gradle`：正常转移、三类异常回调、失败输入缓存、失效预览与材料保留
- `tools/structure-regression/abundance.init.gradle`：正常抽样与使用、异常数据隔离、药水准备失败、空池保留材料与重载恢复
- `tools/structure-regression/icarus-hardening.init.gradle`：隔离客户端验证尺寸/格式拒绝、损坏资源包回退、缓存和修复后资源重载

运行命令为 `gradlew.bat -I <入口文件> runServer --offline --console=plain`；伊卡洛斯入口改用 `runClient`。
