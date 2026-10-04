# -*- coding: utf-8 -*-
# 「XX变压器」GUI 底图生成器（与 gen_gui_storage.py 同风格，双能量条布局）。
#
# 布局（190 高，256×256 画布，字体 9px）：
#   (8,6)   标题（Java 层画，低压档主题色）
#   (8,19)  160×14 低压池条井（Java 层填低压档 accent，fill (10,21)-(166,31)）
#   (8,37)  低压池存量行
#   (8,49)  160×14 高压池条井（Java 层填高压档 accent，fill (10,51)-(166,61)）
#   (8,67)  高压池存量行
#   (8,79)  吞吐行；(8,90) 电压桥接行
#   (8,100) 分隔线； (8,102) 物品栏标签
#   玩家背包：3×9 槽 (8,113/131/149) + 热栏 (8,167)，标准 18px 栅格（ySize=190）
#
# 用法：py gen_gui_transformer.py [--write]
from PIL import Image, ImageDraw, ImageFont
import os

BASE = os.path.dirname(os.path.abspath(__file__))
GUI_DIR = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                        'assets', 'solarpower', 'textures', 'gui'))
PREVIEW_DIR = os.path.join(BASE, 'preview')

W, H = 176, 190
CANVAS = 256

BG = (198, 198, 198, 255)
BEVEL_LIGHT = (255, 255, 255, 255)
BEVEL_DARK = (85, 85, 85, 255)
BEVEL_BLACK = (0, 0, 0, 255)
SLOT_DARK = (55, 55, 55, 255)
SLOT_MID = (139, 139, 139, 255)


def bevel_panel(d, x0, y0, x1, y1):
    d.rectangle([x0, y0, x1, y1], fill=BEVEL_DARK)
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=BEVEL_LIGHT)
    d.rectangle([x0 + 1, y1 - 2, x1 - 1, y1 - 1], fill=BEVEL_DARK)
    d.rectangle([x1 - 2, y0 + 1, x1 - 1, y1 - 1], fill=BEVEL_DARK)
    d.rectangle([x0 + 1, y0 + 1, x1 - 2, y1 - 2], fill=BG)


def bevel_slot(d, x, y):
    d.rectangle([x, y, x + 17, y + 17], fill=SLOT_DARK)
    d.rectangle([x, y, x + 16, y + 16], fill=SLOT_MID)
    d.rectangle([x + 1, y + 1, x + 16, y + 16], fill=BEVEL_LIGHT)
    d.rectangle([x + 1, y + 1, x + 15, y + 15], fill=SLOT_MID)


def build_texture():
    img = Image.new('RGBA', (CANVAS, CANVAS), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bevel_panel(d, 0, 0, W, H)
    # 两条能量池井（Java 层在 (10,21)-(166,31) / (10,51)-(166,61) 填色）
    for y0 in (19, 49):
        d.rectangle([7, y0, 169, y0 + 14], fill=SLOT_DARK)
        d.rectangle([8, y0 + 1, 168, y0 + 13], fill=SLOT_MID)
    # 数据区与背包区分隔线
    d.rectangle([8, 100, 167, 100], fill=SLOT_MID)
    # 玩家背包 3×9 + 热栏（ySize=190：主行 113/131/149，热栏 167）
    for row in range(3):
        for col in range(9):
            bevel_slot(d, 8 + col * 18, 113 + row * 18)
    for col in range(9):
        bevel_slot(d, 8 + col * 18, 167)
    return img


def mock(img, path):
    S = 3
    big = img.resize((CANVAS * S, CANVAS * S), Image.NEAREST).convert('RGB')
    d = ImageDraw.Draw(big)
    try:
        zh = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 13 * S // 2)
        zh_small = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 6 * S)
    except OSError:
        zh = zh_small = ImageFont.load_default()
    accent_lo = (79, 209, 197)     # 混合档 accent（低压侧示例）
    accent_hi = (224, 180, 92)     # 高级档 accent（高压侧示例）
    dark = (40, 44, 52)
    d.text((8 * S, 5 * S), '混合变压器', font=zh, fill=dark)
    # 低压池 46%（fill 区 10..166 × 12 高：21..33 / 51..63）
    d.rectangle([10 * S, 21 * S, 10 * S + round(156 * S * 0.46), 31 * S], fill=accent_lo)
    d.rectangle([10 * S, 51 * S, 10 * S + round(156 * S * 0.71), 61 * S], fill=accent_hi)
    rows = [
        (37, '低压池：24,060 / 52,428 EU（混合侧 T2）'),
        (67, '高压池：37,124 / 52,428 EU（高级侧 T1）'),
        (79, '吞吐：128 EU/t（双向）'),
        (90, '电压：混合 T2 (512) ↔ 高级 T1 (128)'),
    ]
    for y, text in rows:
        d.text((8 * S, y * S), text, font=zh_small, fill=(63, 68, 78))
    d.text((8 * S, 101 * S), '物品栏', font=zh_small, fill=(63, 68, 78))
    big.save(path)


if __name__ == '__main__':
    import sys
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    img = build_texture()
    mock(img, os.path.join(PREVIEW_DIR, 'gui_transformer_mock.png'))
    print('preview ->', os.path.join(PREVIEW_DIR, 'gui_transformer_mock.png'))
    if '--write' in sys.argv:
        os.makedirs(GUI_DIR, exist_ok=True)
        out = os.path.join(GUI_DIR, 'transformer.png')
        img.save(out)
        print('written ->', out)
