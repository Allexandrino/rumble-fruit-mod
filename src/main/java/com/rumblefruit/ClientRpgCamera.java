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
    private static int lastPanel = -1;
    private static final long PANEL_TICKS = 28; // 1.4s per comic panel
    private static long castCineUntil = 0L; // brief pull-back on big casts

    // skill cinematic: a snappy pull-back for heavy casts (C/V)
    public static void castCine() {
        castCineUntil = System.currentTimeMillis() + 550;
    }

    // impact feedback: every cast kicks the camera (recoil shake + fov punch)
    public static void addShake(float amount) {
        shake = Math.max(shake, amount);
    }

    public static void impactPulse() {
        fovKick -= 11.0F;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            roll = 0.0F;
            return;
        }
        // banking: sideways speed rolls the horizon like a chase cam
        Vec3 vel = mc.player.getDeltaMovement();
        float rollTarget = com.rumblefruit.core.CameraMath.bankRoll(
                new com.rumblefruit.core.Vec(vel.x, vel.y, vel.z), mc.player.getYRot());
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
            // skill micro-cinematic: a snappy pull-back while a heavy cast fires
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON
                    && System.currentTimeMillis() < castCineUntil) {
                net.minecraft.world.phys.Vec3 eye0 = mc.player.getEyePosition();
                net.minecraft.world.phys.Vec3 look0 = mc.player.getLookAngle();
                net.minecraft.world.phys.Vec3 back = eye0.add(-look0.x * 3.2, 1.0, -look0.z * 3.2);
                if (cinePos == null) {
                    cinePos = back;
                }
                cinePos = cinePos.lerp(back, 0.35);
                com.rumblefruit.mixin.CameraAccessor acc0 = (com.rumblefruit.mixin.CameraAccessor) camera;
                acc0.rumblefruit$setPosition(cinePos);
                acc0.rumblefruit$setDetached(true);
                com.rumblefruit.core.Vec dir = new com.rumblefruit.core.Vec(
                        eye0.x - cinePos.x, eye0.y - cinePos.y, eye0.z - cinePos.z);
                acc0.rumblefruit$setRotation(com.rumblefruit.core.CameraMath.lookYaw(dir),
                        com.rumblefruit.core.CameraMath.lookPitch(dir), roll * 0.3F);
                return;
            }
            // the comic is over — the strip is redrawn into real minecraft
            cinePos = null;
            lastPanel = -1;
            return;
        }
        net.minecraft.world.phys.Vec3 eye = mc.player.getEyePosition();
        net.minecraft.world.phys.Vec3 target;
        boolean hardCut = false;
        if (combo == 9) {
            // ascension: front shot — level with the face, 15 blocks out
            net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
            target = eye.add(look.x * 15.0, 2.5, look.z * 15.0);
            lastPanel = -1;
        } else {
            // the slow-mo fall as a 4-panel comic: every panel is a hard cut
            // to a new angle on the falling body
            long elapsed = ClientCombatAnim.elapsedOf(mc.player.getUUID());
            int panel = (int) Math.min(3, Math.max(0, elapsed) / PANEL_TICKS);
            hardCut = panel != lastPanel;
            lastPanel = panel;
            net.minecraft.world.phys.Vec3 look = mc.player.getLookAngle();
            float yawRad = mc.player.getYRot() * 0.0174533F;
            net.minecraft.world.phys.Vec3 side = new net.minecraft.world.phys.Vec3(
                    Math.cos(yawRad), 0.0, -Math.sin(yawRad));
            target = switch (panel) {
                case 0 -> eye.add(look.x * 5.0, -3.5, look.z * 5.0);    // low hero shot
                case 1 -> eye.add(side.x * 6.0, 0.5, side.z * 6.0);     // side profile
                case 2 -> eye.add(3.0, 8.0, 3.0);                       // crane overhead
                default -> eye.add(look.x * 12.0, -6.0, look.z * 12.0); // wide finale
            };
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
        // comic cut: snap straight to the new panel angle; otherwise glide in
        // gently from clear of the body
        if (hardCut) {
            cinePos = target;
        } else if (cinePos == null) {
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
        com.rumblefruit.core.Vec lookDir = new com.rumblefruit.core.Vec(
                eye.x - cinePos.x, eye.y - 0.5 - cinePos.y, eye.z - cinePos.z);
        acc.rumblefruit$setRotation(com.rumblefruit.core.CameraMath.lookYaw(lookDir),
                com.rumblefruit.core.CameraMath.lookPitch(lookDir), roll * 0.3F);
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
