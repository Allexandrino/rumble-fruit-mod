package com.lightningfruit;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

// replaces the vanilla health bar with lightning-themed hearts while the player
// has the Lightning fruit power
@EventBusSubscriber(modid = LightningFruitMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class LightningHeartsHud {
    private static final ResourceLocation HEART =
            ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "textures/gui/lightning_heart.png");

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(LightningFruitMod.MOD_ID, "hearts"),
                (graphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();
            if (mc.player == null || mc.options.hideGui || mc.player.isCreative() || mc.player.isSpectator()) {
                return;
            }
            if (!ClientPowerData.has()) {
                return;
            }
            int maxHearts = Math.min(10, (int) Math.ceil(mc.player.getMaxHealth() / 2.0));
            int x0 = width / 2 - 91;
            int y0 = height - 39;
            for (int i = 0; i < maxHearts; i++) {
                graphics.blit(HEART, x0 + i * 8, y0, 0, 0, 9, 9, 9, 9);
            }
        });
    }
}
