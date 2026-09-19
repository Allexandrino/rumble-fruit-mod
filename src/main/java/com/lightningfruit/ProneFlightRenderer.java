package com.lightningfruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

// cult-of-azazel style flight: while the wings are out and the player is airborne,
// the whole body pitches into a prone superman pose (leaning with the gaze);
// on the ground the player just walks normally
@EventBusSubscriber(modid = LightningFruitMod.MOD_ID, value = Dist.CLIENT)
public class ProneFlightRenderer {

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        if (!(event.getEntity() instanceof AbstractClientPlayer player)) {
            return;
        }
        if (!ClientWingsData.isActive(player.getUUID())) {
            return;
        }
        if (player.onGround() || player.isInWater() || player.isPassenger()) {
            return;
        }
        float partial = event.getPartialTick();
        float pitch = player.getViewXRot(partial);
        // level flight lies almost flat; diving tilts steeper with the gaze
        float lean = Mth.clamp(78.0F + pitch * 0.35F, 35.0F, 100.0F);
        // the lean must happen in BODY space: vanilla applies its yaw rotation after
        // this event, so conjugate by that yaw — otherwise the body lies pointing
        // in one fixed world direction like a compass needle
        float yawDeg = 180.0F - Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        PoseStack poseStack = event.getPoseStack();
        poseStack.translate(0.0, 0.9, 0.0);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
        poseStack.mulPose(Axis.XP.rotationDegrees(-lean));
        poseStack.mulPose(Axis.YP.rotationDegrees(-yawDeg));
        poseStack.translate(0.0, -0.9, 0.0);
    }
}
