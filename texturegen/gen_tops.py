# -*- coding: utf-8 -*-
# 太阳能板顶面贴图生成器（15 级 × 16x16）
#
# 设计：深色机框（角码与侧面机框同色系）+ 3x3 倒角电池片 + 深色缝隙 + 斜向玻璃高光。
# 每级颜色来自 tier_palettes.json（base/base/light 为该级深/基/亮三色）。
#
# 用法：
#   py gen_tops.py           # 预览：生成 preview/tops_preview.png
#   py gen_tops.py --write   # 写入项目 src/.../textures/block/*_top.png
from PIL import Image, ImageDraw
import json, os

BASE = os.path.dirname(os.path.abspath(__file__))
TEX_DIR = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                        'assets', 'solarpower', 'textures', 'block'))
PREVIEW_DIR = os.path.join(BASE, 'preview')
palettes = json.load(open(os.path.join(BASE, 'tier_palettes.json')))

FRAME_DARK = (33, 36, 38)   # 机框深色（与侧面机框同族）
FRAME_LITE = (50, 52, 55)   # 机框角码
SEP = {4, 9}                # 缝隙行列（贴图内部 0..13 坐标）
SEP_DARKNESS = 0.85         # 缝隙深度（向最深色压多少）
SEAM_BLACK = (10, 11, 13)   # 缝隙再压暗的目标近黑色
SEAM_BLACKEN = 0.30         # 缝隙向近黑压暗的比例
WHITE = (255, 255, 255)

def mix(c1, c2, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(c1, c2))

def build(pal):
    deep, base, bright = pal['light'], pal['base'], pal['dark']
    c_dark = mix(mix(base, deep, SEP_DARKNESS), SEAM_BLACK, SEAM_BLACKEN)
    c_dk2 = mix(base, deep, 0.35)
    img = Image.new('RGBA', (16, 16))
    px = img.load()

    def put(x, y, c):
        px[x, y] = (c[0], c[1], c[2], 255)

    # 外圈机框，四角 3px 角码提亮（与侧面机框呼应）
    for i in range(16):
        for e in (0, 15):
            for a, b in ((e, i), (i, e)):
                corner = (a <= 2 or a >= 13) and (b <= 2 or b >= 13)
                put(a, b, FRAME_LITE if corner else FRAME_DARK)

    # 内部 14x14 玻璃区
    for ry in range(14):
        for rx in range(14):
            x, y = rx + 1, ry + 1
            if rx in SEP or ry in SEP:
                put(x, y, c_dark)
                continue
            band = lambda v: 0 if v < 4 else (1 if v < 9 else 2)
            ci, cj = band(rx), band(ry)
            lx = rx - (0, 5, 10)[ci]
            ly = ry - (0, 5, 10)[cj]
            if ly == 0:
                c = mix(base, bright, 0.30)      # 上倒角
            elif lx == 0:
                c = mix(base, bright, 0.15)      # 左倒角
            elif ly == 3:
                c = mix(base, deep, 0.45)        # 下阴影
            elif lx == 3:
                c = mix(base, deep, 0.25)        # 右阴影
            else:
                c = mix(base, deep, 0.12)        # 电池芯
                if (ci + cj) % 2 == 0 and lx == 1 and ly == 1:
                    c = mix(base, bright, 0.55)  # 交替内芯高光
                elif (ci + cj) % 2 == 1 and lx == 2 and ly == 2:
                    c = c_dk2                    # 交替内芯阴影
            # 斜向玻璃高光（两条对角光带）
            if (x + y) % 16 == 7:
                c = mix(c, WHITE, 0.28)
            elif (x + y) % 16 == 8:
                c = mix(c, WHITE, 0.12)
            put(x, y, c)
    return img

if __name__ == '__main__':
    import sys
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    if '--write' in sys.argv:
        for name in palettes:
            build(palettes[name]).save(os.path.join(TEX_DIR, name + '_top.png'))
        print('written %d tops ->' % len(palettes), TEX_DIR)
    else:
        order = list(palettes)
        S, pad, cols = 12, 6, 5
        rows = (len(order) + cols - 1) // cols
        W = cols * (16 * S + pad) + pad
        H = rows * (16 * S + pad + 14) + pad
        sheet = Image.new('RGBA', (W, H), (70, 70, 70, 255))
        d = ImageDraw.Draw(sheet)
        for i, name in enumerate(order):
            x = pad + (i % cols) * (16 * S + pad)
            y = pad + (i // cols) * (16 * S + pad + 14)
            sheet.paste(build(palettes[name]).resize((16 * S, 16 * S), Image.NEAREST), (x, y))
            d.text((x + 2, y + 16 * S + 1),
                   name.replace('_solar_panel', '').replace('solar_panel', 'basic'),
                   fill=(255, 255, 255, 255))
        out = os.path.join(PREVIEW_DIR, 'tops_preview.png')
        sheet.save(out)
        print('preview ->', out)
