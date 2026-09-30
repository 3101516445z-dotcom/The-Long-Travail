# 独立视觉剥夺

服务端独立状态仍决定触发、持续时间和解除。客户端不再借用原版黑暗亮度、脉动、夜视抑制或天空隐藏；真正的黑暗、失明与夜视照常运行。没有新增 HUD 图标。

世界与手持物渲染完成后、HUD 和菜单之前，以一次全屏混合绘制实现整体变暗与柔和椭圆边缘遮蔽。无需复制画面，不修改 FOV，无模糊或镜头抖动；无视觉强度时跳过绘制。

距离遮蔽使用 Forge 雾适配，不读取 Oculus 内部深度纹理。它只收紧已有雾范围，不放宽水、岩浆或其他模组更严格的雾。光影包可能自行计算雾，因此距离部分的表现取决于光影包；独立屏幕遮蔽与距离适配可分别调节。

## 动画规则

- 渐入、渐出使用 smoothstep。两段时长之和超过持续时间时，按比例缩短，取消旧的 40% 上限。
- 重复触发沿用当前强度与波动相位；渐出中再次触发按 refreshBlendTicks 回升。
- 牛奶、命令和 API 清理立即结束逻辑状态，画面按 clearFadeOutTicks 退场。重复清理不会延长退场。
- 死亡、重生、切换维度、重置日记、登出或玩家对象变化立即清空。
- 视觉参数同步后用 10 刻平滑切换；渐变时长在下一次触发或解除时读取。
- 单人暂停时冻结动画。波动默认关闭；客户端可独立关闭波动。

## 配置

common 配置新增 `[visualDeprivation]`：

| 键 | 默认值 | 含义 |
|---|---:|---|
| worldDarkening | 0.65 | 整体变暗比例，0–1 |
| peripheralOpacity | 0.98 | 外围附加黑色遮蔽比例，0–1 |
| clearRadius | 0.45 | 中心无额外围遮蔽区域的半径；1 到达横纵屏幕边缘 |
| edgeSoftness | 0.45 | 边缘从透明过渡到最浓的宽度 |
| distanceVeilEnabled | true | 是否使用距离雾适配 |
| distanceVeilStart | 1.25 | 最浓时雾开始的距离，单位为方块 |
| distanceVeilEnd | 5.0 | 最浓时雾结束的距离；开始距离会自动限制在它以内 |
| pulsePeriodSeconds | 4.0 | 一个波动周期的秒数 |
| pulseDepth | 0.0 | 波动深度；0 无波动，0.2 在 80%–100% 强度之间波动 |
| pulseAffectsRadius | false | 波动减弱时是否同步放大中心区域 |
| refreshBlendTicks | 5 | 渐出中再次触发的回升刻数 |
| clearFadeOutTicks | 4 | 主动解除后的视觉退场刻数 |

client 配置新增 `reduceVisualMotion = false`；设为 true 后，本客户端保持恒定强度，但保留正常渐入和渐出。

原 `deepValley.visualDeprivationChanceOnHit`、`visualDeprivationDurationSeconds`、`visualDeprivationFadeInTicks`、`visualDeprivationFadeOutTicks` 继续有效。旧 `visualDeprivationIntensity` 与 `visualDeprivationPulseFloor` 一度保留兼容但从不参与计算，现已连同配置项一起删除；变暗强度改用 `worldDarkening`，波动改用 `pulseDepth`，旧 gamma 强度不能直接按数值换算为新的遮蔽比例。

网络协议升级到 8；多人游戏需要客户端和服务端同时更新此模组。

## 本次验证（2026-09-29）

- 动画回归：玩家绑定、短时长比例压缩、渐出中续期的连续性、主动解除、重复清理、立即清空、自然到期、登出、波动范围与减少动态选项。
- 原有性能和配置安全回归通过；隔离专用服务器的全部 smoke 测试通过，包含视觉状态包、清理、到期与重新登录。
- Forge 47.4.20 原版渲染客户端实测：世界和手持物变暗，HUD 保持正常，解除后恢复。
- Oculus 1.8.0 + Embeddium 0.3.31 + Complementary Reimagined 5.8.1 实测：独立遮蔽、渐入、解除、隐藏 HUD、背包界面均正常。该光影包没有采用本模组的距离雾，因此中心仍能看到远景；本版本不保证光影下的 5 格距离限制。
- 客户端测试只使用 build/visual-client 内的隔离存档和配置。截图位于该目录的 vanilla-screenshots、oculus-screenshots 与 screenshots。
- 测试用源码与启动脚本位于 tools/visual-client、tools/visual.init.gradle，不包含在发布 JAR 中。

构建日志：build/visual-final-build.log；服务端日志：build/visual-server-review.log；最终光影实测日志：build/visual-oculus-final.log。
