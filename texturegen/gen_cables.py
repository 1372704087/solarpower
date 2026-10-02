# -*- coding: utf-8 -*-
# 玻璃电缆资源生成器：
#   - 11 级贴图（16x16，配色取自 IU 电缆贴图，见 cable_palettes.json）
#   - 11 个 multipart 方块状态 + 每级 7 个模型（core + 六向臂，显式盒子不做旋转）
#   - 语言文件、战利品表、镐标签同步
#
# 用法：
#   py gen_cables.py           # 生成全部资源 + 预览图
#   py gen_cables.py --write   # 同上（资源直接写入项目，等价）
import json, os
from PIL import Image, ImageDraw

BASE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(BASE, '..'))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'solarpower')
DATA = os.path.join(ROOT, 'src', 'main', 'resources', 'data')
palettes = json.load(open(os.path.join(BASE, 'cable_palettes.json')))

# 与 GlassCableTier.java 一一对应：id、IU 线损/容量、中英文名
TIERS = [
    ('glass_cable',    '基础玻璃电缆', 'Basic Glass Fibre Cable'),
    ('glass_cable_2',  '混合玻璃电缆', 'Hybrid Glass Fibre Cable'),
    ('ultimate_hybrid_glass_cable',  '完美玻璃电缆', 'Perfect Glass Fibre Cable'),
    ('glass_cable_4',  '量子玻璃电缆', 'Quantum Glass Fibre Cable'),
    ('glass_cable_5',  '光谱玻璃电缆', 'Spectral Glass Fibre Cable'),
    ('glass_cable_6',  '质子玻璃电缆', 'Protonic Glass Fibre Cable'),
    ('glass_cable_7',  '奇异玻璃电缆', 'Singular Glass Fibre Cable'),
    ('glass_cable_8',  '衍射玻璃电缆', 'Diffractive Glass Fibre Cable'),
    ('glass_cable_9',  '光子玻璃电缆', 'Photonic Glass Fibre Cable'),
    ('glass_cable_10', '中子玻璃电缆', 'Neutron Glass Fibre Cable'),
    ('glass_cable_11', '无限玻璃电缆', 'Infinite Glass Fibre Cable'),
]
IU_KEYS = ['glass', 'glass1', 'glass2', 'glass3', 'glass4', 'glass5',
           'glass6', 'glass7', 'glass8', 'glass9', 'glass10']

def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    io.open(path, 'w', encoding='utf-8', newline='\n').write(text)

def write_json(path, obj):
    write(path, json.dumps(obj, indent=2, ensure_ascii=False) + '\n')

import io  # noqa: E402

# ---------- 1) 贴图 ----------
os.makedirs(os.path.join(ASSETS, 'textures', 'block'), exist_ok=True)
for (tid, _, _), key in zip(TIERS, IU_KEYS):
    pal = palettes[key]
    deep, mid, bright = tuple(pal['deep']), tuple(pal['mid']), tuple(pal['bright'])
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = (mid[0], mid[1], mid[2], 255)
    for x in range(16):
        px[x, 0] = (bright[0], bright[1], bright[2], 255)   # 顶部高光
        px[x, 15] = (deep[0], deep[1], deep[2], 255)        # 底部阴影
    for y in range(16):
        px[0, y] = (deep[0], deep[1], deep[2], 255)
        px[15, y] = (deep[0], deep[1], deep[2], 255)
    for y in range(2, 14):                                   # 高光芯线
        px[3, y] = (bright[0], bright[1], bright[2], 255)
    img.save(os.path.join(ASSETS, 'textures', 'block', tid + '.png'))
print('textures: 11')

# ---------- 2) 模型（core + 六向臂，盒子坐标显式） ----------
ARMS = {
    'north': (6, 6, 0, 10, 10, 6),
    'south': (6, 6, 10, 10, 10, 16),
    'west':  (0, 6, 6, 6, 10, 10),
    'east':  (10, 6, 6, 16, 10, 10),
    'up':    (6, 10, 6, 10, 16, 10),
    'down':  (6, 0, 6, 10, 6, 10),
}
MODEL_DIR = os.path.join(ASSETS, 'models', 'block')
for (tid, _, _) in TIERS:
    tex = 'solarpower:block/' + tid
    write_json(os.path.join(MODEL_DIR, tid + '_core.json'), {
        'parent': 'block/block',
        'textures': {'particle': tex, 'tex': tex},
        'elements': [{'from': [6, 6, 6], 'to': [10, 10, 10],
                      'faces': {f: {'texture': '#tex'} for f in
                                ('north', 'south', 'east', 'west', 'up', 'down')}}],
    })
    for side, box in ARMS.items():
        write_json(os.path.join(MODEL_DIR, '%s_arm_%s.json' % (tid, side)), {
            'parent': 'block/block',
            'textures': {'particle': tex, 'tex': tex},
            'elements': [{'from': list(box[:3]), 'to': list(box[3:]),
                          'faces': {f: {'texture': '#tex'} for f in
                                    ('north', 'south', 'east', 'west', 'up', 'down')}}],
        })
# 物品模型（展示核心段）
ITEM_DIR = os.path.join(ASSETS, 'models', 'item')
for (tid, _, _) in TIERS:
    write_json(os.path.join(ITEM_DIR, tid + '.json'), {
        'parent': 'solarpower:block/' + tid + '_core',
    })
print('models:', len(TIERS) * 7, '+ item', len(TIERS))

# ---------- 3) 方块状态（1.12.2 单状态：连接臂由自定义烘焙模型动态拼装） ----------
# "" 与 "inventory" 是真实使用的模型；"arm_*" 键仅供 ModelBakeEvent 取烘焙臂模型
CONNS = ['north', 'south', 'east', 'west', 'up', 'down']
for (tid, _, _) in TIERS:
    variants = {'': {'model': 'solarpower:block/%s_core' % tid},
                'inventory': {'model': 'solarpower:block/%s_core' % tid}}
    for side in CONNS:
        variants['arm_' + side] = {'model': 'solarpower:block/%s_arm_%s' % (tid, side)}
    write_json(os.path.join(ASSETS, 'blockstates', tid + '.json'), {'variants': variants})
print('blockstates: 11')

# ---------- 4) 语言文件 ----------
zh_path = os.path.join(ASSETS, 'lang', 'zh_cn.json')
en_path = os.path.join(ASSETS, 'lang', 'en_us.json')
zh = json.load(open(zh_path, encoding='utf-8'))
en = json.load(open(en_path, encoding='utf-8'))
for (tid, zh_name, en_name) in TIERS:
    zh['block.solarpower.' + tid] = zh_name
    en['block.solarpower.' + tid] = en_name
write(zh_path, json.dumps(zh, ensure_ascii=False, indent=2) + '\n')
write(en_path, json.dumps(en, ensure_ascii=False, indent=2) + '\n')
print('lang updated')

# ---------- 5) 战利品表 + 镐标签 ----------
for (tid, _, _) in TIERS:
    write_json(os.path.join(DATA, 'solarpower', 'loot_table', 'blocks', tid + '.json'), {
        'type': 'minecraft:block',
        'pools': [{'rolls': 1, 'bonus_rolls': 0,
                   'entries': [{'type': 'minecraft:item', 'name': 'solarpower:' + tid}],
                   'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
    })
tag_path = os.path.join(DATA, 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json')
tag = json.load(open(tag_path, encoding='utf-8'))
for (tid, _, _) in TIERS:
    if 'solarpower:' + tid not in tag['values']:
        tag['values'].append('solarpower:' + tid)
write(tag_path, json.dumps(tag, indent=2) + '\n')
print('loot + tags updated')

# ---------- 6) 预览图 ----------
S, pad, cols = 8, 6, 6
rows = (len(TIERS) + cols - 1) // cols
W = cols * (16 * S + pad) + pad
H = rows * (16 * S + pad + 14) + pad
sheet = Image.new('RGBA', (W, H), (70, 70, 70, 255))
d = ImageDraw.Draw(sheet)
for i, (tid, zh_name, _) in enumerate(TIERS):
    x = pad + (i % cols) * (16 * S + pad)
    y = pad + (i // cols) * (16 * S + pad + 14)
    im = Image.open(os.path.join(ASSETS, 'textures', 'block', tid + '.png'))
    sheet.paste(im.resize((16 * S, 16 * S), Image.NEAREST), (x, y))
    d.text((x + 2, y + 16 * S + 1), zh_name[:-2], fill=(255, 255, 255, 255))
os.makedirs(os.path.join(BASE, 'preview'), exist_ok=True)
sheet.save(os.path.join(BASE, 'preview', 'cables_preview.png'))
print('preview -> preview/cables_preview.png')
