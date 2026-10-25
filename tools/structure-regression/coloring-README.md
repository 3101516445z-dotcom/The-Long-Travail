# 施色隔离验证

运行环境：Minecraft 1.20.1、Forge 47.4.10、Java 17 编译目标。使用工程现有 `run/eula.txt`，仅监听 `127.0.0.1:25649`。各运行串行执行，保留 build 下的环境和日志，不使用正式整合包存档。

```
gradlew.bat -I tools/structure-regression/coloring.init.gradle runServer --offline
gradlew.bat -I tools/structure-regression/coloring.init.gradle -PcoloringApotheosis runServer --offline
gradlew.bat -I tools/structure-regression/coloring.init.gradle -PcoloringApotheosis -PcoloringExtras runServer --offline
gradlew.bat -I tools/structure-regression/coloring.init.gradle runClient --offline
gradlew.bat -I tools/structure-regression/coloring.init.gradle -PcoloringApotheosis runClient --offline
```

首次缺 Gradle 缓存时去掉 `--offline`。

## 依赖

神化由 Gradle 获取 Apotheosis 7.4.8、Placebo 8.6.3；`build/coloring-deps` 放 ApothicAttributes 1.3.7 和 Patchouli 1.20.1-85-FORGE 的官方 JAR。Patchouli 用于完整加载神化手册数据，避免缺少 guide_book 的战利品解析错误。

extras 由 Gradle 获取 Quark 4.0-462、Zeta 1.0-31、Caelus 3.2.0；`build/coloring-extra-deps` 放：

- EnigmaticLegacy 2.30.1（可复用本机官方 JAR）。
- Patchouli 1.20.1-85-FORGE。
- TConstruct 1.20.1-3.12.1.231（Modrinth version `Fv0zTUuh`）。
- Mantle 1.20.1-1.11.117（Modrinth version `37ccXi7G`）。

本轮下载文件已按 Modrinth API 提供的 SHA-512 核验。正式发布包与 Gradle 开发映射产物分开记录。依赖不加入正式 mods，也不内嵌到 TLT 成品。

## 结果与边界

- `build/coloring-standalone`、`build/coloring-apotheosis`、`build/coloring-extras`：`result.txt`、`checks.txt`，可选 `affix-checks.txt` / `extra-checks.txt`。
- 对应目录后加 `-client`：`client-result.txt`、`client-checks.txt`、`screenshots`。
- 客户端先复制最小服务端生成的测试世界；每次开始清空测试玩家背包，保证重复运行不把上轮产物计入本轮。
- 客户端测试通过真实开窗与菜单包同步；截图夹具显式绘制输出 Tooltip，避免测试窗口失焦时系统鼠标不产生悬停。该辅助仅存在测试源码，不打包到正式模组。
- 神化夹具包含 100 个品质/名称组合、300 次真实 ReforgingMenu 预览/领取。通用夹具包含三类物品的 9,900 组状态转换、铁砧、数据保存恢复和菜单守恒。
- extras 检查匠魂材料名称及公共构建 API、神秘遗物 display 合并 API、Quark 符文 API 的组合，不等于完整测试匠魂工作站换材料或各模组全部 GUI。
- 完整整合包使用下方独立入口；远程双客户端、Sophisticated 背包真实库存和主动启用光影的组合仍需要额外验收。

已见过 Quark 首次 common_setup 的 ConcurrentModificationException；保留崩溃日志并检查调用栈，不修改生产代码掩盖它。重启后通过只表示后续这次运行成功，不表示修复初始化竞态。

## 2026-10-25 压力回归扩展

上述命令现在执行扩展测试：服务端 30 轮基础矩阵（9,900 次转换、30,000 次名称读取），固定种子 20261025 的 2,000 个菜单各 20 次随机操作，以及部分合并、满光标、不同光标、非所有者请求、64 层嵌套名称检查。神化执行 300 次真实铁砧改名与重铸交替，并在每轮重新选择染色效果。

客户端在原有十效果截图和资源重载之后，持续至少 600 秒切换选择、接收菜单同步、读取渲染材质并绘制 Tooltip，再验证领取与关闭返还。`client-stress.txt` 记录持续秒数、同步检查数、渲染帧数、帧间隔分位数和峰值已用堆内存。帧间隔包含窗口、系统调度和测试环境开销，不能作为完整整合包性能结论；峰值堆也不能证明没有内存泄漏。

服务端测试玩家使用无操作连接，随机测试不等同真实多人网络；客户端为真实单客户端与内置服务器同步。

GUI 快速回归：在 runClient 命令加 `-PcoloringQuick`，持续阶段缩为约一秒；十种选项、输出领取和关闭使用屏幕鼠标命中，仍保留资源重载检查。完整压力回归不传该参数。

## 整合包副本验收

先执行最小服务端夹具生成 `build/coloring-standalone/coloring-test`，再构建正式模组与单独测试模组：

```powershell
.\gradlew.bat -I tools/structure-regression/coloring.init.gradle build jarJar reobfColoringTestJar --offline
python tools/structure-regression/coloring-fullpack.py --pack '<整合包目录>' --output '<全新的验收目录>' --java '<Java 17 的 java.exe>'
```

入口复制完整 mods、配置、脚本和资源，替换副本中的 TLT，追加测试模组；只复制夹具世界，不读取正式游玩存档或账号令牌。输出目录必须不存在。启动提示页可由测试夹具跳过，不属于模组功能。保留整合包资源包配置，调整为中文、GUI 比例 2、60 FPS 上限和较低视距；光影保持原配置。

这一入口执行十种选项、真实菜单点击与同步、资源重载、至少 60 秒持续切换、领取与关闭返还。它是整合包集成验收，不能替代上面的 600 秒隔离压力回归或远程联机测试。`mod-sha256.json` 记录实际加载 JAR，`process.json` 记录环境，结果在 `client-result.txt`、`client-checks.txt`、`client-stress.txt`、`console.log` 和 `exit.json`；失败的副本同样保留，不覆盖历史证据。

同步阶段等待实际菜单、输出标记或背包状态满足断言，超过 600 个客户端 tick 仍不一致则失败；不重复提交操作，也不把未确认状态计为通过。这样可以容纳整合包首次配方扫描的短时阻塞，不能据此宣称服务器没有卡顿。测试 JAR 的重映射类路径同时包含主源码输出和 Minecraft 编译依赖，且不重复声明主模组的 Mixin 配置。

领取和关闭检查完成后，还会打开拾遗，断言四件助力物品与六列中的中间四列中心一致，保存 `catalog-position.png`、`catalog-description.png`、`catalog-tooltip.png`。这些截图用于核对定稿正文、分段、语义染色和中文标点换行。
