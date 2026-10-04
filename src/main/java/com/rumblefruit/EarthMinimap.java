package com.rumblefruit;

import com.mojang.blaze3d.platform.NativeImage;
import com.rumblefruit.earth.EarthBorderTracker;
import com.rumblefruit.earth.EarthData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// minimap of the ancient world, top-left corner, toggled with M.
// the picture is re-rendered from the real heightmap + city layer
// whenever the player moves 32+ blocks
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT)
public class EarthMinimap {

    private static final int SIZE = 144;          // px
    private static final int WINDOW = 6000;       // половина окна в метрах (12 км вид)
    private static final ResourceLocation TEX_ID =
            ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "earth_minimap");

    private static boolean visible = true;
    private static DynamicTexture texture;
    private static int lastCX = Integer.MIN_VALUE, lastCZ = Integer.MIN_VALUE;
    private static long lastRefresh = 0;

    @EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {
        @SubscribeEvent
        public static void registerOverlays(RegisterGuiLayersEvent event) {
            event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(
                            RumbleFruitMod.MOD_ID, "earth_minimap"),
                    (graphics, deltaTracker) -> render(graphics));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (ModKeyBindings.MAP.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null
                    && !mc.player.level().dimension().equals(EarthBorderTracker.EARTH)) {
                mc.player.displayClientMessage(Component.literal(
                        "Карта работает в древнем мире — телепортируйся: /rumblefruit earth"), true);
                continue;
            }
            visible = !visible;
            lastCX = Integer.MIN_VALUE; // форс-обновление при включении
        }
    }

    private static void render(net.minecraft.client.gui.GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible || mc.player == null || mc.level == null || mc.options.hideGui) return;
        if (!mc.player.level().dimension().equals(EarthBorderTracker.EARTH)) return;

        int px = (int) mc.player.getX(), pz = (int) mc.player.getZ();
        if (texture == null) {
            texture = new DynamicTexture(SIZE, SIZE, true);
            mc.getTextureManager().register(TEX_ID, texture);
        }
        long now = System.currentTimeMillis();
        if (Math.abs(px - lastCX) > 96 || Math.abs(pz - lastCZ) > 96
                || now - lastRefresh > 8000) {
            redraw(px, pz);
            lastCX = px; lastCZ = pz; lastRefresh = now;
        }

        // рамка и карта
        g.fill(4, 4, 4 + SIZE + 4, 4 + SIZE + 20, 0xAA1A1208);
        g.blit(TEX_ID, 6, 6, 0, 0, SIZE, SIZE, SIZE, SIZE);

        // метка игрока: белая стрелка по курсу
        int cx = 6 + SIZE / 2, cy = 6 + SIZE / 2;
        double yaw = Math.toRadians(mc.player.getYRot());
        int ax = (int) Math.round(-Math.sin(yaw) * 5), ay = (int) Math.round(-Math.cos(yaw) * 5);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        g.fill(cx + ax - 1, cy + ay - 1, cx + ax + 1, cy + ay, 0xFFFF4040);

        // государство — внутри рамки внизу, ничего не перекрывает
        String country = EarthData.countryAt(px, pz, true);
        String label = country != null ? country : "Международные воды";
        g.drawString(mc.font, Component.literal(label), 8, 4 + SIZE + 8, 0xFFE8C96A, true);
    }

    private static void redraw(int px, int pz) {
        NativeImage img = texture.getPixels();
        if (img == null) return;
        double step = WINDOW * 2.0 / SIZE;
        for (int j = 0; j < SIZE; j++) {
            for (int i = 0; i < SIZE; i++) {
                int wx = px - WINDOW + (int) (i * step);
                int wz = pz - WINDOW + (int) (j * step);
                img.setPixelRGBA(i, j, colorFor(wx, wz));
            }
        }
        // города — золотые точки (ABGR для NativeImage)
        int gold = 0xFF000000 | (0x4A << 16) | (0xD2 << 8) | 0xFF;
        for (EarthData.Place p : EarthData.places()) {
            if (!p.city()) continue;
            int cx = EarthData.blockFromLon(p.lon()), cz = EarthData.blockFromLat(p.lat());
            int ix = (int) ((cx - (px - WINDOW)) / step);
            int iz = (int) ((cz - (pz - WINDOW)) / step);
            if (ix < 1 || ix >= SIZE - 1 || iz < 1 || iz >= SIZE - 1) continue;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    img.setPixelRGBA(ix + dx, iz + dz, gold);
                }
            }
        }
        texture.upload();
    }

    // NativeImage хранит ABGR
    private static int abgr(int r, int g, int b) {
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    private static int colorFor(int x, int z) {
        int h = EarthData.worldHeight(x, z);
        double lat = EarthData.latFromBlock(z);
        if (h < EarthData.SEA_LEVEL) {
            int depth = Math.min(40, EarthData.SEA_LEVEL - h);
            int blue = Math.max(90, 200 - depth * 3);
            return abgr(30, 60, blue);                       // море
        } else if (h >= 250) {
            return abgr(245, 245, 255);                      // снега
        } else if (h >= 190) {
            return abgr(141, 133, 121);                      // скалы
        } else if (lat < 33.5 && h < 130) {
            return abgr(220, 194, 122);                      // пустыня
        } else {
            int t = Math.min(100, h - EarthData.SEA_LEVEL);
            return abgr(96 + t, 140 - t / 3, 70 + t / 4);    // луга → холмы
        }
    }
}
