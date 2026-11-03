# 摘要生成客户端回归

运行 `gradlew.bat -I tools/structure-regression/summary-preview.init.gradle runClient --offline --no-daemon`，使用 Java 17。

独立目录为 `build/summary-preview`，进入标题画面后自动检查并退出，不进入正式存档。
`result.txt` 记录结果。覆盖五种语言的摘要直接生成、完整 ItemStack 提示、重复处理、字符串化、旧键恢复、未知物品旧键清理、文字及染色、属性和女仆说明顺序。

`LegacyThemeTooltips.java` 是提交 `5b74687` 的修复前实现副本，仅改包名和类名作为行为对照，不进入发布包。
此测试不代表 FTB 任务界面或反馈者完整模组环境的视觉验收，也不覆盖真实服务端启动。

扩展覆盖：默认状态拾遗详情动态键、长按提示递归识别、真实 GatherComponents 事件中的进度条组件生成，以及整份语言映射缺失时的包内英文回退和未知键诊断。

专用服务端启动脚本为 `tools/structure-regression/run-summary-server.py`，使用 `build/summary-server-audit` 已准备的 Forge 库、TLT 与 Curios；到达 Done 后发送 stop，保存并退出。

## 状态与资源重载扩展

覆盖永生花催生开关及同步默认值、魂数负值/零/正常值/整型上界、春意一隅无花/单花/全部花组合、永生芽标记、液化丰饶三种模式、星之声概率同步值 0/1。显式检查开关和三种模式的文本确实不同。拾遗详情同时生成恶意与见证段落；这不等于真实玩家战斗状态的验证。

遍历全部术语正文，在五语言运行后执行英文、简体中文两次实际资源重载，重新执行文字、染色、进度条组件及状态检查。只改变隔离测试客户端。

`verifyTranslationReferences` 已接入 Gradle `check`，校验固定源码引用及登记术语键。动态表达式仍依赖客户端回归。

## 收到新反馈时

收集 TLT 完整文件名、物品及界面、语言与资源包、截图、日志中的 Missing tooltip translation 行。补充普通背包是否同样异常、是否只在语言切换或资源重载后出现。仅需相关日志片段，避免收集启动参数中的账号令牌。
