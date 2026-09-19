package com.lightningfruit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// client-side mirror of who is channeling the LMB force-lightning
public class ClientChannelData {
    private static final Map<UUID, Boolean> ACTIVE = new ConcurrentHashMap<>();

    public static void set(UUID playerId, boolean active) {
        if (active) {
            ACTIVE.put(playerId, true);
        } else {
            ACTIVE.remove(playerId);
        }
    }

    public static boolean isActive(UUID playerId) {
        return ACTIVE.getOrDefault(playerId, false);
    }
}
