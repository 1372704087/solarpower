# -*- coding: utf-8 -*-
# 「XX阳光化合物」「XX小块阳光化合物」「XX阳光合金」物品贴图生成器
# （3 × 66 张 16x16 + 对应物品模型 JSON）
#
# 设计（用户方案 A，2026-10-03；算法逐家族指定）：
#   化合物 / 小块 → v1：只平移有彩像素的色相到该档主题色，饱和度按主题色饱和度
#   比例缩放（银白档自动降饱和），明暗结构与灰白高光完全不动——鲜亮感优先
#   （v2 亮度斜坡压暗发闷、v3 高光调整均被用户否决）。
#   阳光合金 → v2：亮度斜坡重映射——合金是高亮平板，色彩身份全在高亮中饱和像素里，
#   v1 会让 66 档几乎一样发白；v2 把有彩像素按亮度归位到主题色
#   深(×0.45)→基色→亮(+55% 白)→高光(+80% 白) 渐变带，边框鲜艳、面板呈渐变。
# 基础档沿用三张本体；富集阳光化合物/富集阳光合金本体不动。
#
# 用法：
#   py gen_sunnarium.py           # 预览：preview/sunnarium_preview.png
#   py gen_sunnarium.py --write   # 写入 textures/item/*.png + models/item/*.json
from PIL import Image, ImageDraw
import colorsys, json, os, re

BASE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                       'assets', 'solarpower'))
TEX_DIR = os.path.join(ASSETS, 'textures', 'item')
MODEL_DIR = os.path.join(ASSETS, 'models', 'item')
PREVIEW_DIR = os.path.join(BASE, 'preview')
SRC_TIER = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'java',
                                         'com', 'example', 'solarpower', 'solar',
                                         'SolarTier.java'))
# (底图, 变体注册名后缀, 算法)；基础档沿用本体，不生成
FAMILIES = [
    ('sunnarium.png', '_sunnarium', 'v1'),
    ('sunnarium_part.png', '_sunnarium_part', 'v1'),
    ('sunnarium_alloy.png', '_sunnarium_alloy', 'v2'),
]
WHITE = (255, 255, 255)
GRAY_EPS = 0.15       # v1：低于该饱和度的像素视为灰白高光，不改
SAT_REF = 0.85        # v1：主题色饱和度参照值（比例缩放像素饱和度）


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


def variant_id(tier_id, suffix):
    return tier_id[:-len('_solar_panel')] + suffix


def mix(c1, c2, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(c1, c2))


def luma(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def recolor_v1(img, accent):
    """v1：色相平移到主题色 + 饱和度按主题色缩放，明暗结构原样保留。"""
    ah, asat, _ = colorsys.rgb_to_hsv(accent[0] / 255.0, accent[1] / 255.0, accent[2] / 255.0)
    out = Image.new('RGBA', img.size)
    sp, op = img.load(), out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255.0, g / 255.0, b / 255.0)
            if s < GRAY_EPS:
                op[x, y] = (r, g, b, a)          # 灰白高光不动
                continue
            ns = min(1.0, s * min(asat, 1.0) / SAT_REF)
            nr, ng, nb = colorsys.hsv_to_rgb(ah, ns, v)
            op[x, y] = (round(nr * 255), round(ng * 255), round(nb * 255), a)
    return out


def make_ramp(accent):
    """v2 主题色 4 档斜坡：深(×0.45) → 基色 → 亮(+55% 白) → 高光(+80% 白)。"""
    deep = tuple(round(c * 0.45) for c in accent)
    return [deep, accent, mix(accent, WHITE, 0.55), mix(accent, WHITE, 0.8)]


def sample_ramp(ramp, t):
    if t <= 0:
        return ramp[0]
    if t >= 1:
        return ramp[-1]
    seg = t * (len(ramp) - 1)
    i = min(int(seg), len(ramp) - 2)
    return mix(ramp[i], ramp[i + 1], seg - i)


def recolor_v2(img, accent):
    """v2：亮度斜坡重映射——有彩像素按亮度归位到主题色斜坡；灰白像素不动。"""
    sp = img.load()
    colored = [sp[x, y][:3] for y in range(img.height) for x in range(img.width)
               if sp[x, y][3] > 0 and _colorfulness(sp[x, y][:3]) >= GRAY_EPS]
    lums = sorted(luma(c) for c in colored)
    lo, hi = lums[0], lums[-1]
    ramp = make_ramp(accent)
    out = Image.new('RGBA', img.size)
    op = out.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            if _colorfulness((r, g, b)) < GRAY_EPS:
                op[x, y] = (r, g, b, a)          # 灰白高光不动
                continue
            t = (luma((r, g, b)) - lo) / (hi - lo) if hi > lo else 0.5
            op[x, y] = sample_ramp(ramp, t) + (a,)
    return out


def _colorfulness(c):
    mx, mn = max(c), min(c)
    return 0.0 if mx == 0 else (mx - mn) / mx


RECOLORS = {'v1': recolor_v1, 'v2': recolor_v2}


if __name__ == '__main__':
    import sys
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    accents = tier_accents()
    assert len(accents) == 67, len(accents)
    ids = [t for t in accents if t != 'solar_panel']      # 基础档沿用本体
    assert len(ids) == 66, len(ids)
    if '--write' in sys.argv:
        os.makedirs(TEX_DIR, exist_ok=True)
        os.makedirs(MODEL_DIR, exist_ok=True)
        for base_png, suffix, algo in FAMILIES:
            img = Image.open(os.path.join(TEX_DIR, base_png)).convert('RGBA')
            for t in ids:
                vid = variant_id(t, suffix)
                RECOLORS[algo](img, accents[t]).save(os.path.join(TEX_DIR, vid + '.png'))
                with open(os.path.join(MODEL_DIR, vid + '.json'), 'w', encoding='utf-8',
                          newline='\n') as f:
                    f.write(json.dumps({
                        'parent': 'builtin/generated',
                        'textures': {'layer0': 'solarpower:item/' + vid},
                    }, indent=2) + '\n')
        print('written %d x %d variants ->' % (len(FAMILIES), len(ids)), TEX_DIR)
    else:
        S, pad, cols = 8, 6, 8
        flat = [(t, base_png, suffix, algo) for base_png, suffix, algo in FAMILIES for t in ids]
        rows = (len(flat) + cols - 1) // cols
        W = cols * (16 * S + pad) + pad
        H = rows * (16 * S + pad + 14) + pad
        sheet = Image.new('RGBA', (W, H), (70, 70, 70, 255))
        d = ImageDraw.Draw(sheet)
        cache = {}
        for i, (t, base_png, suffix, algo) in enumerate(flat):
            if base_png not in cache:
                cache[base_png] = Image.open(os.path.join(TEX_DIR, base_png)).convert('RGBA')
            x = pad + (i % cols) * (16 * S + pad)
            y = pad + (i // cols) * (16 * S + pad + 14)
            im = RECOLORS[algo](cache[base_png], accents[t]).resize((16 * S, 16 * S), Image.NEAREST)
            sheet.paste(im, (x, y), im)
            d.text((x + 2, y + 16 * S + 1),
                   variant_id(t, suffix).replace('_sunnarium_alloy', '.alloy')
                   .replace('_sunnarium_part', '.part').replace('_sunnarium', ''),
                   fill=(255, 255, 255, 255))
        out = os.path.join(PREVIEW_DIR, 'sunnarium_preview.png')
        sheet.save(out)
        print('preview ->', out)
