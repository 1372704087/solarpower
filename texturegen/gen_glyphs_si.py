# -*- coding: utf-8 -*-
# 侧面图案底图生成器（全 60 档统一 X 徽标）。
#
# 做法：从基础板 glyph 提取"纯机框"画布（去掉基础板的十字图案），
# 再画上 10x10 的 X 交叉徽标（交点白热）。颜色只用中性灰阶（深/中/亮/白热），
# gen_sides.py 会按形状内亮度排名重新映射到该级色阶，因此这里只需保证
# 相对明暗正确，且不能使用机框本身的颜色（否则会被当成机框保留）。
#
# 档位清单直接取 tier_palettes.json 的键（与配色表始终保持一致）。
#
# 用法：
#   py gen_glyphs_si.py           # 预览：preview/glyphs_si_preview.png
#   py gen_glyphs_si.py --write   # 写入 glyphs/<等级>_f0.png
from PIL import Image, ImageDraw
import json, os

BASE = os.path.dirname(os.path.abspath(__file__))
GLYPH_DIR = os.path.join(BASE, 'glyphs')
PREVIEW_DIR = os.path.join(BASE, 'preview')

# 中性四阶：d 深 / m 中 / b 亮 / w 白热（亮度递增，供色阶排名用）
TONES = {
    'd': (56, 58, 68),
    'm': (94, 96, 108),
    'b': (205, 208, 218),
    'w': (246, 248, 255),
}

# 10x10 徽标，'.' = 保留机框。原点 (3,3)，即机框正中的 14x14 内区。
# X 交叉，交点白热。
X_GRID = [
    'bb.....bdd',
    '.bb...bdd.',
    '..bb.bdd..',
    '...bbdd...',
    '....bb....',
    '....bb....',
    '...bddb...',
    '..bdd.bb..',
    '.bdd...bb.',
    'bdd.....bb',
]

# 全部档位 = 配色表的键（60 个，按枚举顺序）
TIERS = list(json.load(open(os.path.join(BASE, 'tier_palettes.json'),
                            encoding='utf-8')))
EMBLEMS = {name: X_GRID for name in TIERS}


def frame_canvas():
    """从基础板 glyph 反推纯机框：cols 3-12、rows 3-12 内不等于本列
    row-13 基准色的像素即图案，用基准色盖掉。"""
    src = Image.open(os.path.join(GLYPH_DIR, 'solar_panel_f0.png')).convert('RGBA')
    out = src.copy()
    px = out.load()
    for x in range(3, 13):
        ref = src.getpixel((x, 13))
        for y in range(3, 13):
            if px[x, y] != ref:
                px[x, y] = ref
    return out


def build(name, grid):
    img = frame_canvas()
    px = img.load()
    for ry, row in enumerate(grid):
        assert len(row) == 10, '%s row %d: %r' % (name, ry, row)
        for rx, ch in enumerate(row):
            if ch != '.':
                px[3 + rx, 3 + ry] = TONES[ch] + (255,)
    return img


if __name__ == '__main__':
    import sys
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    S, pad = 12, 6
    names = list(EMBLEMS)
    cols = 10
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * (16 * S + pad) + pad,
                               rows * (16 * S + pad + 14) + pad), (70, 70, 70, 255))
    d = ImageDraw.Draw(sheet)
    for i, name in enumerate(names):
        img = build(name, EMBLEMS[name])
        x = pad + (i % cols) * (16 * S + pad)
        y = pad + (i // cols) * (16 * S + pad + 14)
        sheet.paste(img.resize((16 * S, 16 * S), Image.NEAREST), (x, y))
        d.text((x + 2, y + 16 * S + 1), name.replace('_solar_panel', ''),
               fill=(255, 255, 255, 255))
    if '--write' in sys.argv:
        for name in names:
            build(name, EMBLEMS[name]).save(os.path.join(GLYPH_DIR, name + '_f0.png'))
        print('written %d glyphs ->' % len(names), GLYPH_DIR)
    out = os.path.join(PREVIEW_DIR, 'glyphs_si_preview.png')
    sheet.save(out)
    print('preview ->', out)
