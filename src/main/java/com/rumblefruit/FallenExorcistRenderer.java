package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

// the Fallen Exorcist, 50 blocks tall: a corrupted angel-knight in a dark
// gold-trimmed robe, horned white mask, burning golden eyes, great feathered
// wings — floating over the cavern floor
public class FallenExorcistRenderer extends EntityRenderer<FallenExorcistEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/exorcist.png");
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
        // the exorcist never touches the ground: it floats, slowly bobbing
        float hover = Mth.sin((entity.tickCount + partialTicks) * 0.07F) * 0.5F + 2.5F;
        pose.translate(0.0, hover / 30.8F, 0.0);
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F - entityYaw));
        pose.scale(30.8F, 30.8F, 30.8F); // 26-px model -> 50-block colossus
        model.setupAnim(entity, 0.0F, 0.0F, entity.tickCount + partialTicks, 0.0F, 0.0F);
        model.renderToBuffer(pose, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)),
                0xF000F0, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        pose.popPose();
        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FallenExorcistEntity entity) {
        return TEXTURE;
    }
}
