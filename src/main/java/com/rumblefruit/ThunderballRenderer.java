package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ThunderballRenderer extends EntityRenderer<ThunderballEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/thunderball.png");

    private final ThunderballModel model;

    public ThunderballRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ThunderballModel(context.bakeLayer(ThunderballModel.LAYER_LOCATION));
        this.shadowRadius = 1.5F;
    }

    @Override
    public void render(ThunderballEntity entity, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        float age = entity.tickCount + partialTicks;
        boolean grounded = entity.getPhase() == ThunderballEntity.PHASE_GROUNDED;
        float base = 10.0F * entity.getChargeLevel();
        float forming = entity.getPhase() == ThunderballEntity.PHASE_FORMING
                ? Mth.clamp(age / 20.0F, 0.0F, 1.0F)
                : 1.0F;
        float pulse = grounded ? 0.15F : 0.08F;
        float scale = base * forming * (1.0F + pulse * Mth.sin(age * 0.3F));

        poseStack.pushPose();
        poseStack.translate(0.0, 1.0, 0.0);
        poseStack.scale(scale, scale, scale);
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(age * (grounded ? 0.8F : 2.0F))));
        poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(age * 0.7F)));

        VertexConsumer swirl = buffer.getBuffer(
                RenderType.energySwirl(TEXTURE, age * 0.01F, age * 0.005F));
        model.renderToBuffer(poseStack, swirl, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, -1);

        VertexConsumer core = buffer.getBuffer(RenderType.eyes(TEXTURE));
        poseStack.pushPose();
        poseStack.scale(0.6F, 0.6F, 0.6F);
        model.renderToBuffer(poseStack, core, LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();

        poseStack.popPose();

        if (grounded) {
            float groundedAge = entity.tickCount - entity.getPhaseStartTick() + partialTicks;
            if (groundedAge < 15.0F) {
                renderBeam(poseStack, buffer, 1.0F - groundedAge / 15.0F);
            }
        }

        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    private void renderBeam(PoseStack poseStack, MultiBufferSource buffer, float fade) {
        VertexConsumer vc = buffer.getBuffer(RenderType.lightning());
        poseStack.pushPose();
        Matrix4f mat = poseStack.last().pose();
        float w = 0.5F + 1.0F * fade;
        float y0 = -0.5F;
        float y1 = 40.0F;
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI * i / 4.0;
            float dx = (float) Math.cos(angle) * w;
            float dz = (float) Math.sin(angle) * w;
            vc.addVertex(mat, -dx, y0, -dz).setColor(1.0F, 0.95F, 0.5F, fade);
            vc.addVertex(mat, dx, y0, dz).setColor(1.0F, 0.95F, 0.5F, fade);
            vc.addVertex(mat, dx, y1, dz).setColor(1.0F, 0.95F, 0.5F, fade);
            vc.addVertex(mat, -dx, y1, -dz).setColor(1.0F, 0.95F, 0.5F, fade);
        }
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ThunderballEntity entity) {
        return TEXTURE;
    }
}
