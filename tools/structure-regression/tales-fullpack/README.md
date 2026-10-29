# 古今语怪整包压力测试

使用已有整包环境的独立副本，不修改原环境或正式存档。测试探针只打入独立 fixture JAR。

```powershell
# 预先将 JAVA_HOME 指向本机 JDK 17
.\gradlew.bat -I tools/structure-regression/tales-fullpack.init.gradle talesFullpackJar reobfTalesFullpackJar --offline --no-daemon
python -X utf8 tools/structure-regression/tales-fullpack.py --pack build/fullpack-baseline --runtime ../.. --output build/tales-fullpack-run --java "$env:JAVA_HOME\bin\java.exe"
```

`--pack` 指定既有整包配置与模组环境；`--runtime` 指向包含启动 JSON、客户端 JAR 和 natives 的 PCL 版本目录。输出必须是工程 build 下的新目录。沿用基线的启用模组选择，只把 TLT 换为当前发布构建，并替换旧磷叶石测试探针。测试世界来自隔离的 `build/tales-server/tales`，不复制玩家正式存档。

客户端自动进入测试世界，在集成服务器中分批完成：

- 100,000 次转移计算，遍历整包注册附魔，检查筛选、等级守恒、副本与 NBT 往返。
- 10,000 次真实工作台操作，包含双向、三个筛选模式、Shift 点击、堆叠来源、满背包与掉落剩余物。
- 1,000 轮铁砧检查、100 次真实附魔台操作、1,000 次书架漏斗插入与取出。
- 写入 255 级附魔书到测试箱子，正常退出；随后可用同样命令追加 `--resume --persistence-only` 进行完整 JVM 重启读回检查。

再次启动前保存首次的 `console.log`、`client-result.txt`、`process.json`、`exit.json` 和 `source-integrity.json`，避免覆盖证据。脚本保留 20 分钟子进程超时保护，只终止本次启动的进程。

结果见输出目录的 `server-stress-result.txt`、`client-result.txt`、`tales-enchantments.txt`、`tales-loaded-mods.txt` 和 `source-integrity.json`。计时只衡量测试批次，不代表整个整包的 TPS/FPS；四个 FakePlayer 也不代表真实多人联机验收。
