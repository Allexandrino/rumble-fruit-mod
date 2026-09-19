package com.lightningfruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// jagged glowing bolt: midpoint-displacement path from the sky to the strike point,
// rendered as crossed additive quads (blue outer arc + white-hot core) with a flicker
public class ElectroBoltRenderer extends EntityRenderer<ElectroBoltEntity> {
    private static final double BOLT_HEIGHT = 40.0;

    public ElectroBoltRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(ElectroBoltEntity bolt, float entityYaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        float fade = bolt.getFade();
        if (fade <= 0.0F) {
            return;
        }
        int age = bolt.getBoltAge();
        // flicker: fast strobe like a real discharge
        float flicker = 0.7F + 0.3F * (float) Math.sin(age * 2.7) * (float) Math.sin(age * 5.1 + 1.3);
        float alpha = fade * flicker;

        Random random = new Random(bolt.getUUID().getMostSignificantBits());
        List<Vec3> mainPath = buildPath(new Vec3(0.0, BOLT_HEIGHT, 0.0), Vec3.ZERO, random);
        List<Vec3> branch = null;
        if (mainPath.size() > 6) {
            Vec3 from = mainPath.get(mainPath.size() * 2 / 3);
            Vec3 to = from.add((random.nextDouble() - 0.5) * 10.0, -8.0 - random.nextDouble() * 6.0,
                    (random.nextDouble() - 0.5) * 10.0);
            branch = buildPath(from, to, random);
        }

        VertexConsumer buffer = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        if (bolt.isHoly()) {
            // golden angel lightning
            drawPath(buffer, matrix, mainPath, random, 0.30F, 0.95F, 0.75F, 0.3F, alpha * 0.55F);
            drawPath(buffer, matrix, mainPath, random, 0.12F, 1.0F, 0.97F, 0.85F, alpha * 0.95F);
            if (branch != null) {
                drawPath(buffer, matrix, branch, random, 0.16F, 1.0F, 0.85F, 0.45F, alpha * 0.45F);
            }
        } else {
            drawPath(buffer, matrix, mainPath, random, 0.30F, 0.45F, 0.75F, 1.0F, alpha * 0.55F);
            drawPath(buffer, matrix, mainPath, random, 0.12F, 1.0F, 1.0F, 1.0F, alpha * 0.95F);
            if (branch != null) {
                drawPath(buffer, matrix, branch, random, 0.16F, 0.55F, 0.8F, 1.0F, alpha * 0.45F);
            }
        }
    }

    private static List<Vec3> buildPath(Vec3 start, Vec3 end, Random random) {
        List<Vec3> points = new ArrayList<>();
        points.add(start);
        points.add(end);
        for (int it = 0; it < 5; it++) {
            List<Vec3> next = new ArrayList<>();
            for (int i = 0; i + 1 < points.size(); i++) {
                Vec3 a = points.get(i);
                Vec3 b = points.get(i + 1);
                next.add(a);
                double disp = a.distanceTo(b) * 0.16;
                next.add(a.add(b).scale(0.5).add(
                        (random.nextDouble() - 0.5) * disp,
                        (random.nextDouble() - 0.5) * disp,
                        (random.nextDouble() - 0.5) * disp));
            }
            next.add(points.get(points.size() - 1));
            points = next;
        }
        return points;
    }

    // each segment becomes four planes (0/45/90/135 degrees around the axis) for real volume,
    // slightly wider toward the base of the arc
    private static void drawPath(VertexConsumer buffer, Matrix4f matrix, List<Vec3> path,
                                 Random random, float width,
                                 float r, float g, float b, float alpha) {
        for (int i = 0; i + 1 < path.size(); i++) {
            Vec3 a = path.get(i);
            Vec3 c = path.get(i + 1);
            Vec3 dir = c.subtract(a);
            if (dir.lengthSqr() < 1.0E-6) {
                continue;
            }
            dir = dir.normalize();
            float taper = 1.0F + 0.5F * ((float) i / (float) (path.size() - 1));
            Vec3 up = Math.abs(dir.y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
            Vec3 n1 = dir.cross(up).normalize().scale(width * taper * (0.8F + random.nextFloat() * 0.4F));
            Vec3 n2 = dir.cross(n1).normalize().scale(width * taper * (0.8F + random.nextFloat() * 0.4F));
            Vec3 n3 = n1.add(n2).scale(0.7071); // 45 degrees
            Vec3 n4 = n1.subtract(n2).scale(0.7071); // 135 degrees
            quad(buffer, matrix, a, c, n1, r, g, b, alpha);
            quad(buffer, matrix, a, c, n2, r, g, b, alpha);
            quad(buffer, matrix, a, c, n3, r, g, b, alpha);
            quad(buffer, matrix, a, c, n4, r, g, b, alpha);
        }
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix, Vec3 a, Vec3 b, Vec3 n,
                             float r, float g, float bl, float alpha) {
        buffer.addVertex(matrix, (float) (a.x - n.x), (float) (a.y - n.y), (float) (a.z - n.z)).setColor(r, g, bl, alpha);
        buffer.addVertex(matrix, (float) (a.x + n.x), (float) (a.y + n.y), (float) (a.z + n.z)).setColor(r, g, bl, alpha);
        buffer.addVertex(matrix, (float) (b.x + n.x), (float) (b.y + n.y), (float) (b.z + n.z)).setColor(r, g, bl, alpha);
        buffer.addVertex(matrix, (float) (b.x - n.x), (float) (b.y - n.y), (float) (b.z - n.z)).setColor(r, g, bl, alpha);
    }

    @Override
    public ResourceLocation getTextureLocation(ElectroBoltEntity entity) {
        return null; // RenderType.lightning needs no texture
    }
}
