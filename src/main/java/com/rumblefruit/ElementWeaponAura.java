package com.rumblefruit;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

// element charge crackling directly ON the hands: both fists smoulder in the
// fruit's color while the power is active, and the drawn sword/bow crackles
// along the blade — not around the body, right on the weapon
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID, value = Dist.CLIENT)
public class ElementWeaponAura {

    private ElementWeaponAura() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !ClientPowerData.has()) {
            return;
        }
        if (mc.player.tickCount % 2 != 0) {
            return;
        }
        var element = Element.byId(ClientPowerData.element());
        int stance = ClientStanceData.get(mc.player.getUUID());
        float yaw = mc.player.getYRot() * 0.0174533F;
        // hand offsets from the body centre (right/left)
        double rx = Math.cos(yaw);
        double rz = -Math.sin(yaw);
        double px = mc.player.getX();
        double py = mc.player.getY();
        double pz = mc.player.getZ();
        if (stance == StanceData.SWORD) {
            // crackle along the blade: from the grip out to the tip
            var look = mc.player.getLookAngle();
            for (int i = 1; i <= 3; i++) {
                double d = 0.5 + i * 0.35;
                mc.level.addParticle(element.spark(),
                        px + rx * 0.5 + look.x * d, py + 1.25 + look.y * d,
                        pz + rz * 0.5 + look.z * d, 0.0, 0.01, 0.0);
            }
        } else if (stance == StanceData.BOW) {
            // sparks on the bowstring hand
            mc.level.addParticle(element.spark(),
                    px - rx * 0.45, py + 1.3, pz - rz * 0.45, 0.0, 0.01, 0.0);
            mc.level.addParticle(element.spark(),
                    px + rx * 0.45, py + 1.25, pz + rz * 0.45, 0.0, 0.01, 0.0);
        } else {
            // both fists burn in the element's color
            mc.level.addParticle(element.spark(),
                    px + rx * 0.45, py + 1.25, pz + rz * 0.45, 0.0, 0.01, 0.0);
            mc.level.addParticle(element.spark(),
                    px - rx * 0.45, py + 1.25, pz - rz * 0.45, 0.0, 0.01, 0.0);
        }
    }
}
