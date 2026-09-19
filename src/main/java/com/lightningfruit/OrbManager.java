package com.lightningfruit;

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
@EventBusSubscriber(modid = LightningFruitMod.MOD_ID)
public class OrbManager {
    public static final int MAX_ORBS = 4;
    private static final long RECHARGE_TICKS = 100; // 5 seconds

    private static class OrbData {
        int orbs = MAX_ORBS;
        long nextRechargeTick = -1;
    }

    private static final Map<UUID, OrbData> DATA = new HashMap<>();

    private static OrbData get(UUID id) {
        return DATA.computeIfAbsent(id, key -> new OrbData());
    }

    public static int getOrbs(UUID id) {
        return get(id).orbs;
    }

    // consume up to `amount` orbs; returns how many were actually consumed
    public static int consume(ServerPlayer player, int amount) {
        OrbData d = get(player.getUUID());
        int taken = Math.min(amount, d.orbs);
        d.orbs -= taken;
        if (d.orbs < MAX_ORBS && d.nextRechargeTick < 0) {
            d.nextRechargeTick = player.level().getGameTime() + RECHARGE_TICKS;
        }
        sync(player);
        return taken;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        OrbData d = DATA.get(player.getUUID());
        if (d != null && d.nextRechargeTick >= 0 && player.level().getGameTime() >= d.nextRechargeTick) {
            d.orbs = Math.min(MAX_ORBS, d.orbs + 1);
            d.nextRechargeTick = d.orbs < MAX_ORBS ? player.level().getGameTime() + RECHARGE_TICKS : -1;
            sync(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DATA.remove(event.getEntity().getUUID());
    }

    public static void sync(ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToAllPlayers(
                new OrbSyncPacket(player.getUUID(), get(player.getUUID()).orbs));
    }
}
