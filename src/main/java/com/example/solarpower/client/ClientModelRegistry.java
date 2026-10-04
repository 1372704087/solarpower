package com.example.solarpower.client;

import com.example.solarpower.SolarPower;
import com.example.solarpower.registry.ModBlocks;
import com.example.solarpower.registry.ModItems;
import com.example.solarpower.solar.GlassCableTier;
import com.example.solarpower.solar.SolarTier;

import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 客户端模型注册：1.12.2 必须手动 setCustomModelResourceLocation，否则物品是紫黑缺失贴图。 */
@SideOnly(Side.CLIENT)
@EventBusSubscriber(modid = SolarPower.MODID, value = Side.CLIENT)
public final class ClientModelRegistry {

    @SubscribeEvent
    public static void onModelRegistry(ModelRegistryEvent event) {
        for (SolarTier tier : SolarTier.values()) {
            Item item = ModItems.panelItem(tier);
            if (item != null) {
                ModelLoader.setCustomModelResourceLocation(item, 0,
                        new ModelResourceLocation(item.getRegistryName(), "inventory"));
            }
        }
        for (GlassCableTier tier : GlassCableTier.values()) {
            Item item = ModItems.cableItem(tier);
            if (item != null) {
                ModelLoader.setCustomModelResourceLocation(item, 0,
                        new ModelResourceLocation(item.getRegistryName(), "inventory"));
            }
        }
        for (SolarTier tier : SolarTier.values()) {
            Item item = ModItems.storageBoxItem(tier);
            if (item != null) {
                ModelLoader.setCustomModelResourceLocation(item, 0,
                        new ModelResourceLocation(item.getRegistryName(), "inventory"));
            }
        }
        // 分子重组机方块物品 + 太阳能合成物品
        for (Item item : ModItems.craftingItems()) {
            ModelLoader.setCustomModelResourceLocation(item, 0,
                    new ModelResourceLocation(item.getRegistryName(), "inventory"));
        }
        Item transformer = Item.getItemFromBlock(ModBlocks.molecularTransformer());
        if (transformer != null) {
            ModelLoader.setCustomModelResourceLocation(transformer, 0,
                    new ModelResourceLocation(transformer.getRegistryName(), "inventory"));
        }
    }

    private ClientModelRegistry() {
    }
}
