package com.example.solarpower.client;

import com.example.solarpower.SolarPower;
import com.example.solarpower.solar.GlassCableTier;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.EnumMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * 玻璃电缆动态烘焙模型。
 * <p>1.12.2 无属性方块 blockstate 键可能是 "" 或 "normal"，两种都查。
 */
@EventBusSubscriber(modid = SolarPower.MODID, value = Side.CLIENT)
public final class ModelBakeHandler {

    @SubscribeEvent
    public static void onModelBake(ModelBakeEvent event) {
        net.minecraft.util.registry.IRegistry<ModelResourceLocation, IBakedModel> registry =
                event.getModelRegistry();
        for (GlassCableTier tier : GlassCableTier.values()) {
            String id = "solarpower:" + tier.id();
            IBakedModel core = firstNonNull(registry,
                    new ModelResourceLocation(id, ""),
                    new ModelResourceLocation(id, "normal"),
                    new ModelResourceLocation(id, "inventory"));
            if (core == null) {
                core = bakeFromJson(new ResourceLocation(SolarPower.MODID,
                        "block/" + tier.id() + "_core"));
            }
            if (core == null) {
                continue;
            }
            Map<EnumFacing, IBakedModel> arms = new EnumMap<>(EnumFacing.class);
            for (EnumFacing dir : EnumFacing.values()) {
                IBakedModel arm = registry.getObject(
                        new ModelResourceLocation(id, "arm_" + dir.getName()));
                if (arm == null) {
                    arm = bakeFromJson(new ResourceLocation(SolarPower.MODID,
                            "block/" + tier.id() + "_arm_" + dir.getName()));
                }
                if (arm != null) {
                    arms.put(dir, arm);
                }
            }
            GlassCableBakedModel model = new GlassCableBakedModel(core, arms);
            registry.putObject(new ModelResourceLocation(id, ""), model);
            registry.putObject(new ModelResourceLocation(id, "normal"), model);
            registry.putObject(new ModelResourceLocation(id, "inventory"), model);
        }
    }

    @Nullable
    private static IBakedModel firstNonNull(
            net.minecraft.util.registry.IRegistry<ModelResourceLocation, IBakedModel> registry,
            ModelResourceLocation... keys) {
        for (ModelResourceLocation key : keys) {
            IBakedModel model = registry.getObject(key);
            if (model != null) {
                return model;
            }
        }
        return null;
    }

    @Nullable
    private static IBakedModel bakeFromJson(ResourceLocation modelId) {
        try {
            IModel model = ModelLoaderRegistry.getModel(modelId);
            if (model == null || model == ModelLoaderRegistry.getMissingModel()) {
                return null;
            }
            final TextureMap map = Minecraft.getMinecraft().getTextureMapBlocks();
            return model.bake(model.getDefaultState(), DefaultVertexFormats.BLOCK,
                    location -> map.getAtlasSprite(location.toString()));
        } catch (Exception e) {
            return null;
        }
    }

    private ModelBakeHandler() {
    }
}
