# -*- coding: utf-8 -*-
# mt_core（分子重组核心）贴图生成器：从 AI 生成图（mt_core_source.png）抠图降采样。
# 流程：缩到 64x64 → 背景判定（近白 + 从边缘泛洪，保留球心高光）→ 彩色像素定裁剪框
#       （排除灰色水印）→ 抠除背景 → 16x16 盒式降采样 → 输出 item/mt_core.png
#
# 用法：
#   py gen_mt_core.py           # 预览：preview/mt_core_from_image.png
#   py gen_mt_core.py --write   # 写入 src/.../textures/item/mt_core.png
from PIL import Image
import os

BASE = os.path.dirname(os.path.abspath(__file__))
SOURCE = os.path.join(BASE, 'mt_core_source.png')
TEX_ITEM = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                         'assets', 'solarpower', 'textures', 'item'))
PREVIEW_DIR = os.path.join(BASE, 'preview')

WORK = 64          # 中间分辨率（泛洪在此尺度做，便宜且足够）
OUT = 16           # 物品贴图尺寸
WHITE_MIN = 225    # RGB 三通道都不低于该值视为“近白”
SPREAD_MAX = 14    # 近白且通道差不超过该值（排除彩色）


def is_whiteish(px):
    r, g, b = px[:3]
    return min(r, g, b) >= WHITE_MIN and max(r, g, b) - min(r, g, b) <= SPREAD_MAX


def main():
    img = Image.open(SOURCE).convert('RGB')
    small = img.resize((WORK, WORK), Image.LANCZOS)
    px = small.load()

    def colored(c):
        r, g, b = c[:3]
        return (max(r, g, b) - min(r, g, b) > 25) or (max(r, g, b) < 180)

    # 1) 彩色像素包围盒 = 球体范围（近白背景与灰色水印都不是“彩色”，自然排除）
    xs, ys = [], []
    for y in range(WORK):
        for x in range(WORK):
            if colored(px[x, y]):
                xs.append(x)
                ys.append(y)
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    pad = 1
    x0, y0 = max(0, x0 - pad), max(0, y0 - pad)
    x1, y1 = min(WORK - 1, x1 + pad), min(WORK - 1, y1 + pad)

    # 2) 从四边泛洪：与边界连通的近白像素 = 背景（球心高光不与边界连通，得以保留）
    from collections import deque
    bg = [[False] * WORK for _ in range(WORK)]
    q = deque()
    for x in range(WORK):
        for y in (0, WORK - 1):
            if is_whiteish(px[x, y]) and not bg[y][x]:
                bg[y][x] = True
                q.append((x, y))
    for y in range(WORK):
        for x in (0, WORK - 1):
            if is_whiteish(px[x, y]) and not bg[y][x]:
                bg[y][x] = True
                q.append((x, y))
    while q:
        x, y = q.popleft()
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= nx < WORK and 0 <= ny < WORK and not bg[ny][nx] and is_whiteish(px[nx, ny]):
                bg[ny][nx] = True
                q.append((nx, ny))

    # 3) 抠除背景 → RGBA
    cut = small.crop((x0, y0, x1 + 1, y1 + 1)).convert('RGBA')
    cpx = cut.load()
    cw, ch = cut.size
    ox, oy = x0, y0
    for y in range(ch):
        for x in range(cw):
            if bg[oy + y][ox + x]:
                cpx[x, y] = (0, 0, 0, 0)

    # 4) 16x16 盒式降采样（alpha 一起平均，边缘自然半透明）
    out = cut.resize((OUT, OUT), Image.BOX)
    opx = out.load()
    for y in range(OUT):
        for x in range(OUT):
            r, g, b, a = opx[x, y]
            if a < 24:
                opx[x, y] = (0, 0, 0, 0)
            elif a > 230:
                opx[x, y] = (r, g, b, 255)

    # 5) 只保留主连通域（光晕弧线在角落的残渣逐个丢弃）
    from collections import deque as _dq
    seen = [[False] * OUT for _ in range(OUT)]
    best_id, best_size, comp = 0, 0, {}
    for y in range(OUT):
        for x in range(OUT):
            if seen[y][x] or opx[x, y][3] == 0:
                continue
            comp_id = len(comp) + 1
            pixels = []
            q = _dq(((x, y),))
            seen[y][x] = True
            while q:
                cx, cy = q.popleft()
                pixels.append((cx, cy))
                for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                    if 0 <= nx < OUT and 0 <= ny < OUT and not seen[ny][nx] and opx[nx, ny][3] > 0:
                        seen[ny][nx] = True
                        q.append((nx, ny))
            comp[comp_id] = pixels
            if len(pixels) > best_size:
                best_id, best_size = comp_id, len(pixels)
    keep = set(comp.get(best_id, []))
    for y in range(OUT):
        for x in range(OUT):
            if opx[x, y][3] > 0 and (x, y) not in keep:
                opx[x, y] = (0, 0, 0, 0)

    # 6) 去白边：半透明像素是「颜色×α + 白×(1-α)」的混合，反解出原本的颜色
    for y in range(OUT):
        for x in range(OUT):
            r, g, b, a = opx[x, y]
            if 0 < a < 255:
                f = a / 255.0
                r = max(0, min(255, round((r - (1 - f) * 255) / f)))
                g = max(0, min(255, round((g - (1 - f) * 255) / f)))
                b = max(0, min(255, round((b - (1 - f) * 255) / f)))
                opx[x, y] = (r, g, b, a)

    # 预览：原图框选 + 16x 放大结果
    preview_src = img.copy()
    pd = preview_src.load()
    scale = img.size[0] // WORK
    pdraw = __import__('PIL.ImageDraw', fromlist=['Draw']).Draw(preview_src)
    pdraw.rectangle((x0 * scale, y0 * scale, (x1 + 1) * scale - 1, (y1 + 1) * scale - 1),
                    outline=(255, 60, 60), width=6)
    up = out.resize((OUT * 16, OUT * 16), Image.NEAREST)
    sheet = Image.new('RGBA', (preview_src.size[0] // 2 + up.size[0] + 30,
                               max(preview_src.size[1] // 2, up.size[1]) + 20), (40, 40, 46, 255))
    sheet.paste(preview_src.resize((preview_src.size[0] // 2, preview_src.size[1] // 2)), (10, 10))
    sheet.paste(up, (preview_src.size[0] // 2 + 20, 10), up)
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    out_path = os.path.join(PREVIEW_DIR, 'mt_core_from_image.png')
    sheet.save(out_path)
    print('preview ->', out_path)

    if '--write' in os.sys.argv:
        os.makedirs(TEX_ITEM, exist_ok=True)
        out.save(os.path.join(TEX_ITEM, 'mt_core.png'))
        print('written ->', os.path.join(TEX_ITEM, 'mt_core.png'))


if __name__ == '__main__':
    main()
