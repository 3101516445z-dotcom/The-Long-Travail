"""从现有日记背景提取窄条前景纸张。"""
from pathlib import Path
import argparse
import re
from PIL import Image

parser = argparse.ArgumentParser()
parser.add_argument('--check', action='store_true', help='Verify without modifying the checked-in PNG')
options = parser.parse_args()
root = Path(__file__).resolve().parents[2]
java = (root / 'src/main/java/com/thelongtravail/client/diary/TravelDiaryScreen.java').read_text(encoding='utf8')
def constant(name):
    return int(re.search(r'\b' + name + r'\s*=\s*(\d+)\s*;', java)[1])
first, step, height, count, insertion = [constant('BOOKMARK_' + name) for name in
    ['FIRST_Y', 'STEP', 'HEIGHT', 'COUNT', 'INSERT_SOURCE_X']]
directory = root / 'src/main/resources/assets/the_long_travail/textures/gui'
source = Image.open(directory / 'diary_base.png').convert('RGBA')
overlay = Image.new('RGBA', (220, 1086))
boundaries = []
for y in range(140, 941):
    gui_y = y * 360 / 1086
    # 重叠一个 GUI 像素，避免亚像素栅格化露出书签尖端。
    if not any(first - 1 + i * step <= gui_y < first + i * step + height + 1 for i in range(count)):
        continue
    # 锚定封面与书页接缝，不延伸至纸面。
    boundaries.append(insertion)
    for x in range(insertion, 220):
        r, g, b, _ = source.getpixel((x, y))
        overlay.putpixel((x, y), (r, g, b, 255))
target = directory / 'diary_page_overlay.png'
if options.check:
    existing = Image.open(target).convert('RGBA')
    if existing.size != overlay.size or existing.tobytes() != overlay.tobytes():
        raise SystemExit('FAIL: overlay differs from source texture/layout; regenerate it')
    print('PASS: overlay matches source texture and Java layout constants')
else:
    overlay.save(target)
    print('Paper insertion x range:', min(boundaries), max(boundaries))
