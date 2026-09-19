package com.lightningfruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// draws the LMB force-lightning: two thick jagged 3d bolts from the player's hands
// to the crosshair target, blox-fruits style. rendered for every channeling player
// (state synced via ChannelSyncPacket) so it shows in first AND third person.
@EventBusSubscriber(modid = LightningFruitMod.MOD_ID, value = Dist.CLIENT)
public class ChannelBoltRenderer {
    private static final double RANGE = 14.0;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        long reshuffle = mc.level.getGameTime() / 2; // new bolt shape every 2 ticks

        PoseStack poseStack = event.getPoseStack();
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderType.lightning());

        for (AbstractClientPlayer player : mc.level.players()) {
            boolean channeling = ClientChannelData.isActive(player.getUUID())
                    || (player == mc.player && ClientChannelInput.isChanneling());
            if (!channeling) {
                continue;
            }
            Vec3 eye = player.getEyePosition(partial);
            Vec3 view = player.getViewVector(partial);
            Vec3 reach = eye.add(view.scale(RANGE));
            BlockHitResult hit = player.level().clip(new ClipContext(
                    eye, reach, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            Vec3 target = hit.getType() == HitResult.Type.MISS ? reach : hit.getLocation();

            Vec3 up = new Vec3(0.0, 1.0, 0.0);
            Vec3 right = view.cross(up).normalize();
            boolean holy = ClientWingsData.isActive(player.getUUID());
            for (int side = -1; side <= 1; side += 2) {
                Vec3 start = eye.add(right.scale(0.35 * side))
                        .add(view.scale(0.7)).add(0.0, -0.3, 0.0);
                long seed = reshuffle * 31L + player.getUUID().hashCode() + side * 7L;
                drawRay(matrix, buffer, start.subtract(cam), target.subtract(cam), seed, holy);
            }
            // bright flare at the impact point
            drawFlash(matrix, buffer, target.subtract(cam), reshuffle);
        }
        // make sure the lightning quads actually reach the gpu this frame
        mc.renderBuffers().bufferSource().endBatch(RenderType.lightning());
    }

    // electro ray: a thick continuous energy beam from the hand to the target.
    // three nested straight ribbons (wide halo / mid glow / hot core) with a faint
    // energy wobble; holy = golden-white angel rays
    private static void drawRay(Matrix4f matrix, VertexConsumer buffer, Vec3 start, Vec3 end, long seed,
                                boolean holy) {
        Random random = new Random(seed);
        // slight wobble: one midpoint nudged perpendicular to the beam
        Vec3 dir = end.subtract(start).normalize();
        Vec3 wobbleAxis = dir.cross(CAM_FORWARD);
        if (wobbleAxis.lengthSqr() < 1.0E-4) {
            wobbleAxis = new Vec3(0.0, 1.0, 0.0);
        }
        wobbleAxis = wobbleAxis.normalize();
        Vec3 mid = start.add(end).scale(0.5)
                .add(wobbleAxis.scale((random.nextDouble() - 0.5) * 0.3));
        List<Vec3> points = new ArrayList<>();
        points.add(start);
        points.add(mid);
        points.add(end);

        float pulse = 0.9F + random.nextFloat() * 0.2F;
        if (holy) {
            drawPolyline(matrix, buffer, points, 0.20F * pulse, 0.95F, 0.8F, 0.4F, 0.10F); // golden halo
            drawPolyline(matrix, buffer, points, 0.09F * pulse, 0.98F, 0.85F, 0.5F, 0.35F); // gold glow
            drawPolyline(matrix, buffer, points, 0.035F * pulse, 1.0F, 0.98F, 0.9F, 0.95F); // white core
        } else {
            drawPolyline(matrix, buffer, points, 0.20F * pulse, 0.35F, 0.65F, 1.0F, 0.10F); // cyan halo
            drawPolyline(matrix, buffer, points, 0.09F * pulse, 0.5F, 0.8F, 1.0F, 0.35F); // electric glow
            drawPolyline(matrix, buffer, points, 0.035F * pulse, 0.92F, 0.98F, 1.0F, 0.95F); // white core
        }
    }

    // camera-facing ribbon: the quad is always perpendicular to the camera's forward axis
    // (points are in camera space, camera looks down -Z) — stable even near the camera
    private static final Vec3 CAM_FORWARD = new Vec3(0.0, 0.0, -1.0);

    private static void drawPolyline(Matrix4f matrix, VertexConsumer buffer, List<Vec3> points,
                                     float width, float r, float g, float b, float a) {
        for (int i = 0; i + 1 < points.size(); i++) {
            Vec3 p0 = points.get(i);
            Vec3 p1 = points.get(i + 1);
            Vec3 dir = p1.subtract(p0);
            if (dir.lengthSqr() < 1.0E-6) {
                continue;
            }
            dir = dir.normalize();
            Vec3 perp = dir.cross(CAM_FORWARD);
            if (perp.lengthSqr() < 1.0E-4) {
                perp = new Vec3(0.0, 1.0, 0.0); // segment points at the camera: any axis works
            }
            perp = perp.normalize().scale(width);
            Vec3 q0 = p0.subtract(perp), q1 = p0.add(perp);
            Vec3 q2 = p1.add(perp), q3 = p1.subtract(perp);
            buffer.addVertex(matrix, (float) q0.x, (float) q0.y, (float) q0.z).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) q1.x, (float) q1.y, (float) q1.z).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) q2.x, (float) q2.y, (float) q2.z).setColor(r, g, b, a);
            buffer.addVertex(matrix, (float) q3.x, (float) q3.y, (float) q3.z).setColor(r, g, b, a);
        }
    }

    // small bright star flash at the impact point
    private static void drawFlash(Matrix4f matrix, VertexConsumer buffer, Vec3 pos, long seed) {
        Random random = new Random(seed * 17L);
        float s = 0.5F + random.nextFloat() * 0.2F;
        float x = (float) pos.x, y = (float) pos.y, z = (float) pos.z;
        float r = 0.9F, g = 0.97F, b = 1.0F, a = 0.6F;
        // YZ plane
        quad(buffer, matrix, x, y - s, z - s, x, y + s, z - s, x, y + s, z + s, x, y - s, z + s, r, g, b, a);
        // XZ plane
        quad(buffer, matrix, x - s, y, z - s, x + s, y, z - s, x + s, y, z + s, x - s, y, z + s, r, g, b, a);
        // XY plane
        quad(buffer, matrix, x - s, y - s, z, x + s, y - s, z, x + s, y + s, z, x - s, y + s, z, r, g, b, a);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float r, float g, float b, float a) {
        buffer.addVertex(matrix, x0, y0, z0).setColor(r, g, b, a);
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
        buffer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a);
    }
}
