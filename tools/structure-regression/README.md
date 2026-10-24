# 结构优化隔离回归

在仓库根目录运行，使用项目现有 Gradle/Forge 依赖及 run/eula.txt 中已接受的 EULA。
服务端仅绑定 127.0.0.1，测试数据写入 build 下独立目录，不读取整包存档。


## 日常功能入口

优先使用功能名，历史轮次继续作为实施记录保留：

    node tools/structure-regression/suite.mjs all --build
    node tools/structure-regression/suite.mjs config
    node tools/structure-regression/suite.mjs combat
    node tools/structure-regression/suite.mjs maid
    node tools/structure-regression/suite.mjs restart
    node tools/structure-regression/suite.mjs core
    node tools/structure-regression/suite.mjs maid-social

all 顺序运行基础 01/02/03、配置 10/11、战斗 12、女仆 13、跨重启 14 两阶段。--build 只在第一个阶段构建；每阶段仍校验独立 PASS、退出码与输入指纹，任一失败立即停止。聚合报告位于 build/structure-suites，并核对阶段间输入是否一致。--list 可查看入口。

maid-social 是本机可选组合，不包含在默认 all：读取 ../../mods 中唯一的 Affection 与 Personality JAR，按仓库既有开发适配方式生成 build/optional-api 副本，记录原件和副本 SHA-256。其 SRG/Mojmap 注解及开发环境客户端分支适配只用于测试副本，不能视为原发布包整包验收。缺依赖时明确失败，不自动跳过。

restart 启动两个独立 JVM；prepare 正常保存退出，verify 使用同一隔离世界和配置。内部沿用 14 before/after 命名，这里的阶段表示准备/重启验证，并不表示源码修改前后。prepare 将旧世界归档，verify 不清空世界；单独执行 verify 要求已有成功准备数据。

覆盖：一般配置值、黑名单顺序和重复项、用户删去默认条目后标记 true 不补回、真实世界女仆 UUID/稳定身份/主人、SavedData 冷却精确恢复、离线身份账本、恢复后按 tick 递减、显式置位的 armed 临时状态不跨重启。

## 第一轮：时间换算

    ./gradlew.bat build -I tools/structure-regression/round-01.init.gradle runServer --offline

结果：build/structure-round-01-server/result.txt 和 checks.txt。
用冻结的原算法对照公共方法及原调用入口，覆盖浮点特殊值、取整边界、溢出和固定种子随机值，共 2,400,270 个断言。
这属于专用服务端内的算法压力检查，不代表长时间多人玩法或 TPS 测试。

## 第二轮：旅程变更维护

    ./gradlew.bat -I tools/structure-regression/round-02.init.gradle -PstructurePhase=before runServer --offline
    ./gradlew.bat build -I tools/structure-regression/round-02.init.gradle -PstructurePhase=after runServer --offline

before 必须在该轮源码修改之前运行，after 在修改之后运行；参数只选择独立运行目录，不自动回退源码。
结果：build/structure-round-02-before 或 after 下 journey-api-result.txt；断言数及明细见 Gradle/服务端日志。
沿用已存在的旅程、状态、重置、查询夹具，旅程 API 场景连续重复 25 轮。模拟玩家的网络发送为空实现，不验证客户端收包和渲染。
每次运行在启动前清除成功标志为 NOT_RUN，进程正常退出且标志为 PASS 才通过。

测试源码与元数据纳入 Git 允许跟踪范围，构建产物不纳入。--offline 需要本机已缓存项目依赖。

## 第三轮：饰品查询

    ./gradlew.bat -I tools/structure-regression/round-03.init.gradle -PstructurePhase=before runServer --offline
    ./gradlew.bat build -I tools/structure-regression/round-03.init.gradle -PstructurePhase=after runServer --offline

结果：build/structure-round-03-before 或 after 下 accessory-restore-result.txt。
复用 11 类饰品恢复、克隆、重复装备、所有者及已消费书限制夹具；新增固定种子 2,000 组装备布局，对照冻结的旧查询算法作 58,000 次比较，包含外观槽、错误槽位、重复装备、日记前置条件，并检查死亡玩家不启用穷遐装备。
恢复快照分支仍在 AccessoryEquipRules，CurioLookup 只查询当前 Curios 背包。

## 统一入口

    node tools/structure-regression/run.mjs 04 before
    node tools/structure-regression/run.mjs 04 after --build

轮次支持 01–17，阶段必须为 before/after，默认离线运行。第一轮仍使用历史固定目录，其余轮次按阶段分开。参数仅选择测试目录，不会回退源码。
每次在 build/structure-reports 下建立时间戳目录，保存 Gradle 日志、结果文件及 report.json。报告记录 src/main、gradle、测试工具和主要构建配置的 SHA-256；进程退出码为 0、结果为 PASS 且运行期间输入不变才通过。Node.js 18+，现有 Java/Gradle/依赖缓存及 run/eula.txt 是运行前提。
此入口不快照世界，也不保证可还原外部 JAR；不要把 before/after 参数当作源码版本。

## 第四轮：女仆基础访问

    node tools/structure-regression/run.mjs 04 before
    node tools/structure-regression/run.mjs 04 after --build
    node tools/structure-regression/run.mjs 01 after

第四轮显式加载项目 maidApiDependency 对应的真实 TLM，夹具使用模拟玩家、真实女仆及原生胶片还原；新增 1,000 组布局、上下线切换、稳定身份、64 个主题掩码、死亡及移除检查。不是长期多人或真实客户端网络测试。
旧夹具的 99% 减伤断言现使用显式 .99 输入，场景结束恢复当前配置；不改变生产默认 .6。未启用 MaidSpell、Affection、Personality 等额外组合。
第一轮附加无 TLM 时的兼容入口回退检查，保留原有 2,400,270 次时间算法对照。

## 第五轮：女仆规则查询

    node tools/structure-regression/run.mjs 05 before
    node tools/structure-regression/run.mjs 05 after --build
    node tools/structure-regression/run.mjs 01 after

第四/五轮共用持续维护的 maid 夹具；历史运行的具体输入以各 report.json 中的指纹为准。
新增 64 个主题掩码 × 5 种主人状态 × 16 个装备选项，共 5,120 组矩阵：在线、离线、无日记、切换至相反阶段的主人、已断开连接。
核对阶段与装备条件差异、原 ItemStack 引用、主人日记不被查询修改，以及死亡/移除和非女仆回退。新增 102,407 项检查，含夹具初始化检查；完整套件 120,130 项。
第一轮无 TLM 场景新增 15 项规则入口回退检查，与原时间换算断言分别计数。

## 第六至九轮：女仆计算、反馈、周期维护、状态生命周期

逐轮执行，不要在运行期间修改源码或测试工具：

    node tools/structure-regression/run.mjs 06 before
    node tools/structure-regression/run.mjs 06 after --build

07、08、09 使用同样命令模式。每轮必须在改动之前运行 before；上一轮 after 通过后才进入下一轮。端口依次为 25648、25649、25650、25651，均绑定 localhost，世界与配置各自隔离在 build/structure-round-XX-before 或 after。

04–09 共用 maid 夹具。当前完整套件为 151,041 项，历史结果按当时报告计数：

- 原访问、规则、战斗和胶片恢复：120,130 项。
- 计算对照：30,002 项，冻结旧算法与固定种子，包含浮点特殊值、免疫结果及后续随机数一致性。
- 周期维护：502 项，100 轮属性不累积、卸装恢复、治疗间隔与三个冷却递减检查。
- 生命周期：407 项，100 轮换主/卸装失效及移除/死亡清理；START 不执行、END FIFO、执行中追加延后、停服丢弃未执行任务。

LegacyMaidCombatMath.java 是提取前冻结参考，位于 travail.smoke，不能放入正式模组包造成 Forge 模块 split-package。测试失败时应核对原因，不应跟随待测实现修改参考算法。

最后运行 01 after 验证未加载 TLM 时的门禁：15 项规则回退、4 项计算回退、4 个反馈调用不抛异常、药效对象引用不变；同时运行 2,400,270 次时间换算对照。该场景与真实 TLM 场景分开，不混算为女仆压力检查数。

测试使用真实 TLM 实体、模拟玩家和专用 Forge 服务端；未覆盖客户端渲染/网络、MaidSpell/Affection/Personality 组合及长期多人 TPS。开发资源已知警告不等于本次新增回归，也不应被描述为零告警。

## 第十至十三轮

沿用 before / after --build，按轮次顺序修改并测试：

- 10：ManagedConfig 写盘与发布步骤，871 项转换检查 + 33 项真实监听、损坏恢复及资源重载检查。
- 11：默认列表追加及 live-first 初始化，879 项专项检查 + 33 项真实监听检查。显式加载 TLM，以实际已注册物品测试初始化；测试注入正常拒绝和抛异常两种 save 行为。
- 12：CombatDamageRecord 数值记录，4,410 项战斗检查。含 100 轮嵌套作用域、1,000 组浮点记录对照及真实 hurt 异常退出，继续运行已有吸收、取消、反击、回放及真实女仆场景。
- 13：女仆状态与周期调度依赖收尾，151,041 项既有 maid 夹具检查。

端口依次为 25652–25655，均为 localhost。配置轮次 10/11 需要全新损坏输入及归档计数，因此重复运行会把旧阶段目录移动到同级带 archive 时间戳的目录，然后新建本轮隔离目录；不删除旧数据。其他轮次继续沿用原阶段目录规则。

配置夹具的实际路径故障仅作用于 build 下 transition-fixture；live-first 的 save 拒绝由测试替身注入，不能称为真实操作系统权限拒绝。未知键按当前 schema 纠正，测试不承诺保存所有未知键。事件异常、损坏配置等预期报错会写入日志，应以最终结果及断言判断。

完成后执行 01 after 验证无 TLM 回退。各轮报告独立记录输入指纹；历史计数按相应 report 和结果文件判断，不用后来的夹具计数覆盖旧记录。


## 进程防护与持续场景

功能名仅接受自有键。单轮 15 分钟、聚合子轮 20 分钟超时；Windows 中断/超时仅终止本轮 PID 的进程树。Gradle 使用 --no-daemon。node tools/structure-regression/test-process-guard.mjs 验证非法参数、真实子孙进程超时和中断清理。非 Windows 当前只终止直接子进程，不宣称具有相同的进程树保证。

node tools/structure-regression/suite.mjs soak 启动专用持续场景，目标 2,400 个真实 tick，默认 all 不包含。每轮将旧 build/structure-round-16-after 归档，避免旧账本污染数量断言。覆盖 16 个女仆反复胶片恢复与额外远端区块个体，模拟主人查询可用性；不等价于真实网络客户端断线重连，也不是长期多人 TPS 证明。

## 原始发布 JAR 双进程验证

前置：在 build/release-validation 安装 Forge 1.20.1-47.4.10 专用服务端；只使用此隔离目录。安装器来源为 Forge 官方 Maven。使用已接受的 run/eula.txt，端口 25659、127.0.0.1。

在仓库根目录依次执行：

    node tools/structure-regression/prepare-release.cjs
    ./gradlew.bat -I tools/structure-regression/release-smoke.init.gradle reobfReleaseSmokeJar --offline --no-daemon
    node tools/structure-regression/run-release.mjs

要求 build/libs 中已有当前正式 TLT JAR；本入口不自动替代正式构建。准备脚本按唯一文件名复制本机 Curios、女仆、爱恋、性格原件，校验哈希；不做开发映射修改。测试夹具单独 reobf，SavedData 继承调用显式以声明类型发出，避免外部自定义子类的映射缺失。Java 可由 JAVA_HOME 指定。阶段报告在 build/release-validation/evidence-*。这只是指定组合，不包含 MaidSpell、整包或客户端验收；日志中缺少未加载扩展物品的配方错误不视为已解决。

## 第十七轮：原版默认结构检测

    node tools/structure-regression/suite.mjs structures --build

独立于默认 all。先在种子 12345 的自然世界生成并定位六主题的 12 种结构，再正常保存退出，以第二个 JVM 重读同一世界复查。结果位于 build/structure-defaults-server/structures-result.txt；坐标保存在 structures.properties，运行器分别归档两阶段证据。before 表示生成，after 表示重启复查。

覆盖实际 JourneySpatial.loadedAt 的多片段、多高度、缓存与范围外检查，以及默认池、默认抽取数和扩池抽样。扩池使用原版候选模拟；不覆盖玩家客户端显示、所有世界种子或全部联动结构。初始化脚本为 round-17.init.gradle，夹具为 structures/DefaultStructuresSmoke.java。
