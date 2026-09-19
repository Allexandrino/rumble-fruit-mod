package com.lightningfruit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// client-side mirror of the transformation charge (filled by ChargePacket)
public class ClientChargeData {
    private static final Map<UUID, Float> CHARGE = new ConcurrentHashMap<>();

    public static void set(UUID playerId, float charge) {
        CHARGE.put(playerId, charge);
    }

    public static float get(UUID playerId) {
        return CHARGE.getOrDefault(playerId, 0.0F);
    }
}
