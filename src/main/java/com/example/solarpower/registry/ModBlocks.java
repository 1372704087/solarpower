package com.example.solarpower.registry;

import com.example.solarpower.SolarPower;
import com.example.solarpower.block.GlassCableBlock;
import com.example.solarpower.block.MolecularTransformerBlock;
import com.example.solarpower.block.SolarPanelBlock;
import com.example.solarpower.block.StorageBoxBlock;
import com.example.solarpower.block.TransformerBlock;
import com.example.solarpower.energy.EuTier;
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
    private static final Map<SolarTier, StorageBoxBlock> STORAGE_BOXES = new EnumMap<>(SolarTier.class);
    private static final Map<EuTier, TransformerBlock> TRANSFORMERS = new EnumMap<>(EuTier.class);
    private static MolecularTransformerBlock molecularTransformer;

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
        for (SolarTier tier : SolarTier.values()) {
            StorageBoxBlock block = new StorageBoxBlock(tier);
            block.setRegistryName(SolarPower.MODID, tier.id() + "_storage_box");
            block.setUnlocalizedName(SolarPower.MODID + "." + tier.id() + "_storage_box");
            event.getRegistry().register(block);
            STORAGE_BOXES.put(tier, block);
        }
        // 变压器 65 档：桥接电压 T↔T+1（T=1..65），id = 电压枚举小写 + _transformer
        for (EuTier tier : EuTier.values()) {
            if (tier.iuTier() > 65) {
                continue;
            }
            TransformerBlock block = new TransformerBlock(tier);
            block.setRegistryName(SolarPower.MODID, tier.name().toLowerCase() + "_transformer");
            block.setUnlocalizedName(SolarPower.MODID + "." + tier.name().toLowerCase() + "_transformer");
            event.getRegistry().register(block);
            TRANSFORMERS.put(tier, block);
        }
        molecularTransformer = new MolecularTransformerBlock();
        molecularTransformer.setRegistryName(SolarPower.MODID, "molecular_transformer");
        molecularTransformer.setUnlocalizedName(SolarPower.MODID + ".molecular_transformer");
        event.getRegistry().register(molecularTransformer);
    }

    public static SolarPanelBlock panel(SolarTier tier) {
        return PANELS.get(tier);
    }

    public static GlassCableBlock cable(GlassCableTier tier) {
        return CABLES.get(tier);
    }

    public static StorageBoxBlock storageBox(SolarTier tier) {
        return STORAGE_BOXES.get(tier);
    }

    public static TransformerBlock transformer(EuTier tier) {
        return TRANSFORMERS.get(tier);
    }

    public static MolecularTransformerBlock molecularTransformer() {
        return molecularTransformer;
    }

    private ModBlocks() {
    }
}
