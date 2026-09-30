# -*- coding: utf-8 -*-
"""玻璃电缆贴图重做：IC2 风格玻璃光纤
- 半透明玻璃外壳 + 中央金属芯
- 顶部高光 / 底部阴影 / 侧缘暗边
- 微像素噪点模拟玻璃纤维质感
"""
import json
import os
from PIL import Image, ImageDraw

BASE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(BASE, ".."))

TIERS = [
    ("glass_cable",    "基础玻璃电缆",  (189, 147, 15),   (255, 217, 87),   (255, 243, 143)),
    ("glass_cable_2",  "混合玻璃电缆",  (131, 30, 156),   (203, 49, 241),   (250, 113, 255)),
    ("glass_cable_3",  "完美玻璃电缆",  (87, 87, 87),     (149, 149, 149),  (191, 191, 191)),
    ("glass_cable_4",  "量子玻璃电缆",  (169, 24, 12),    (227, 65, 65),    (255, 120, 106)),
    ("glass_cable_5",  "光谱玻璃电缆",  (21, 142, 45),    (48, 199, 48),    (89, 255, 89)),
    ("glass_cable_6",  "质子玻璃电缆",  (28, 13, 109),    (70, 46, 191),    (87, 90, 228)),
    ("glass_cable_7",  "奇异玻璃电缆",  (154, 154, 154),  (224, 224, 224),  (255, 255, 255)),
    ("glass_cable_8",  "衍射玻璃电缆",  (168, 160, 25),   (250, 243, 125),  (255, 251, 185)),
    ("glass_cable_9",  "光子玻璃电缆",  (17, 152, 148),   (60, 219, 215),   (123, 255, 251)),
    ("glass_cable_10", "中子玻璃电缆",  (67, 51, 12),     (111, 90, 37),    (160, 135, 72)),
    ("glass_cable_11", "无限玻璃电缆",  (58, 58, 58),     (83, 83, 83),     (110, 110, 110)),
]


def clamp(v):
    return max(0, min(255, int(v)))


def mix(a, b, t):
    return tuple(clamp(a[i] + (b[i] - a[i]) * t) for i in range(3))


def darken(c, t):
    return mix(c, (0, 0, 0), t)


def lighten(c, t):
    return mix(c, (255, 255, 255), t)


def glass_pixel(deep, mid, bright, x, y, noise=0):
    """16x16 玻璃光纤截面感：外缘暗、玻璃亮、中央金属芯、顶部高光。"""
    # 纵向明暗（顶亮底暗）
    vert = y / 15.0
    base = mix(bright, deep, vert * 0.85)

    # 横向：两侧玻璃壳，中间金属芯
    # 芯宽约 x=6..9（4px），玻璃壳 x=2..5 / 10..13，边缘 x=0..1 / 14..15
    if x <= 1 or x >= 14:
        # 玻璃外缘：更暗、更透
        col = darken(deep, 0.25)
        # 边缘偶发高光点
        if (x + y) % 7 == 0:
            col = lighten(mid, 0.35)
    elif x <= 5 or x >= 10:
        # 玻璃壳体
        t = 0.15 if x in (2, 5, 10, 13) else 0.45
        col = mix(base, bright, t)
        # 玻璃纤维纵向丝
        if y % 3 == 0 and x not in (0, 15):
            col = lighten(col, 0.12)
        # 玻璃壳上的细高光条（左壳 x=3，右壳 x=12）
        if x in (3, 12) and 2 <= y <= 12:
            col = lighten(bright, 0.35)
    else:
        # 金属芯 x=6..9
        # 芯两侧更深的“接缝”
        if x in (6, 9):
            col = darken(mid, 0.35)
        else:
            # 芯中央：亮金属 + 顶部更亮
            core_t = 0.55 if x == 7 else 0.35
            if y <= 2:
                core_t += 0.25
            elif y >= 13:
                core_t -= 0.2
            col = mix(mid, bright, core_t)
            # 金属芯中央细高光
            if x == 7 and 3 <= y <= 12:
                col = lighten(col, 0.4)
            if x == 8 and y in (4, 5, 8, 9):
                col = lighten(col, 0.15)

    # 玻璃壳顶部/底部封口感
    if y == 0:
        col = lighten(col, 0.35)
    elif y == 15:
        col = darken(col, 0.2)

    # 轻微像素噪点（玻璃纤维）
    if noise:
        n = ((x * 13 + y * 7) % 5) - 2
        col = tuple(clamp(c + n * noise) for c in col)

    return (col[0], col[1], col[2], 255)


def make_cable(tid, deep, mid, bright):
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = glass_pixel(deep, mid, bright, x, y, noise=3)
    return img


def save_both(src_root, dst_roots, img, name):
    for root in dst_roots:
        d = os.path.join(root, "src", "main", "resources", "assets", "solarpower", "textures", "block")
        os.makedirs(d, exist_ok=True)
        img.save(os.path.join(d, name + ".png"))


def main():
    roots = [
        os.path.normpath(os.path.join(ROOT)),  # modern
        os.path.normpath(os.path.join(ROOT, "forge-1.12.2")),
    ]
    preview_dir = os.path.join(BASE, "preview")
    os.makedirs(preview_dir, exist_ok=True)

    S, pad, cols = 8, 8, 6
    rows = (len(TIERS) + cols - 1) // cols
    W = cols * (16 * S + pad) + pad
    H = rows * (16 * S + pad + 18) + pad
    sheet = Image.new("RGBA", (W, H), (40, 42, 48, 255))
    draw = ImageDraw.Draw(sheet)

    for i, (tid, zh_name, deep, mid, bright) in enumerate(TIERS):
        img = make_cable(tid, deep, mid, bright)
        save_both(ROOT, roots, img, tid)
        x = pad + (i % cols) * (16 * S + pad)
        y = pad + (i // cols) * (16 * S + pad + 18)
        # 底衬让半透明感更明显
        draw.rectangle([x, y, x + 16 * S - 1, y + 16 * S - 1], fill=(55, 58, 66, 255))
        sheet.paste(img.resize((16 * S, 16 * S), Image.NEAREST), (x, y), img.resize((16 * S, 16 * S), Image.NEAREST))
        draw.text((x + 2, y + 16 * S + 2), zh_name, fill=(230, 230, 235, 255))

    out = os.path.join(preview_dir, "cables_preview.png")
    sheet.save(out)
    print("textures written to", len(roots), "projects")
    print("preview ->", out)


if __name__ == "__main__":
    main()
