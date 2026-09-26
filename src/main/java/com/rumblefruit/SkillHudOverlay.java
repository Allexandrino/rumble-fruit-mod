package com.rumblefruit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

// Blox Fruits style skill list HUD on the right side: skill icons + key letters +
// cooldown sweep + orb pips. Shown while holding the energy charge.
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class SkillHudOverlay {
    private static final ResourceLocation[] ICONS = {
            icon("z"), icon("x"), icon("c"), icon("f"), icon("v"), icon("r")
    };
    private static final String[] SKILL_KEYS = {"z", "x", "c", "f", "v", "j"};
    private static final ResourceLocation ORB = icon("orb");
    private static final ResourceLocation ORB_EMPTY = icon("orb_empty");
    private static final String[] KEYS = {"Z", "X", "C", "F", "V", "J"};
    private static final int ICON_SIZE = 18;
    private static final int SPACING = 22;

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "textures/gui/skills/" + name + ".png");
    }

    // every element has its own icon set (lightning keeps the classic bolts)
    private static ResourceLocation iconFor(com.rumblefruit.core.ElementCatalog element, int index) {
        if (element.isLightning()) {
            return ICONS[index];
        }
        return ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID,
                "textures/gui/skills/" + element.key() + "/" + SKILL_KEYS[index] + ".png");
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(RumbleFruitMod.MOD_ID, "skills"),
                (graphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            int width = graphics.guiWidth();
            int height = graphics.guiHeight();
            if (mc.player == null || mc.options.hideGui) {
                return;
            }
            // meteor countdown, top center — always visible
            if (ClientMeteorData.getTicksLeft() >= 0) {
                String meteor = "☄ Метеорит: " + ClientMeteorData.format();
                graphics.drawString(mc.font, meteor, width / 2 - mc.font.width(meteor) / 2, 6, 0x7FD4FF, true);
            }
            // skill table is always visible once the player has eaten the fruit
            if (!ClientPowerData.has()) {
                return;
            }

            int x = width - ICON_SIZE - 6;
            int y = height / 2 - (ICONS.length * SPACING) / 2;

            // current combat stance above the skill list (gold while transformed)
            int stance = ClientStanceData.get(mc.player.getUUID());
            String stanceName = switch (stance) {
                case 1 -> "КОПЬЁ";
                case 2 -> "ЛУК";
                default -> "КУЛАКИ";
            };
            boolean holy = ClientWingsData.isActive(mc.player.getUUID());
            int stanceColor = holy ? 0xFFD24A : stance == 1 ? 0x7FD4FF : stance == 2 ? 0xB0FF9E : 0xC0C0C0;
            graphics.drawString(mc.font, stanceName, x + ICON_SIZE - mc.font.width(stanceName), y - 12, stanceColor, true);

            // the fruit (or the transformation) above the stance, in the element's color
            var element = com.rumblefruit.core.ElementCatalog.byId(ClientPowerData.element());
            String elementName = net.minecraft.network.chat.Component.translatable(
                    holy ? "rumblefruit.form." + element.key() : "rumblefruit.element." + element.key()).getString();
            graphics.drawString(mc.font, elementName, x + ICON_SIZE - mc.font.width(elementName), y - 24,
                    element.color(), true);

            for (int i = 0; i < ICONS.length; i++) {
                int iconY = y + i * SPACING;
                // every element has its own icon art: flames, spirals, snowflakes, leaves
                graphics.blit(iconFor(element, i), x, iconY, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
                // cooldown sweep (dark overlay from bottom up); Z, F and R have no cooldown
                if (i != 0 && i != 3 && i != 5) {
                    float frac = ClientSkillInput.cooldownFraction(i);
                    if (frac > 0.0F) {
                        int overlayH = Math.round(ICON_SIZE * frac);
                        graphics.fill(x, iconY + ICON_SIZE - overlayH, x + ICON_SIZE, iconY + ICON_SIZE, 0xB0000000);
                    }
                }
                // key letter to the left of the icon
                graphics.drawString(mc.font, KEYS[i], x - 10, iconY + 5, 0xFFFFFF, true);
            }

            // orb pips under the skills (also tinted by the element)
            int orbs = ClientOrbData.getOrbs(mc.player.getUUID());
            int orbY = y + ICONS.length * SPACING + 2;
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(
                    ((element.color() >> 16) & 0xFF) / 255.0F,
                    ((element.color() >> 8) & 0xFF) / 255.0F,
                    (element.color() & 0xFF) / 255.0F, 1.0F);
            for (int i = 0; i < OrbManager.MAX_ORBS; i++) {
                ResourceLocation tex = i < orbs ? ORB : ORB_EMPTY;
                graphics.blit(tex, x + 1 + (i % 2) * 9, orbY + (i / 2) * 9, 0, 0, 8, 8, 8, 8);
            }
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // transformation power bar: vertical golden meter left of the skill icons
            float charge = ClientChargeData.get(mc.player.getUUID());
            float frac = charge / PowerChargeData.MAX;
            int barH = ICONS.length * SPACING;
            int barX = x - 12;
            graphics.fill(barX - 1, y - 1, barX + 5, y + barH + 1, 0x90000000); // frame
            graphics.fill(barX, y, barX + 4, y + barH, 0xB0181818); // background
            int fillH = Math.round(barH * frac);
            int barColor;
            if (frac >= 1.0F) {
                // pulse when full: ready to transform
                float pulse = 0.75F + 0.25F * (float) Math.sin(System.currentTimeMillis() * 0.006);
                barColor = ((int) (255 * pulse) << 24) | 0xFFD24A;
            } else {
                barColor = charge >= PowerChargeData.TRANSFORM_COST ? 0xFFFFD24A : 0xFF9A9A9A;
            }
            graphics.fill(barX, y + barH - fillH, barX + 4, y + barH, barColor);
        });
    }
}
