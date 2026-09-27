package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

// the Fallen Exorcist, 50 blocks tall: a colossal horned face with four vast
// wings looming out of a mound of living darkness. a second emissive pass
// makes the eyes and the cracks in the dark burn
public class FallenExorcistRenderer extends EntityRenderer<FallenExorcistEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/exorcist.png");
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/exorcist_glow.png");
    private final ExorcistModel model;

    public FallenExorcistRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ExorcistModel(context.bakeLayer(ExorcistModel.LAYER));
        this.shadowRadius = 8.0F;
    }

    @Override
    public void render(FallenExorcistEntity entity, float entityYaw, float partialTicks,
                       PoseStack pose, MultiBufferSource buffer, int packedLight) {
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - entityYaw));
        pose.scale(30.8F, 30.8F, 30.8F); // 26-unit model -> 50-block colossus
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);
        model.renderToBuffer(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        // emissive pass: eyes, gems and the cracks in the dark burn
        model.renderToBuffer(pose, buffer.getBuffer(RenderType.eyes(GLOW)),
                0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FallenExorcistEntity entity) {
        return TEXTURE;
    }
}
