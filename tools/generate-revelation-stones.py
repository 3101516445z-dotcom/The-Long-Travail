from pathlib import Path
import json
import shutil

root = Path(__file__).resolve().parents[1]
resources = root / 'src/main/resources'
assets = resources / 'assets/the_long_travail'
data = resources / 'data/the_long_travail'
textures = root.parent.parent / '制作人杂项文件/图片暂存处'

stones = [
    ('flourishing', '繁茂', 'Flourishing', ['podzol', 'grass_block', 'dark_oak_log', 'cherry_sapling', 'sunflower', 'mangrove_propagule', 'bamboo_block', 'melon', 'rose_bush']),
    ('abyss', '归墟', 'Abyss', ['pufferfish', 'prismarine_shard', 'tropical_fish', 'trident', 'heart_of_the_sea', 'nautilus_shell', 'sea_pickle', 'sponge', 'mushroom_stew']),
    ('far_reach', '穷遐', 'Far Reach', ['smooth_sandstone', 'cactus', '#the_long_travail:stone_terracotta', 'dead_bush', 'map', 'emerald_ore', 'powder_snow_bucket', 'rabbit_foot', 'blue_ice']),
    ('deep_valley', '幽谷', 'Deep Valley', ['stone', 'mossy_stone_bricks', 'pointed_dripstone', 'polished_deepslate', 'sculk_shrieker', ['big_dripleaf', 'small_dripleaf'], 'clay', 'lava_bucket', 'amethyst_cluster']),
    ('underworld', '冥府', 'Underworld', ['weeping_vines', 'ghast_tear', 'nether_wart', 'gold_block', 'wither_skeleton_skull', 'ancient_debris', 'fire_charge', 'warped_nylium', 'magma_cream']),
    ('boundless', '无垠', 'Boundless', ['ender_pearl', 'chorus_flower', 'end_rod', 'end_crystal', 'dragon_head', 'dragon_breath', 'end_stone_bricks', 'shulker_shell', 'purpur_pillar']),
]

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def ingredient(value):
    if isinstance(value, list): return [ingredient(v) for v in value]
    if value.startswith('#'): return {'tag': value[1:]}
    return {'item': value if ':' in value else 'minecraft:' + value}

for aspect, chinese, english, inputs in stones:
    item = aspect + '_stone'
    destination = assets / 'textures/item' / (item + '.png')
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(textures / (chinese + '之石.png'), destination)
    write(assets / 'models/item' / (item + '.json'), {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'the_long_travail:item/' + item}})
    recipe = {'type': 'minecraft:crafting_shaped', 'category': 'misc', 'pattern': ['ABC', 'DEF', 'GHI'],
              'key': {letter: ingredient(value) for letter, value in zip('ABCDEFGHI', inputs)},
              'result': {'item': 'the_long_travail:' + item, 'count': 1}}
    write(data / 'recipes' / (item + '.json'), recipe)

colors = 'white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split()
write(data / 'tags/items/stone_terracotta.json', {'replace': False, 'values': ['minecraft:terracotta'] + ['minecraft:' + color + '_terracotta' for color in colors]})

for locale in ['zh_cn', 'en_us']:
    path = assets / 'lang' / (locale + '.json')
    lang = json.loads(path.read_text(encoding='utf-8'))
    for aspect, chinese, english, _ in stones:
        lang['item.the_long_travail.' + aspect + '_stone'] = chinese + '之石' if locale == 'zh_cn' else english + ' Stone'
    lang.update({
        'tooltip.the_long_travail.revelation.hidden': '你需要制作此处对应的解密之石，使用后将显示反转条件。' if locale == 'zh_cn' else 'Craft and use the corresponding revelation stone to display the reversal requirements.',
        'tooltip.the_long_travail.revelation_stone': '右键使用，获悉如何让%1$s的恶意因你的旅程而反转。' if locale == 'zh_cn' else 'Right-click to learn how your journey can reverse the malice of %1$s.',
        'gui.the_long_travail.diary.journey_locked': '反转条件·未解密' if locale == 'zh_cn' else 'Reversal · Unrevealed',
        'gui.the_long_travail.diary.journey': '反转条件·已解密' if locale == 'zh_cn' else 'Reversal · Revealed',
        'message.the_long_travail.revealed': '%1$s的秘密已然揭晓。' if locale == 'zh_cn' else 'The mist surrounding %1$s has cleared.',
        'message.the_long_travail.already_revealed': '你已经揭开了%1$s的迷雾，无需再次使用。' if locale == 'zh_cn' else 'You have already revealed %1$s; the stone was not consumed.',
    })
    write(path, lang)

print('Generated six original textures, item models, recipes, terracotta tag and both locales.')
