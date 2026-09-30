# -*- coding: utf-8 -*-
"""按 Industrial Upgrade 原版像素结构重做玻璃电缆贴图。

结构（与 IU glass_cable.png 一致）：
- 透明背景
- 暗边框 (25,25,25)
- 横线 y=6..9 + 竖线 x=6..9 十字
- 线芯用 deep/mid/bright
物品图标：水平线缆段（IC2/IU 风格）
"""
import os
from PIL import Image, ImageDraw

ROOTS = [
    r"G:\solarpower",
    r"G:\solarpower\forge-1.12.2",
]

# 我们的 id + IU 观感配色 deep / mid / bright
MAP = [
    ("glass_cable",    (189, 147, 15),  (255, 217, 87),  (255, 243, 143)),
    ("glass_cable_2",  (131, 30, 156),  (203, 49, 241),  (250, 113, 255)),
    ("glass_cable_3",  (87, 87, 87),    (149, 149, 149), (191, 191, 191)),
    ("glass_cable_4",  (169, 24, 12),   (227, 65, 65),   (255, 120, 106)),
    ("glass_cable_5",  (21, 142, 45),   (48, 199, 48),   (89, 255, 89)),
    ("glass_cable_6",  (28, 13, 109),   (70, 46, 191),   (87, 90, 228)),
    ("glass_cable_7",  (154, 154, 154), (224, 224, 224), (255, 255, 255)),
    ("glass_cable_8",  (168, 160, 25),  (250, 243, 125), (255, 251, 185)),
    ("glass_cable_9",  (17, 152, 148),  (60, 219, 215),  (123, 255, 251)),
    ("glass_cable_10", (67, 51, 12),    (111, 90, 37),   (160, 135, 72)),
    ("glass_cable_11", (58, 58, 58),    (83, 83, 83),    (110, 110, 110)),
    # ---- 扩展 13 档（对应太阳能 12~59 档分组） ----
    ("glass_cable_12", (168, 74, 16),   (232, 120, 32),  (255, 180, 90)),
    ("glass_cable_13", (76, 28, 140),   (128, 52, 214),  (190, 120, 255)),
    ("glass_cable_14", (110, 20, 150),  (172, 44, 220),  (225, 120, 255)),
    ("glass_cable_15", (158, 18, 84),   (226, 44, 132),  (255, 125, 190)),
    ("glass_cable_16", (170, 140, 40),  (240, 208, 80),  (255, 244, 160)),
    ("glass_cable_17", (94, 16, 130),   (160, 36, 210),  (220, 110, 255)),
    ("glass_cable_18", (28, 16, 96),    (56, 40, 170),   (110, 95, 235)),
    ("glass_cable_19", (16, 130, 140),  (44, 205, 215),  (130, 250, 250)),
    ("glass_cable_20", (28, 96, 180),   (58, 150, 235),  (140, 210, 255)),
    ("glass_cable_21", (24, 130, 40),   (52, 196, 72),   (130, 250, 140)),
    ("glass_cable_22", (20, 40, 120),   (42, 74, 190),   (100, 140, 250)),
    ("glass_cable_23", (70, 74, 84),    (128, 134, 148), (190, 198, 214)),
    ("glass_cable_24", (150, 128, 70),  (222, 198, 130), (255, 242, 190)),
]

NAMES = [
    "基础玻璃电缆", "混合玻璃电缆", "完美玻璃电缆", "量子玻璃电缆",
    "光谱玻璃电缆", "质子玻璃电缆", "奇异玻璃电缆", "衍射玻璃电缆",
    "光子玻璃电缆", "中子玻璃电缆", "无限玻璃电缆",
    "聚变玻璃电缆", "全能宇宙玻璃电缆", "真空衰变玻璃电缆", "悖论玻璃电缆",
    "神启玻璃电缆", "混沌玻璃电缆", "虚空裂隙玻璃电缆", "逆熵玻璃电缆",
    "觉醒玻璃电缆", "创世者玻璃电缆", "星海玻璃电缆", "永寂玻璃电缆",
    "终末玻璃电缆",
]

DARK = (25, 25, 25, 255)

# 自研"肋状芯"结构（程序化构建，非 AE2 几何）：
# 6px 臂；描边 E 虚线化（隔像素镂空 → 玻璃透光感）；芯线 B 亮带/D 深肋交替；
# 中心 2×2 白色能量节点 W（固定）；四角斜向灰点 N（固定）。
# 角色：E=描边 M=主体 B=亮带 D=深肋 W=中心白点 N=灰点 .=透明镂空
_g = [['.'] * 16 for _ in range(16)]

def _put(x, y, ch):
    _g[y][x] = ch

for x in range(16):                      # 横臂 y=5..10
    for y in range(5, 11):
        if y in (5, 10):
            _put(x, y, 'E' if x % 2 == 0 else '.')   # 虚线描边
        elif y in (7, 8):
            if x in (7, 8):
                _put(x, y, 'W')
            elif x in (6, 9):
                _put(x, y, 'B')
            else:
                _put(x, y, 'D' if x % 4 == 3 else 'B')
        else:                            # y == 6, 9
            if x in (6, 9):
                _put(x, y, 'B')
            else:
                _put(x, y, 'D' if x % 4 == 3 else 'M')

for y in range(16):                      # 竖臂 x=5..10（跳过横臂已写区域）
    for x in range(5, 11):
        if 5 <= y <= 10:
            continue
        if x in (5, 10):
            _put(x, y, 'E' if y % 2 == 0 else '.')   # 虚线描边
        elif x in (7, 8):
            _put(x, y, 'B' if y % 4 != 3 else 'D')
        else:                            # x == 6, 9
            _put(x, y, 'D' if y % 4 == 3 else 'M')

for dx, dy in ((1, 2), (2, 1), (13, 2), (14, 1),
               (1, 13), (2, 14), (13, 13), (14, 14)):
    _put(dx, dy, 'N')                    # 四角斜向灰点

TEMPLATE = [''.join(row) for row in _g]


def mix3(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def ae2_colors(deep, mid, bright):
    """由档位三色推导模板各角色的实际颜色。"""
    return {
        'E': mix3(deep, (10, 11, 13), 0.40) + (255,),  # 描边（虚线，间隙镂空）
        'M': mid + (255,),
        'B': bright + (255,),
        'D': deep + (255,),
        'W': (247, 247, 247, 255),                     # 中心白点：固定
        'N': (191, 189, 196, 255),                     # 四角灰点：固定
    }


def make_block_iu(deep, mid, bright):
    """方块贴图：与物品一致，透明镂空（方块走 CUTOUT 渲染层做 alpha 测试）。"""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cols = ae2_colors(deep, mid, bright)
    for y, row in enumerate(TEMPLATE):
        for x, ch in enumerate(row):
            if ch != '.':
                px[x, y] = cols[ch]
    return img


def make_item_iu(deep, mid, bright):
    """AE2 风格物品图标：同一结构，保留透明抖动（物品渲染支持 alpha）。"""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    cols = ae2_colors(deep, mid, bright)
    for y, row in enumerate(TEMPLATE):
        for x, ch in enumerate(row):
            if ch != '.':
                px[x, y] = cols[ch]
    return img


def write_png(root, rel, img):
    path = os.path.join(root, "src", "main", "resources", "assets", "solarpower", rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
    return path


def main():
    preview = []
    for i, (tid, deep, mid, bright) in enumerate(MAP):
        block = make_block_iu(deep, mid, bright)
        item = make_item_iu(deep, mid, bright)
        for root in ROOTS:
            write_png(root, "textures/block/" + tid + ".png", block)
            write_png(root, "textures/item/" + tid + ".png", item)
        preview.append((tid, NAMES[i], block, item))
        print(tid, "deep", deep, "mid", mid, "bright", bright)

    S, pad, cols = 10, 8, 6
    cell = 16 * S + pad
    W = cols * cell + pad
    H = 2 * (16 * S + 28) + pad
    sheet = Image.new("RGBA", (W, H), (48, 50, 56, 255))
    draw = ImageDraw.Draw(sheet)
    for i, (tid, zh, block, item) in enumerate(preview):
        col = i % cols
        x = pad + col * cell
        y0 = pad
        draw.rectangle([x, y0, x + 16 * S - 1, y0 + 16 * S - 1], fill=(70, 72, 80, 255))
        b = block.resize((16 * S, 16 * S), Image.NEAREST)
        sheet.paste(b, (x, y0), b)
        draw.text((x + 2, y0 + 16 * S + 2), zh + " 方块", fill=(230, 230, 235, 255))
        y1 = pad + 16 * S + 28
        draw.rectangle([x, y1, x + 16 * S - 1, y1 + 16 * S - 1], fill=(70, 72, 80, 255))
        it = item.resize((16 * S, 16 * S), Image.NEAREST)
        sheet.paste(it, (x, y1), it)
        draw.text((x + 2, y1 + 16 * S + 2), zh + " 物品", fill=(230, 230, 235, 255))

    out = os.path.join(r"G:\solarpower\texturegen\preview", "cables_iu_style.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sheet.save(out)
    print("preview ->", out)


if __name__ == "__main__":
    main()
