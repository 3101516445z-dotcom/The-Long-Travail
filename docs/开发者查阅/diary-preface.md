# 卷首与题记

札记首次打开显示卷首；同一次游戏运行中，再次打开沿用上次选择的卷首或主题。左右页标题分别为卷首、题记，诗文维护于五语言 diary.json 的 preface.left / preface.right。

诗文逐行居中，使用 ProseAlignment 修正标点的视觉偏移；字号为 1.35 倍，行距 17，段距额外增加 13。短诗在正文区域内居中并上移最多 12 个界面单位；长文分左右页独立滚动。恶意、见证、石头及对应译词分别使用红色 #C83E24、蓝色 #2864B5、绿色 #278044，并加粗。

TravelDiaryScreen 管理七枚书签、入口和页面记忆，DiaryPreface 管理双页诗文、样式与滚动。布局在 init 和绘制时更新，打开或调整窗口后首帧前的输入使用当前坐标。

七枚左侧书签共用 diary_bookmark.png，顺序为卷首、繁茂、归墟、穷遐、幽谷、冥府、无垠。起始 y=59，间距 34，高度 27，横向截取材质至 74 宽；起点 x=28。卷首染色为 #BE713D，保留原布纹及缝线。进入卷首时隐藏主题效果与反转条件标签。

书签在封皮与纸页交界处插入：背景、书签、diary_page_overlay.png 依次绘制。遮罩从原背景源图 x=69 起提取，只覆盖书签所在行；插入口固定，左侧点击边界随书签伸缩。工具 tools/structure-regression/preface-preview/extract-page-overlay.py 读取 Java 常量生成遮罩，--check 核对现有像素与布局。

两页分别保存滚动位置，滚轮作用于鼠标所在页，方向键和翻页键作用于最近指向的正文页。预览工具入口见 tools/structure-regression/preface-preview/README.md。
