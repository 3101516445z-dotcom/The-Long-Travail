# Current handwriting font

The diary heading, six aspect names, and malice/witness state labels use the unmodified LiuJianMaoCao-Regular.ttf from:
https://github.com/google/fonts/tree/main/ofl/liujianmaocao

License: SIL OFL 1.1, bundled at META-INF/licenses/LiuJianMaoCao-OFL.txt.
Provider: the_long_travail:liujianmaocao.ttf (the loader prepends font/).
All required Simplified Chinese headings and printable ASCII were checked against the actual font cmap. Localized text is preserved; the former Traditional Chinese substitutions are removed. Body text and witness counts retain their existing font. Name ghost and gold material are unchanged.

Validation: resource resolution and license checks, font cmap coverage, Gradle build, installed JAR resource and hash checks. In-game visual verification is still pending.

## Previous implementation and loading diagnosis (historical)

# 局部手写字体与名称重影

采用辰宇落雁体 2.0 Thin 原版字体文件，未修改字体字形。

- 来源：https://github.com/Chenyu-otf/chenyuluoyan_thin
- 原文件：`ChenYuluoyan-2.0-Thin.ttf`
- 授权原文随 JAR 附于 `META-INF/licenses/ChenYuluoyan-OFL.txt`。
- 字体仅应用于日记界面的“旅行日記”、六个区块名“繁茂／歸墟／窮遐／幽谷／冥府／無垠”、状态“惡意／見證”。用户已确认这些位置采用繁体字形；字库覆盖检查全部通过。
- 见证计数、正文、功能页签、物品描述及书签 HUD 保持原字体。

苦旅名称保留原烫金渲染，额外绘制一层同材质的淡重影，不产生重影的额外阴影。6 秒平滑呼吸，透明度 8%～14%，向右位移 0.25～0.55 GUI 像素，向上位移 0.18～0.40 GUI 像素。该位移采用浮点坐标，不随帧率变化。

验证：资源检查、九处字形覆盖、重影范围与周期测试、编译及打包资源一致性检查。未进行完整整合包内的实际视觉验证。

2026-09-24 载入修复：游戏日志确认旧配置重复拼接了 `font/`，实际查找 `font/font/chenyuluoyan.ttf` 后回退默认字体。TTF provider 的 file 已改为 `the_long_travail:chenyuluoyan.ttf`，资源测试按游戏实际规则添加 `font/` 前缀检查文件存在。

Modern UI 3.12 的 TextLayout 在普通位图模式会将字形坐标按分辨率取整。重影现在通过独立矩阵在排版后平移，保留原有强度与周期，避免小位移受排版取整影响。首次着色器载入、名称和正文材质实际提交绘制时各记录一次日志；运行时视觉效果仍需重启游戏核验。
