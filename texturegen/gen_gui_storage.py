# -*- coding: utf-8 -*-
# 「XX储电盒」GUI 底图生成器（自定义样式，区别于太阳能板界面）。
#
# 布局（176×166，256×256 画布）：
#   (8,6)   标题（Java 层画，档位主题色）
#   (8,20)  160×16 能量条井：内嵌凹槽，Java 层按存量比例画主题色填充
#   (8,44)  三行数据（Java 层画）：蓄电量 / 容量 / 输出速率
#   玩家背包：3×9 槽 (8,84) + 热栏 (8,142)，标准 18px 栅格
#
# 用法：
#   py gen_gui_storage.py           # 只出预览（preview/gui_storage_mock.png，含假数据模拟）
#   py gen_gui_storage.py --write   # 写入 textures/gui/storage_box.png
from PIL import Image, ImageDraw, ImageFont
import os

BASE = os.path.dirname(os.path.abspath(__file__))
GUI_DIR = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                        'assets', 'solarpower', 'textures', 'gui'))
PREVIEW_DIR = os.path.join(BASE, 'preview')

W, H = 176, 166
CANVAS = 256

# 原版容器配色
BG = (198, 198, 198, 255)
BEVEL_LIGHT = (255, 255, 255, 255)
BEVEL_DARK = (85, 85, 85, 255)
BEVEL_BLACK = (0, 0, 0, 255)
SLOT_DARK = (55, 55, 55, 255)
SLOT_MID = (139, 139, 139, 255)


def bevel_panel(d, x0, y0, x1, y1):
    """凸起面板：外圈黑、上左亮、下右暗。"""
    d.rectangle([x0, y0, x1, y1], fill=BEVEL_DARK)
    d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=BEVEL_LIGHT)
    d.rectangle([x0 + 1, y1 - 2, x1 - 1, y1 - 1], fill=BEVEL_DARK)
    d.rectangle([x1 - 2, y0 + 1, x1 - 1, y1 - 1], fill=BEVEL_DARK)
    d.rectangle([x0 + 1, y0 + 1, x1 - 2, y1 - 2], fill=BG)


def bevel_slot(d, x, y):
    """物品槽（18×18 格）：上左暗、下右亮、内部中灰。"""
    d.rectangle([x, y, x + 17, y + 17], fill=SLOT_DARK)
    d.rectangle([x, y, x + 16, y + 16], fill=SLOT_MID)
    d.rectangle([x + 1, y + 1, x + 16, y + 16], fill=BEVEL_LIGHT)
    d.rectangle([x + 1, y + 1, x + 15, y + 15], fill=SLOT_MID)


def build_texture():
    img = Image.new('RGBA', (CANVAS, CANVAS), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    bevel_panel(d, 0, 0, W, H)

    # 能量条井：凹陷框（ Java 层在 (10,22)-(166,34) 画主题色填充）
    d.rectangle([7, 19, 169, 37], fill=SLOT_DARK)
    d.rectangle([8, 20, 168, 36], fill=SLOT_MID)

    # 数据区与背包区之间的分隔线
    d.rectangle([8, 78, 167, 78], fill=SLOT_MID)

    # 玩家背包 3×9 + 热栏
    for row in range(3):
        for col in range(9):
            bevel_slot(d, 8 + col * 18, 84 + row * 18)
    for col in range(9):
        bevel_slot(d, 8 + col * 18, 142)
    return img


def mock(img, path):
    """3× 放大 + 假数据模拟（标题/能量条/三行文本），给用户过目的设计预览。"""
    S = 3
    big = img.resize((CANVAS * S, CANVAS * S), Image.NEAREST).convert('RGB')
    d = ImageDraw.Draw(big)
    try:
        zh = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 13 * S // 2)
        zh_small = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 6 * S)
    except OSError:
        zh = zh_small = ImageFont.load_default()
    accent = (224, 180, 92)          # 高级太阳能面板的主题色做示例
    dark = (40, 44, 52)
    # 标题
    d.text((8 * S, 5 * S), '高级储电盒', font=zh, fill=dark)
    # 能量条填充（62%）
    frac = 0.62
    fill_w = round(156 * S * frac)
    d.rectangle([10 * S, 22 * S, 10 * S + fill_w, 34 * S - 1], fill=accent)
    # 三行数据
    rows = [
        '蓄电量：3.02 万 / 4.88 万 EU',
        '容量：4.88 万 EU',
        '输出：128 EU/t（EV）',
    ]
    y = 42
    for text in rows:
        d.text((8 * S, y * S), text, font=zh_small, fill=(63, 68, 78))
        y += 11
    d.text((8 * S, 74 * S), '物品栏', font=zh_small, fill=(63, 68, 78))
    big.save(path)


if __name__ == '__main__':
    import sys
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    img = build_texture()
    mock(img, os.path.join(PREVIEW_DIR, 'gui_storage_mock.png'))
    print('preview ->', os.path.join(PREVIEW_DIR, 'gui_storage_mock.png'))
    if '--write' in sys.argv:
        os.makedirs(GUI_DIR, exist_ok=True)
        out = os.path.join(GUI_DIR, 'storage_box.png')
        img.save(out)
        print('written ->', out)
