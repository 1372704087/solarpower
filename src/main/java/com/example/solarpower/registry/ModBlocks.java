package com.example.solarpower.registry;

import com.example.solarpower.SolarPower;
import com.example.solarpower.block.GlassCableBlock;
import com.example.solarpower.block.SolarPanelBlock;
import com.example.solarpower.solar.GlassCableTier;
import com.example.solarpower.solar.SolarTier;

import net.minecraft.block.Block;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.EnumMap;
import java.util.Map;

/** 方块注册（1.12.2 RegistryEvent 风格）。 */
@EventBusSubscriber(modid = SolarPower.MODID)
public final class ModBlocks {

    private static final Map<SolarTier, SolarPanelBlock> PANELS = new EnumMap<>(SolarTier.class);
    private static final Map<GlassCableTier, GlassCableBlock> CABLES = new EnumMap<>(GlassCableTier.class);

    @SubscribeEvent
    public static void onRegisterBlocks(RegistryEvent.Register<Block> event) {
        for (SolarTier tier : SolarTier.values()) {
            SolarPanelBlock block = new SolarPanelBlock(tier);
            block.setRegistryName(SolarPower.MODID, tier.id());
            block.setUnlocalizedName(SolarPower.MODID + "." + tier.id());
            event.getRegistry().register(block);
            PANELS.put(tier, block);
        }
        for (GlassCableTier tier : GlassCableTier.values()) {
            GlassCableBlock block = new GlassCableBlock(tier);
            block.setRegistryName(SolarPower.MODID, tier.id());
            // 1.12.2 语言键：tile.solarpower.<registry>.name；各档位必须分开
            block.setUnlocalizedName(SolarPower.MODID + "." + tier.id());
            block.setCreativeTab(SolarPower.CREATIVE_TAB);
            event.getRegistry().register(block);
            CABLES.put(tier, block);
        }
    }

    public static SolarPanelBlock panel(SolarTier tier) {
        return PANELS.get(tier);
    }

    public static GlassCableBlock cable(GlassCableTier tier) {
        return CABLES.get(tier);
    }

    private ModBlocks() {
    }
}
