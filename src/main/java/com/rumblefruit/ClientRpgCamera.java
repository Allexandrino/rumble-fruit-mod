package com.rumblefruit;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

// dynamic action-rpg camera for third person: banks into turns and strafes,
// punches the fov on every slash, slow-zooms during the R ascension and
// shakes the world apart when it detonates
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT)
public class ClientRpgCamera {
    private static float roll = 0.0F;
    private static float fovKick = 0.0F;
    private static float shake = 0.0F;
    private static int prevTicksSinceSlash = 100;
    private static net.minecraft.world.phys.Vec3 cinePos = null;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            roll = 0.0F;
            return;
        }
        // banking: sideways speed rolls the horizon like a chase cam
        Vec3 vel = mc.player.getDeltaMovement();
        float yawRad = mc.player.getYRot() * Mth.DEG_TO_RAD;
        double lateral = -vel.x * Math.cos(yawRad) - vel.z * Math.sin(yawRad);
        float rollTarget = Mth.clamp((float) lateral * 14.0F, -7.0F, 7.0F);
        roll += (rollTarget - roll) * 0.1F;

        // fov punch on every sword slash
        int sinceSlash = ClientStanceCombat.ticksSinceSlash();
        if (sinceSlash < prevTicksSinceSlash) {
            fovKick = 7.0F;
            shake = Math.max(shake, 0.35F);
        }
        prevTicksSinceSlash = sinceSlash;

        // R ultimate: cinematic pull-in while charging, blast shake on detonation
        if (ClientCombatAnim.comboOf(mc.player.getUUID()) == 9) {
            float t = ClientCombatAnim.progressOf(mc.player.getUUID());
            fovKick = Math.min(fovKick, 0.0F) - 9.0F * t; // slow zoom-in
            if (t > 0.78F && t < 0.92F) {
                shake = 4.0F; // the blast
            }
        }
        fovKick *= 0.82F;
        if (Math.abs(fovKick) < 0.05F) {
            fovKick = 0.0F;
        }
        shake *= 0.8F;
    }

    // true while the cinematic ultimate camera is framing the local player
    public static boolean isCinematic() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }
        int combo = ClientCombatAnim.comboOf(mc.player.getUUID());
        return combo == 9 || combo == 20;
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON || !event.usedConfiguredFov()) {
            return;
        }
        event.setFOV(event.getFOV() + fovKick);
    }

    // called from CameraMixin after vanilla positioned the camera
    public static void applyCinematic(net.minecraft.client.Camera camera, net.minecraft.world.entity.Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || entity != mc.player) {
            cinePos = null;
            return;
        }
        int combo = ClientCombatAnim.comboOf(mc.player.getUUID());
        if (combo == 9 || combo == 20) {
            System.out.println("[rumblefruit] cinematic active, combo=" + combo);
        }
        if (combo != 9 && combo != 20) {
            cinePos = null;
            return;
        }
        net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
        net.minecraft.world.phys.Vec3 target;
        if (combo == 9) {
            // ascension: front shot — level with the face, 15 blocks out
            net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
            target = eye.add(look.x * 15.0, 2.5, look.z * 15.0);
        } else {
            // knockout: crane shot — high above the crater, looking down
            target = eye.add(5.0, 9.0, 5.0);
        }
        var hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                eye, target, net.minecraft.world.level.ClipContext.Block.VISUAL,
                net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player));
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            target = eye.add(hit.getLocation().subtract(eye).scale(0.85));
            // too cramped for a shot — pull up into a raised three-quarter view
            if (target.distanceTo(eye) < 5.0) {
                target = eye.add(3.0, 10.0, 3.0);
            }
        }
        // smooth cinematic glide — starts already clear of the body
        if (cinePos == null) {
            net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
            cinePos = eye.add(look.x * 5.0, 1.5, look.z * 5.0);
        }
        // never let the camera sit inside a solid block: freeze the glide
        // instead of sliding into a wall
        net.minecraft.world.phys.Vec3 next = cinePos.lerp(target, 0.15);
        if (!mc.level.getBlockState(net.minecraft.core.BlockPos.containing(next)).isSolid()) {
            cinePos = next;
        }
        if (mc.level.getBlockState(net.minecraft.core.BlockPos.containing(cinePos)).isSolid()) {
            return; // no clean shot this frame — keep the vanilla camera
        }
        com.rumblefruit.mixin.CameraAccessor acc = (com.rumblefruit.mixin.CameraAccessor) camera;
        acc.rumblefruit$setPosition(cinePos);
        acc.rumblefruit$setDetached(true);
        // frame the caster
        double dx = eye.x - cinePos.x, dy = eye.y - 0.5 - cinePos.y, dz = eye.z - cinePos.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        acc.rumblefruit$setYRot((float) Math.toDegrees(Math.atan2(-dx, dz)));
        acc.rumblefruit$setXRot((float) Math.toDegrees(-Math.atan2(dy, flat)));
        acc.rumblefruit$setRoll(roll * 0.3F);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || isCinematic()) {
            return; // the cinematic shot owns the camera (CameraMixin)
        }
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            return;
        }
        event.setRoll(event.getRoll() + roll + (float) (Math.random() - 0.5) * shake);
        if (shake > 0.05F) {
            event.setYaw(event.getYaw() + (float) (Math.random() - 0.5) * shake * 0.6F);
            event.setPitch(event.getPitch() + (float) (Math.random() - 0.5) * shake * 0.6F);
        }
    }
}
