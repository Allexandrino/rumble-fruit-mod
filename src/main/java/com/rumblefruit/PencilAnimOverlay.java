package com.rumblefruit;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

// the pencil cast flipbook: on every skill cast a hand-drawn animation plays —
// a sketchy figure winds up and fires, paper-white and ink-black, with ONLY
// the energy tinted in the fruit's color. no shaders, no camera tricks:
// just drawings, one after another
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT)
public class PencilAnimOverlay {
    private static final int FRAMES = 5;
    private static final long FRAME_MILLIS = 90;

    private static final ResourceLocation[] CAST = new ResourceLocation[FRAMES];
    private static final ResourceLocation[] GLOW = new ResourceLocation[FRAMES];
    static {
        for (int i = 0; i < FRAMES; i++) {
            CAST[i] = ResourceLocation.fromNamespaceAndPath(
                    RumbleFruitMod.MOD_ID, "textures/pencil/cast_" + i + ".png");
            GLOW[i] = ResourceLocation.fromNamespaceAndPath(
                    RumbleFruitMod.MOD_ID, "textures/pencil/glow_" + i + ".png");
        }
    }

    private static long start = -1;

    public static void play() {
        start = System.currentTimeMillis();
    }

    private static boolean playing() {
        return start >= 0 && System.currentTimeMillis() - start < FRAME_MILLIS * FRAMES;
    }

    @SubscribeEvent
    public static void onRenderGui(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        if (!playing()) {
            start = -1;
            return;
        }
        int frame = (int) Math.min(FRAMES - 1,
                (System.currentTimeMillis() - start) / FRAME_MILLIS);
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        var gfx = event.getGuiGraphics();
        gfx.pose().pushPose();
        // hand-held feel: the whole frame wobbles a pixel like the artist's hand
        int jx = (int) Math.round(Math.sin((System.currentTimeMillis() % 9999) * 0.045) * 1.5);
        int jy = (int) Math.round(Math.cos((System.currentTimeMillis() % 9999) * 0.038) * 1.5);
        gfx.pose().translate(jx, jy, 0.0);
        gfx.blit(CAST[frame], 0, 0, 0, 0, w, h, w, h);
        // only the energy burns in the fruit's color
        int color = com.rumblefruit.core.ElementCatalog.byId(ClientPowerData.element()).color();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, 1.0F);
        gfx.blit(GLOW[frame], 0, 0, 0, 0, w, h, w, h);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        gfx.pose().popPose();
    }
}
