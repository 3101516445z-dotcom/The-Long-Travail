# 日记素材与字体

## 资源

- `src/main/resources/assets/the_long_travail/textures/gui/diary_base.png`：1448×1086，透明背景的平整双页日记主体。
- `src/main/resources/assets/the_long_travail/textures/gui/diary_bookmark.png`：2172×724，透明背景的中性布料书签。
- 渲染时创建六个独立书签，各自拥有颜色、位置、选中状态、伸缩动画和固定点击区域。六个组件共享中性贴图，颜色由界面着色，根部由日记主体遮挡；合计七个独立绘制组件。无需维护六张重复纹理。

原背景 `travel_diary.png` 保留作为美术参考，当前界面使用新底图。

## 图像生成记录

两张素材均通过内置 imagegen 工具生成，参考原背景图。原始输出保留在 Codex generated_images 中，最终资源已复制到本项目。没有使用 CLI/API 后备模式。

### 日记主体提示词

Edit this Minecraft pixel-art diary GUI background into a production-ready base texture. Preserve the brown leather cover, restrained golden corner fittings, warm parchment palette and straight-on open-book composition. Remove ALL six colored bookmarks entirely and reconstruct the underlying cover edges. Both left and right pages MUST be perfectly flat, rectangular, straight horizontal top/bottom edges, NO curled corners, NO curved or bulging pages, NO perspective distortion, NO shadows or gradients across the writing areas. Make this look like two flat sheets side by side, separated by a narrow vertical brown binding. Very subtle parchment grain, each writing area broadly blank and evenly pale cream; wear only near outer edges. Sharp pixel-art edges, original visual style. No text, no symbols, no bookmarks, no buttons. Landscape 4:3 canvas, centered book fills about 94% width and 90% height with even transparent margins. REAL transparent background outside the book. This is the single diary base asset; do not produce a mockup, labels or multiple panels.

### 书签提示词

Create ONE isolated pixel-art fabric bookmark sprite matching the cloth bookmarks at the left edge of this reference diary. This is a new game UI asset, not a mockup. Horizontal rectangular ribbon, 3:1 width-to-height, left free end has a small symmetrical V notch, right end is perfectly straight and extends horizontally to tuck under a book. Neutral WHITE / light GRAY cloth only, with subtle gray pixel-weave texture, darker stitched edge; no colored hue, so the game can tint it green/cyan/gold/gray/red/purple. Flat top-down view, no perspective, no fold, no shadow outside the fabric. NO book, NO text, NO symbols, NO labels, ONE single ribbon only. Entire background genuinely transparent, ribbon fills central 90 percent width with even small margins. Clean hard pixel edges.

当前界面使用游戏默认字体，已移除内置手写字体及专用字体定义。旧版 diary-preview.png 仅作历史参考。
