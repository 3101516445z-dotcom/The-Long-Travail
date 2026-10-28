# 伤害链维护工具

在工程根目录运行：

```powershell
node tools/damage-contracts/check.mjs
node --test tools/damage-contracts/check.test.mjs
```

`catalog.json` 记录效果阶段、金额、归属、保护、源码与可能后继；`entryOwners` 分类显式伤害、生命写入及死亡入口文件。一个文件可能承担多个效果，详细关系见 effects 与[伤害链文档](../../docs/开发者查阅/伤害结算与追加效果.md)。

检查器验证目录引用与入口分类，并比较 `baseline.json`：覆盖全部生产 Java、伤害类型/标签和 Mixin 注册文件，统一 CRLF/LF 后计算 SHA-256。任一文件新增、删除或变化均要求复查，所以修改非战斗 Java 也会报告。这样可以发现通过辅助类或回调引入、没有新增字面 hurt 调用的变化。候选扫描按文本定位入口，源码指纹负责报告文件变化。

退出码 0 表示目录和源码基线一致；1 表示目录错误或有变更需要复查。运行时行为与递归约束由对应 Forge 场景检查。

## 更新约定

1. 查看报告文件的差异；按文档中的阶段、金额、身份、交叉边复查。
2. 有行为变化时同步目录、交叉表与相关 Forge 回归；无伤害影响时确认调用关系未改变。
3. 生成候选：`node tools/damage-contracts/check.mjs --candidate`。该命令写入 `build/damage-contracts/candidate.json`，保留原基线；存在差异或目录错误时返回 1，否则返回 0。
4. 完成复查后，用候选内容更新 `tools/damage-contracts/baseline.json`，再次运行检查与工具自身测试。基线与行为变更一起审阅。

更新基线前完成源码复查。目录中的 successors 表示带条件的效果资格关系；递归判定同时核对同步作用域与跨刻续接规则，重点包括 `SUN_CHAIN`、`InsightRetaliation.CHAIN`、Replay Frame、特殊来源早退顺序及各队列清理。

## 运行回归选择

| 修改范围 | 现有入口 | 对照重点 |
| --- | --- | --- |
| 玩家逐日/籠釣瓶、直击身份 | `gradlew.bat -I tools/sun-blade.init.gradle runServer --offline --console=plain` | 免火、禁疗、凋零延长、固定比例、重放归属 |
| 女仆逐日/籠釣瓶 | `gradlew.bat -I tools/structure-regression/round-05.init.gradle -PstructurePhase=sun-blade-maid runServer --offline --console=plain` | 女仆主体、主人阶段、原生饰品、伤害与保存恢复 |
| 战斗公共结构 | `node tools/structure-regression/suite.mjs combat` | 共用属性、反伤与结算分支 |
| 属性边界 | `gradlew.bat -I tools/combat-attributes.init.gradle runServer --offline` | 见切归零、霸主等号、必杀、取消与吸收 |

Gradle 入口使用各自的隔离服务端、测试配置和已接受的 EULA；依赖准备见对应脚本。运行记录放 `build` 或 `local-archive`。各入口的具体断言见对应夹具；新增效果按下表补充所涉及的组合。

## 新增效果的场景设计

`tools/sun-blade-smoke/DamageChainCases.java` 使用真实玩家 hurt 检查双向逐日/见切、零冷却玫瑰、特殊类型结算、全吸收、霸主、取消及嵌套异常。燃烧快照由测试实体强制为真，用于覆盖免火装备下的极端组合。`DamageReplayCases.java` 在自然服务器 tick 中使用实际无穷花，检查必定续接与卸装失效。两者由逐日/籠釣瓶入口执行。

为隔离测试开启日志可加 `-PdamageTrace`。正式运行的诊断参数见[伤害链诊断](../../docs/开发者查阅/伤害结算与追加效果.md#伤害链诊断)。

| 场景 | 必须观察的量 |
| --- | --- |
| 双方逐日、见切与花冠，冷却取零 | 各来源 hurt 次数、最大嵌套深度、每攻击者逐日次数、返回后作用域清理 |
| 堂吉诃德/洛伦兹/见切/玫瑰配逐日与籠釣瓶 | 交叉表允许和禁止的后继、原来源标记与直击转换后的识别 |
| 无穷花续接概率取 1、同时携带逐日 | 每 tick 出队不超过一笔、每重放独立逐日范围、容量与停止条件 |
| 吸收覆盖全部输入、见切/霸主归零、事件取消 | 确认次数、total、healthLoss、受击惩罚、反击资格与冷却恢复 |
| 燃烧状态在确认阶段改变 | 使用开始快照；当前点燃不回填追加资格 |
| hurt 内抛异常、嵌套命中相同目标 | ThreadLocal 恢复，后续无关攻击不继承标记，冷却恢复不撤销嵌套有效伤害 |
| 卸装、死亡、移除、换维度、服务端停止 | 延迟任务失效、装备实例匹配、队列清理与计数去重 |
| 同主人多女仆、弹射物、部位实体、假人 | 主体 UUID、责任/直接实体、父实体确认、假人不生成虚假生命损失 |

断言应针对行为，不仅检查某个 if 或字段存在。例如删除逐日认领后，运行场景应通过实际追加次数发现错误；本目录的变异测试只验证复查工具能发现该源码变化。
