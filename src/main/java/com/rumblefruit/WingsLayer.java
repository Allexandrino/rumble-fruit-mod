package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
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
    private static final float WING_SCALE = 5.5F; // huge long wings on every fruit

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
        // wings sweep out from the back like a real bird's at rest — not
        // sticking straight up; wider than tall, they drape the shoulders
        poseStack.translate(0.0, 0.05, 0.14);
        float scale = WING_SCALE * eased;
        poseStack.scale(scale * 1.3F, scale, scale); // wingspan over height
        poseStack.translate(0.0, -0.05, -0.14);
        // elemental transformation: own wing shape AND color per element —
        // angel feathers, flame tongues, bat membrane, ice shards, leaf fan
        var element = com.rumblefruit.core.ElementCatalog.byId(ClientPowerData.element());
        int tint = 0xFF000000 | element.color();
        model.renderWings(element.id(), poseStack, vertexConsumer, packedLight, OverlayTexture.NO_OVERLAY, tint);

        // golden lightning arcs crackling ON the wings (wing space: they follow the flaps)
        if (unfold > 0.4F) {
            VertexConsumer lightning = buffer.getBuffer(RenderType.lightning());
            renderWingArcs(poseStack, lightning, ageInTicks, flying, unfold);
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
        // headgear fits the fruit: the exorcist mask belongs ONLY to the
        // lightning angel; the fire demon gets horns, the void king a crown,
        // the seraphim keeps the halo, mother nature wears a flower circlet
        VertexConsumer emissive = buffer.getBuffer(RenderType.eyes(TEXTURE));
        switch (element.id()) {
            case 0 -> {
                model.renderMask(poseStack, emissive, packedLight, OverlayTexture.NO_OVERLAY);
                model.renderHalo(poseStack, emissive, packedLight, OverlayTexture.NO_OVERLAY, tint);
            }
            case 1 -> model.renderHorns(poseStack, emissive, packedLight, OverlayTexture.NO_OVERLAY,
                    0xFF3A1A10); // charcoal demon horns
            case 2 -> model.renderCrown(poseStack, emissive, packedLight, OverlayTexture.NO_OVERLAY,
                    0xFFFFD24A); // royal gold on the king of darkness
            case 3 -> model.renderHalo(poseStack, emissive, packedLight, OverlayTexture.NO_OVERLAY, tint);
            case 4 -> model.renderCirclet(poseStack, emissive, packedLight, OverlayTexture.NO_OVERLAY, tint);
            default -> {
            }
        }
        poseStack.popPose();

        // the form itself lives: the fire demon BURNS, the void king leaks
        // darkness, snowflakes drift off the seraphim, life motes off nature
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level != null && player.tickCount % 3 == 0) {
            double px = player.getX();
            double py = player.getY();
            double pz = player.getZ();
            switch (element.id()) {
                case 1 -> {
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,
                            px + (Math.random() - 0.5) * 0.7, py + Math.random() * 1.6,
                            pz + (Math.random() - 0.5) * 0.7, 0.0, 0.06, 0.0);
                    mc.level.addParticle(Element.INFERNO.spark(),
                            px + (Math.random() - 0.5) * 0.6, py + 0.5 + Math.random(),
                            pz + (Math.random() - 0.5) * 0.6, 0.0, 0.08, 0.0);
                }
                case 2 -> {
                    mc.level.addParticle(Element.VOID.spark(),
                            px + (Math.random() - 0.5) * 0.8, py + Math.random() * 1.7,
                            pz + (Math.random() - 0.5) * 0.8, 0.0, 0.02, 0.0);
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                            px + (Math.random() - 0.5) * 0.6, py + Math.random() * 1.5,
                            pz + (Math.random() - 0.5) * 0.6, 0.0, 0.02, 0.0);
                }
                case 3 -> mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                        px + (Math.random() - 0.5) * 0.8, py + 0.4 + Math.random() * 1.4,
                        pz + (Math.random() - 0.5) * 0.8, 0.0, -0.02, 0.0);
                case 4 -> mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                        px + (Math.random() - 0.5) * 0.8, py + Math.random() * 1.6,
                        pz + (Math.random() - 0.5) * 0.8, 0.0, 0.03, 0.0);
                default -> {
                }
            }
        }
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
