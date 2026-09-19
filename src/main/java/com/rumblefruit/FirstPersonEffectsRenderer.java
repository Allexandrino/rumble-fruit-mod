package com.rumblefruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.SubscribeEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Random;

// first-person weapon + energy orb, rendered in the HAND pass (RenderHandEvent):
// we never cancel the event — vanilla renders the arm, we draw the weapon in the fist
// and the orb above it on top of the same buffers (vanilla flushes them after).
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT)
public class FirstPersonEffectsRenderer {
    private static final ResourceLocation GLOW =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/entity/orb_glow.png");
    private static final int FULL_BRIGHT = 0xF000F0;

    private static long mc_level_time(LocalPlayer player) {
        return player.level() != null ? player.level().getGameTime() : player.tickCount;
    }

    private static WeaponModels swordModel = null;
    private static WeaponModels bowModel = null;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (ClientRpgCamera.isCinematic()) {
            // cinematic ultimate camera: no first-person hand in the shot
            event.setCanceled(true);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !ClientPowerData.has()) {
            return;
        }
        if (swordModel == null) {
            swordModel = WeaponModels.sword(mc.getEntityModels().bakeLayer(WeaponModels.SWORD_LAYER));
            bowModel = WeaponModels.bow(mc.getEntityModels().bakeLayer(WeaponModels.BOW_LAYER));
        }
        int stance = ClientStanceData.get(player.getUUID());
        float partial = event.getPartialTick();
        float time = player.tickCount + partial;
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        // the vanilla first-person item anchor; vanilla keeps rendering the arm itself
        poseStack.translate(0.34, -0.40, -0.64);

        if (stance == StanceData.FISTS) {
            renderFistCharge(poseStack, event.getMultiBufferSource(), player, time);
        } else {
            renderWeapon(poseStack, event.getMultiBufferSource(), player, stance, time, partial);
        }
        poseStack.popPose();
    }

    // fists stance: the fist itself crackles with electricity — no orb
    private static void renderFistCharge(PoseStack poseStack, MultiBufferSource buffers, LocalPlayer player, float time) {
        boolean holy = ClientWingsData.isActive(player.getUUID());
        float r = holy ? 1.0F : 0.55F, g = holy ? 0.9F : 0.85F, b = holy ? 0.5F : 1.0F;
        poseStack.pushPose();
        poseStack.translate(-0.10, 0.18, 0.05); // over the knuckles
        Matrix4f m = poseStack.last().pose();
        VertexConsumer boltBuf = buffers.getBuffer(RenderType.lightning());
        Random random = new Random((long) (time * 4.0F));
        for (int i = 0; i < 3; i++) {
            // jagged arc hugging the fist
            float x = (random.nextFloat() - 0.5F) * 0.09F;
            float y = (random.nextFloat() - 0.5F) * 0.09F;
            for (int seg = 0; seg < 4; seg++) {
                float nx = x + (random.nextFloat() - 0.5F) * 0.1F;
                float ny = y + (random.nextFloat() - 0.5F) * 0.1F;
                quad(boltBuf, m, x - 0.008F, y, x + 0.008F, y, nx + 0.008F, ny, nx - 0.008F, ny,
                        r, g, b, 0.9F);
                x = nx;
                y = ny;
            }
        }
        poseStack.popPose();
    }

    // the stance weapon (custom 3d model) in the fist, with combo/draw animations
    private static float prevYaw = 0.0F;
    private static float prevPitch = 0.0F;
    private static float swayYaw = 0.0F;
    private static float swayPitch = 0.0F;

    private static void renderWeapon(PoseStack poseStack, MultiBufferSource buffers, LocalPlayer player,
                                     int stance, float time, float partial) {
        boolean holy = ClientWingsData.isActive(player.getUUID());
        WeaponModels model = stance == StanceData.SWORD ? swordModel : bowModel;
        ResourceLocation texture = stance == StanceData.SWORD
                ? (holy ? WeaponModels.SWORD_HOLY_TEXTURE : WeaponModels.SWORD_TEXTURE)
                : (holy ? WeaponModels.BOW_HOLY_TEXTURE : WeaponModels.BOW_TEXTURE);
        poseStack.pushPose();
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90.0F));
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(28.0F));
        poseStack.translate(1.13F / 16.0F, 3.2F / 16.0F, 1.13F / 16.0F);
        poseStack.scale(0.85F, 0.85F, 0.85F);

        // weapon draw: rises into the grip when the stance switches
        long sinceDraw = (mc_level_time(player)) - ClientStanceData.lastChangeTick;
        if (sinceDraw >= 0 && sinceDraw < 14) {
            float p = 1.0F - (float) Math.pow(1.0F - sinceDraw / 14.0F, 3.0);
            poseStack.translate(0.0, -0.5 * (1 - p), -0.12 * (1 - p));
            poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-55.0F * (1 - p))));
        }

        // inertia: the weapon lags slightly behind fast camera turns (weight feel)
        float dYaw = player.getYRot() - prevYaw;
        float dPitch = player.getXRot() - prevPitch;
        prevYaw = player.getYRot();
        prevPitch = player.getXRot();
        swayYaw += (Mth.clamp(dYaw, -8.0F, 8.0F) - swayYaw) * 0.12F;
        swayPitch += (Mth.clamp(dPitch, -8.0F, 8.0F) - swayPitch) * 0.12F;
        poseStack.translate(swayYaw * 0.006F, swayPitch * 0.006F, 0.0F);
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(swayYaw * 0.8F)));

        if (ClientStanceCombat.isDrawing() && stance == StanceData.BOW) {
            applyBowDraw(poseStack, partial);
        } else {
            float sinceCast = time - ClientSkillInput.lastCastTick;
            boolean flourish = sinceCast >= 0.0F && sinceCast <= 14.0F;
            if (flourish) {
                SkillAnim.applyFlourish(poseStack, sinceCast);
            } else if (stance == StanceData.SWORD) {
                applySword(poseStack, player, partial, time);
            } else {
                applyBow(poseStack, player, partial, time);
            }
        }

        int light = Minecraft.getInstance().getEntityRenderDispatcher().getPackedLightCoords(player, partial);
        model.renderWeapon(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light);
        poseStack.popPose();
    }

    // stickman-fight style combos: 360 spin slash / leap slash / dash thrust,
    // all with spark particles streaming off the blade
    private static void applySword(PoseStack poseStack, LocalPlayer player, float partial, float time) {
        applyIdle(poseStack, player, partial, time);
        float since = ClientStanceCombat.ticksSinceSlash() - partial;
        if (since >= 0.0F && since < 20.0F) {
            float t = since / 20.0F;
            float strike = (float) Math.pow(phase(t, 0.15F, 0.5F), 0.5F); // explosive snap
            float recover = phase(t, 0.55F, 1.0F);
            int combo = ClientStanceCombat.combo;
            if (combo == 0) {
                // CYCLONE: the blade whips TWO full circles around the hand
                float spin = strike * 720.0F;
                float arc = (float) Math.sin(strike * Math.PI);
                poseStack.translate(-0.22F * arc, 0.12F * arc, -0.3F * arc);
                poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(spin)));
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-45.0F * arc)));
            } else if (combo == 1) {
                // RISING DRAGON: drag low, then tear up past the camera
                float up = phase(t, 0.0F, 0.32F);
                float down = (float) Math.pow(phase(t, 0.38F, 0.62F), 0.4F);
                float y = lerp(lerp(0.0F, 0.85F, up), -0.9F, down);
                y = lerp(y, 0.0F, recover);
                poseStack.translate(0.0F, y, -0.2F * down * (1.0F - recover));
                float rotX = lerp(lerp(0.0F, -140.0F, up), 70.0F, down);
                rotX = lerp(rotX, 0.0F, recover);
                poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(rotX)));
            } else {
                // PIERCING FANG: blade spears deep into the screen
                float lunge = (float) Math.pow(phase(t, 0.15F, 0.42F), 0.5F);
                float back = phase(t, 0.55F, 1.0F);
                float z = lerp(-1.25F * lunge, 0.0F, back);
                poseStack.translate(0.0F, -0.08F * lunge, z);
                poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(lerp(85.0F * lunge, 0.0F, back))));
                poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(lerp(20.0F * lunge, 0.0F, back))));
            }
            // spark particles streaming off the blade while it moves
            if (strike > 0.0F && strike < 1.0F && player.level() != null) {
                spawnBladeSparks(player);
            }
        }
    }

    // spark trail at the blade's reach while the sword swings
    private static void spawnBladeSparks(LocalPlayer player) {
        net.minecraft.world.phys.Vec3 eye = player.getEyePosition();
        net.minecraft.world.phys.Vec3 view = player.getLookAngle();
        net.minecraft.world.phys.Vec3 pos = eye.add(view.scale(1.4));
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 3; i++) {
            player.level().addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    pos.x + (random.nextDouble() - 0.5) * 0.4,
                    pos.y - 0.2 + (random.nextDouble() - 0.5) * 0.4,
                    pos.z + (random.nextDouble() - 0.5) * 0.4,
                    0.0, 0.0, 0.0);
        }
    }

    // real-life archery: the draw pulls the bow to the screen center, aiming down the middle
    private static void applyBowDraw(PoseStack poseStack, float partial) {
        float draw = Math.min(1.0F, (ClientStanceCombat.drawTicks() + partial) / 18.0F);
        float ease = draw * draw * (3.0F - 2.0F * draw);
        poseStack.translate(-0.22F * ease, 0.16F * ease, 0.08F * ease);
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(6.0F * ease)));
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(-3.0F * ease)));
    }

    private static float phase(float t, float start, float end) {
        return Math.max(0.0F, Math.min(1.0F, (t - start) / (end - start)));
    }

    private static float lerp(float a, float b, float k) {
        return a + (b - a) * k;
    }

    private static void applyBow(PoseStack poseStack, LocalPlayer player, float partial, float time) {
        applyIdle(poseStack, player, partial, time);
        float since = ClientStanceCombat.ticksSinceSlash() - partial;
        if (since >= 0.0F && since < 8.0F) {
            float t = since / 8.0F;
            float flick = Mth.sin(t * (float) Math.PI);
            poseStack.translate(0.0F, -0.06F * flick, 0.1F * flick);
        }
    }

    private static void applyIdle(PoseStack poseStack, LocalPlayer player, float partial, float time) {
        float bob = Mth.sin(time * 0.09F) * 0.006F;
        float swayX = Mth.cos(time * 0.07F) * 0.005F;
        poseStack.translate(swayX, bob, 0.0F);
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(Mth.sin(time * 0.06F) * 1.5F)));
        float walkPos = player.walkAnimation.position(partial);
        float walkSpeed = player.walkAnimation.speed(partial);
        poseStack.translate(Mth.sin(walkPos) * 0.03F * walkSpeed,
                -Math.abs(Mth.cos(walkPos)) * 0.02F * walkSpeed, 0.0F);
    }

    private static void texturedQuad(VertexConsumer buffer, Matrix4f m, float s,
                                     float r, float g, float b, float alpha) {
        int overlay = OverlayTexture.NO_OVERLAY;
        buffer.addVertex(m, -s, s, 0.0F).setColor(r, g, b, alpha).setUv(0.0F, 1.0F)
                .setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
        buffer.addVertex(m, -s, -s, 0.0F).setColor(r, g, b, alpha).setUv(0.0F, 0.0F)
                .setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
        buffer.addVertex(m, s, -s, 0.0F).setColor(r, g, b, alpha).setUv(1.0F, 0.0F)
                .setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
        buffer.addVertex(m, s, s, 0.0F).setColor(r, g, b, alpha).setUv(1.0F, 1.0F)
                .setOverlay(overlay).setLight(FULL_BRIGHT).setNormal(0.0F, 0.0F, 1.0F);
    }

    private static void quad(VertexConsumer buffer, Matrix4f m,
                             float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                             float r, float g, float b, float a) {
        buffer.addVertex(m, x0, y0, 0.0F).setColor(r, g, b, a);
        buffer.addVertex(m, x1, y1, 0.0F).setColor(r, g, b, a);
        buffer.addVertex(m, x2, y2, 0.0F).setColor(r, g, b, a);
        buffer.addVertex(m, x3, y3, 0.0F).setColor(r, g, b, a);
    }
}
