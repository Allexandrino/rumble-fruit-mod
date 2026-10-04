package com.rumblefruit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

// the J landing from the first person: the blast whites out the world, the
// fall happens with eyes shut (black), and at the bottom of the crater the
// eyes OPEN — two dark lids slide apart revealing the pit
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class WakeUpOverlay {

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "wakeup"),
                (graphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options.hideGui) {
                return;
            }
            if (mc.options.getCameraType() != net.minecraft.client.CameraType.FIRST_PERSON) {
                return;
            }
            int combo = ClientCombatAnim.comboOf(mc.player.getUUID());
            if (combo != 20 && combo != 21) {
                return;
            }
            float t = ClientCombatAnim.progressOf(mc.player.getUUID());
            int w = graphics.guiWidth();
            int h = graphics.guiHeight();
            if (combo == 20) {
                // the slow-mo plunge: the world drains to black as the eyes close
                float darkness = Math.min(1.0F, t * 4.0F) * 0.92F;
                int alpha = (int) (darkness * 255.0F) << 24;
                graphics.fill(0, 0, w, h, alpha);
                return;
            }
            // touchdown: a white flash, then the lids slide open over the crater floor
            if (t < 0.10F) {
                int alpha = (int) ((1.0F - t / 0.10F) * 255.0F) << 24;
                graphics.fill(0, 0, w, h, alpha | 0x00FFFFFF);
            }
            float open = ease(phase(t, 0.10F, 0.65F)); // lids peel apart
            int lid = Math.round((h / 2.0F) * (1.0F - open));
            if (lid > 0) {
                graphics.fill(0, 0, w, lid, 0xF0000000);
                graphics.fill(0, h - lid, w, h, 0xF0000000);
            }
        });
    }

    private static float phase(float t, float from, float to) {
        return Math.max(0.0F, Math.min(1.0F, (t - from) / (to - from)));
    }

    private static float ease(float x) {
        return x < 0.5F ? 2.0F * x * x : 1.0F - (float) Math.pow(-2.0F * x + 2.0F, 2.0F) / 2.0F;
    }
}
