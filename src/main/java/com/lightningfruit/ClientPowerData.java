package com.lightningfruit;

// client-side mirror of "has the local player eaten the lightning fruit".
// the server is the source of truth (player persistent data); it pushes this
// on login, respawn and when the fruit is eaten. NEVER read player NBT on the
// client — it is not synced and leaks between sessions.
public class ClientPowerData {
    private static boolean hasPower = false;

    public static void set(boolean value) {
        hasPower = value;
    }

    public static boolean has() {
        return hasPower;
    }
}
