# texturegen — 贴图生成管线（太阳能板 / 电缆 / 物品族）

> **素材来源**（用户 2026-10-04 定稿）：除下列三处外，本项目全部贴图均为本管线
> 原创绘制，配色参考 Industrial Upgrade（IU）——
> - **分子重组仪全套**（14 个合成物品、机器方块、GUI 底图）：直接采用 ASP
>   （Advanced Solar Panels）原版素材（2026-10-01 起），不在本管线生成；
>   布局坐标见 `MolecularTransformerContainer` / `GuiMolecularTransformer` 注释。
> - **SSP 光谱组件族** 4 张物品贴图（太阳光分解器 + 红/绿/蓝光谱组件）：直接采用
>   SSP（Super Solar Panels）原版素材（2026-10-03 起）。
> - **阳光化合物 / 小块 / 阳光合金**三族：以 ASP 三张原贴图为底逐档改色
>   （底图沿用，改色算法为本管线实现）。
>
> 机器家族（储电盒 / 变压器 / 充电座）的编辑器模板亦按用户要求从 IU 红色系原贴图
> 逐像素转写（见 `cable_pixel_editor.html`），其余面板/电缆/核心等均为原创。

## 覆盖范围

| 内容 | 规模 | 生成器 |
|---|---|---|
| 太阳能板顶面 / 侧面 | 67 档 × 2 = 134 张 | `gen_tops.py` / `gen_sides.py` |
| 玻璃电缆方块 + 物品贴图 | 68 档 × 2 = 136 张 | `gen_cables_iu.py` |
| 「XX储电盒」方块贴图 + 资产 | 67 档 × 3 = 201 张 + blockstates/模型/lang | `gen_storages.py` |
| 「XX玻璃板」物品贴图 | 67 张 | `gen_panes.py` |
| 「XX核心」物品贴图 | 66 张（2 档起） | `gen_cores.py` |
| 阳光化合物 / 小块 / 合金 | 3 × 66 张 | `gen_sunnarium.py` |

电缆的 blockstates / 模型 JSON 不由生成器产出，见 `migrate_cable_tiers.py`。

## 运行

依赖 Pillow：`py -m pip install pillow`

```bash
# 预览（写入 texturegen/preview/，不动项目贴图）
py gen_tops.py
py gen_sides.py
py gen_cores.py

# 实际写入项目贴图（src/main/resources/assets/solarpower/textures/）
py gen_tops.py --write
py gen_sides.py --write
py gen_cores.py --write
py gen_panes.py --write
py gen_sunnarium.py --write

# 电缆：直接运行即写入（本仓库 + forge-1.12.2 双根目录）并输出预览拼图
py gen_cables_iu.py
```

写入的文件名与模型引用一致（`<等级>_top.png` / `<等级>_side.png`），无需改任何 JSON。
侧面动画的 `.png.mcmeta` 不由脚本管理，始终沿用现有文件（9 帧、frametime 2、
插值、第 4 帧驻留 20 tick）。

## 文件说明

| 文件 | 作用 |
|---|---|
| `tier_palettes.json` | 太阳能板 67 档三色：`base` 基色 / `light` 深色 / `dark` 亮色（键名沿用提取时的命名，实际 light=深、dark=亮） |
| `glyphs/<等级>_f0.png` | 侧面图案底图（16×16）：机框 + 居中修正后的图案轮廓，仅 gen_sides.py 使用 |
| `gen_tops.py` | 顶面生成：深色机框 + 3×3 倒角电池片 + 深色缝隙 + 斜向玻璃高光 |
| `gen_sides.py` | 侧面生成：图案重上色 + 色阶归一化 + 9 帧呼吸动画 |
| `gen_panes.py` | 「XX玻璃板」物品贴图 + 模型 JSON（图标 = 面板顶面） |
| `gen_cores.py` | 「XX核心」生成：编辑器定稿的核心主模板逐档着色（M=主题色 D=×0.55，E/B/W/N 固定）+ 模型 JSON |
| `gen_sunnarium.py` | 阳光化合物/小块/合金：以 ASP 原图为底逐档改色，算法逐族指定（化合物/小块 = v1 色相平移，合金 = v2 亮度斜坡） |
| `gen_cables_iu.py` | 当前电缆生成器：68 档方块 + 物品贴图，结构按 IU glass_cable 像素结构（十字线芯 + 暗边框），配色为 IU 观感的自定 deep/mid/bright（脚本内 `MAP` 表），写入双根目录，无 --write 开关 |
| `gen_storages.py` | 「XX储电盒」生成器（2026-10-04）：67 档正面/侧面/顶底贴图（编辑器 IU 红转写模板逐档换色：红三阶 M/R/B = 面板 accent 的 deep/mid/bright，石灰与能量芯固定）+ blockstates（facing 四向）+ 方块/物品模型 + 双语 lang 追加；模板正本在本文件 TEMPLATE，编辑器图案与它同步 |
| `gen_gui_storage.py` | 储电盒 GUI 底图（2026-10-04，自定义样式）：176×166 宽能量条 + 三行数据 + 玩家背包，槽位坐标与 StorageBoxContainer 的 8+18n 栅格一致；--write 写 textures/gui/storage_box.png |
| `cable_pixel_editor.html` | 素材像素编辑器（16×16 模板设计，浏览器直接打开）。图案：核心/电缆（与 `gen_cores.py` / `gen_cables_iu.py` 的 TEMPLATE 同步）+ 储电盒/储电盒侧面/储电盒顶底/变压器/充电座（2026-10-04 用户定：IU 红色素材原图逐像素转写——mfsu_front / mfsu_leftrightback / mfsu_bottomtop / hv_transformer_front / chargepad_mfsu_top，各自带独立调色板，画布即原图配色。IU 里变压器侧面（hv_transformer_side）与充电桩非顶面与储能仓对应面完全相同，无需另画；导出变量 TEMPLATE_STORAGE_BOX / STORAGE_SIDE / STORAGE_TOPBOTTOM / TEMPLATE_TRANSFORMER / TEMPLATE_CHARGEPAD，生成脚本待对应机器方块落地后创建）+ 红石矿石（2026-10-04 用户定：原版 1.12.2 `redstone_ore.png` 逐像素转写，石头 k/s/t/u 4 档灰 + 红石 a/b/c/d/R 5 档红，导出变量 TEMPLATE_ORE，生成脚本待矿石方块落地后创建）+ 铁锭（原版 `iron_ingot.png` 逐像素转写，灰度阶 a-j 共 10 档，导出变量 TEMPLATE_INGOT）+ 火药（原版 `gunpowder.png` 逐像素转写，灰度阶 a-g 共 7 档，导出变量 TEMPLATE_DUST），后两者生成脚本待对应物品落地后创建。铁锭 11 角色超出快捷键 10 位，第 11 个角色不设热键、点击选用） |
| `migrate_cable_tiers.py` | 电缆档位资产迁移器：按 GlassCableTier 生成/改名全套 blockstates + 模型 JSON（每档 18 件） |
| `migrate_solar_tier.py` | SolarTier.java 紧凑格式（10 参常量）迁移器：spec 表 + 整文件重生成 |
| `verify_cable_ratio.py` | 电缆贴图比例校验小工具 |
| `add_core_lang.py` / `add_pane_lang.py` / `add_sunnarium_lang.py` / `fix_cable_lang.py` | 一次性 lang 批处理（已完成，留档） |
| 遗留（不再维护，备查） | `gen_cables.py`（早期 11 档方案）、`gen_cables_pretty.py`、`cable_palettes.json`（早期 11 档配色）、`gen_glyphs_si.py`、`gen_mt.py` / `gen_mt_core.py`（MT 改为直接采用 ASP 素材前的生成器）、`fix_1122_assets.py`、`fix_cable_item_models.py`、`migrate_assets.py`、`patch_solar_sim.py` |

## 常改参数

**换色**：编辑 `tier_palettes.json` 对应级的三个颜色，两个脚本都重跑
（`--write`），顶面侧面即同步换色。电缆配色改 `gen_cables_iu.py` 的 `MAP` 表。

**侧面动画节奏**：`gen_sides.py` 的 `PULSE` 列表（9 个 0~1 亮度值）。
注意保持与 `.mcmeta` 的 9 帧结构一致，第 4 帧为驻留峰值帧。

**闪光强度**：`gen_sides.py` 的 `GLINT_STRONG / GLINT_WEAK`。

**顶面电池片**：`gen_tops.py` 的 `SEP`（缝隙位置）、`SEP_DARKNESS`（缝隙深度）、
倒角与高光的 mix 系数（`build()` 内注释处）。

## 注意

- `glyphs/` 是侧面的形状来源，删除后 gen_sides.py 无法运行（顶面无此依赖）。
- 除 `gen_cables_iu.py`（直接写入）外，生成器默认只出预览，`--write` 才动项目贴图。
- 各脚本确定性的：同样输入重跑输出逐字节一致，可放心重复执行。
