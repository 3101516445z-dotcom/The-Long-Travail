from pathlib import Path
import json
import shutil

root = Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources'
assets = resources / 'assets/the_long_travail'
def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

shutil.copyfile(root.parent.parent / '制作人杂项文件/图片暂存处/归乡.png', assets / 'textures/item/homecoming.png')
write(assets / 'models/item/homecoming.json', {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'the_long_travail:item/homecoming'}})
write(resources / 'data/the_long_travail/recipes/homecoming.json', {
    'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['ABA', 'CDC', 'EFF'],
    'key': {letter: {'item': 'minecraft:' + item} for letter, item in zip('ABCDEF', ['clay', 'red_dye', 'paper', 'writable_book', 'lapis_lazuli', 'diamond'])},
    'result': {'item': 'the_long_travail:homecoming', 'count': 1}
})
chinese = {
    'item.the_long_travail.homecoming': '归乡',
    'tooltip.the_long_travail.homecoming.prose.0': '世事维艰，旅途艰难。',
    'tooltip.the_long_travail.homecoming.prose.1': '也许应该先回到家中，',
    'tooltip.the_long_travail.homecoming.prose.2': '多做一些准备再出发。',
    'tooltip.the_long_travail.homecoming.use': '使用“归乡”可以摘下“苦旅”。',
    'message.the_long_travail.homecoming.success': '你摘下了苦旅。',
    'message.the_long_travail.homecoming.unavailable': '你尚未佩戴苦旅。',
}
english = dict(zip(chinese, [
    'Homecoming', 'Life is hard, and the journey harder.', 'Perhaps it is time to return home,',
    'and prepare a little more before setting out.',
    'Use Homecoming to unequip The Long Travail.', 'You have unequipped The Long Travail.',
    'You are not wearing The Long Travail.'
]))
for locale, additions in [('zh_cn', chinese), ('en_us', english)]:
    path = assets / 'lang' / (locale + '.json')
    lang = json.loads(path.read_text(encoding='utf-8'))
    lang.update(additions)
    write(path, lang)
print('Generated Homecoming texture, model, recipe and tooltip translations.')
