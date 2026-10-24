# 联动探索预设实机回归

`targets.json` 记录期望结构及主题。每个目标模组独立启动，加载该模组、必需前置、Curios、GeckoLib、TLT 和测试夹具；不装其他可选玩法模组。

```powershell
./gradlew.bat jarJar reobfWorldgenPresetsSmokeJar -I tools/worldgen-presets/test.init.gradle --offline --no-daemon
node tools/worldgen-presets/run.mjs
```

运行器从整合包 `../../mods` 复制原始 JAR 并记录 SHA-256；使用已有 `build/release-validation/libraries` Forge 47.4.10 库和 `run/eula.txt`。Java 默认从 `JAVA_HOME` 选择，未配置时使用本机 JDK 21。所有世界、日志和报告写入新的 `build/worldgen-presets-时间戳`，不修改整合包安装文件。

可用 `WORLDGEN_ONLY` 指定一个命名空间，`WORLDGEN_TEST_ROOT` 指定独立输出目录。不要对已运行的世界重用目录作为“新配置”测试。

最终打包后，设置 `WORLDGEN_RELOAD_ONLY=1` 并提供上一轮 `WORLDGEN_TEST_ROOT`，可用最终 JAR 复查保存的坐标；结果单独写入 `final-report.json`，保留首次生成和重载报告。

种子 12345：检查配置补入、主题、显示键、权重、去重与抽取数；定位自然结构，检查首/中/末片段多个高度的 JourneySpatial 判定、缓存命中和边界外反例；另起 JVM 读取同一世界复查。Alex 检查六个自然群系的实际位置与读档保持。没有用 /place 替代自然生成。

每个生成或重载阶段最长 20 分钟，超时仅终止本运行器拥有的进程树。`report.json` 同时要求进程正常退出、夹具 PASS、原始及复制 JAR 哈希不变；失败日志保留。

覆盖的是固定种子的抽样到访检测，不是全种子保证、多人压力或完整客户端验收。`structure-presets` 旧注册表夹具仅验证配置机制，不能充当这些真实模组的生成证据。
