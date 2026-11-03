# 古今语怪隔离回归

从项目根目录，使用 JDK 17 运行：

```powershell
# 预先将 JAVA_HOME 指向本机 JDK 17
.\gradlew.bat -I tools/structure-regression/tales.init.gradle runServer --offline --no-daemon
```

复用项目 `run/eula.txt`，仅在 `build/tales-server` 建立测试世界和服务器配置，监听本机 25749 端口。测试完成后服务器自行退出；Gradle 根据 `tales-result.txt` 决定成功或失败。

覆盖纯计算、附魔书降级与清空、非法数据、六种配方、真实菜单取出、Shift 点击与堆叠剩余物、铁砧、当前附魔系统的候选选择、真实附魔动作以及书架存档读写。此回归不替代客户端显示、完整模组包、多人和漏斗自动化验收。

高等级测试数据用 `EnchantmentHelper.setEnchantments` 创建。不要用 1.20.1 的 `ItemStack.enchant` 创建超过 127 的测试等级，其内部字节转换会导致负等级输入。


附魔设计回归还执行 100,000 次注册附魔转移、10,000 次候选抽取，以及各 1,000 次实际附魔台、铁砧和合成操作。附魔台通过方块的菜单提供器创建，不能手动创建原版菜单来代替模组菜单。

将神化及其依赖放入 `build/apotheosis-tales/deps` 后，可运行：

```powershell
.\gradlew.bat -I tools/structure-regression/tales.init.gradle -PtalesApotheosis runServer --offline --console=plain
```

该组使用 `build/tales-apotheosis-server`。结果分别写入 `tales-result.txt`、`tales-pressure-result.txt` 和 `apotheosis-overlap-result.txt`。亡灵视角或剑与灯联动不符合预期时，测试抛出断言并使任务失败。
神化会替换部分附魔注册对象，测试须在注册完成后读取，不能用测试类初始化时捕获的旧对象。普通附魔书的 NBT 追加行为与独立物品不同，铁砧等级对照使用普通装备。


一千年后回归：`ApotheosisOverlapProbe` 使用真实 `EnchantmentHelper` 命中入口，覆盖1万次节肢杀手回调、1万轮局部视角嵌套与异常清理、1000轮自定义附魔异常和防御回调隔离，并核对剑与灯见证前后差值等于当前亡灵加成配置。分别在基线和 `-PtalesApotheosis` 环境运行。类型判断的适用范围见项目书中的“一千年后与冥府共用入口”。

嵌套生物攻击回归在攻击附魔回调内调用真实 `Mob.doHurtTarget`，比较携带亡灵杀手的生物对蜘蛛的伤害输入与回调外对照，并检查正常返回、异常退出及外层视角恢复。目标夹具仅截获 `hurt` 输入，避免外部暴击、护甲和无敌帧影响入口隔离检查。


成功命中与女仆回归：使用真实伤害处理，检查生命损失、内层附魔回调次数、作用域恢复以及单独节肢杀手的缓慢效果。女仆组使用真实实体、原生饰品栏、在线主人索引与冥府见证，分别检查装备与卸下“一千年后”的状态。神化的亡灵杀手也会施加缓慢，因此效果探针不同时附加亡灵杀手。

加载项目配置的女仆API对应运行依赖：

```powershell
.\gradlew.bat --offline -I tools/structure-regression/tales.init.gradle -PtalesMaid runServer --console=plain
.\gradlew.bat --offline -I tools/structure-regression/tales.init.gradle -PtalesMaid -PtalesApotheosis runServer --console=plain
```

未指定 `-PtalesMaid` 时保留普通生物成功命中用例，未加载女仆时跳过女仆组。在线主人仅由夹具登记在服务端玩家索引中，不建立真实网络连接。
