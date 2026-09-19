package com.lightningfruit;

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
public class LightningPillarRenderer extends EntityRenderer<LightningPillarEntity> {

    public LightningPillarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(LightningPillarEntity pillar, float entityYaw, float partialTicks,
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
        double height = LightningPillarEntity.PILLAR_HEIGHT;
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
        // swirling portal at the top where the beam comes from
        drawPortal(buffer, matrix, height, age, alpha, holy);
        // expanding ground ring
        drawGroundRing(buffer, matrix, age, alpha, holy);
    }

    // a portal/rift opening in the sky: concentric glowing rings + rotating arcs + dark vortex center
    private static void drawPortal(VertexConsumer buffer, Matrix4f matrix, double beamTop, int age, float alpha,
                                   boolean holy) {
        double y = beamTop + 0.2;
        float rot = age * 0.15F;
        int rings = 3;
        for (int ring = 0; ring < rings; ring++) {
            float radius = 2.2F + ring * 1.2F;
            int segments = 24;
            for (int i = 0; i < segments; i++) {
                double a0 = Math.PI * 2 * i / segments + rot * (ring % 2 == 0 ? 1 : -1);
                double a1 = Math.PI * 2 * (i + 1) / segments + rot * (ring % 2 == 0 ? 1 : -1);
                // alternate bright/dim segments for a swirling look
                float seg = (i % 2 == 0) ? 1.0F : 0.4F;
                float r = holy ? (ring == rings - 1 ? 1.0F : 0.95F) : (ring == rings - 1 ? 0.6F : 0.45F);
                float g = holy ? (ring == rings - 1 ? 0.9F : 0.78F) : 0.75F;
                float b = holy ? 0.35F : 1.0F;
                float th = 0.18F; // ring thickness
                float x0 = (float) (Math.cos(a0) * (radius - th)), z0 = (float) (Math.sin(a0) * (radius - th));
                float x1 = (float) (Math.cos(a1) * (radius - th)), z1 = (float) (Math.sin(a1) * (radius - th));
                float x2 = (float) (Math.cos(a1) * (radius + th)), z2 = (float) (Math.sin(a1) * (radius + th));
                float x3 = (float) (Math.cos(a0) * (radius + th)), z3 = (float) (Math.sin(a0) * (radius + th));
                buffer.addVertex(matrix, x0, (float) y, z0).setColor(r, g, b, alpha * seg);
                buffer.addVertex(matrix, x1, (float) y, z1).setColor(r, g, b, alpha * seg);
                buffer.addVertex(matrix, x2, (float) y, z2).setColor(r, g, b, alpha * seg);
                buffer.addVertex(matrix, x3, (float) y, z3).setColor(r, g, b, alpha * seg);
            }
        }
        // dark vortex center (flat dark disc that the beam pierces)
        int centerSeg = 16;
        for (int i = 0; i < centerSeg; i++) {
            double a0 = Math.PI * 2 * i / centerSeg + rot;
            double a1 = Math.PI * 2 * (i + 1) / centerSeg + rot;
            float r0 = 0.0F, r1 = 1.4F;
            buffer.addVertex(matrix, (float) (Math.cos(a0) * r0), (float) y, (float) (Math.sin(a0) * r0)).setColor(holy ? 0.3F : 0.05F, holy ? 0.18F : 0.05F, holy ? 0.02F : 0.2F, alpha);
            buffer.addVertex(matrix, (float) (Math.cos(a1) * r0), (float) y, (float) (Math.sin(a1) * r0)).setColor(holy ? 0.3F : 0.05F, holy ? 0.18F : 0.05F, holy ? 0.02F : 0.2F, alpha);
            buffer.addVertex(matrix, (float) (Math.cos(a1) * r1), (float) y, (float) (Math.sin(a1) * r1)).setColor(holy ? 0.3F : 0.05F, holy ? 0.18F : 0.05F, holy ? 0.02F : 0.2F, alpha);
            buffer.addVertex(matrix, (float) (Math.cos(a0) * r1), (float) y, (float) (Math.sin(a0) * r1)).setColor(holy ? 0.3F : 0.05F, holy ? 0.18F : 0.05F, holy ? 0.02F : 0.2F, alpha);
        }
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

    // flat expanding ring of quads at the base, like a shockwave
    private static void drawGroundRing(VertexConsumer buffer, Matrix4f matrix, int age, float alpha,
                                       boolean holy) {
        float radius = 1.5F + age * 0.25F;
        if (radius > 8.0F) {
            return;
        }
        float a = alpha * (1.0F - radius / 8.0F);
        float rr = holy ? 1.0F : 0.6F, gg = holy ? 0.85F : 0.8F, bb = holy ? 0.4F : 1.0F;
        int quads = 12;
        for (int i = 0; i < quads; i++) {
            double a0 = Math.PI * 2 * i / quads;
            double a1 = Math.PI * 2 * (i + 1) / quads;
            float x0 = (float) (Math.cos(a0) * radius);
            float z0 = (float) (Math.sin(a0) * radius);
            float x1 = (float) (Math.cos(a1) * radius);
            float z1 = (float) (Math.sin(a1) * radius);
            float inner = radius * 0.7F;
            float ix0 = (float) (Math.cos(a0) * inner);
            float iz0 = (float) (Math.sin(a0) * inner);
            float ix1 = (float) (Math.cos(a1) * inner);
            float iz1 = (float) (Math.sin(a1) * inner);
            buffer.addVertex(matrix, ix0, 0.05F, iz0).setColor(rr, gg, bb, a);
            buffer.addVertex(matrix, x0, 0.05F, z0).setColor(rr, gg, bb, a);
            buffer.addVertex(matrix, x1, 0.05F, z1).setColor(rr, gg, bb, a);
            buffer.addVertex(matrix, ix1, 0.05F, iz1).setColor(rr, gg, bb, a);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(LightningPillarEntity entity) {
        return null; // RenderType.lightning needs no texture
    }
}
