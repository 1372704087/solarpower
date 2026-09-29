package com.example.solarpower.registry;

import com.example.solarpower.SolarPower;
import com.example.solarpower.solar.GlassCableTier;
import com.example.solarpower.solar.SolarTier;

import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.EnumMap;
import java.util.Map;

/** 物品注册（BlockItem + 创造标签）。 */
@EventBusSubscriber(modid = SolarPower.MODID)
public final class ModItems {

    private static final Map<SolarTier, Item> PANEL_ITEMS = new EnumMap<>(SolarTier.class);
    private static final Map<GlassCableTier, Item> CABLE_ITEMS = new EnumMap<>(GlassCableTier.class);

    @SubscribeEvent
    public static void onRegisterItems(RegistryEvent.Register<Item> event) {
        for (SolarTier tier : SolarTier.values()) {
            Item item = new ItemBlock(ModBlocks.panel(tier)).setRegistryName(
                    SolarPower.MODID, tier.id());
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            PANEL_ITEMS.put(tier, item);
        }
        for (GlassCableTier tier : GlassCableTier.values()) {
            Item item = new ItemBlock(ModBlocks.cable(tier)).setRegistryName(
                    SolarPower.MODID, tier.id());
            item.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(item);
            CABLE_ITEMS.put(tier, item);
        }
    }

    public static Item panelItem(SolarTier tier) {
        return PANEL_ITEMS.get(tier);
    }

    public static Item cableItem(GlassCableTier tier) {
        return CABLE_ITEMS.get(tier);
    }

    private ModItems() {
    }
}
