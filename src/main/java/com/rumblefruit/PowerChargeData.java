package com.rumblefruit;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// the transformation power bar: fills when the player deals damage (works even while
// transformed), drains while the angel form is active. F transforms only with enough charge.
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class PowerChargeData {
    public static final float MAX = 100.0F;
    public static final float TRANSFORM_COST = 30.0F;
    private static final float DRAIN_PER_TICK = 0.06F; // ~55 seconds of transformation at full bar

    private static final Map<UUID, Float> CHARGE = new ConcurrentHashMap<>();

    public static float get(UUID playerId) {
        return CHARGE.getOrDefault(playerId, 0.0F);
    }

    public static void add(ServerPlayer player, float amount) {
        CHARGE.put(player.getUUID(), Math.min(MAX, get(player.getUUID()) + amount));
        sync(player);
    }

    public static void drain(ServerPlayer player, float amount) {
        CHARGE.put(player.getUUID(), Math.max(0.0F, get(player.getUUID()) - amount));
    }

    public static boolean canTransform(ServerPlayer player) {
        return get(player.getUUID()) >= TRANSFORM_COST;
    }

    public static void sync(ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new ChargePacket(player.getUUID(), get(player.getUUID())));
    }

    // damage dealt by the player fills the bar (25% of the damage amount)
    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player
                && RumblePowerData.hasPower(player) && event.getNewDamage() > 0.0F) {
            add(player, event.getNewDamage() * 0.25F);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sync(player);
        }
    }

    // called from WingsData's player tick: drains the bar while transformed
    public static void tickDrain(ServerPlayer player) {
        drain(player, DRAIN_PER_TICK);
        if (player.level().getGameTime() % 20 == 0) {
            sync(player);
        }
    }
}
