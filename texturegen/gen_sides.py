# -*- coding: utf-8 -*-
# 太阳能板侧面贴图生成器（15 级 × 16x144，9 帧动画）
#
# 设计：保留原版机框（竖纹+螺栓）与每级图案轮廓（glyphs/ 下的居中底图），
# 图案按形状内亮度排名映射到该级"深→基→亮→白热"四段色阶，
# 动画为呼吸脉动（峰值帧驻留，由已有 .mcmeta 控制）+ 峰值白色闪光。
#
# 用法：
#   py gen_sides.py           # 预览：preview/sides_preview.png + preview/sides_anim.gif
#   py gen_sides.py --write   # 写入项目 src/.../textures/block/*_side.png
from PIL import Image, ImageDraw
import json, os

BASE = os.path.dirname(os.path.abspath(__file__))
TEX_DIR = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                        'assets', 'solarpower', 'textures', 'block'))
PREVIEW_DIR = os.path.join(BASE, 'preview')
palettes = json.load(open(os.path.join(BASE, 'tier_palettes.json')))

WHITE = (255, 255, 255)
CASING = (33, 36, 38)                     # 图案调暗时向此色（机框深色）收敛
PULSE = [0.30, 0.45, 0.60, 0.80, 1.00, 0.80, 0.60, 0.45, 0.30]  # 9 帧亮度曲线
GLINT_FRAMES = (2, 4, 6)                  # 闪光出现帧（4 为驻留峰值帧）
GLINT_STRONG = 0.45                       # 峰值帧白闪强度
GLINT_WEAK = 0.25                         # 回声帧白闪强度

def mix(c1, c2, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(c1, c2))

def lum(c): return (c[0]*299 + c[1]*587 + c[2]*114) // 1000

def ramp(pal, t):
    deep, base, bright = pal['light'], pal['base'], pal['dark']
    if t >= 0.85:  return mix(bright, WHITE, (t - 0.85) / 0.15 * 0.7)
    if t >= 0.55:  return mix(base, bright, (t - 0.55) / 0.30)
    if t >= 0.25:  return mix(deep, base, (t - 0.25) / 0.30)
    return deep

def build_frame(pal, art, p, frame):
    """art: 16x16 居中底图；p: 当前帧脉动亮度 0..1；frame: 帧号"""
    src = art.load()
    casing_cols = set()                    # 机框色板取自底图上下边缘（无图案区）
    for x in range(16):
        for y in (0, 1, 2, 13, 14, 15):
            casing_cols.add(src[x, y])
    shape = [(x, y) for x in range(16) for y in range(16) if src[x, y] not in casing_cols]
    # 形状内亮度百分位排名 -> 完整色阶覆盖（窄亮度范围的形状也不会塌色）
    order_px = sorted(shape, key=lambda xy: lum(src[xy]))
    trank = {xy: i / max(1, len(order_px) - 1) for i, xy in enumerate(order_px)}
    img = Image.new('RGBA', (16, 16))
    px = img.load()
    for x in range(16):
        for y in range(16):
            c = src[x, y]
            if (x, y) not in set(shape):   # 机框：保留原像素
                px[x, y] = c
            else:
                px[x, y] = mix(CASING, ramp(pal, trank[(x, y)]), 0.35 + 0.65 * p) + (255,)
    if frame in GLINT_FRAMES:              # 最亮的几颗像素做白色闪光
        top = sorted(shape, key=lambda xy: -lum(src[xy]))[:5]
        for i, (x, y) in enumerate(top):
            if (i + frame) % 2 == 0:
                px[x, y] = mix(px[x, y][:3], WHITE, GLINT_STRONG if frame == 4 else GLINT_WEAK) + (255,)
    return img

def build_side(pal, art):
    sheet = Image.new('RGBA', (16, 144))
    for f, p in enumerate(PULSE):
        sheet.paste(build_frame(pal, art, p, f), (0, f * 16))
    return sheet

if __name__ == '__main__':
    import sys
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    order = list(palettes)
    arts = {n: Image.open(os.path.join(BASE, 'glyphs', n + '_f0.png')).convert('RGBA')
            for n in order}
    if '--write' in sys.argv:
        for n in order:
            build_side(palettes[n], arts[n]).save(os.path.join(TEX_DIR, n + '_side.png'))
        print('written %d sides ->' % len(order), TEX_DIR)
    else:
        S, pad, cols = 10, 6, 5
        rows = (len(order) + cols - 1) // cols
        W = cols * (16 * S + pad) + pad
        H = 2 * rows * (16 * S + pad + 14) + pad
        sheet = Image.new('RGBA', (W, H), (70, 70, 70, 255))
        d = ImageDraw.Draw(sheet)
        for i, n in enumerate(order):
            for j, f in ((0, 0), (1, 4)):   # 待机帧 / 峰值帧
                im = build_frame(palettes[n], arts[n], PULSE[f], f)
                x = pad + (i % cols) * (16 * S + pad)
                y = pad + (i // cols + j * rows) * (16 * S + pad + 14)
                sheet.paste(im.resize((16 * S, 16 * S), Image.NEAREST), (x, y))
                d.text((x + 2, y + 16 * S + 1),
                       n.replace('_solar_panel', '').replace('solar_panel', 'basic')
                       + (f' f{f}' if j else ''),
                       fill=(255, 255, 255, 255))
        sheet.save(os.path.join(PREVIEW_DIR, 'sides_preview.png'))
        gifs = ['solar_panel', 'advanced_solar_panel']
        Z = 10
        frames = []
        for f in range(9):
            row = Image.new('RGBA', (len(gifs) * 16 * Z + 12, 16 * Z + 8), (70, 70, 70, 255))
            for gi, n in enumerate(gifs):
                fr = build_frame(palettes[n], arts[n], PULSE[f], f).resize((16 * Z, 16 * Z), Image.NEAREST)
                row.paste(fr, (4 + gi * (16 * Z + 4), 4))
            frames.append(row)
        frames[0].save(os.path.join(PREVIEW_DIR, 'sides_anim.gif'), save_all=True,
                       append_images=frames[1:], duration=100, loop=0)
        print('preview ->', os.path.join(PREVIEW_DIR, 'sides_preview.png'))
        print('preview ->', os.path.join(PREVIEW_DIR, 'sides_anim.gif'))
