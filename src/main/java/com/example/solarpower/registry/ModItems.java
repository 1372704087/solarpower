package com.example.solarpower.registry;

import com.example.solarpower.SolarPower;
import com.example.solarpower.block.GlassCableBlock;
import com.example.solarpower.block.SolarPanelBlock;
import com.example.solarpower.energy.EuTier;
import com.example.solarpower.item.CableItemBlock;
import com.example.solarpower.item.PanelItemBlock;
import com.example.solarpower.solar.GlassCableTier;
import com.example.solarpower.solar.SolarTier;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 物品注册（BlockItem + 太阳能合成物品 + 创造标签）。 */
@EventBusSubscriber(modid = SolarPower.MODID)
public final class ModItems {

    private static final Map<SolarTier, Item> PANEL_ITEMS = new EnumMap<>(SolarTier.class);
    private static final Map<GlassCableTier, Item> CABLE_ITEMS = new EnumMap<>(GlassCableTier.class);
    private static final Map<SolarTier, Item> STORAGE_BOX_ITEMS = new EnumMap<>(SolarTier.class);
    private static final Map<EuTier, Item> TRANSFORMER_ITEMS = new EnumMap<>(EuTier.class);
    private static final Map<SolarTier, Item> PANE_ITEMS = new EnumMap<>(SolarTier.class);
    private static final Map<SolarTier, Item> SUNNARIUM_ITEMS = new EnumMap<>(SolarTier.class);
    private static final Map<SolarTier, Item> SUNNARIUM_PART_ITEMS = new EnumMap<>(SolarTier.class);
    private static final Map<SolarTier, Item> SUNNARIUM_ALLOY_ITEMS = new EnumMap<>(SolarTier.class);
    private static final Map<SolarTier, Item> CORE_ITEMS = new EnumMap<>(SolarTier.class);
    private static final Map<String, Item> CRAFTING_ITEMS = new LinkedHashMap<>();

    /** 太阳能合成物品（ASP 合成链 + SSP 光谱组件族，注册名与材质沿用其命名）。 */
    private static final String[] CRAFTING_IDS = {
            "sunnarium_part", "sunnarium", "enriched_sunnarium",
            "sunnarium_alloy", "enriched_sunnarium_alloy",
            "iridium_ingot", "iridium_iron_plate", "reinforced_iridium_iron_plate",
            "uranium_ingot", "irradiant_uranium",
            "irradiant_glass_pane", "irradiant_reinforced_plate",
            "mt_core", "quantum_core",
            // Super Solar Panels 的光谱组件族（太阳光分解器 = 光谱档核心材料）
            "solarsplitter", "bluecomponent", "greencomponent", "redcomponent"
    };

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        for (SolarTier tier : SolarTier.values()) {
            Item item = new PanelItemBlock(ModBlocks.panel(tier)).setRegistryName(
                    SolarPower.MODID, tier.id());
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            PANEL_ITEMS.put(tier, item);
        }
        for (GlassCableTier tier : GlassCableTier.values()) {
            Item item = new CableItemBlock(ModBlocks.cable(tier)).setRegistryName(
                    SolarPower.MODID, tier.id());
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            CABLE_ITEMS.put(tier, item);
        }
        // 储电盒的方块物品（普通 ItemBlock：电量 NBT 由方块 getDrops/onBlockPlacedBy 处理）
        for (SolarTier tier : SolarTier.values()) {
            Item item = new ItemBlock(ModBlocks.storageBox(tier)).setRegistryName(
                    SolarPower.MODID, tier.id() + "_storage_box");
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            STORAGE_BOX_ITEMS.put(tier, item);
        }
        // 变压器的方块物品
        for (EuTier tier : EuTier.values()) {
            if (tier.iuTier() > 65) {
                continue;
            }
            Item item = new ItemBlock(ModBlocks.transformer(tier)).setRegistryName(
                    SolarPower.MODID, tier.name().toLowerCase() + "_transformer");
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            TRANSFORMER_ITEMS.put(tier, item);
        }
        // 分子重组仪的方块物品
        Item transformer = new ItemBlock(ModBlocks.molecularTransformer()).setRegistryName(
                SolarPower.MODID, "molecular_transformer");
        transformer.setCreativeTab(SolarPower.CREATIVE_TAB);
        event.getRegistry().register(transformer);
        // 合成链物品
        for (String id : CRAFTING_IDS) {
            Item item = new Item().setUnlocalizedName(SolarPower.MODID + "." + id);
            item.setRegistryName(SolarPower.MODID, id);
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            CRAFTING_ITEMS.put(id, item);
        }
        // 「XX玻璃板」面板材料：67 档各一张，用于对应面板的合成
        for (SolarTier tier : SolarTier.values()) {
            Item item = new Item().setUnlocalizedName(SolarPower.MODID + "." + tier.paneId());
            item.setRegistryName(SolarPower.MODID, tier.paneId());
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            PANE_ITEMS.put(tier, item);
        }
        // 「XX阳光化合物」：2 档起各一份（逐档改色，基础档沿用 ASP 的阳光化合物本体），
        // 富集阳光化合物本体不动，仍是整条阶梯的增幅材料
        for (SolarTier tier : SolarTier.values()) {
            if (tier == SolarTier.BASIC) {
                continue;
            }
            Item item = new Item().setUnlocalizedName(SolarPower.MODID + "." + tier.sunnariumId());
            item.setRegistryName(SolarPower.MODID, tier.sunnariumId());
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            SUNNARIUM_ITEMS.put(tier, item);
        }
        // 「XX小块阳光化合物」：同上逐档改色，基础档沿用小块本体；9 张压 1 张对应档化合物
        for (SolarTier tier : SolarTier.values()) {
            if (tier == SolarTier.BASIC) {
                continue;
            }
            Item item = new Item().setUnlocalizedName(SolarPower.MODID + "." + tier.sunnariumPartId());
            item.setRegistryName(SolarPower.MODID, tier.sunnariumPartId());
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            SUNNARIUM_PART_ITEMS.put(tier, item);
        }
        // 「XX阳光合金」：同上逐档改色，基础档沿用合金本体；8 铱锭抱对应档化合物合成
        for (SolarTier tier : SolarTier.values()) {
            if (tier == SolarTier.BASIC) {
                continue;
            }
            Item item = new Item().setUnlocalizedName(SolarPower.MODID + "." + tier.sunnariumAlloyId());
            item.setRegistryName(SolarPower.MODID, tier.sunnariumAlloyId());
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            SUNNARIUM_ALLOY_ITEMS.put(tier, item);
        }
        // 「XX核心」：仿 IU 激发核，2 档起各一枚（用户设计的核心模板逐档着色），
        // 用于对应面板四角；基础面板不需要核心。
        // 量子档沿用 ASP 合成链的量子核心本体（quantum_core 已在 CRAFTING_IDS 注册，
        // 同名再注册一次会让注册表冻结时的同步校验失败 → 启动崩溃）
        for (SolarTier tier : SolarTier.values()) {
            if (tier == SolarTier.BASIC || tier == SolarTier.QUANTUM) {
                continue;
            }
            Item item = new Item().setUnlocalizedName(SolarPower.MODID + "." + tier.coreId());
            item.setRegistryName(SolarPower.MODID, tier.coreId());
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            CORE_ITEMS.put(tier, item);
        }
    }

    public static Item panelItem(SolarTier tier) {
        return PANEL_ITEMS.get(tier);
    }

    public static Item cableItem(GlassCableTier tier) {
        return CABLE_ITEMS.get(tier);
    }

    public static Item storageBoxItem(SolarTier tier) {
        return STORAGE_BOX_ITEMS.get(tier);
    }

    public static Item transformerItem(EuTier tier) {
        return TRANSFORMER_ITEMS.get(tier);
    }

    /** 按注册名取合成链物品（配方表使用）。 */
    public static Item crafting(String id) {
        return CRAFTING_ITEMS.get(id);
    }

    /** 该档位面板对应的「XX玻璃板」材料（配方表使用）。 */
    public static Item paneItem(SolarTier tier) {
        return PANE_ITEMS.get(tier);
    }

    /** 该档位对应的「XX阳光化合物」（基础档即阳光化合物本体；配方表使用）。 */
    public static Item sunnariumItem(SolarTier tier) {
        return tier == SolarTier.BASIC ? crafting("sunnarium") : SUNNARIUM_ITEMS.get(tier);
    }

    /** 该档位对应的「XX小块阳光化合物」（基础档即小块本体；配方表使用）。 */
    public static Item sunnariumPartItem(SolarTier tier) {
        return tier == SolarTier.BASIC ? crafting("sunnarium_part") : SUNNARIUM_PART_ITEMS.get(tier);
    }

    /** 该档位对应的「XX阳光合金」（基础档即合金本体；配方表使用）。 */
    public static Item sunnariumAlloyItem(SolarTier tier) {
        return tier == SolarTier.BASIC ? crafting("sunnarium_alloy") : SUNNARIUM_ALLOY_ITEMS.get(tier);
    }

    /** 该档位对应的「XX核心」（基础档返回 null，量子档即 ASP 量子核心本体；配方表使用）。 */
    public static Item coreItem(SolarTier tier) {
        if (tier == SolarTier.BASIC) {
            return null;
        }
        if (tier == SolarTier.QUANTUM) {
            return crafting("quantum_core");
        }
        return CORE_ITEMS.get(tier);
    }

    /**
     * 全部合成材料（ASP 合成链 + SSP 光谱组件 + 玻璃板/化合物/小块/合金/核心），
     * 创造标签与模型注册共用。量子核心虽属 ASP 链，展示时按档位插进核心堆
     * （混合核心之后、光谱核心之前，用户指定）。
     */
    public static List<Item> craftingItems() {
        List<Item> items = new ArrayList<>();
        for (Map.Entry<String, Item> entry : CRAFTING_ITEMS.entrySet()) {
            if (!"quantum_core".equals(entry.getKey())) {
                items.add(entry.getValue());
            }
        }
        for (SolarTier tier : SolarTier.values()) {
            items.add(PANE_ITEMS.get(tier));
        }
        for (Map<SolarTier, Item> family : new Map[]{SUNNARIUM_ITEMS, SUNNARIUM_PART_ITEMS,
                SUNNARIUM_ALLOY_ITEMS}) {
            for (SolarTier tier : SolarTier.values()) {
                Item item = family.get(tier);
                if (item != null) {
                    items.add(item);
                }
            }
        }
        for (SolarTier tier : SolarTier.values()) {
            if (tier == SolarTier.QUANTUM) {
                items.add(crafting("quantum_core"));   // 量子档核心 = ASP 量子核心本体
                continue;
            }
            Item item = CORE_ITEMS.get(tier);
            if (item != null) {
                items.add(item);
            }
        }
        return Collections.unmodifiableList(items);
    }

    private ModItems() {
    }
}
