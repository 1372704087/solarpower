package com.example.solarpower;

import com.example.solarpower.block.GlassCableBlock;
import com.example.solarpower.block.SolarPanelBlock;
import com.example.solarpower.client.GuiHandler;
import com.example.solarpower.registry.ModBlocks;
import com.example.solarpower.registry.ModItems;
import com.example.solarpower.registry.ModRecipes;
import com.example.solarpower.solar.GlassCableTier;
import com.example.solarpower.solar.SolarTier;
import com.example.solarpower.tileentity.GlassCableTile;
import com.example.solarpower.tileentity.MolecularTransformerTile;
import com.example.solarpower.tileentity.SolarPanelTile;
import com.example.solarpower.tileentity.StorageBoxTile;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
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
        acceptedMinecraftVersions = "[1.12,1.13)",
        // 软依赖：装了 IC2 时排在其后加载，保证注册进 IC2 电网时其 EnergyNet 已就绪；没装则忽略
        dependencies = "after:ic2")
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

        /** 固定排列：面板(1→67) → 储电盒(1→67) → 电缆(1→31) → 合成材料+分子重组仪；不随注册顺序漂移。 */
        @Nonnull
        @Override
        public void displayAllRelevantItems(@Nonnull NonNullList<ItemStack> items) {
            for (SolarTier tier : SolarTier.values()) {
                items.add(new ItemStack(ModItems.panelItem(tier)));
            }
            for (SolarTier tier : SolarTier.values()) {
                items.add(new ItemStack(ModItems.storageBoxItem(tier)));
            }
            for (GlassCableTier tier : GlassCableTier.values()) {
                items.add(new ItemStack(ModItems.cableItem(tier)));
            }
            for (Item item : ModItems.craftingItems()) {
                items.add(new ItemStack(item));
            }
            items.add(new ItemStack(Item.getItemFromBlock(ModBlocks.molecularTransformer())));
        }
    };

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        GameRegistry.registerTileEntity(SolarPanelTile.class,
                new ResourceLocation(MODID, "solar_panel"));
        GameRegistry.registerTileEntity(GlassCableTile.class,
                new ResourceLocation(MODID, "glass_cable"));
        GameRegistry.registerTileEntity(MolecularTransformerTile.class,
                new ResourceLocation(MODID, "molecular_transformer"));
        GameRegistry.registerTileEntity(StorageBoxTile.class,
                new ResourceLocation(MODID, "storage_box"));
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
        ModRecipes.register();
    }
}
