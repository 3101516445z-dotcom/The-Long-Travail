# 目录迁移回归入口

从项目根目录运行，复用本机已经准备好的 Forge 离线依赖、可选 API 和隔离世界。
服务端需要 `run/eula.txt`。客户端需要图形会话与 `build/reading-client/saves/visual-world` 种子世界；雨测试需要 `build/rain-client/saves/visual-world`。药水和附魔使用 `build/abundance-server/abundance`，附魔预览还需要已有的重映射 JEI 依赖。

```powershell
node tools/structure-regression/relocation/run.mjs --list
node tools/structure-regression/relocation/run.mjs hotpath dream
node tools/structure-regression/relocation/run.mjs reading rain-client boundless lantern-client abundance-client tales-client
```

逐个运行，失败即停。不得同时运行多个入口，因为 Forge 构建输出与各夹具目录共享。运行前保存旧结果、写入 NOT_RUN，运行后要求退出码为零、当前结果 PASS、源码与工具指纹一致。每次日志、结果和新截图保存在 `build/relocation-evidence/<时间>/`。超时只终止本次启动的进程树。

`hotpath` 是完整检查，覆盖 NBT 与维护队列。`hotpath-relocation` 是明确排除 NBT 和维护队列的有限模式，不能替代完整验收。NBT 键名快照内新增的键在下一轮扫描处理；夹具同时检查下一轮游标和生产维护入口的周期重扫。

梦境夹具按当前饰品规则检查功能槽，并使用注册命令 `tlt timestop`。

客户端组分别覆盖日记阅读与界面生命周期、雨技能按键和同步、梦境与时停往返及退出清理、剑灯光照、药水长按与取消，以及附魔物品提示/拾遗详情/JEI。客户端默认使用基础 Forge 环境，截图写入各自工作目录的 screenshots 文件夹。测试模组使用独立源集，不进入生产包。

雨客户端按实际配置验证冷却范围，并检查拾遗机制标题与翻译。药水客户端读取真实 ItemStack 提示链路，检查当前转化类型。
