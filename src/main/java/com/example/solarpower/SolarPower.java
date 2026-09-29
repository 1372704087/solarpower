package com.example.solarpower;

import com.example.solarpower.block.GlassCableBlock;
import com.example.solarpower.block.SolarPanelBlock;
import com.example.solarpower.client.GuiHandler;
import com.example.solarpower.registry.ModBlocks;
import com.example.solarpower.registry.ModItems;
import com.example.solarpower.solar.GlassCableTier;
import com.example.solarpower.solar.SolarTier;
import com.example.solarpower.tileentity.GlassCableTile;
import com.example.solarpower.tileentity.SolarPanelTile;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nonnull;

/** 工业太阳能 1.12.2 Forge 版。功能与 NeoForge 1.21.1 版一致。 */
@Mod(modid = SolarPower.MODID, name = SolarPower.NAME, version = SolarPower.VERSION,
        acceptedMinecraftVersions = "[1.12,1.13)")
public final class SolarPower {

    public static final String MODID = "solarpower";
    public static final String NAME = "Industrial Solar";
    public static final String VERSION = "1.0.0";

    @Mod.Instance(MODID)
    public static SolarPower instance;

    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs("solarpower") {
        @Nonnull
        @Override
        public ItemStack getTabIconItem() {
            return new ItemStack(ModItems.panelItem(SolarTier.BASIC));
        }
    };

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(SolarPanelTile.class,
                new ResourceLocation(MODID, "solar_panel"));
        GameRegistry.registerTileEntity(GlassCableTile.class,
                new ResourceLocation(MODID, "glass_cable"));
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
    }
}
