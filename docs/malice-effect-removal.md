# 恶意剥离：直接移除与到期兜底

恶意随机剥离一个、剥离全部正面效果，统一通过 `EffectChanges.remove(player, effect, true)` 执行：

1. 调用 `removeEffectNoUpdate`，绕过可取消的 `MobEffectEvent.Remove` 事件。
2. 取到已移除的实例后，通过 Mixin Invoker 调用原版 `onEffectRemoved`，保留属性撤销、效果状态刷新及服务端玩家的移除包同步。
3. 检查玩家当前是否仍有该效果。若仍存在，只对当前实例设置一次 `duration = 1`，并清空 `hiddenEffect`，防止较弱的隐藏效果接替。无限时长同样改为 1 tick。
4. 同步缩短后的时长。兜底分支不会立即宣告剥离成功；确认效果到期消失后，才发送恶意剥离提示。

每次调用仅尝试一次直接移除，不循环强删。已有正面效果筛选、随机选择和触发概率不变；见证净化及其他非恶意清理仍允许其他模组取消。没有新增配置项。

第一层适用于“锁定”等取消移除事件的保护。第二层用于第一层被更底层干预、或收尾过程中效果重新出现的情况。这不是禁止重新获取效果：以后再次施加的效果仍可生效；如果其他模组连自然到期也取消或冻结计时，兜底仍可能受阻。

测试入口：`tools/smoke/MaliceRemovalSmoke.java`。带真实更多药水效果模组运行：`gradlew --offline -I tools/review.init.gradle -PreviewMpe runServer`（需先将被测 JAR 复制到 `build/mpe-compat-libs/more_potion_effects-2.6.0-forge-1.20.1.jar`）。测试素材不进入发布 JAR。
