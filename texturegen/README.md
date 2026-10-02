# texturegen — 太阳能板贴图生成管线

> 分子重组仪相关的贴图（14 个合成物品、机器方块、GUI 底图）自 2026-10-01 起
> 直接采用 ASP（Advanced Solar Panels）原版素材（用户指定），不在本管线生成；
> 布局坐标见 `MolecularTransformerContainer` / `GuiMolecularTransformer` 注释。

本项目全部太阳能板贴图（15 级 × 顶面/侧面，共 30 张 PNG）由本目录下的脚本生成，
改参数重跑即可批量调整，无需手绘。

## 运行

依赖 Pillow：`py -m pip install pillow`

```bash
# 预览（写入 texturegen/preview/，不动项目贴图）
py gen_tops.py
py gen_sides.py

# 实际写入项目贴图（src/main/resources/assets/solarpower/textures/block/）
py gen_tops.py --write
py gen_sides.py --write
```

写入的文件名与模型引用一致（`<等级>_top.png` / `<等级>_side.png`），无需改任何 JSON。
侧面动画的 `.png.mcmeta` 不由脚本管理，始终沿用现有文件（9 帧、frametime 2、
插值、第 4 帧驻留 20 tick）。

## 文件说明

| 文件 | 作用 |
|---|---|
| `tier_palettes.json` | 每级三色：`base` 基色 / `light` 深色 / `dark` 亮色（键名沿用提取时的命名） |
| `glyphs/<等级>_f0.png` | 侧面图案底图（16×16）：原版机框 + 居中修正后的图案轮廓，仅 gen_sides.py 使用 |
| `gen_tops.py` | 顶面生成：深色机框 + 3×3 倒角电池片 + 深色缝隙 + 斜向玻璃高光 |
| `gen_sides.py` | 侧面生成：图案重上色 + 色阶归一化 + 9 帧呼吸动画 |
| `gen_cables.py` | 玻璃电缆生成：11 级贴图 + multipart 方块状态 + 模型/物品模型 |
| `cable_palettes.json` | 玻璃电缆 11 级配色（提取自 IU 电缆贴图） |
| `preview/` | 预览输出（对比图 / 动画 GIF） |

## 常改参数

**换色**：编辑 `tier_palettes.json` 对应级的三个颜色，两个脚本都重跑
（`--write`），顶面侧面即同步换色。

**侧面动画节奏**：`gen_sides.py` 的 `PULSE` 列表（9 个 0~1 亮度值）。
注意保持与 `.mcmeta` 的 9 帧结构一致，第 4 帧为驻留峰值帧。

**闪光强度**：`gen_sides.py` 的 `GLINT_STRONG / GLINT_WEAK`。

**顶面电池片**：`gen_tops.py` 的 `SEP`（缝隙位置）、`SEP_DARKNESS`（缝隙深度）、
倒角与高光的 mix 系数（`build()` 内注释处）。

## 注意

- `glyphs/` 是侧面的形状来源，删除后 gen_sides.py 无法运行（顶面无此依赖）。
- 两脚本均确定性的：同样输入重跑输出逐字节一致，可放心重复执行。
