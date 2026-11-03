# 直接 API 兼容回归

正式代码将六类可选兼容改为直接调用，测试夹具可以使用反射准备状态或对照旧调用结果。

## 编译依赖

`direct-compat-api.json` 记录固定 API JAR 的 SHA-256；依赖不提交、不内嵌，不自动成为运行依赖。准备脚本只读取指定目录内的 JAR，支持从万法皆通 all.jar 提取内部模组。

```powershell
python tools/structure-regression/prepare-direct-compat.py --search <模组目录> --search <额外API目录>
.\gradlew.bat build --offline --no-daemon
```

缺少的固定 API 会集中列出，脚本不会用其他版本静默替代。RSI 固定使用 1.5.3 API，旧版适配已移除。可用 `-PcompatApiDirectory=<目录>` 指定非默认目录。此目录中的文件名须为锁文件键名加 `-local.jar`。

## 检查入口

- `direct-compat.init.gradle`：`-PdirectProfile=absent` 检查可选依赖全部缺席；`small` 检查 AttributesLib、FTB Teams 和墓碑。包含真实 API 对照、受控队伍故障、重复读取和源物品不变检查。
- `direct-maid-health.init.gradle`：生命值修饰器移除专项压力测试，每轮执行 20,000 次；加 `-PmaidSpellSmoke` 加载万法皆通。
- `direct-maid.init.gradle`：复用女仆结构回归及压力用例；加 `-PmaidSpellSmoke` 加载万法皆通。
- `direct-fluid.init.gradle`：复用外部存储矩阵和物质球压力用例。`-Pmatrix=11` 加载所有存储适配；加 `-PstressRestart` 在独立进程回读持久化结果；`-PmatrixOnly` 运行常规组合矩阵。
- `tremor.init.gradle`：原连锁挖掘回归，使用 `-PtremorPressure` 开启压力用例。

以上通过 `gradlew.bat -I <入口> <属性> runServer --offline --no-daemon` 执行，使用独立测试世界。运行后以结果文件及进程退出码共同判定，不能仅以服务端成功启动作为通过。

`rsi-new` 是当前安装版不兼容拒绝的专项检查，另需将目标 JAR 以 `rsicurrent-local.jar` 放入 `build/reflection-removal/api`，不会替换编译使用的旧 API。该测试不代表新版统一磁盘已获复制支持。

RSI 1.5.3 专项：`direct-fluid.init.gradle -Pmatrix=11 -Prsi153 -PdirectRun=rsi153-special`，首次执行检查长整数库存、三类魔法资源、备份拒绝和回滚；第二次在同一目录启动验证持久化读取。

## 发布环境验证

`direct-release.init.gradle` 将 direct、maid、fluid 三套夹具构建为独立的重混淆测试 JAR。使用 `reobfDirectAcceptanceJar`、`reobfMaidAcceptanceJar`、`reobfFluidAcceptanceJar` 生成；夹具不进入正式 TLT JAR。将一套测试 JAR、正式 TLT JAR、Curios 和该场景依赖放入隔离 Forge 1.20.1 服务端，按各套结果文件检查。

女仆堂吉诃德追加伤害断言按当前 `don_quixote.trueFraction` 计算，目标最大生命为 1000 时默认追加 40；旧测试的固定 20 已不符合当前默认值。连锁挖掘压力报告中的人为移除后掉落异常仍需保留损失记录：原版对照同样丢失掉落，作用域恢复通过不代表方块破坏具有原子回滚能力。
