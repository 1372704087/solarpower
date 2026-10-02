# -*- coding: utf-8 -*-
# 分子重组仪 + 太阳能合成物品 贴图生成器（原创像素画，配色参考 ASP 的品阶色彩语言）
#
# 生成内容（全部写入 src/main/resources/assets/solarpower/textures/）：
#   block/molecular_transformer_{side,side_active,top,bottom}.png   机器方块 16x16
#   item/<14 个合成物品>.png                                        物品 16x16
#   gui/molecular_transformer.png                                   界面底图 256x166（主区 176x166 + 辅助件）
#
# 用法：
#   py gen_mt.py           # 预览：preview/mt_items.png + mt_machine.png + mt_gui.png
#   py gen_mt.py --write   # 写入项目贴图
from PIL import Image, ImageDraw
import os, math

BASE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                    'assets', 'solarpower', 'textures'))

def mix(c1, c2, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(c1[:3], c2[:3]))

def canvas():
    return Image.new('RGBA', (16, 16), (0, 0, 0, 0))

# ---------------------------------------------------------------- 物件模板

def plate(base, light, dark, style='plain', brace=(242, 169, 60)):
    """12x12 圆角金属板：左上受光、右下背光、四角铆钉。
    style: plain / sheen(斜向白光带) / slashes(斜向加强筋) / cross(橙色 X 形撑条)"""
    img = canvas(); px = img.load()
    for x in range(2, 14):
        for y in range(2, 14):
            c = base
            if x in (2, 3) or y in (2, 3):      c = mix(c, light, 0.45)
            if x in (12, 13) or y in (12, 13):  c = mix(c, dark, 0.55)
            px[x, y] = c + (255,)
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):   # 铆钉
        px[cx, cy] = mix(dark, (0, 0, 0), 0.35) + (255,)
    if style == 'sheen':
        for x in range(2, 14):
            for y in range(2, 14):
                if (x + y) in (8, 9):
                    px[x, y] = mix(px[x, y], (255, 255, 255), 0.5) + (255,)
    elif style == 'slashes':
        for x in range(2, 14):
            for y in range(2, 14):
                if (x - y) in (-3, 0, 3):
                    px[x, y] = mix(px[x, y], dark, 0.7) + (255,)
                elif (x - y) in (-2, 1, 4):
                    px[x, y] = mix(px[x, y], light, 0.35) + (255,)
    elif style == 'cross':
        for x in range(2, 14):
            for y in range(2, 14):
                d = min(abs(x - y), abs(x + y - 15))
                if d == 0:
                    px[x, y] = mix(brace, (255, 255, 255), 0.15) + (255,)
                elif d == 1:
                    px[x, y] = mix(px[x, y], brace, 0.35) + (255,)
    return img

def ingot(base, light, dark, glow=False):
    """经典等轴锭：亮色顶面 + 基色立面 + 暗色端头；glow 加辉光点。"""
    img = canvas(); px = img.load()
    def put(x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            px[x, y] = c + (255,)
    # 顶面（梯形，左右两边同时外扩）
    for y, (x0, x1) in ((5, (6, 11)), (6, (5, 11)), (7, (4, 11))):
        for x in range(x0, x1 + 1):
            put(x, y, mix(light, (255, 255, 255), 0.3 if y == 5 else 0.0))
    # 立面
    for y in range(8, 13):
        for x in range(3, 13):
            c = base
            if x == 3:                 c = mix(c, light, 0.35)
            if x >= 12:                c = mix(c, dark, 0.55)
            if y == 12:                c = mix(c, dark, 0.5)
            if y == 8 and x < 12:      c = mix(c, light, 0.15)
            put(x, y, c)
    put(6, 6, (255, 255, 255)); put(7, 5, (255, 255, 255))   # 高光
    if glow:
        for gx, gy in ((4, 10), (8, 11), (11, 9), (9, 7)):
            put(gx, gy, mix(light, (255, 255, 255), 0.7))
    return img

def lump(base, light, dark, glow_dots=()):
    """圆块团（团状物）：暗底 + 基色主体 + 左上高光。"""
    img = canvas(); px = img.load()
    d = ImageDraw.Draw(img)
    d.ellipse((3, 4, 12, 13), fill=dark + (255,))
    d.ellipse((4, 5, 11, 12), fill=base + (255,))
    d.ellipse((5, 6, 8, 9), fill=light + (255,))
    # 底部收边的暗环带
    for x in range(4, 12):
        px[x, 12] = mix(dark, (0, 0, 0), 0.2) + (255,)
    for gx, gy in glow_dots:
        px[gx, gy] = mix(light, (255, 255, 255), 0.6) + (255,)
    return img

def shards(base, light, dark):
    """碎片簇：三枚小晶体 + 顶端闪光。"""
    img = canvas(); px = img.load()
    def crystal(x0, y0, w, h):
        for i in range(h):
            y = y0 + i
            half = (w - 1) // 2 if i < h - 1 else 0
            for x in range(x0 - half, x0 + half + 1):
                c = base
                if x == x0 - half and half: c = mix(c, light, 0.5)
                if x == x0 + half and half: c = mix(c, dark, 0.5)
                px[x, y] = c + (255,)
        px[x0, y0] = (255, 255, 255, 255)
    crystal(5, 3, 3, 7)
    crystal(10, 5, 3, 6)
    crystal(7, 8, 3, 5)
    return img

def pane():
    """辐光玻璃板：半透明浅青 + 绿色荧光斑 + 斜向高光。"""
    img = canvas(); px = img.load()
    tint = (191, 232, 240, 185)
    edge_light = (235, 250, 252, 220)
    edge_dark = (110, 150, 165, 200)
    for x in range(2, 14):
        for y in range(2, 14):
            c = tint
            if x == 2 or y == 2:   c = edge_light
            if x == 13 or y == 13: c = edge_dark
            if (x - y) in (4, 5):  c = (225, 245, 250, 150)
            px[x, y] = c
    for gx, gy in ((5, 5), (9, 8), (11, 4), (4, 10), (8, 12)):
        px[gx, gy] = (88, 224, 122, 255)
    px[10, 6] = (255, 255, 255, 230)
    return img

def core(panel, ring, accent):
    """核心器件：暗色面板 + 同心环 + 白热中心 + 角部固定件。"""
    img = canvas(); px = img.load()
    dark = mix(panel, (0, 0, 0), 0.5)
    light = mix(panel, (255, 255, 255), 0.25)
    for x in range(2, 14):
        for y in range(2, 14):
            c = panel
            if x in (2, 13) or y in (2, 13): c = dark
            px[x, y] = c + (255,)
    d = ImageDraw.Draw(img)
    d.ellipse((4, 4, 12, 12), outline=ring + (255,))
    d.ellipse((6, 6, 10, 10), outline=mix(ring, (255, 255, 255), 0.4) + (255,))
    for cx, cy in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[cx, cy] = accent + (255,)
    for cx, cy in ((3, 3), (12, 3), (3, 12), (12, 12)):
        px[cx, cy] = light + (255,)
    return img

# ---------------------------------------------------------------- 物品定义
# 配色语言参考 ASP 品阶：铱系银白 / 阳炎系黄→黄绿 / 铀系绿 / 核心器件暗板彩环。
# 注意：mt_core / quantum_core 直接采用 ASP 原版贴图（用户指定），不在本脚本生成，
# 重跑 --write 不会覆盖这两张。
ITEMS = {
    'sunnarium_part':                shards((247, 214, 74), (255, 246, 190), (196, 150, 20)),
    'sunnarium':                     lump((240, 200, 60), (255, 236, 140), (170, 130, 24)),
    'enriched_sunnarium':            lump((190, 230, 70), (230, 255, 150), (120, 160, 30),
                                          glow_dots=((6, 6), (9, 8), (7, 11))),
    'sunnarium_alloy':               plate((242, 227, 160), (255, 244, 205), (190, 168, 105), 'sheen'),
    'enriched_sunnarium_alloy':      plate((159, 219, 79), (214, 255, 150), (96, 150, 45), 'sheen'),
    'iridium_ingot':                 ingot((228, 231, 238), (255, 255, 255), (160, 166, 178)),
    'iridium_iron_plate':            plate((185, 198, 214), (226, 235, 245), (120, 134, 152), 'plain'),
    'reinforced_iridium_iron_plate': plate((90, 98, 112), (136, 146, 162), (54, 60, 70), 'slashes'),
    'uranium_ingot':                 ingot((127, 191, 63), (190, 240, 130), (78, 128, 34)),
    'irradiant_uranium':             ingot((79, 212, 31), (170, 255, 120), (40, 140, 16), glow=True),
    'irradiant_glass_pane':          pane(),
    'irradiant_reinforced_plate':    plate((110, 118, 128), (160, 168, 178), (70, 76, 86), 'cross'),
}

ITEM_ORDER = ['sunnarium_part', 'sunnarium', 'enriched_sunnarium', 'sunnarium_alloy',
              'enriched_sunnarium_alloy', 'iridium_ingot', 'iridium_iron_plate',
              'reinforced_iridium_iron_plate', 'uranium_ingot', 'irradiant_uranium',
              'irradiant_glass_pane', 'irradiant_reinforced_plate']

# ---------------------------------------------------------------- 机器方块
# 机壳沿用 gen_sides 的面板机框色系（竖纹 + 螺栓），正面开观察窗露出红色能量核心。
CASING = (33, 36, 38)
CASING_LIGHT = (72, 78, 86)
CASING_DARK = (16, 18, 21)
CORE_OFF = ((178, 52, 42), (214, 74, 56))      # 熄灭：暗红 / 亮红
CORE_ON = ((235, 80, 60), (255, 160, 120))     # 工作：亮红 / 白热

def machine_base():
    img = Image.new('RGBA', (16, 16), CASING + (255,))
    px = img.load()
    for y in range(16):                        # 竖纹
        px[0, y] = mix(CASING, CASING_LIGHT, 0.5) + (255,)
        px[15, y] = mix(CASING, CASING_DARK, 0.5) + (255,)
    for x in range(16):
        px[x, 0] = mix(CASING, CASING_LIGHT, 0.65) + (255,)
        px[x, 15] = mix(CASING, CASING_DARK, 0.65) + (255,)
    for bx, by in ((1, 1), (14, 1), (1, 14), (14, 14)):    # 螺栓
        px[bx, by] = CASING_LIGHT + (255,)
    return img, px

def machine_side(active):
    img, px = machine_base()
    core, core_lit = CORE_ON if active else CORE_OFF
    # 观察窗：外框 + 暗腔 + 核心条
    for x in range(3, 13):
        for y in range(4, 12):
            px[x, y] = (60, 64, 72, 255)
    for x in range(4, 12):
        for y in range(5, 11):
            px[x, y] = (16, 18, 22, 255)
    if active:   # 工作时腔内泛红光
        for x in range(4, 12):
            for y in range(5, 11):
                px[x, y] = mix((16, 18, 22), core, 0.22) + (255,)
    for x in range(5, 11):
        for y in range(7, 10):
            px[x, y] = core + (255,)
    for x in range(6, 10):
        px[x, 8] = core_lit + (255,)
    if active:
        px[5, 7] = (255, 255, 255, 255)
        px[10, 9] = (255, 220, 200, 255)
    return img

def machine_top():
    img, px = machine_base()
    for x in range(2, 14):
        for y in range(2, 14):
            px[x, y] = (26, 29, 31, 255)
    for sy in (4, 7, 10):                      # 散热栅片
        for x in range(4, 12):
            px[x, sy] = (18, 20, 23, 255)
            px[x, sy + 1] = mix((26, 29, 31), CASING_LIGHT, 0.3) + (255,)
    return img

def machine_bottom():
    img, px = machine_base()
    for x in range(3, 13):
        for y in range(3, 13):
            px[x, y] = (24, 27, 29, 255)
    for i in range(3, 13):                     # 十字槽
        px[i, 8] = (16, 18, 21, 255)
        px[8, i] = (16, 18, 21, 255)
    return img

# ---------------------------------------------------------------- GUI 底图
# 布局（与 GuiMolecularTransformer 常量一一对应）：
#   主区 176x166：输入槽(52,28) 输出槽(110,28) 进度箭头(75,28,24x16)
#   能量条(52,53,72x5) 文本区 y=63..96 玩家背包 84/142
#   辅助区 x>=176：进度箭头填充 (176,0) 能量条填充 (176,20) 状态灯 (176,30)/(188,30)
BG = (35, 39, 45)
FRAME = (52, 57, 64)
FRAME_OUT = (24, 27, 32)
INSET = (23, 27, 33)
INSET_DARK = (14, 17, 21)
INSET_LIGHT = (72, 78, 86)
PROGRESS = (224, 72, 58)
PROGRESS_LIT = (255, 150, 120)
ENERGY = (79, 209, 197)

def build_gui():
    img = Image.new('RGBA', (256, 166), BG + (255,))
    px = img.load()
    for i in range(256):
        for y, c in ((0, FRAME_OUT), (1, FRAME), (2, FRAME)):
            px[i, y] = c + (255,)
            px[i, 165 - y] = c + (255,)
    for j in range(166):
        for x, c in ((0, FRAME_OUT), (1, FRAME), (2, FRAME)):
            px[x, j] = c + (255,)
            px[175 - x, j] = c + (255,)

    def slot(sx, sy):
        for x in range(sx, sx + 18):
            for y in range(sy, sy + 18):
                c = INSET
                if x == sx or y == sy:       c = INSET_DARK
                if x == sx + 17 or y == sy + 17: c = INSET_LIGHT
                px[x, y] = c + (255,)

    slot(51, 27); slot(109, 27)                # 输入/输出槽框
    for x in range(51, 125):                   # 能量条框（内区 72x5 供运行时填充）
        for y in range(52, 59):
            c = INSET
            if x in (51, 124) or y in (52, 58): c = INSET_DARK
            px[x, y] = c + (255,)
    for i in range(3):                         # 玩家背包 36 槽
        for j in range(9):
            slot(7 + j * 18, 83 + i * 18)
    for j in range(9):
        slot(7 + j * 18, 141)

    def arrow(cx, cy, color, tip):             # 24x16 箭头
        for x in range(cx, cx + 16):
            for y in range(cy + 6, cy + 10):
                px[x, y] = color + (255,)
        for k in range(6):
            for y in range(cy + 6 - k, cy + 10 + k):
                px[cx + 16 + k, y] = color + (255,)
        for y in range(cy + 5, cy + 11):       # 箭头尖高光
            px[cx + 21, y] = tip + (255,)

    arrow(75, 28, INSET, INSET_LIGHT)          # 底槽箭头
    arrow(176, 0, PROGRESS, PROGRESS_LIT)      # 填充用箭头（同形红色）
    for x in range(176, 248):                  # 能量条填充条 72x5
        for y in range(20, 25):
            t = (x - 176) / 71
            px[x, y] = mix(ENERGY, (200, 255, 246), t) + (255,)
    for ex, ey, lit in ((176, 30, False), (188, 30, True)):   # 状态灯 8x8
        for x in range(ex, ex + 8):
            for y in range(ey, ey + 8):
                px[x, y] = (INSET if not lit else PROGRESS) + (255,)
        px[ex, ey] = FRAME_OUT + (255,); px[ex, ey + 7] = INSET_LIGHT + (255,)
        px[ex + 7, ey] = INSET_LIGHT + (255,); px[ex + 7, ey + 7] = INSET_LIGHT + (255,)
        if lit:
            px[ex + 2, ey + 2] = (255, 200, 180, 255)
    return img

# ---------------------------------------------------------------- 输出

def preview(name, images, cols):
    cell = 72; pad = 8
    rows = math.ceil(len(images) / cols)
    sheet = Image.new('RGBA', (cols * (cell + pad) + pad, rows * (cell + pad + 14) + pad), (34, 34, 40, 255))
    d = ImageDraw.Draw(sheet)
    for i, (label, img) in enumerate(images):
        x = pad + (i % cols) * (cell + pad)
        y = pad + (i // cols) * (cell + pad + 14)
        sheet.paste(img.resize((cell, cell), Image.NEAREST), (x, y), img.resize((cell, cell), Image.NEAREST))
        d.text((x, y + cell + 1), label, fill=(220, 220, 220, 255))
    out = os.path.join(BASE, 'preview', name)
    sheet.save(out)
    print('preview ->', out)

def main():
    write = '--write' in os.sys.argv
    os.makedirs(os.path.join(TEX, 'item'), exist_ok=True)
    for name in ITEM_ORDER:
        img = ITEMS[name]
        if write:
            img.save(os.path.join(TEX, 'item', name + '.png'))
    machine = [
        ('molecular_transformer_side', machine_side(False)),
        ('molecular_transformer_side_active', machine_side(True)),
        ('molecular_transformer_top', machine_top()),
        ('molecular_transformer_bottom', machine_bottom()),
    ]
    for name, img in machine:
        if write:
            img.save(os.path.join(TEX, 'block', name + '.png'))
    gui = build_gui()
    if write:
        gui.save(os.path.join(TEX, 'gui', 'molecular_transformer.png'))

    preview('mt_items.png', [(n, ITEMS[n]) for n in ITEM_ORDER], cols=7)
    preview('mt_machine.png', machine, cols=4)
    print('GUI', gui.size, '| write =', write)

if __name__ == '__main__':
    main()
