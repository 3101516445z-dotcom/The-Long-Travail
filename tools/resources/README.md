# 资源生成

`extract-page-overlay.py` 使用 Python 与 Pillow，读取正式日记背景及 Java 布局常量，生成 `src/main/resources/assets/the_long_travail/textures/gui/diary_page_overlay.png`。

从仓库根目录运行 `python tools/resources/extract-page-overlay.py --check` 可校验现有遮罩；省略 `--check` 才会写入目标图片。
