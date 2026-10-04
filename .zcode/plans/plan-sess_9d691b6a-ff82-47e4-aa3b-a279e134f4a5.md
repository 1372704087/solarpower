# 67 档储电盒实施计划

## 目标
每个面板档位配一个储电盒（67 个方块 + 1 个共用方块实体），IU 风格：正面能量芯（可朝向）、五面进电、正面出电、比较器输出、打掉保留电量。

## 数值体系（挂在现有档位上）
- 每档绑定 `SolarTier` 同序档（67 常量一一对应），注册名 `<面板档id>_storage_box`（如 `advanced_solar_panel_storage_box`），zh 名「XX储电盒」（XX 沿用面板中文名前缀；IU 官方中文是"储能仓"，要换随时说）。
- 容量 = `tier.capacityEu() × 64`（面板自带缓冲的 64 倍，BigInteger；倍数实现时可调）。
- 输出速率 = `tier.maxOutputEu()`（与同档面板推入电缆的速率一致）；电压 = `tier.voltage()`（T 档盒只接受 ≤T 的包，电缆网会自动把它当 sink 充电）。

## Phase 1 资产（texturegen）
新建 `gen_storages.py`：
- 读编辑器导出的 `TEMPLATE_STORAGE_BOX / STORAGE_SIDE / STORAGE_TOPBOTTOM`（已是 IU 红 1:1 转写），每档重上色：红三阶 M/R/B → 面板 accent 的 deep/mid/bright（沿用电缆三阶规则 ×0.545 / 原色 / 混白 0.45）；石灰 E/G/D/N 与能量芯 O/W 固定。
- 产出（脚本直接写 src 资产 + 预览拼图）：67×3 方块贴图、67 个 blockstates（水平 4 向 facing 变体旋转模型，同 IU wiring_storage 结构）、67 个 `models/block/<id>.json`（cube：north=正面，up/down=顶底，其余=侧面）、67 个 `models/item/<id>.json`（parent=方块模型，无需 item 贴图）。
- 两份 lang 各加 67 行 `tile.solarpower.<id>.name=`；创造栏在面板之后按档插入 67 项（`displayAllRelevantItems` 固定顺序）。

## Phase 2 Java
1. `StorageBoxBlock`（BlockContainer + `FACING` 水平属性）：放置时正面（能量芯面）朝向玩家；`onBlockActivated` 开 GUI。
2. `StorageBoxTile`（TileEntity + ITickable + IEuEnergy）：
   - 基于现成 `EuEnergyStorage` + `SolarPanelTile` 的 BigInteger 套路：room/显示缓存、NBT 用 Base64(toByteArray)、NBT 读取时档位未解析 → 首 tick `clampToCapacity`、从方块懒解析档位。
   - **重写 `shouldRefresh` = `oldState.getBlock() != newState.getBlock()`**（facing 翻转不杀 TE，项目已知坑）。
   - 每 tick：`EuCableNet.push` 仅从正面邻面推送（给 push 加方向参数，不走面板的六面 ×N 旧语义）。
   - 打掉时方块掉落物带 energy NBT，放置时读回（电量保留）；比较器按存量比例输出（仿分子重组仪）。
3. `EuCableNet` 小改：源收集时把「facing 朝向本面的储电盒」也算源（正面出电语义）；sink 已天然支持（网络扫描任意 IEuEnergy 邻居）。
4. 注册按既有模式补齐：`ModBlocks` 循环注册、`ModItems` ItemBlock（带 NBT 的子类）、`SolarPower.preInit` 注册 TE（一个 TE 类）、`ClientModelRegistry` 绑定、`GuiHandler` 新 id=2。

## Phase 3 GUI（先看图再写码）
- 新建 `gen_gui_storage.py` 画自定义 GUI 底图（大能量条 + 存/容数值 + 输出速率文本位，176×167 规格含玩家背包区），先出预览图给你过目定稿；
- 定稿后写 `StorageBoxContainer` + `GuiStorageBox`（能量条用 accentColor 着色，数值用 EuFormat.displayParts 大数显示）。充电槽等物品电池内容等以后有 EU 物品再加。

## 明确不做（本轮）
充电桩（下一步单独做）、升级套件换档、红石输出模式、物品充放电槽。

## 验证
- `build.bat` 编译通过；HMCL 测试实例：放盒接电缆看充放电、翻面 TE 不丢、拆放保留电量、比较器信号、GUI 显示与预览一致；贴图预览拼图逐档抽查。

## 实施中会用到的命令
- `py texturegen/gen_storages.py`（生成资产）、`build.bat`（编译）、git 提交推送。