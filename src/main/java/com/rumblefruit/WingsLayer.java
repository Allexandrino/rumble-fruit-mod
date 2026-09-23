package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.Random;

// renders the angel transformation: huge wings that unfold with an animation,
// golden lightning arcs crackling directly ON the wings (following the flaps),
// plus the hazbin exorcist mask and the golden halo
public class WingsLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/wings.png");
    private static final ResourceLocation ROBE =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/angel_robe.png");
    private static final float WING_SCALE = 4.0F;

    private final WingsModel model;

    public WingsLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                      EntityModelSet modelSet) {
        super(parent);
        this.model = new WingsModel(modelSet.bakeLayer(WingsModel.LAYER_LOCATION));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        if (!ClientWingsData.isActive(player.getUUID())) {
            return;
        }
        float unfold = ClientWingsData.unfoldProgress(player.getUUID(), ageInTicks);
        // ease-out-back: the wings burst out and settle
        float eased = 1.0F + 2.7F * (float) Math.pow(unfold - 1.0F, 3.0F)
                + 1.7F * (float) Math.pow(unfold - 1.0F, 2.0F);
        eased = Math.max(0.05F, eased);

        boolean flying = !player.onGround();
        model.setFlap(ageInTicks, flying);
        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        // adam-scale angel wings: scale the whole wing model up around the shoulder attach point
        poseStack.translate(0.0, 0.05, 0.14);
        float scale = WING_SCALE * eased;
        poseStack.scale(scale, scale, scale);
        poseStack.translate(0.0, -0.05, -0.14);
        // elemental tint: fire burns orange, void purple, frost pale, nature green
        int tint = 0xFF000000 | com.rumblefruit.core.ElementCatalog.byId(ClientPowerData.element()).color();
        model.renderWings(poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, tint);

        // golden lightning arcs crackling ON the wings (wing space: they follow the flaps)
        if (unfold > 0.4F) {
            VertexConsumer lightning = buffer.getBuffer(RenderType.lightning());
            renderWingArcs(poseStack, lightning, ageInTicks, flying, unfold);
            // electro transformation: golden arcs crackle across the body too
            renderBodyArcs(poseStack, lightning, ageInTicks, unfold);
            // no manual endBatch here: the entity buffer source flushes everything
            // after all layers — ending it early kills the shared builder mid-frame
        }
        poseStack.popPose();

        // electro-god costume extras: high collar + sharp coat tails (robe texture)
        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        VertexConsumer robeConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(ROBE));
        model.renderCostume(poseStack, robeConsumer, packedLight, OverlayTexture.NO_OVERLAY, tint);
        poseStack.popPose();

        poseStack.pushPose();
        this.getParentModel().head.translateAndRotate(poseStack);
        // fullbright: the exorcist mask and the halo glow in the dark
        VertexConsumer emissive = buffer.getBuffer(RenderType.eyes(TEXTURE));
        model.renderMask(poseStack, emissive, packedLight, OverlayTexture.NO_OVERLAY);
        model.renderHalo(poseStack, emissive, packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    // jagged golden arcs along each wing, attached in wing space (post-scale),
    // rotated with the same flap values the model uses
    private void renderWingArcs(PoseStack poseStack, VertexConsumer buffer, float ageInTicks,
                                boolean flying, float unfold) {
        float speed = flying ? 0.45F : 0.1F;
        float amp = flying ? 0.45F : 0.06F;
        float fold = Mth.sin(ageInTicks * speed) * amp;
        float lift = Mth.cos(ageInTicks * speed) * amp * 0.5F;

        for (int side = -1; side <= 1; side += 2) {
            poseStack.pushPose();
            // wing root in model space (matches PartPose.offset(±1.2, 0.8, 2.2) in pixels)
            poseStack.translate(side * 1.2 / 16.0, 0.8 / 16.0, 2.2 / 16.0);
            float yRot = side < 0 ? 0.2F + fold : -0.2F - fold;
            float zRot = side < 0 ? lift : -lift;
            poseStack.mulPose(com.mojang.math.Axis.YP.rotation(yRot));
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotation(zRot));

            long reshuffle = (long) (ageInTicks * 3.0F);
            // arcs run along the feather fan: upper / middle / lower feather lines,
            // spanning almost the whole wing (local space is scaled up by the layer)
            float[][] arcs = {
                    {0.52F, -0.34F},
                    {0.60F, -0.08F},
                    {0.55F, 0.16F},
            };
            for (int arc = 0; arc < arcs.length; arc++) {
                Random random = new Random(reshuffle + side * 131L + arc * 17L);
                float len = arcs[arc][0] + random.nextFloat() * 0.08F;
                float droop = arcs[arc][1] + (random.nextFloat() - 0.5F) * 0.05F;
                drawArc(poseStack, buffer, random, side * len, droop, unfold);
            }
            poseStack.popPose();
        }
    }

    private void drawArc(PoseStack poseStack, VertexConsumer buffer, Random random,
                         double tipX, double tipY, float unfold) {
        Matrix4f matrix = poseStack.last().pose();
        int segments = 6;
        float[][] pts = new float[segments + 1][3];
        for (int i = 0; i <= segments; i++) {
            float t = (float) i / segments;
            pts[i][0] = (float) (tipX * t) + (i == 0 || i == segments ? 0 : (random.nextFloat() - 0.5F) * 0.05F);
            pts[i][1] = (float) (tipY * t) + (i == 0 || i == segments ? 0 : (random.nextFloat() - 0.5F) * 0.05F);
            pts[i][2] = (i == 0 || i == segments ? 0 : (random.nextFloat() - 0.5F) * 0.03F);
        }
        float flicker = (0.65F + 0.35F * random.nextFloat()) * unfold;
        for (int pass = 0; pass < 2; pass++) {
            float width = pass == 0 ? 0.018F : 0.008F;
            float r = pass == 0 ? 0.95F : 1.0F;
            float g = pass == 0 ? 0.75F : 0.97F;
            float b = pass == 0 ? 0.3F : 0.85F;
            float a = (pass == 0 ? 0.65F : 1.0F) * flicker;
            for (int i = 0; i < segments; i++) {
                float x0 = pts[i][0], y0 = pts[i][1], z0 = pts[i][2];
                float x1 = pts[i + 1][0], y1 = pts[i + 1][1], z1 = pts[i + 1][2];
                for (int axis = 0; axis < 2; axis++) {
                    float wx = axis == 0 ? width : 0.0F;
                    float wz = axis == 0 ? 0.0F : width;
                    buffer.addVertex(matrix, x0 - wx, y0, z0 - wz).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x0 + wx, y0, z0 + wz).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x1 + wx, y1, z1 + wz).setColor(r, g, b, a);
                    buffer.addVertex(matrix, x1 - wx, y1, z1 - wz).setColor(r, g, b, a);
                }
            }
        }
    }

    // golden arcs dancing across the torso of the electro-angel
    private void renderBodyArcs(PoseStack poseStack, VertexConsumer buffer, float ageInTicks, float unfold) {
        long reshuffle = (long) (ageInTicks * 4.0F);
        // short arcs over the chest and shoulders (wing-scaled space: 1/4 of torso coords)
        float[][] spots = {
                {0.0F, 0.09F, -0.055F, 0.06F, 0.13F, -0.05F},
                {-0.05F, 0.11F, -0.05F, 0.04F, 0.07F, -0.06F},
                {0.05F, 0.06F, -0.055F, -0.05F, 0.12F, -0.05F},
        };
        for (int i = 0; i < spots.length; i++) {
            Random random = new Random(reshuffle + i * 91L);
            float[] s = spots[i];
            drawBodyArc(poseStack, buffer, random, s[0], s[1], s[2], s[3], s[4], s[5], unfold);
        }
    }

    private void drawBodyArc(PoseStack poseStack, VertexConsumer buffer, Random random,
                             float x0, float y0, float z0, float x1, float y1, float z1, float unfold) {
        Matrix4f matrix = poseStack.last().pose();
        int segments = 4;
        float flicker = (0.55F + 0.45F * random.nextFloat()) * unfold;
        for (int pass = 0; pass < 2; pass++) {
            float width = pass == 0 ? 0.012F : 0.006F;
            float r = pass == 0 ? 0.95F : 1.0F;
            float g = pass == 0 ? 0.75F : 0.97F;
            float b = pass == 0 ? 0.3F : 0.85F;
            float a = (pass == 0 ? 0.55F : 0.95F) * flicker;
            float px = x0, py = y0, pz = z0;
            for (int i = 1; i <= segments; i++) {
                float t = (float) i / segments;
                float nx = x0 + (x1 - x0) * t + (random.nextFloat() - 0.5F) * 0.02F;
                float ny = y0 + (y1 - y0) * t + (random.nextFloat() - 0.5F) * 0.02F;
                float nz = z0 + (z1 - z0) * t + (random.nextFloat() - 0.5F) * 0.012F;
                for (int axis = 0; axis < 2; axis++) {
                    float wx = axis == 0 ? width : 0.0F;
                    float wz = axis == 0 ? 0.0F : width;
                    buffer.addVertex(matrix, px - wx, py, pz - wz).setColor(r, g, b, a);
                    buffer.addVertex(matrix, px + wx, py, pz + wz).setColor(r, g, b, a);
                    buffer.addVertex(matrix, nx + wx, ny, nz + wz).setColor(r, g, b, a);
                    buffer.addVertex(matrix, nx - wx, ny, nz - wz).setColor(r, g, b, a);
                }
                px = nx;
                py = ny;
                pz = nz;
            }
        }
    }
}
