package com.lightningfruit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// client-side mirror of combat stances (filled by StanceSyncPacket)
public class ClientStanceData {
    private static final Map<UUID, Integer> STANCE = new ConcurrentHashMap<>();
    // when the local player last switched stance (for the weapon draw animation)
    public static long lastChangeTick = -100;

    public static void set(UUID playerId, int stance) {
        Integer prev = STANCE.put(playerId, stance);
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && playerId.equals(mc.player.getUUID())
                && (prev == null || prev != stance)) {
            lastChangeTick = mc.level != null ? mc.level.getGameTime() : 0L;
        }
    }

    public static int get(UUID playerId) {
        return STANCE.getOrDefault(playerId, StanceData.FISTS);
    }
}
