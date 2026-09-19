package com.lightningfruit;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// client-side cache of orb counts (fed by OrbSyncPacket)
public class ClientOrbData {
    private static final Map<UUID, Integer> ORBS = new HashMap<>();

    public static void set(UUID playerId, int orbs) {
        ORBS.put(playerId, orbs);
    }

    public static int getOrbs(UUID playerId) {
        return ORBS.getOrDefault(playerId, OrbManager.MAX_ORBS);
    }
}
