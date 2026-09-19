package com.lightningfruit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// client-side mirror of the wings state (filled by WingsSyncPacket).
// also remembers when the transformation started (for the unfold animation)
public class ClientWingsData {
    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> SINCE = new ConcurrentHashMap<>();

    public static void set(UUID playerId, boolean active) {
        if (active) {
            ACTIVE.put(playerId, true);
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            SINCE.put(playerId, mc.level != null ? mc.level.getGameTime() : 0L);
        } else {
            ACTIVE.remove(playerId);
            SINCE.remove(playerId);
        }
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.getOrDefault(playerId, false);
    }

    // 0..1 progress of the unfold animation (~16 ticks)
    public static float unfoldProgress(UUID playerId, float ageInTicks) {
        Long since = SINCE.get(playerId);
        if (since == null) {
            return 1.0F;
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        float t = (now + (ageInTicks % 1.0F) - since) / 16.0F;
        return Math.max(0.0F, Math.min(1.0F, t));
    }
}
