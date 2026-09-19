package com.rumblefruit;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// blox-fruits orb charge: 4 electric orbs behind the back, Z/V/F consume them, 5s recharge
@EventBusSubscriber(modid = RumbleFruitMod.MOD_ID)
public class OrbManager {

    private OrbManager() {
    }
    public static final int MAX_ORBS = 4;

    private static final Map<UUID, com.rumblefruit.core.OrbPool> POOLS;

    static {
        POOLS = new HashMap<>();
    }

    private static com.rumblefruit.core.OrbPool pool(UUID id) {
        return POOLS.computeIfAbsent(id, key -> new com.rumblefruit.core.OrbPool());
    }

    public static int getOrbs(UUID id) {
        return pool(id).getOrbs();
    }

    // consume up to `amount` orbs; returns how many were actually consumed
    public static int consume(ServerPlayer player, int amount) {
        int taken = pool(player.getUUID()).consume(amount, player.level().getGameTime());
        sync(player);
        return taken;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        com.rumblefruit.core.OrbPool p = POOLS.get(player.getUUID());
        if (p != null && p.tick(player.level().getGameTime())) {
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        POOLS.remove(event.getEntity().getUUID());
    }

    public static void sync(ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new OrbSyncPacket(player.getUUID(), pool(player.getUUID()).getOrbs()));
    }
}
