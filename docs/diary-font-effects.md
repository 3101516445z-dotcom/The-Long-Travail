# 苦旅字体效果（2026-09-24）

- 描述首行由 `appendHoverText` 显式输出“栏位：旅行日记”，随后空一行；禁用重复的 Curios 自动栏位文本。
- 名称：深棕金底材、流动金纹、少量浅金亮点，4 秒循环；增大字内明暗对比，模拟旧书烫金。
- 正文：全部文字（含玩家名）使用同一棕金材质，移植原名称的 8 秒平滑渐变，在古铜、赭金、浅棕金之间变化。所有正文行共用最长行的实际宽度；另有亮带中心在 12 秒内从最左端移动到最右端，然后回到最左端开始下一周期。保留玩家名替换、原文分行和默认正体字。
- 栏位说明使用金色前缀，参数“旅行日记”单独使用原版黄色 `ChatFormatting.YELLOW`。
- 名称方案已获确认并实现：参考暗夜立方 `item.goeticlegacy.night_cube` 的 `gl:v` 材质及 `font_void.glsl` 的流动裂隙和局部亮点，使用独立的金色材质算法。
- 参考本地 Goetic Legacy 的字体材质、时间驱动与 bitmap/SDF 分流设计；参考 Enigmatic Legacy 的正文与功能提示层次。没有修改参考模组，也未复制其复杂字体效果。
- `DiaryFontMixin` 在字体入口识别苦旅文本标记，移除标记后调用当前字体实现，并为本次绘制包装缓冲区。原字形图集、布局与渲染状态沿用；材质着色器仅用于苦旅名称和正文。混合其他内容的文本保持原渲染，避免影响整条聊天消息。
- 原版 bitmap/intensity 与 Modern UI SDF fill 分别处理透明覆盖率；stroke 保留原渲染。着色器未就绪时保留普通字体渲染。
- 构建及资源检查通过。`tools/DiaryFontShaderTest.java` 在隐藏的真实 OpenGL 上下文中验证 GLSL 编译链接、8 秒周期、正文亮度幅度，以及三种图集的透明/不透明覆盖率。完整整合包内的实际显示与第三方提示框仍需游戏内检查。

## Reference correction (2026-09-24)

Night Cube uses `gl:v`. FontEffectParser maps that token to JADE + SPECTRAL_BREATH, not VOID. StringRenderOutputMixin.renderSpectralGhost draws an additional offset glyph with animated opacity. GlyphSpectralBreathMotion has a 14.6-second event cycle. The installed Travail gold-foil material remains unchanged; this investigation did not add a ghost layer or fonts.
