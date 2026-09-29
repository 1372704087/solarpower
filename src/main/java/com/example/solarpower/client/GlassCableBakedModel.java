package com.example.solarpower.client;

import com.example.solarpower.block.GlassCableBlock;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * 玻璃电缆的动态烘焙模型：核心段恒在，六向臂按扩展状态里的
 * 连接掩码拼接（物品形态 state 为 null 时显示完整十字）。
 */
public final class GlassCableBakedModel implements IBakedModel {

    private final IBakedModel core;
    private final Map<EnumFacing, IBakedModel> arms = new EnumMap<>(EnumFacing.class);

    public GlassCableBakedModel(IBakedModel core, Map<EnumFacing, IBakedModel> arms) {
        this.core = core;
        this.arms.putAll(arms);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        int mask = 0x3F;
        if (state instanceof net.minecraftforge.common.property.IExtendedBlockState) {
            Integer value = ((net.minecraftforge.common.property.IExtendedBlockState) state)
                    .getValue(GlassCableBlock.CONNECTIONS);
            if (value != null) {
                mask = value;
            }
        }
        List<BakedQuad> quads = new ArrayList<>(this.core.getQuads(state, side, rand));
        for (EnumFacing dir : EnumFacing.values()) {
            if ((mask & (1 << dir.getIndex())) == 0) {
                continue;
            }
            IBakedModel arm = this.arms.get(dir);
            if (arm != null) {
                quads.addAll(arm.getQuads(state, side, rand));
            }
        }
        return quads;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.core.getParticleTexture();
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.core.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }
}
