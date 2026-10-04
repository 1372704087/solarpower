# -*- coding: utf-8 -*-
# 「XX变压器」方块家族生成器（65 档：桥接电压 T↔T+1，与 IU lv→超弦语义一致）。
#
# 模板：cable_pixel_editor.html 里的「变压器」图案（IU hv_transformer_front 红色
# 1:1 转写）作正面；侧面 = mfsu_front 设计（IU 变压器侧面与储能仓正面同图）。
# 与 gen_storages.py 相同：模板正本在本文件，编辑器图案与它同步。
#
# 换色：与储电盒一致——红三阶 M/R/B = 电压档 accent 的 deep(×0.545)/原色/混白 0.45。
#   accent 取 EuTier iuTier 对应的同档面板 accentColor（SolarTier 首个匹配 iuTier 的档）。
#   灯珠（O/W/N）与石灰（E/G/D/N）固定。每档 2 张贴图（正面/侧面，其余面复用侧面）。
#
# 产出：textures/block/<id>_front|_side.png、blockstates、models/block、models/item、
#       双语 lang（zh 前四档 IU 官方名：低压/中压/高压/超高压，其余 <ENUM> 变压器）。
#
# 用法：py gen_transformers.py [--write]
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
SRC_EUTIER = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'java',
                                           'com', 'example', 'solarpower', 'energy',
                                           'EuTier.java'))
SRC_SOLARTIER = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'java',
                                              'com', 'example', 'solarpower', 'solar',
                                              'SolarTier.java'))

SUFFIX = '_transformer'

# ===== 变压器正面（IU hv_transformer_front 转写，编辑器「变压器」图案同步） =====
TEMPLATE_FRONT = [
"DDDDDDBDDBDDDDDD",
"DDGEEGRMMRGEEGDD",
"DGEOOEGRRGEOOEGD",
"DEONWOEBBEONWOED",
"DEOWWOEBBEOWWOED",
"DGEOOEMEEMEOOEGD",
"BRGEEMEOOEMEEGRB",
"DMRBBEONWOEBBRMD",
"DMRBBEOWWOEBBRMD",
"BRGEEMEOOEMEEGRB",
"DGEOOEMEEMEOOEGD",
"DEONWOEBBEONWOED",
"DEOWWOEBBEOWWOED",
"DGEOOEGRRGEOOEGD",
"DDGEEGRMMRGEEGDD",
"DDDDDDBDDBDDDDDD",
]
# ===== 侧面（IU 变压器侧面 = mfsu_front，与储电盒正面同图） =====
TEMPLATE_SIDE = [
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

# 各面固定色（IU 原图采样）：正面有灯珠三色，侧面是储能仓正面配色（能量芯橙）
FIXED_FRONT = {
    'E': (0x3D, 0x3D, 0x3D), 'G': (0xD8, 0xDE, 0xDE), 'D': (0x9C, 0xAC, 0xAD),
    'O': (0xC9, 0xBD, 0x55), 'W': (0xF2, 0xF1, 0x70), 'N': (0xF5, 0xFF, 0xD1),
}
FIXED_SIDE = {
    'E': (0x3D, 0x3D, 0x3D), 'G': (0xD8, 0xDE, 0xDE), 'D': (0x9C, 0xAC, 0xAD),
    'N': (0xB9, 0xC3, 0xC3), 'O': (0xC4, 0x83, 0x01), 'W': (0xE7, 0xB7, 0x07),
}
DEEP_FACTOR = 0.545
BRIGHT_MIX = 0.45

# zh 名：IU 官方前四档，其余用枚举名
ZH_TABLE = {
    'LV': '低压变压器', 'MV': '中压变压器', 'HV': '高压变压器', 'EV': '超高压变压器',
}


def eu_tiers():
    """从 EuTier.java 提取 (enum名, iuTier)，按 iuTier 升序。"""
    src = open(SRC_EUTIER, encoding='utf-8').read()
    body = re.search(r'enum EuTier \{(.*?);', src, re.S).group(1)
    pairs = re.findall(r'([A-Z][A-Z0-9]*)\((\d+)\)', body)
    tiers = sorted((int(iu), name) for name, iu in pairs)
    assert len(tiers) == 66 and tiers[0] == (1, 'LV'), tiers[:3]
    return tiers


def accent_by_iu_tier():
    """SolarTier 首个匹配 iuTier 的 accentColor：{iuTier: (r,g,b)}。"""
    src = open(SRC_SOLARTIER, encoding='utf-8').read()
    starts = [m for m in re.finditer(r'^    [A-Z_0-9]+\("([a-z_0-9]+)",\s*(\d+),', src, re.M)]
    accents = {}
    for i, m in enumerate(starts):
        end = starts[i + 1].start() if i + 1 < len(starts) else len(src)
        hx = re.findall(r'0x([0-9A-Fa-f]{6})', src[m.start():end])
        assert len(hx) == 1
        iu = int(m.group(2))
        accents.setdefault(iu, tuple(int(hx[0][j:j + 2], 16) for j in (0, 2, 4)))
    return accents


def deep_of(accent):
    return tuple(round(c * DEEP_FACTOR) for c in accent)


def bright_of(accent):
    return tuple(round(c + (255 - c) * BRIGHT_MIX) for c in accent)


def build(accent):
    color_front = dict(FIXED_FRONT, M=deep_of(accent), R=accent, B=bright_of(accent))
    color_side = dict(FIXED_SIDE, M=deep_of(accent), R=accent, B=bright_of(accent))
    imgs = []
    for template, color in ((TEMPLATE_FRONT, color_front), (TEMPLATE_SIDE, color_side)):
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
            'up': 'solarpower:block/{id}_side',
            'down': 'solarpower:block/{id}_side',
            'particle': 'solarpower:block/{id}_side',
        },
    }


def item_model_json():
    return {'parent': 'solarpower:block/{id}'}


def append_lang_lines(entries):
    """entries: [(id, zh, en)]；两份 lang 幂等追加。"""
    for code, col in (('zh_cn', 1), ('en_us', 2)):
        path = os.path.join(LANG_DIR, code + '.lang')
        with open(path, encoding='utf-8') as f:
            content = f.read()
        existing = {m.group(1) for m in
                    re.finditer(r'tile\.solarpower\.([a-z_0-9]+)\.name=', content)}
        missing = []
        for bid, zh, en in entries:
            if bid in existing:
                continue
            missing.append((bid, zh if code == 'zh_cn' else en))
        if not missing:
            print('%s: lang already up to date' % code)
            continue
        with open(path, 'a', encoding='utf-8', newline='\n') as f:
            if not content.endswith('\n'):
                f.write('\n')
            f.write('\n# ===== 变压器（gen_transformers.py 追加）=====\n')
            for bid, name in missing:
                f.write('tile.solarpower.%s.name=%s\n' % (bid, name))
        print('%s: appended %d lang lines' % (code, len(missing)))


if __name__ == '__main__':
    import sys
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    tiers = [t for t in eu_tiers() if t[0] <= 65]
    assert len(tiers) == 65, len(tiers)
    accents = accent_by_iu_tier()
    if '--write' in sys.argv:
        for d in (TEX_DIR, BLOCKSTATE_DIR, BLOCK_MODEL_DIR, ITEM_MODEL_DIR):
            os.makedirs(d, exist_ok=True)
        entries = []
        for iu, name in tiers:
            bid = name.lower() + SUFFIX
            accent = accents[iu]
            front, side = build(accent)
            front.save(os.path.join(TEX_DIR, bid + '_front.png'))
            side.save(os.path.join(TEX_DIR, bid + '_side.png'))
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
            zh = ZH_TABLE.get(name, name + ' 变压器')
            entries.append((bid, zh, name + ' Transformer'))
        print('written %d transformer texture sets + models ->' % len(tiers), TEX_DIR)
        append_lang_lines(entries)
    # 预览：抽 8 档正/侧面
    picks = [tiers[int(round(i * (len(tiers) - 1) / 7))] for i in range(8)]
    S, pad, cols = 8, 10, 2
    rows = len(picks)
    W = cols * (16 * S + pad) + pad
    H = rows * (16 * S + pad + 30) + pad
    sheet = Image.new('RGBA', (W, H), (48, 50, 56, 255))
    d = ImageDraw.Draw(sheet)
    for r, (iu, name) in enumerate(picks):
        accent = accents[iu]
        imgs = build(accent)
        for c, im in enumerate(imgs):
            x = pad + c * (16 * S + pad)
            y = pad + r * (16 * S + pad + 30)
            big = im.resize((16 * S, 16 * S), Image.NEAREST)
            sheet.paste(big, (x, y), big)
            if r == 0:
                d.text((x, y - 12), ['front', 'side'][c], fill=(220, 226, 234, 255))
        d.text((pad, y + 16 * S + 2),
               '%s  #%02x%02x%02x' % (name.lower() + SUFFIX, *accent),
               fill=(240, 244, 250, 255))
    out = os.path.join(PREVIEW_DIR, 'transformers_preview.png')
    sheet.save(out)
    print('preview ->', out)
