package com.lightningfruit;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

// client-side stance combat input (epic-fight style):
// sword stance: LMB clicks chain combo slashes; bow stance: hold LMB to draw, release to fire
@EventBusSubscriber(modid = LightningFruitMod.MOD_ID, value = Dist.CLIENT)
public class ClientStanceCombat {
    private static boolean prevAttack = false;
    private static int tick = 0;

    // sword combo state (read by SkillCastHandRenderer)
    public static int combo = 0;
    public static int lastSlashTick = -100;
    // fist combo state: 0 jab, 1 cross, 2 uppercut, 3 roundhouse
    private static int fistCombo = 0;

    // bow draw state (read by SkillCastHandRenderer)
    private static int drawStartTick = -1;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        tick++;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            prevAttack = false;
            drawStartTick = -1;
            return;
        }
        int stance = ClientStanceData.get(mc.player.getUUID());
        boolean ready = ClientPowerData.has() && mc.player.getMainHandItem().isEmpty();
        boolean attack = mc.options.keyAttack.isDown();

        if (ready && stance == StanceData.SWORD) {
            // combo slashes on each click
            if (attack && !prevAttack) {
                combo = (combo + 1) % 3;
                lastSlashTick = tick;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AttackPacket(0, combo));
            }
        } else if (ready && stance == StanceData.FISTS) {
            // punches and kicks on each click
            if (attack && !prevAttack) {
                fistCombo = (fistCombo + 1) % 4;
                lastSlashTick = tick;
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AttackPacket(2, fistCombo));
            }
        } else if (ready && stance == StanceData.BOW) {
            if (attack && drawStartTick < 0) {
                drawStartTick = tick;
            }
            if (!attack && drawStartTick >= 0) {
                net.neoforged.neoforge.network.PacketDistributor.sendToServer(new AttackPacket(1, tick - drawStartTick));
                lastSlashTick = tick; // release flick animation
                drawStartTick = -1;
            }
        } else {
            drawStartTick = -1;
        }
        prevAttack = attack;

        // fists stance: hands (and the kicking foot) crackle with electricity
        if (mc.level != null && tick % 3 == 0) {
            for (net.minecraft.client.player.AbstractClientPlayer p : mc.level.players()) {
                if (ClientStanceData.get(p.getUUID()) == StanceData.FISTS) {
                    spawnFistSparks(p);
                }
            }
        }
    }

    private static void spawnFistSparks(net.minecraft.client.player.AbstractClientPlayer p) {
        float yawRad = p.getYRot() * 0.0174533F;
        double fx = -Math.sin(yawRad), fz = Math.cos(yawRad);
        double rx = fz, rz = -fx; // player's right
        java.util.concurrent.ThreadLocalRandom rnd = java.util.concurrent.ThreadLocalRandom.current();
        for (int side = -1; side <= 1; side += 2) {
            p.level().addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    p.getX() + rx * 0.45 * side + fx * 0.25 + rnd.nextGaussian() * 0.08,
                    p.getY() + 1.25 + rnd.nextGaussian() * 0.08,
                    p.getZ() + rz * 0.45 * side + fz * 0.25 + rnd.nextGaussian() * 0.08,
                    0.0, 0.02, 0.0);
        }
        // during the roundhouse the FOOT discharges instead
        if (ClientCombatAnim.comboOf(p.getUUID()) == 13) {
            for (int i = 0; i < 3; i++) {
                p.level().addParticle(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                        p.getX() + fx * 0.6 + rnd.nextGaussian() * 0.12,
                        p.getY() + 0.5 + rnd.nextGaussian() * 0.12,
                        p.getZ() + fz * 0.6 + rnd.nextGaussian() * 0.12,
                        0.0, 0.03, 0.0);
            }
        }
    }

    public static boolean isDrawing() {
        return drawStartTick >= 0;
    }

    public static int drawTicks() {
        return drawStartTick >= 0 ? tick - drawStartTick : 0;
    }

    public static int ticksSinceSlash() {
        return tick - lastSlashTick;
    }
}
