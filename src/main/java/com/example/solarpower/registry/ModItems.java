package com.example.solarpower.registry;

import com.example.solarpower.SolarPower;
import com.example.solarpower.block.GlassCableBlock;
import com.example.solarpower.block.SolarPanelBlock;
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
    private static final Map<String, Item> CRAFTING_ITEMS = new LinkedHashMap<>();

    /** 太阳能合成物品（移植自 Advanced Solar Panels 的合成链，注册名与材质沿用其命名）。 */
    private static final String[] CRAFTING_IDS = {
            "sunnarium_part", "sunnarium", "enriched_sunnarium",
            "sunnarium_alloy", "enriched_sunnarium_alloy",
            "iridium_ingot", "iridium_iron_plate", "reinforced_iridium_iron_plate",
            "uranium_ingot", "irradiant_uranium",
            "irradiant_glass_pane", "irradiant_reinforced_plate",
            "mt_core", "quantum_core"
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
    }

    public static Item panelItem(SolarTier tier) {
        return PANEL_ITEMS.get(tier);
    }

    public static Item cableItem(GlassCableTier tier) {
        return CABLE_ITEMS.get(tier);
    }

    /** 按注册名取合成链物品（配方表使用）。 */
    public static Item crafting(String id) {
        return CRAFTING_ITEMS.get(id);
    }

    public static List<Item> craftingItems() {
        return Collections.unmodifiableList(new ArrayList<>(CRAFTING_ITEMS.values()));
    }

    private ModItems() {
    }
}
