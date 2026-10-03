# -*- coding: utf-8 -*-
# 「XX核心」物品贴图生成器（66 张 16x16 + 对应物品模型 JSON）
#
# 模板：用户在拓展版 cable_pixel_editor.html 里设计的核心主模板（2026-10-03 导出）。
# 角色：E=描边 M=主体 B=亮带 D=深肋 W=中心白点 N=灰点 .=透明
# 配色（用户指定）：绿色/深绿色是变体——M 主体取该档面板主题色（accentColor）、
# D 深肋取主题色 ×0.55（与电缆 deep 规则一致）；其余颜色全档固定：
#   E=#191b20 描边  B=#fff38f 亮带  W=#f7f7f7 白点  N=#bfbbc4 灰点。
# 2 档起各一枚（IU 激发核从"高级"开始，基础档无本体不生成）。
#
# 用法：
#   py gen_cores.py           # 预览：preview/cores_preview.png
#   py gen_cores.py --write   # 写入 textures/item/*.png + models/item/*.json
from PIL import Image, ImageDraw
import json, os, re

BASE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                       'assets', 'solarpower'))
TEX_DIR = os.path.join(ASSETS, 'textures', 'item')
MODEL_DIR = os.path.join(ASSETS, 'models', 'item')
PREVIEW_DIR = os.path.join(BASE, 'preview')
SRC_TIER = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'java',
                                         'com', 'example', 'solarpower', 'solar',
                                         'SolarTier.java'))

# ===== 主模板(用户设计) =====
TEMPLATE = [
"........B.......",
"..B..B.BN.B..B..",
".BBBENEEEENE.BB.",
"...NEEEDEDEENB..",
"..EEDMMMEMEDEE..",
".BNEEEEMEMEMENB.",
"..EDMMMNNMEMEE..",
"BNEEEEN..NMMDEB.",
".BEDMMN..NEEEENB",
"..EEMEMNNMMMDE..",
".BNEMEMEMEEEENB.",
"..EEDEMEMMMDEE..",
"..BNEEDEDEEEN...",
".BB.ENEEEENEBBB.",
"..B..B.NB.B..B..",
".......B........",
]
# 角色: E=描边 M=主体 B=亮带 D=深肋 W=中心白点 N=灰点 .=透明

FIXED = {
    'E': (25, 27, 32),      # #191b20 描边
    'B': (255, 243, 143),   # #fff38f 亮带
    'W': (247, 247, 247),   # #f7f7f7 白点
    'N': (191, 187, 196),   # #bfbbc4 灰点
}
DEEP_FACTOR = 0.55      # D 深肋 = 主题色 ×0.55（与电缆 deep 规则一致）


def tier_accents():
    """从 SolarTier.java 逐档提取 accentColor（每个常量块里唯一的 0xRRGGBB）。"""
    src = open(SRC_TIER, encoding='utf-8').read()
    starts = [m for m in re.finditer(r'^    [A-Z_]+\("([a-z_]+)",', src, re.M)]
    accents = {}
    for i, m in enumerate(starts):
        end = starts[i + 1].start() if i + 1 < len(starts) else len(src)
        hexes = re.findall(r'0x([0-9A-Fa-f]{6})', src[m.start():end])
        assert len(hexes) == 1, (m.group(1), hexes)
        hx = hexes[0]
        accents[m.group(1)] = tuple(int(hx[j:j + 2], 16) for j in (0, 2, 4))
    return accents


def core_id(tier_id):
    return tier_id[:-len('_solar_panel')] + '_core'


def build(accent):
    """用户模板 → 该档核心贴图：M=主题色 D=主题色×0.55 其余固定。"""
    deep = tuple(round(c * DEEP_FACTOR) for c in accent)
    color = dict(FIXED, M=accent, D=deep)
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(TEMPLATE):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch == '.':
                continue
            px[x, y] = color[ch] + (255,)
    return img


if __name__ == '__main__':
    import sys
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    accents = tier_accents()
    assert len(accents) == 67, len(accents)
    ids = [t for t in accents if t != 'solar_panel']      # 基础档不生成
    assert len(ids) == 66, len(ids)
    if '--write' in sys.argv:
        os.makedirs(TEX_DIR, exist_ok=True)
        os.makedirs(MODEL_DIR, exist_ok=True)
        for t in ids:
            cid = core_id(t)
            build(accents[t]).save(os.path.join(TEX_DIR, cid + '.png'))
            with open(os.path.join(MODEL_DIR, cid + '.json'), 'w', encoding='utf-8',
                      newline='\n') as f:
                f.write(json.dumps({
                    'parent': 'builtin/generated',
                    'textures': {'layer0': 'solarpower:item/' + cid},
                }, indent=2) + '\n')
        print('written %d cores ->' % len(ids), TEX_DIR)
    else:
        S, pad, cols = 8, 6, 8
        rows = (len(ids) + cols - 1) // cols
        W = cols * (16 * S + pad) + pad
        H = rows * (16 * S + pad + 14) + pad
        sheet = Image.new('RGBA', (W, H), (70, 70, 70, 255))
        d = ImageDraw.Draw(sheet)
        for i, t in enumerate(ids):
            x = pad + (i % cols) * (16 * S + pad)
            y = pad + (i // cols) * (16 * S + pad + 14)
            im = build(accents[t]).resize((16 * S, 16 * S), Image.NEAREST)
            sheet.paste(im, (x, y), im)
            d.text((x + 2, y + 16 * S + 1),
                   core_id(t).replace('_core', ''), fill=(255, 255, 255, 255))
        out = os.path.join(PREVIEW_DIR, 'cores_preview.png')
        sheet.save(out)
        print('preview ->', out)
