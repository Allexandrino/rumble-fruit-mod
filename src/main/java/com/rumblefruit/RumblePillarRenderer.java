package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

// continuous vertical lightning beam from the cloud to the ground:
// thick crossed quads along the full height, bright core + blue glow, strobe flicker
public class RumblePillarRenderer extends EntityRenderer<RumblePillarEntity> {

    public RumblePillarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(RumblePillarEntity pillar, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float fade = pillar.getFade();
        if (fade <= 0.0F) {
            return;
        }
        int age = pillar.tickCount;
        float flicker = 0.75F + 0.25F * (float) Math.sin(age * 3.1) * (float) Math.sin(age * 5.7 + 0.9);
        float alpha = fade * flicker;

        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        boolean holy = pillar.isHoly();
        double height = RumblePillarEntity.PILLAR_HEIGHT;
        int segments = 36;
        if (holy) {
            // golden angel pillar
            drawBeam(buffer, matrix, height, segments, 3.0F, 0.95F, 0.75F, 0.3F, alpha * 0.5F, age);
            drawBeam(buffer, matrix, height, segments, 1.4F, 1.0F, 0.97F, 0.85F, alpha * 0.95F, age);
        } else {
            // outer blue beam — huge 3D tube
            drawBeam(buffer, matrix, height, segments, 3.0F, 0.45F, 0.75F, 1.0F, alpha * 0.5F, age);
            // white-hot core — huge 3D tube
            drawBeam(buffer, matrix, height, segments, 1.4F, 1.0F, 1.0F, 1.0F, alpha * 0.95F, age);
        }
        // портал-кольца и ударная волна убраны по просьбе — только луч
    }

    // 3D tube: N-sided polygonal ring per segment — real volume from every angle
    private static final int SIDES = 12;

    private static void drawBeam(VertexConsumer buffer, Matrix4f matrix, double height, int segments,
                                 float width, float r, float g, float b, float alpha, int age) {
        for (int i = 0; i < segments; i++) {
            double y0 = height * i / segments;
            double y1 = height * (i + 1) / segments;
            double wob0 = Math.sin(i * 1.7 + age * 0.6) * 0.2;
            double wob1 = Math.sin((i + 1) * 1.7 + age * 0.6) * 0.2;
            float taper = 1.0F - 0.3F * ((float) i / segments); // slightly wider at the base
            float rad = width * taper;

            for (int s = 0; s < SIDES; s++) {
                double a0 = Math.PI * 2 * s / SIDES;
                double a1 = Math.PI * 2 * (s + 1) / SIDES;
                // ring at y0
                float x00 = (float) (Math.cos(a0) * rad + wob0);
                float z00 = (float) (Math.sin(a0) * rad);
                float x01 = (float) (Math.cos(a1) * rad + wob0);
                float z01 = (float) (Math.sin(a1) * rad);
                // ring at y1
                float x10 = (float) (Math.cos(a0) * rad + wob1);
                float z10 = (float) (Math.sin(a0) * rad);
                float x11 = (float) (Math.cos(a1) * rad + wob1);
                float z11 = (float) (Math.sin(a1) * rad);

                buffer.addVertex(matrix, x00, (float) y0, z00).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, x01, (float) y0, z01).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, x11, (float) y1, z11).setColor(r, g, b, alpha);
                buffer.addVertex(matrix, x10, (float) y1, z10).setColor(r, g, b, alpha);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(RumblePillarEntity entity) {
        return null; // RenderType.lightning needs no texture
    }
}
