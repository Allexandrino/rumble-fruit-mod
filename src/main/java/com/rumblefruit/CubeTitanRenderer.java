package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

// the Cube Titan is rendered as what it is: a colossal cluster of floating
// cubes — a magma core that glows from within, wrapped in a slowly orbiting
// shell of crying obsidian shards
public class CubeTitanRenderer extends EntityRenderer<CubeTitanEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public CubeTitanRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.shadowRadius = 2.5F;
    }

    @Override
    public void render(CubeTitanEntity entity, float entityYaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        float time = entity.tickCount + partialTicks;
        // core: a 3x3x3 magma cluster, pulsing
        float pulse = 1.0F + 0.05F * (float) Math.sin(time * 0.15F);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    pose.pushPose();
                    pose.translate(dx * 1.15F * pulse, 2.2F + dy * 1.15F * pulse, dz * 1.15F * pulse);
                    pose.scale(1.1F, 1.1F, 1.1F);
                    pose.translate(-0.5F, -0.5F, -0.5F);
                    blockRenderer.renderSingleBlock(Blocks.MAGMA_BLOCK.defaultBlockState(),
                            pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY);
                    pose.popPose();
                }
            }
        }
        // shell: eight crying-obsidian shards slowly orbiting the core
        for (int i = 0; i < 8; i++) {
            double angle = time * 0.04F + i * Math.PI / 4.0;
            double bob = Math.sin(time * 0.09F + i) * 0.5;
            pose.pushPose();
            pose.translate(Math.cos(angle) * 3.2, 2.2 + bob, Math.sin(angle) * 3.2);
            pose.scale(0.9F, 0.9F, 0.9F);
            pose.mulPose(com.mojang.math.Axis.YP.rotation((float) angle * 2.0F));
            pose.translate(-0.5F, -0.5F, -0.5F);
            blockRenderer.renderSingleBlock(Blocks.CRYING_OBSIDIAN.defaultBlockState(),
                    pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        // crown: four glowstone shards hovering above
        for (int i = 0; i < 4; i++) {
            double angle = -time * 0.06F + i * Math.PI / 2.0;
            pose.pushPose();
            pose.translate(Math.cos(angle) * 1.6, 5.6 + Math.sin(time * 0.12F + i) * 0.3,
                    Math.sin(angle) * 1.6);
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.translate(-0.5F, -0.5F, -0.5F);
            blockRenderer.renderSingleBlock(Blocks.GLOWSTONE.defaultBlockState(),
                    pose, buffer, 0xF000F0, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CubeTitanEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
