package com.lightningfruit;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

// while V is held: the thunderball inflates ABOVE THE HEAD (blox fruits style),
// growing with the charge level
@EventBusSubscriber(modid = LightningFruitMod.MOD_ID, value = Dist.CLIENT)
public class HeadChargeRenderer {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "textures/entity/thunderball.png");
    private static ModelPart modelPart;

    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !ClientSkillInput.isChargingV()) {
            return;
        }
        int charge = SkillExecutor.chargeLevel(ClientSkillInput.vHeldTicks());
        if (modelPart == null) {
            modelPart = mc.getEntityModels().bakeLayer(ThunderballModel.LAYER_LOCATION);
        }
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        float age = player.tickCount + partial;
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 pos = player.getPosition(partial)
                .add(0.0, player.getBbHeight() + 3.5, 0.0)
                .subtract(cam);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(pos.x, pos.y, pos.z);
        // grows with charge: 1.0 / 1.5 / 2.0 / 2.5 — readable, not screen-filling
        float scale = 0.5F + 0.5F * charge;
        float pulse = 1.0F + 0.08F * Mth.sin(age * 0.3F);
        poseStack.scale(scale * pulse, scale * pulse, scale * pulse);
        poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(age * 2.0F)));
        poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(age * 0.7F)));
        poseStack.translate(-0.5, -0.5, -0.5); // model is 0..1 block, center it

        // soft translucent glow (NOT energySwirl — that washes out the whole screen)
        VertexConsumer outer = mc.renderBuffers().bufferSource()
                .getBuffer(RenderType.entityTranslucent(TEXTURE));
        modelPart.render(poseStack, outer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        // white-hot core
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        VertexConsumer core = mc.renderBuffers().bufferSource()
                .getBuffer(RenderType.eyes(TEXTURE));
        modelPart.render(poseStack, core, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        poseStack.popPose();
    }
}
