# 伤害链维护工具

在工程根目录运行：

```powershell
node tools/damage-contracts/check.mjs
node --test tools/damage-contracts/check.test.mjs
```

`catalog.json` 记录效果阶段、金额、归属、保护、源码与可能后继；`entryOwners` 分类显式伤害、生命写入及死亡入口文件。一个文件可能承担多个效果，详细关系见 effects 与[源码索引](../../docs/source-index.md)。

检查器验证目录引用与入口分类，并比较 `baseline.json`：覆盖全部生产 Java、伤害类型/标签和 Mixin 注册文件，统一 CRLF/LF 后计算 SHA-256。任一文件新增、删除或变化均要求复查，所以修改非战斗 Java 也会报告。这样可以发现通过辅助类或回调引入、没有新增字面 hurt 调用的变化。候选扫描按文本定位入口，源码指纹负责报告文件变化。

退出码 0 表示目录和源码基线一致；1 表示目录错误或有变更需要复查。运行时行为与递归约束由对应 Forge 场景检查。

## 更新约定

1. 查看报告文件的差异；按文档中的阶段、金额、身份、交叉边复查。
2. 有行为变化时同步目录、交叉表与相关 Forge 回归；无伤害影响时确认调用关系未改变。
3. 生成候选：`node tools/damage-contracts/check.mjs --candidate`。该命令写入 `build/damage-contracts/candidate.json`，保留原基线；存在差异或目录错误时返回 1，否则返回 0。
4. 完成复查后，用候选内容更新 `tools/damage-contracts/baseline.json`，再次运行检查与工具自身测试。基线与行为变更一起审阅。

更新基线前完成源码复查。目录中的 successors 表示带条件的效果资格关系；递归判定同时核对同步作用域与跨刻续接规则，重点包括 `SUN_CHAIN`、`InsightRetaliation.CHAIN`、Replay Frame、特殊来源早退顺序及各队列清理。

运行时添加 JVM 参数 `-Dthelongtravail.damageTrace=true`，通过 `TLT_DAMAGE` 日志查看调用链。
