# 发布检查修正

- Minecraft 依赖版本使用精确范围 `[1.20.1]`，与构建和验证版本一致。
- 资源检查删除已移除的 `.summary` 要求；检查实际使用的 `.effect` 文本，允许项目符号后的普通空格或不换行空格。保留双语键、参数序号、提示文本、字体及透明纹理检查。
- 冒烟测试写出明确结果，`tools/review.init.gradle` 在启动前重置为 `NOT_RUN`，进程结束后只接受 `PASS`。断言失败仍打印堆栈并正常关闭服务器、保存世界，随后 Gradle 任务失败；缺失或未完成结果也不会误报成功。测试夹具不进入发布包。
- 音效回调返回是否执行了播放，仅返回 true 时增加 `play-gain`、`play-loss` 或 `overflow-play`。见证静音仅记录静音原因，延迟队列及溢出分支都遵循此规则，静音条件不变。

验证入口：

```powershell
node tools/verify-diary-resources.cjs
.\gradlew.bat --offline -I tools/review.init.gradle reviewSound reviewTimeline reviewPerformance reviewConfigSafety runServer jarJar
```

故意失败验证入口（预期退出码非零）：

```powershell
.\gradlew.bat --offline -I tools/review.init.gradle -PreviewSmokeFailure runServer
```

验证日志：`build/general-fixes-review.log`、`build/smoke-failure-verification.log`。故意失败场景检查断言、世界保存和 `runServer FAILED`，避免把启动配置失败误当作测试退出机制通过。音效测试核对正常播放、消失通知、1000 次溢出通知及静音后的计数；隔离服务端测试覆盖实际见证规则和卸下装备后的恢复。
