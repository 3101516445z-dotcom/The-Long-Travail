# 高频战栗隔离检查

辅助模组仅由 `tremor.init.gradle` 加入隔离运行环境，正式 JAR 不包含测试方块或 Mixin。

## 开发环境

使用项目要求的 JDK 17，在项目根目录运行：

```text
gradlew.bat -I tools/structure-regression/tremor.init.gradle runServer --offline --console=plain
```

默认目录为 `build/tremor-server`；检查默认配置时用 `-PtremorDirectory=build/<新的测试目录>`，避免旧测试配置影响断言。运行前读取 `run/eula.txt`。`tremor-result.txt` 必须以 `PASS ` 开头，Gradle 才成功。

- `-PtremorPressure`：分 tick 执行高次数效果检查，输出 `pressure-result.json`
- `-PtremorFtbMods=<依赖目录>`：加载 FTB Ultimine、FTB Library、FTB Teams 和 Architectury
- `-PtremorExtras=<逗号分隔的模组名>`：额外兼容环境依赖
- `-PtremorDirectory=<隔离目录>`：指定运行目录，只能选择专用测试目录
- `-PtremorTargeted`：执行目标兼容组合；`-PtremorRestart` 检查同一测试世界的重启恢复

配置用例临时修改隔离服务器的托管配置，并在 finally 中恢复原始字节。故障方块、破坏账本插桩和压测行为仅注册在辅助模组中。

## 正式包运行入口

先运行 `gradlew.bat -I tools/structure-regression/tremor.init.gradle tremorTestJar reobfTremorTestJar jarJar reobfJarJar --offline`，再运行：

```text
node tools/structure-regression/run-tremor-release.mjs <variant> <runId> [配置种子运行目录]
```

variant 为 `core`、`core-boundaries`、`ftb`、`ftb-boundaries`、`chunks`、`storage` 或 `lootr`；runId 使用字母、数字、下划线或连字符，每次选择新的标识。脚本创建 `build/tremor-release-<variant>-<runId>`，从种子目录复制配置；core 模式还检查五次重启。

依赖由环境变量提供：

- `TREMOR_JAVA`：Java 可执行文件，默认取 JAVA_HOME/bin/java.exe 或 PATH 中的 java
- `TREMOR_LIBRARIES`：已准备的 Forge 1.20.1-47.4.10 服务器 libraries 目录，默认 build/tremor-release-verification/libraries
- `TREMOR_MODS`：只读依赖 JAR 目录，默认整合包 mods；各依赖必须恰有一个匹配文件

该入口使用 Windows junction 和 Forge 的 win_args.txt，要求 Windows。脚本不会安装 Forge，也不会修改依赖来源目录。每次运行记录输入哈希、配置来源、Java 路径、结果和日志；进程由 process-guard.mjs 限时管理。
