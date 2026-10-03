# -*- coding: utf-8 -*-
# 「XX玻璃板」物品贴图生成器（67 张 16x16 + 对应物品模型 JSON）
#
# 设计（用户指定 2026-10-03）：物品图标直接复用太阳能板顶面贴图——
# 即 gen_tops.py 的 build()（深色机框 + 3x3 倒角电池片 + 斜向玻璃高光），
# 保证图标与放置后面板顶面完全一致，换色时两脚本同跑 --write 即同步。
#
# 用法：
#   py gen_panes.py           # 预览：生成 preview/panes_preview.png，不动项目文件
#   py gen_panes.py --write   # 写入 textures/item/*.png + models/item/*.json
from PIL import Image, ImageDraw
import json, os

BASE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(BASE, '..', 'src', 'main', 'resources',
                                       'assets', 'solarpower'))
TEX_DIR = os.path.join(ASSETS, 'textures', 'item')
MODEL_DIR = os.path.join(ASSETS, 'models', 'item')
PREVIEW_DIR = os.path.join(BASE, 'preview')

# 单一事实来源：顶面绘制逻辑与调色板都来自 gen_tops.py
from gen_tops import palettes, build as build_top


def pane_id(tier_id):
    if tier_id == 'solar_panel':
        return 'basic_glass_pane'
    return tier_id[:-len('_solar_panel')] + '_glass_pane'


if __name__ == '__main__':
    import sys
    os.makedirs(PREVIEW_DIR, exist_ok=True)
    if '--write' in sys.argv:
        os.makedirs(TEX_DIR, exist_ok=True)
        os.makedirs(MODEL_DIR, exist_ok=True)
        for name in palettes:
            pid = pane_id(name)
            build_top(palettes[name]).save(os.path.join(TEX_DIR, pid + '.png'))
            with open(os.path.join(MODEL_DIR, pid + '.json'), 'w', encoding='utf-8',
                      newline='\n') as f:
                f.write(json.dumps({
                    'parent': 'builtin/generated',
                    'textures': {'layer0': 'solarpower:item/' + pid},
                }, indent=2) + '\n')
        print('written %d panes ->' % len(palettes), TEX_DIR)
    else:
        order = list(palettes)
        S, pad, cols = 8, 6, 8
        rows = (len(order) + cols - 1) // cols
        W = cols * (16 * S + pad) + pad
        H = rows * (16 * S + pad + 14) + pad
        sheet = Image.new('RGBA', (W, H), (70, 70, 70, 255))
        d = ImageDraw.Draw(sheet)
        for i, name in enumerate(order):
            x = pad + (i % cols) * (16 * S + pad)
            y = pad + (i // cols) * (16 * S + pad + 14)
            sheet.paste(build_top(palettes[name]).resize((16 * S, 16 * S), Image.NEAREST), (x, y))
            d.text((x + 2, y + 16 * S + 1),
                   pane_id(name).replace('_glass_pane', ''), fill=(255, 255, 255, 255))
        out = os.path.join(PREVIEW_DIR, 'panes_preview.png')
        sheet.save(out)
        print('preview ->', out)
