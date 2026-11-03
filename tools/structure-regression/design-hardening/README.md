# 候选异常与药效链回归

在项目根目录运行：

```powershell
.\gradlew.bat -I tools/structure-regression/design-hardening.init.gradle runServer --offline --no-daemon
```

使用项目的 Java 运行环境、Forge 开发依赖及已有 `run/eula.txt`。服务端仅监听 `127.0.0.1:25679`，使用 `build/design-hardening-server`，结束时写入 `design-result.txt` 和 `design-evidence.txt` 并停服。它注册的故障物品只存在于测试源集。

- 黄金时代：对单个食物注入查询异常，检查有效池及顺序、其他玩家、黑名单和恢复；实际调用转换入口，验证食物及蜂蜜块生成后扣除金块。
- 永生芽：重复匹配/产出异常保留材料，恢复后能对自定义物品和原版工具正常赋予。
- 午夜拉菲及孢子花：检查完整副本、无限时长、128 节点边界、循环及超限拒绝；通过实际 `addEffect` 验证拒绝和正常时长。
- 伊卡洛斯：以 `getEyeY` 哨兵检查零日照提前返回，并核对非零日照及见证分支。

玩家连接为测试替身，不包含真实菜单点击、客户端网络或长期 TPS 测量。整包伊卡洛斯飞行与同步可复用已停止的隔离测试实例：

```powershell
python tools/structure-regression/client-fullpack.py --instance build/<隔离实例> --runtime <原实例目录> --java <java.exe> --production build/libs/<正式jarJar产物>.jar --modes icarus-off
```

先执行 `build jarJar`。必须显式指定产物，运行器按模组 ID 归档测试副本中的旧版苦旅，避免旧文件名或版本号导致误测、重复加载。测试实例必须已有 `source-integrity.json`；不要指向正式游戏目录。
