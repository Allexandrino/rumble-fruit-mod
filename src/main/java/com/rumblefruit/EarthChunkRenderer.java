package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.math.Axis;

// renders the ripped earth chunk: the real blocks it tore out, arranged back
// into the 3x3x3 slab, slowly spinning like a thrown boulder — reads clearly
// in third person from any distance
public class EarthChunkRenderer extends EntityRenderer<EarthChunkEntity> {

    public EarthChunkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.9F;
    }

    @Override
    public void render(EarthChunkEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        var states = entity.states();
        if (states.isEmpty()) {
            return;
        }
        float age = entity.tickCount + partialTick;
        poseStack.pushPose();
        // a thrown slab of the world spins — slow, heavy, menacing
        poseStack.mulPose(Axis.YP.rotationDegrees(age * 7.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.sin(age * 0.11F) * 9.0F));
        // hover bob
        poseStack.translate(0.0, Math.sin(age * 0.18F) * 0.07 + 0.9, 0.0);
        poseStack.scale(0.6F, 0.6F, 0.6F);
        // center the 3x3x3 grid on the entity origin
        poseStack.translate(-0.5, -1.5, -0.5);
        var dispatcher = Minecraft.getInstance().getBlockRenderer();
        for (int i = 0; i < states.size() && i < EarthChunkEntity.GRID; i++) {
            BlockState state = states.get(i);
            if (state.isAir()) {
                continue;
            }
            int dx = i / 9 - 1;
            int dy = (i % 9) / 3;
            int dz = i % 3 - 1;
            poseStack.pushPose();
            poseStack.translate(dx, dy, dz);
            dispatcher.renderSingleBlock(state, poseStack, buffer, packedLight,
                    OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(EarthChunkEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
