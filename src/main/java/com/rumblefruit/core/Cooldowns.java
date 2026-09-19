package com.rumblefruit.core;

// HUD cooldown sweep math: 0 = ready, 1 = just used
public final class Cooldowns {
    private Cooldowns() {
    }

    public static float fraction(long lastUseTick, long now, long cooldownTicks) {
        float elapsed = now - lastUseTick;
        return AnimCurves.clamp(1.0F - elapsed / (float) cooldownTicks, 0.0F, 1.0F);
    }

    // whole seconds left, for chat messages ("cooldown 3s")
    public static long secondsLeft(long lastUseTick, long now, long cooldownTicks) {
        long left = cooldownTicks - (now - lastUseTick);
        return (left + 19) / 20;
    }
}
