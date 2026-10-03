# texturegen — 太阳能板贴图生成管线

> 分子重组仪相关的贴图（14 个合成物品、机器方块、GUI 底图）自 2026-10-01 起
> 直接采用 ASP（Advanced Solar Panels）原版素材（用户指定），不在本管线生成；
> SSP 光谱组件族 4 张物品贴图（太阳光分解器 + 红/绿/蓝光谱组件）自 2026-10-03 起
> 直接采用 SSP（Super Solar Panels）原版素材（用户指定），同样不在本管线生成。
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
| `gen_panes.py` | 「XX玻璃板」物品贴图生成：67 档各一张——直接复用 `gen_tops.py` 的顶面绘制（用户指定，图标=面板顶面）+ 物品模型 JSON |
| `gen_cores.py` | 「XX核心」生成：用户在拓展版像素编辑器设计的核心主模板逐档着色（M=主题色 D=×0.55，E/B/W/N 固定）66 张 + 物品模型 JSON（2 档起，仿 IU 激发核） |
| `gen_sunnarium.py` | 「XX阳光化合物」+「XX小块阳光化合物」+「XX阳光合金」生成：以 ASP 三张原版贴图为底逐档改色，3 × 66 张 + 物品模型 JSON。改色算法逐家族指定（用户定稿）：化合物/小块 = v1 色相平移 + 饱和度按主题色缩放（明暗结构原样保留，鲜亮优先）；阳光合金 = v2 亮度斜坡重映射（深→基→亮→高光渐变带，平板结构用 v1 会发白）。（v3 高光调整被否决。）基础档沿用本体；富集阳光化合物/富集阳光合金不动 |
| `gen_cables.py` | 玻璃电缆生成：11 级贴图 + multipart 方块状态 + 模型/物品模型 |
| `cable_palettes.json` | 玻璃电缆 11 级配色（提取自 IU 电缆贴图） |
| `cable_pixel_editor.html` | 素材像素编辑器（16×16 模板设计，浏览器直接打开）。图案：核心/电缆（与
  `gen_cores.py` / `gen_cables_iu.py` 的 TEMPLATE 同步）+ 储电盒/储电盒侧面/储电盒顶底/变压器/充电座
  （2026-10-04 用户定：IU 红色素材原图逐像素转写——mfsu_front / mfsu_leftrightback / mfsu_bottomtop /
  hv_transformer_front / chargepad_mfsu_top，各自带独立调色板，画布即原图配色。IU 里变压器侧面
  （hv_transformer_side）与充电桩非顶面（front/leftrightback/bottom）与储能仓对应面完全相同，无需另画；
  导出变量 TEMPLATE_STORAGE_BOX / STORAGE_SIDE / STORAGE_TOPBOTTOM / TEMPLATE_TRANSFORMER /
  TEMPLATE_CHARGEPAD，生成脚本待对应机器方块落地后创建） |
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
