# 卷首客户端预览

运行 gradlew.bat -I tools/structure-regression/preface-preview.init.gradle runClient --offline --no-daemon。

复用既有测试世界副本及本机映射 JEI 依赖，输出至 build/preface-preview。正式世界不参与测试。

映射 JEI 默认从 Gradle 用户缓存读取，也可通过 `-PpreviewJeiJar=<映射后的JEI文件>` 指定；使用与工程 Minecraft 映射一致的开发依赖。`-PprefaceScreenshotOnly` 仅输出正常卷首截图，不注入长文本、不运行压力检查。

覆盖首次卷首、六主题切换及返回、重新打开的位置记忆；使用临时语言覆盖验证左右独立滚动、方向键、翻页键、上下边界和书脊区域不响应滚轮。截图覆盖标准、小窗口及 1920×1080 下的 GUI 1/2/3/自动，GUI 3 另检查实际缩放值。临时文字不会写入生产语言文件。

每次运行重置 result.txt；截图前检查札记是否可见，最终结果必须为 PASS，否则 Gradle 任务失败。预览角色使用创造模式，避免生物干扰。

辅助模组必须包含 `pack.mcmeta`（1.20.1 的 `pack_format` 为 15）；整包脚本在启动前检查本模组和辅助模组的元数据，避免 Forge 因测试包缺少元数据停在加载警告页。运行时遇到 `LoadingErrorScreen` 会保存截图并立即失败，不自动跳过警告。

临时语言覆盖绕过正常资源重载；存在 Modern UI 时，测试辅助代码在切换和恢复语言后调用其 `TextLayoutEngine.clear()` 清除旧组件布局缓存。此操作仅属于测试工具，生产代码不依赖 Modern UI。测试另行断言临时文本已生效，区分文本注入失败与滚动失败。

遮罩脚本 `python tools/structure-regression/preface-preview/extract-page-overlay.py` 从 Java 布局常量和原背景重新生成遮罩。加 `--check` 只读核对像素一致性；修改背景或布局后必须执行此检查。

`LayoutStress` 在基础验收后运行 6 种窗口尺寸 × GUI 1/2/3/自动共 24 组组合。窗口改变稳定后再设置 GUI 缩放，核对当前运行时允许的实际值，1920×1080 下 GUI 3 必须实际生效。Modern UI 将最小 GUI 缩放限制为 2，整包中请求 1 会落到 2；真正的 GUI 1 由不含 Modern UI 的开发环境覆盖。覆盖真实渲染动画中的书签内缘、外侧、间隙和被纸页遮住的一侧；输入坐标按实际 framebuffer 像素中心取整。每组额外快速切换 70 次，检查效果/反转页签、关闭按钮、右键关闭、重开后的首帧前点击及长文滚动。输出 `layout-stress.txt` 和额外 24 张截图，总计 32 张。

这些检查在真实客户端中直接调用 Screen 输入入口，并通过 GLFW 移动悬浮位置；不等同于人工鼠标全流程或原生事件分发测试。装饰性 V 口仍使用矩形点击范围，不要求逐透明像素命中。结果只覆盖记录的窗口尺寸和实际 GUI 缩放，不外推到任意显示器/DPI。

整包验收先运行 `gradlew.bat -I tools/structure-regression/preface-fullpack.init.gradle prefaceFullpackJar reobfPrefaceFullpackJar jarJar --offline --no-daemon`，再运行 `preface-fullpack.py --pack <既有整包> --output <项目build下的新目录> --java <java.exe> --runtime <PCL版本目录>`。仅在副本中替换 TLT、加入探针并使用隔离测试世界；前后核对原环境 mods/config/defaultconfigs/kubejs/options 哈希。通过后保存结果、截图、模组清单和完整性记录，清理副本。

悬浮定位专项：在隔离运行目录放置 `bookmark-hover-check` 标记，可仅拍摄 GUI 2/3 下各三个书签内鼠标位置，不注入长文。洋红十字标记 Screen 实际收到的鼠标坐标。测试经 MouseHandler 的移动回调注入位置并断言更新成功，避免未聚焦窗口忽略 GLFW 移动而产生假通过；此项不代表操作系统鼠标事件链路验收。完成后移除标记可恢复常规验收。
