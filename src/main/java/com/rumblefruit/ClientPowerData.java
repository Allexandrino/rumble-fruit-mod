package com.rumblefruit;

// client-side mirror of "has the local player eaten a fruit, and which
// element it is". the server is the source of truth (player persistent
// data); it pushes this on login, respawn and when a fruit is eaten.
// NEVER read player NBT on the client — it is not synced and leaks
// between sessions.
public class ClientPowerData {
    private static boolean hasPower = false;
    private static int element = 0;

    public static void set(boolean value, int elementId) {
        hasPower = value;
        element = elementId;
    }

    public static boolean has() {
        return hasPower;
    }

    public static int element() {
        return element;
    }
}
