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
    ("glass_cable", (87, 96, 105), (159, 176, 192), (217, 223, 230)),
    ("glass_cable_2", (43, 114, 108), (79, 209, 197), (185, 237, 232)),
    ("ultimate_hybrid_glass_cable", (69, 119, 47), (126, 217, 87), (203, 240, 188)),
    ("glass_cable_4", (42, 92, 140), (78, 168, 255), (184, 220, 255)),
    ("glass_cable_5", (96, 68, 140), (176, 124, 255), (223, 203, 255)),
    ("glass_cable_6", (140, 67, 95), (255, 123, 174), (255, 202, 223)),
    ("glass_cable_7", (110, 116, 123), (201, 212, 224), (233, 238, 243)),
    ("glass_cable_8", (140, 50, 50), (255, 92, 92), (255, 190, 190)),
    ("glass_cable_9", (140, 123, 56), (255, 224, 102), (255, 243, 194)),
    ("glass_cable_10", (78, 91, 110), (143, 166, 201), (210, 219, 233)),
    ("glass_cable_11", (132, 134, 140), (240, 244, 255), (249, 251, 255)),
    ("glass_cable_12", (140, 81, 35), (255, 148, 64), (255, 212, 179)),
    ("glass_cable_13", (26, 43, 139), (48, 79, 254), (172, 185, 255)),
    ("glass_cable_14", (140, 115, 40), (255, 210, 74), (255, 237, 183)),
    ("glass_cable_15", (0, 106, 92), (0, 194, 168), (153, 244, 229)),
    ("glass_cable_16", (140, 129, 32), (255, 235, 59), (255, 247, 177)),
    ("glass_cable_17", (86, 0, 140), (158, 0, 255), (216, 153, 255)),
    ("glass_cable_18", (53, 0, 128), (98, 0, 234), (192, 153, 247)),
    ("glass_cable_19", (13, 140, 140), (24, 255, 255), (163, 255, 255)),
    ("glass_cable_20", (71, 97, 140), (130, 177, 255), (205, 224, 255)),
    ("glass_cable_21", (140, 94, 35), (255, 171, 64), (255, 221, 179)),
    ("glass_cable_22", (57, 132, 95), (105, 240, 174), (195, 249, 223)),
    ("glass_cable_23", (140, 60, 35), (255, 110, 64), (255, 197, 179)),
    ("glass_cable_24", (97, 0, 0), (178, 0, 0), (213, 115, 115)),
    ("glass_cable_25", (97, 140, 48), (178, 255, 89), (224, 255, 189)),
    ("glass_cable_26", (150, 105, 25), (255, 214, 110), (255, 250, 235)),
    ("glass_cable_27", (25, 55, 130), (100, 165, 255), (215, 240, 255)),
    ("glass_cable_28", (75, 25, 120), (160, 100, 210), (245, 230, 250)),
    ("glass_cable_29", (150, 30, 35), (235, 175, 70), (255, 240, 200)),
    ("glass_cable_30", (70, 15, 10), (225, 105, 25), (255, 200, 120)),
    ("glass_cable_31", (25, 10, 45), (115, 45, 165), (230, 205, 250)),
    ("glass_cable_32", (18, 18, 18), (42, 42, 42), (96, 96, 96)),
]

NAMES = [
    "基础玻璃电缆", "混合玻璃电缆", "完美玻璃电缆", "量子玻璃电缆",
    "光谱玻璃电缆", "质子玻璃电缆", "奇异玻璃电缆", "衍射玻璃电缆",
    "光子玻璃电缆", "中子玻璃电缆", "无限玻璃电缆",
    "聚变玻璃电缆", "全能宇宙玻璃电缆", "真空衰变玻璃电缆", "悖论玻璃电缆",
    "神启玻璃电缆", "混沌玻璃电缆", "虚空裂隙玻璃电缆", "逆熵玻璃电缆",
    "觉醒玻璃电缆", "创世者玻璃电缆", "星海玻璃电缆", "永寂玻璃电缆",
    "终焉玻璃电缆",
    "终末玻璃电缆",
    "神光玻璃电缆",
    "天穹玻璃电缆",
    "万神玻璃电缆",
    "天命玻璃电缆",
    "天启玻璃电缆",
    "天终玻璃电缆",
    "永夜玻璃电缆",
]

DARK = (25, 25, 25, 255)

# 用户设计布局(2026-10-02,像素编辑器导出):
# 粗臂 + 亮带勾边能量脊;中心 2x2 白色能量节点;四角灰点;臂内 1px 透明缝增强玻璃感。
# 角色:E=描边 M=主体 B=亮带 D=深肋 W=中心白点 N=灰点 .=透明镂空
TEMPLATE = [
".....DMB.MD.....",
"..N..DM.BMD...N.",
".N...DMB.MD..N..",
".....DM..MD.....",
".....DM.BMD.....",
"DDDDDDMB.MDDDDDD",
"MMMMMMBBBBMMMMMM",
".B..B.B..B.B..B.",
"B..B.BB..BB..B.B",
"MMMMMMBBBBMMMMMM",
"DDDDDDMB.MDDDDDD",
".....DM.BMD.....",
".....DMB.MD.....",
".N...DM..MD..N..",
"..N..DM.BMD...N.",
".....DMB.MD.....",
]


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
