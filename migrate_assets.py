# -*- coding: utf-8 -*-
# 从 1.21 工程迁移资产到 forge-1.12.2：
#   - 贴图/模型/物品模型原样复制
#   - 方块状态：默认变体 "" 改名为 1.12.2 的 "normal"
#   - 语言：JSON 转 .lang，block.* -> tile.*.name
import json, os, shutil

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(ROOT, '..', 'src', 'main', 'resources', 'assets', 'solarpower')
DST = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'solarpower')

# ---------- 1) 贴图原样复制；模型内联化后复制 ----------
for sub in ('textures/block', 'textures/gui'):
    src_dir = os.path.join(SRC, sub)
    dst_dir = os.path.join(DST, sub)
    os.makedirs(dst_dir, exist_ok=True)
    for f in os.listdir(src_dir):
        if f.endswith('.png'):
            shutil.copy(os.path.join(src_dir, f), os.path.join(dst_dir, f))
print('textures copied')

# 1.12.2 的模型加载器把无命名空间的 parent 按当前域名解析（如 block/block ->
# solarpower:block/block，不存在即整链失败），因此迁移时把模型改写为自包含：
# 去掉 parent，显式 elements / display。
CUBE_ELEMENTS = [{
    'from': [0, 0, 0], 'to': [16, 16, 16],
    'faces': {
        'down':  {'uv': [0, 0, 16, 16], 'texture': '#down'},
        'up':    {'uv': [0, 0, 16, 16], 'texture': '#up'},
        'north': {'uv': [0, 0, 16, 16], 'texture': '#north'},
        'south': {'uv': [0, 0, 16, 16], 'texture': '#south'},
        'west':  {'uv': [0, 0, 16, 16], 'texture': '#west'},
        'east':  {'uv': [0, 0, 16, 16], 'texture': '#east'},
    },
}]
BLOCK_DISPLAY = {
    'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [0.625, 0.625, 0.625]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [0.25, 0.25, 0.25]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [0.5, 0.5, 0.5]},
    'thirdperson_righthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'thirdperson_lefthand': {'rotation': [75, 45, 0], 'translation': [0, 2.5, 0], 'scale': [0.375, 0.375, 0.375]},
    'firstperson_righthand': {'rotation': [0, 45, 0], 'translation': [0, 0, 0], 'scale': [0.40, 0.40, 0.40]},
    'firstperson_lefthand': {'rotation': [0, 225, 0], 'translation': [0, 0, 0], 'scale': [0.40, 0.40, 0.40]},
}

def inline_model(d):
    """去掉 parent：cube 类补全六面 elements，其余补 display 变换。"""
    parent = d.get('parent', '')
    d.pop('parent', None)
    if parent.endswith('block/cube'):
        if 'elements' not in d:
            d['elements'] = CUBE_ELEMENTS
    if 'display' not in d:
        d['display'] = BLOCK_DISPLAY
    return d

for sub in ('models/block', 'models/item'):
    src_dir = os.path.join(SRC, sub)
    dst_dir = os.path.join(DST, sub)
    os.makedirs(dst_dir, exist_ok=True)
    for f in os.listdir(src_dir):
        if not f.endswith('.json'):
            continue
        d = json.load(open(os.path.join(src_dir, f), encoding='utf-8'))
        inline_model(d)
        json.dump(d, open(os.path.join(dst_dir, f), 'w', encoding='utf-8'),
                  indent=2, ensure_ascii=False)
print('textures + self-contained models copied')

# ---------- 2) 方块状态：1.12.2 无属性方块变体键必须是 "" ----------
src_bs = os.path.join(SRC, 'blockstates')
dst_bs = os.path.join(DST, 'blockstates')
os.makedirs(dst_bs, exist_ok=True)
count = 0
for f in os.listdir(src_bs):
    if not f.endswith('.json'):
        continue
    d = json.load(open(os.path.join(src_bs, f), encoding='utf-8'))
    # 保留空串键；错误地改成 "normal" 会导致方块/物品全部缺失贴图
    json.dump(d, open(os.path.join(dst_bs, f), 'w', encoding='utf-8'),
              indent=2, ensure_ascii=False)
    count += 1
print('blockstates copied:', count)

# ---------- 3) 语言：JSON -> .lang ----------
for loc in ('zh_cn', 'en_us'):
    src = json.load(open(os.path.join(SRC, 'lang', loc + '.json'), encoding='utf-8'))
    lines = []
    for k, v in src.items():
        if k.startswith('block.'):
            lines.append('tile.%s.name=%s' % (k[len('block.'):], v))
        else:
            lines.append('%s=%s' % (k, v))
    out = os.path.join(DST, 'lang', loc + '.lang')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, 'w', encoding='utf-8', newline='\n') as fh:
        fh.write('\n'.join(lines) + '\n')
print('lang converted')
