package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

// a guardian swirl rendered as a spinning amethyst shard, fullbright so it
// reads against the dark cavern
public class GuardianSwirlRenderer extends EntityRenderer<GuardianSwirlEntity> {
    public GuardianSwirlRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(GuardianSwirlEntity entity, float entityYaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.translate(0.0, 0.6, 0.0);
        float spin = (entity.tickCount + partialTicks) * 4.0F;
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(spin));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(20.0F));
        pose.scale(1.6F, 1.6F, 1.6F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.AMETHYST_CLUSTER.defaultBlockState(), pose, buffer,
                0xF000F0, OverlayTexture.NO_OVERLAY);
        pose.popPose();
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GuardianSwirlEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
