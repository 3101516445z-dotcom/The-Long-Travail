# 拾遗术语浮窗客户端预览

使用 `gradlew.bat -I tools/structure-regression/glossary-preview.init.gradle runClient --offline --no-daemon` 启动独立开发客户端。

依赖本机已有映射 JEI JAR 和 `build/abundance-server/abundance` 测试世界，复制到 `build/glossary-preview`，不修改原测试世界。Java 17。

检查跨段首次标记、别名最长匹配、页间重新计数和正文不变；随后在逐日、死告天使、籠釣瓶、洛伦兹幻想、第六天魔王的真实拾遗页上定位五个术语，截取浮窗。后台运行时测试工具同时更新 MouseHandler 坐标，以免操作系统未投递鼠标移动事件；生产代码不含此处理。

结果文件只证明自动流程完成；必须打开截图检查浮窗实际可见，不能仅凭 PASS 判定视觉验收。
