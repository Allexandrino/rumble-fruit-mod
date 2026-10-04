package com.rumblefruit.core;

// pure math helpers shared by every animation driver — no minecraft types,
// fully unit-testable in a vacuum
public final class AnimCurves {
    private AnimCurves() {
    }

    // 0..1 progress of t inside [start, end], clamped
    public static float phase(float t, float start, float end) {
        return clamp((t - start) / (end - start), 0.0F, 1.0F);
    }

    // smoothstep: slow in, slow out
    public static float easeInOut(float k) {
        return k * k * (3.0F - 2.0F * k);
    }

    public static float lerp(float a, float b, float k) {
        return a + (b - a) * k;
    }

    public static double lerp(double a, double b, double k) {
        return a + (b - a) * k;
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
