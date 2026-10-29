# 古今语怪隔离回归

从项目根目录，使用 JDK 17 运行：

```powershell
# 预先将 JAVA_HOME 指向本机 JDK 17
.\gradlew.bat -I tools/structure-regression/tales.init.gradle runServer --offline --no-daemon
```

复用项目 `run/eula.txt`，仅在 `build/tales-server` 建立测试世界和服务器配置，监听本机 25749 端口。测试完成后服务器自行退出；Gradle 根据 `tales-result.txt` 决定成功或失败。

覆盖纯计算、附魔书降级与清空、非法数据、六种配方、真实菜单取出、Shift 点击与堆叠剩余物、铁砧、与普通书同种子的附魔台选择、真实附魔动作以及书架存档读写。此回归不替代客户端显示、完整模组包、多人和漏斗自动化验收。

高等级测试数据用 `EnchantmentHelper.setEnchantments` 创建。不要用 1.20.1 的 `ItemStack.enchant` 创建超过 127 的测试等级，其内部字节转换会导致负等级输入。
