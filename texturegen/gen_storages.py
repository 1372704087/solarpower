# -*- coding: utf-8 -*-
# 「XX储电盒」方块家族生成器（67 档：每档 3 张方块贴图 + blockstate + 模型 + lang）。
#
# 模板：cable_pixel_editor.html 里的储电盒图案（2026-10-04 用户定，IU 红色素材原图
# 1:1 转写：mfsu_front / mfsu_leftrightback / mfsu_bottomtop）。生成器是模板的正本，
# 编辑器图案与本文件 TEMPLATE 保持同步。
#
# 换色（用户 2026-10-04 定）：结构不变，红三阶随档位换面板主题色——
#   M=deep(×0.545)  R=mid(原色)  B=bright(混白 0.45)，与 gen_cables_iu.py 三阶规则一致；
#   石灰 E/G/D/N 与能量芯 O/W 全档固定（IU 原色）。
#
# 产出：
#   textures/block/<id>_front|_side|_topbottom.png   67×3 张
#   blockstates/<id>.json                            facing 四向 + normal 兜底
#   models/block/<id>.json                           cube：north=正面 up/down=顶底 其余=侧面
#   models/item/<id>.json                            parent=方块模型（无 item 贴图）
#   两份 lang 各 67 行 tile.solarpower.<id>.name=（zh=面板名去「太阳能发电机」+「储电盒」，
#   en=Solar Panel→Storage Box）
#
# 用法：
#   py gen_storages.py           # 预览：preview/storages_preview.png
#   py gen_storages.py --write   # 写入 src 资产 + 两份 lang
from PIL import Image, ImageDraw
import json
import os
import re

BASE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                       'assets', 'solarpower'))
BLOCKSTATE_DIR = os.path.join(ASSETS, 'blockstates')
BLOCK_MODEL_DIR = os.path.join(ASSETS, 'models', 'block')
ITEM_MODEL_DIR = os.path.join(ASSETS, 'models', 'item')
TEX_DIR = os.path.join(ASSETS, 'textures', 'block')
PREVIEW_DIR = os.path.join(BASE, 'preview')
LANG_DIR = os.path.join(ASSETS, 'lang')
SRC_TIER = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'java',
                                         'com', 'example', 'solarpower', 'solar',
                                         'SolarTier.java'))

SUFFIX = '_storage_box'

# ===== 储电盒正面主模板（IU mfsu_front 转写，编辑器「储电盒」图案同步） =====
TEMPLATE_FRONT = [
"DDDDDDBBBBDDDDDD",
"DMMRRRMGGMRRRMMD",
"DGGGGMNGGNMGGGGD",
"DMRBRMNGGNMRBRMD",
"DGGGGGNGGNGGGGGD",
"DNDDNGEEEEGNDDND",
"RRGGREEOOEERGGRR",
"MMRBMEOWWOEMBRMM",
"MMRBMEOWWOEMBRMM",
"RRGGREEOOEERGGRR",
"DNDDNGEEEEGNDDND",
"DGGGGGNGGNGGGGGD",
"DMRBRMNGGNMRBRMD",
"DGGGGMNGGNMGGGGD",
"DMMRRRMGGMRRRMMD",
"DDDDDDBBBBDDDDDD",
]
# ===== 侧面（IU mfsu_leftrightback 转写，编辑器「储电盒侧面」图案同步） =====
TEMPLATE_SIDE = [
"DDDDDDBBBBDDDDDD",
"DMMRRRMGGMRRRMMD",
"DGGGGMNGGNMGGGGD",
"DMRBRMNRRNMRBRMD",
"DGGGGGRGGRGGGGGD",
"DNDDNGNGGNGNDDND",
"RRGGNMMNNMMNGGRR",
"MMRBRMDBBDMRBRMM",
"MMRBRMDBBDMRBRMM",
"RRGGNMMNNMMNGGRR",
"DNDDNGNGGNGNDDND",
"DGGGGGRGGRGGGGGD",
"DMRBRMNRRNMRBRMD",
"DGGGGMNGGNMGGGGD",
"DMMRRRMGGMRRRMMD",
"DDDDDDBBBBDDDDDD",
]
# ===== 顶底（IU mfsu_bottomtop 转写，编辑器「储电盒顶底」图案同步） =====
TEMPLATE_TOPBOTTOM = [
"DDDDDDBDDBDDDDDD",
"DNGGNGRMMRGNGGND",
"DNGGNGNRRNGNGGND",
"DNNNDGNBBNGDNNND",
"DGGGNGNRRNGNGGGD",
"DNDDNGRRRRGNDDND",
"BRGGNMMNNMMNGGRB",
"DMRBRMDBBDMRBRMD",
"DMRBRMDBBDMRBRMD",
"BRGGNMMNNMMNGGRB",
"DNDDNGRRRRGNDDND",
"DGGGNGNRRNGNGGGD",
"DNNNDGNBBNGDNNND",
"DNGGNGNRRNGNGGND",
"DNGGNGRMMRGNGGND",
"DDDDDDBDDBDDDDDD",
]

# 固定色（IU 原图采样）：暗线/框亮/框灰/框灰点/能量芯/芯高光
FIXED = {
    'E': (0x3D, 0x3D, 0x3D),
    'G': (0xD8, 0xDE, 0xDE),
    'D': (0x9C, 0xAC, 0xAD),
    'N': (0xB9, 0xC3, 0xC3),
    'O': (0xC4, 0x83, 0x01),
    'W': (0xE7, 0xB7, 0x07),
}
DEEP_FACTOR = 0.545          # M = accent × 0.545（电缆 deep 规则）
BRIGHT_MIX = 0.45            # B = accent 混白 0.45（电缆 bright 规则）


def tier_accents():
    """从 SolarTier.java 逐档提取 accentColor（每个常量块里唯一的 0xRRGGBB）。"""
    src = open(SRC_TIER, encoding='utf-8').read()
    starts = [m for m in re.finditer(r'^    [A-Z_0-9]+\("([a-z_0-9]+)",', src, re.M)]
    accents = {}
    for i, m in enumerate(starts):
        end = starts[i + 1].start() if i + 1 < len(starts) else len(src)
        hexes = re.findall(r'0x([0-9A-Fa-f]{6})', src[m.start():end])
        assert len(hexes) == 1, (m.group(1), hexes)
        hx = hexes[0]
        accents[m.group(1)] = tuple(int(hx[j:j + 2], 16) for j in (0, 2, 4))
    return accents


def deep_of(accent):
    return tuple(round(c * DEEP_FACTOR) for c in accent)


def bright_of(accent):
    return tuple(round(c + (255 - c) * BRIGHT_MIX) for c in accent)


def box_id(tier_id):
    return tier_id + SUFFIX


def build(accent):
    """三张 16x16 贴图：M/R/B 按档位三阶换色，其余固定。"""
    color = dict(FIXED)
    color['M'] = deep_of(accent)
    color['R'] = accent
    color['B'] = bright_of(accent)
    imgs = []
    for template in (TEMPLATE_FRONT, TEMPLATE_SIDE, TEMPLATE_TOPBOTTOM):
        img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
        px = img.load()
        for y, row in enumerate(template):
            assert len(row) == 16, row
            for x, ch in enumerate(row):
                if ch == '.':
                    continue
                px[x, y] = color[ch] + (255,)
        imgs.append(img)
    return imgs


def blockstate_json():
    return {
        'variants': {
            'facing=north': {'model': 'solarpower:block/{id}'},
            'facing=east': {'model': 'solarpower:block/{id}', 'y': 90},
            'facing=south': {'model': 'solarpower:block/{id}', 'y': 180},
            'facing=west': {'model': 'solarpower:block/{id}', 'y': 270},
            'normal': {'model': 'solarpower:block/{id}'},
        },
    }


def block_model_json():
    return {
        'parent': 'block/cube',
        'textures': {
            'north': 'solarpower:block/{id}_front',
            'south': 'solarpower:block/{id}_side',
            'east': 'solarpower:block/{id}_side',
            'west': 'solarpower:block/{id}_side',
            'up': 'solarpower:block/{id}_topbottom',
            'down': 'solarpower:block/{id}_topbottom',
            'particle': 'solarpower:block/{id}_side',
        },
    }


def item_model_json():
    return {'parent': 'solarpower:block/{id}'}


def panel_lang_names(panel_ids):
    """从两份 lang 提取面板名：{面板 id: 显示名}（只认传入的面板 id）。"""
    names = {}
    for code in ('en_us', 'zh_cn'):
        table = {}
        path = os.path.join(LANG_DIR, code + '.lang')
        for line in open(path, encoding='utf-8'):
            m = re.match(r'tile\.solarpower\.([a-z_0-9]+)\.name=(.+)', line.strip())
            if m and m.group(1) in panel_ids:
                table[m.group(1)] = m.group(2)
        names[code] = table
    return names


def box_lang_name(panel_name, code):
    if code == 'zh_cn':
        assert '太阳能发电机' in panel_name, panel_name
        return panel_name.replace('太阳能发电机', '储电盒')
    assert 'Solar Panel' in panel_name, panel_name
    return panel_name.replace('Solar Panel', 'Storage Box')


def append_lang_lines(names):
    """两份 lang 各追加缺失的储电盒名（幂等）。"""
    for code in ('en_us', 'zh_cn'):
        path = os.path.join(LANG_DIR, code + '.lang')
        with open(path, encoding='utf-8') as f:
            content = f.read()
        existing = {m.group(1) for m in
                    re.finditer(r'tile\.solarpower\.([a-z_0-9]+)\.name=', content)}
        missing = [(tid, box_id(tid), box_lang_name(names[code][tid], code))
                   for tid in names[code] if box_id(tid) not in existing]
        if not missing:
            print('%s: lang already up to date' % code)
            continue
        with open(path, 'a', encoding='utf-8', newline='\n') as f:
            if not content.endswith('\n'):
                f.write('\n')
            f.write('\n# ===== 储电盒（gen_storages.py 追加）=====\n')
            for _, bid, name in missing:
                f.write('tile.solarpower.%s.name=%s\n' % (bid, name))
        print('%s: appended %d lang lines' % (code, len(missing)))


if __name__ == '__main__':
    import sys
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    accents = tier_accents()
    assert len(accents) == 67, len(accents)
    names = panel_lang_names(set(accents))
    for code in ('en_us', 'zh_cn'):
        missing_panels = set(accents) - set(names[code])
        assert not missing_panels, (code, missing_panels)
    if '--write' in sys.argv:
        for d in (TEX_DIR, BLOCKSTATE_DIR, BLOCK_MODEL_DIR, ITEM_MODEL_DIR):
            os.makedirs(d, exist_ok=True)
        for tid, accent in accents.items():
            bid = box_id(tid)
            front, side, topbottom = build(accent)
            front.save(os.path.join(TEX_DIR, bid + '_front.png'))
            side.save(os.path.join(TEX_DIR, bid + '_side.png'))
            topbottom.save(os.path.join(TEX_DIR, bid + '_topbottom.png'))
            subst = lambda obj: json.loads(json.dumps(obj).replace('{id}', bid))
            with open(os.path.join(BLOCKSTATE_DIR, bid + '.json'), 'w',
                      encoding='utf-8', newline='\n') as f:
                f.write(json.dumps(subst(blockstate_json()), indent=2) + '\n')
            with open(os.path.join(BLOCK_MODEL_DIR, bid + '.json'), 'w',
                      encoding='utf-8', newline='\n') as f:
                f.write(json.dumps(subst(block_model_json()), indent=2) + '\n')
            with open(os.path.join(ITEM_MODEL_DIR, bid + '.json'), 'w',
                      encoding='utf-8', newline='\n') as f:
                f.write(json.dumps(subst(item_model_json()), indent=2) + '\n')
        print('written %d box texture sets + models ->' % len(accents), TEX_DIR)
        append_lang_lines(names)
    # 预览：抽 8 档，每档 正面/侧面/顶底 三联
    picks = [list(accents)[i] for i in
             sorted(set(int(round(i * (len(accents) - 1) / 7)) for i in range(8)))]
    S, pad, cols = 8, 10, 3
    rows = len(picks)
    W = cols * (16 * S + pad) + pad
    H = rows * (16 * S + pad + 30) + pad
    sheet = Image.new('RGBA', (W, H), (48, 50, 56, 255))
    d = ImageDraw.Draw(sheet)
    for r, tid in enumerate(picks):
        accent = accents[tid]
        imgs = build(accent)
        labels = ['front', 'side', 'top/bottom']
        for c, im in enumerate(imgs):
            x = pad + c * (16 * S + pad)
            y = pad + r * (16 * S + pad + 30)
            big = im.resize((16 * S, 16 * S), Image.NEAREST)
            sheet.paste(big, (x, y), big)
            if r == 0:
                d.text((x, y - 12), labels[c], fill=(220, 226, 234, 255))
        d.text((pad, y + 16 * S + 2),
               '%s  #%02x%02x%02x' % (box_id(tid), *accent),
               fill=(240, 244, 250, 255))
    out = os.path.join(PREVIEW_DIR, 'storages_preview.png')
    sheet.save(out)
    print('preview ->', out)
