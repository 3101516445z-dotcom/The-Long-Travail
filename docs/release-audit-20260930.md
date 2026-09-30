# 首版发布前自检（2026-09-30）

后续修正：高空滑翔已适配，见 `gliding-slowdown.md`；下文四项一般问题已修正，见 `release-check-fixes.md`。下文保留初次审计时的证据及旧包哈希，不代表修正后的当前状态。

检查对象：当前源码及已安装的 the_long_travail-0.1.0.jar。两处成品 SHA-256 一致：0B11FC850ACC81C957BEABF290BE5DE36567F8EA58F79C4F19A52EA0E48DB435。

本次未修改正式 Java 源码、正式配置或 mods 中的 JAR。新增的复现夹具、字节码摘录和测试日志位于 build/release-audit；该夹具不参与正式打包。测试只使用 build/review-server 隔离世界。

## 建议首版发布前修复

### 1. 独立安装时，无垠见证的通用饰品槽加成不生效

- 位置：src/main/java/com/thelongtravail/helper/TravailCurios.java:20；src/main/resources/data/the_long_travail/curios/entities/player.json。
- 条件：只安装苦旅及其必需依赖，没有其他模组或数据包为玩家分配 curio 槽。
- 原因：syncExtraSlots 仅在 getStacksHandler("curio") 存在时修改槽位；本模组实体槽声明只有 travel_diary。Curios 提供 curio 槽类型定义，并不等于已经把该槽分配给玩家。
- 直接复现：默认玩家槽集合为 [travel_diary]；无垠见证=true、配置要求额外1槽，调用生产同步逻辑后 actualCurio=0。
- 影响：正常获得见证后拿不到承诺的额外饰品位。整合包已有通用槽时可能完全掩盖此问题。
- 修复方向：让模组自身确保玩家拥有可增加的通用槽，基础容量与见证加成分离；验证无见证时不额外赠送容量、获得/失去见证、归乡/全修、其他模组槽位叠加。

### 2. 无垠恶意的高空鞘翅减速缺少玩家本人同步/客户端移动处理

- 位置：src/main/java/com/thelongtravail/event/TravailEvents.java:227、423–434；AltitudeSpeedMixin 仅修改 getSpeed。
- 条件：佩戴尚未获得无垠见证的苦旅，在阈值以上使用鞘翅。
- 原因：鞘翅分支只在 ServerPlayer tick 调用 setDeltaMovement，未向玩家本人同步修正。Minecraft 1.20.1 的本地滑翔物理不读取 Player.getSpeed，因此已有步行减速注入无法补足。
- 复现：速度 (1,-0.2,1) 被服务端缩为约 (0.2,-0.04,0.2)，hurtMarked=false；运行真实 ServerEntity.sendChanges 后玩家本人收到的运动包=0。对照组将 hurtMarked 设为 true 后，同一连接可收到1个运动包，证明捕获链路有效。
- 证据边界：确认的是生产逻辑缺少本地生效路径及本人运动同步；本轮未实机测量滑翔速度。依据原版本地移动路径，这段服务端数值变化不能实现预期的持续滑翔减速。
- 修复方向：在实际滑翔移动路径处理客户端/服务端一致的惩罚。不要仅把每刻强制运动包当最终方案，还应核对滑翔手感、烟花推进、联机抖动和获得见证后的恢复。

## 一般问题及发布准备

1. 版本范围声明过宽：mods.toml 的 Minecraft 范围为 [1.20.1,1.21)，但构建映射和本次验证都是 1.20.1。不应把它当成已验证支持所有 1.20.x；建议限制到实际支持的版本。这是兼容性声明风险，本轮未测试其他 Minecraft 版本，不声称已复现其崩溃。
2. 资源自检脚本过时：tools/verify-diary-resources.cjs:18 要求所有 .summary 键存在，当前两种语言均已无此键，实际界面也不读取它；脚本目前会失败。部分项目符号空格断言也与现有文本不一致。独立检查确认两种语言键一致、当前效果参数数量匹配、PNG尺寸和RGBA正确，不把此失败当作游戏界面损坏。
3. 测试失败可能仍返回 Gradle 成功：tools/smoke/TravailSmoke.java:184 捕获 Throwable 后仅打印 TRAVAIL_SMOKE_FAIL，再正常停止服务器。发布自动化必须校验 PASS/FAIL 标记，或使断言失败产生非零退出状态。本次已检查实际标记。
4. 新正面效果静音的诊断计数不够准确：EffectSounds 的播放回调可以抑制声音并记录 quiet-witness-beneficial-gain；EffectSoundPolicy:56 随后仍记录 play-gain，溢出分支也类似。因此诊断可能同时显示静音和播放。实际静音规则通过测试；这是日志统计问题。

## 验证结果

命令：gradlew.bat --offline -I tools/review.init.gradle reviewTimeline reviewPerformance reviewConfigSafety reviewShader runServer。

- BUILD SUCCESSFUL；视觉时间线、配置编解码、性能逻辑回归通过。
- 真实 OpenGL 着色器编译/链接和材质检查通过。
- 隔离服务端的现有全部 smoke 完成并出现最终 TRAVAIL_SMOKE_PASS：战斗规则、取消事件、嵌套伤害、液体伤害、归乡/全修相关物品行为、旅程/旧NBT、奖励队列、状态API、新音效规则、等级黑名单等；没有 TRAVAIL_SMOKE_FAIL 或 AssertionError。
- 独立环境新增复现日志见 build/release-audit/run.log；原有回归日志见 build/release-audit/regression.log。
- 原有测试预先构造 curio 槽，且飞行测试主要覆盖速度数值/所有权；它们通过不否定本次发现的两处覆盖缺口。

本次不是对所有第三方模组组合的兼容性保证，也未重新做完整客户端联机游玩。建议先处理上述两项实际玩法问题，再进行一次首次获取/佩戴、死亡重生、跨维度、归乡、全修和鞘翅的实机验收后上传首版。
