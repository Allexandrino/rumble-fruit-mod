package com.rumblefruit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// client-side mirror of sword combo slashes (for the third-person combo animations)
public class ClientCombatAnim {
    private static final Map<UUID, Entry> SLASHES = new ConcurrentHashMap<>();

    private record Entry(int combo, long tick) {
    }

    public static void slash(UUID playerId, int combo) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        SLASHES.put(playerId, new Entry(combo, mc.level != null ? mc.level.getGameTime() : 0L));
    }

    // combo index 0..2, 9 = release ultimate pose; or -1 if the player is not animating
    public static int comboOf(UUID playerId) {
        Entry entry = SLASHES.get(playerId);
        if (entry == null) {
            return -1;
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        return now - entry.tick < window(entry.combo) ? entry.combo : -1;
    }

    // 0..1 progress through the animation (1 when done)
    public static float progressOf(UUID playerId) {
        Entry entry = SLASHES.get(playerId);
        if (entry == null) {
            return 1.0F;
        }
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        return Math.min(1.0F, (now - entry.tick) / (float) window(entry.combo));
    }

    // the release pose rides out the whole ascension + blast; punches are snappier
    private static int window(int combo) {
        if (combo == 9) {
            return 75;
        }
        if (combo == 20) {
            return 400; // slow-mo fall: holds until the touchdown packet (21) arrives
        }
        if (combo == 21) {
            return 85; // landed: sprawled in the crater, then getting back up
        }
        return combo >= 10 && combo < 20 ? 18 : 20;
    }
}
