package com.rumblefruit;

// client-side cache of the meteor countdown (fed by MeteorCountdownPacket)
public final class ClientMeteorData {
    private static volatile long ticksLeft = -1;

    private ClientMeteorData() {
    }

    public static void setTicksLeft(long ticks) {
        ticksLeft = ticks;
    }

    public static long getTicksLeft() {
        return ticksLeft;
    }

    // "MM:SS" for the HUD
    public static String format() {
        long totalSeconds = Math.max(0L, ticksLeft) / 20L;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
