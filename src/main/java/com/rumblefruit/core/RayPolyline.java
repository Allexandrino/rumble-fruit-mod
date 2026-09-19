package com.rumblefruit.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// jagged lightning polyline generator (the "ray" from the R ultimate):
// a crooked walk from `from`, forking once mid-way — pure geometry, no minecraft
public final class RayPolyline {
    public static final int SEGMENTS = 14;
    public static final double JITTER = 0.55;
    public static final double FORK_LENGTH = 0.4;

    private RayPolyline() {
    }

    // all points of the main ray plus (maybe) one fork; deterministic per seed
    public static List<Vec> generate(Vec from, double yaw, double pitch, double length, Random random) {
        List<Vec> points = new ArrayList<>();
        walk(from, yaw, pitch, length, random, points);
        return points;
    }

    private static void walk(Vec from, double yaw, double pitch, double length,
                             Random random, List<Vec> out) {
        double dx = Math.cos(yaw) * Math.cos(pitch);
        double dy = Math.sin(pitch);
        double dz = Math.sin(yaw) * Math.cos(pitch);
        Vec pos = from;
        double segLen = length / SEGMENTS;
        for (int i = 0; i < SEGMENTS; i++) {
            dx += (random.nextDouble() - 0.5) * JITTER;
            dy += (random.nextDouble() - 0.5) * JITTER;
            dz += (random.nextDouble() - 0.5) * JITTER;
            double norm = Math.sqrt(dx * dx + dy * dy + dz * dz);
            pos = pos.add(dx / norm * segLen, dy / norm * segLen, dz / norm * segLen);
            out.add(pos);
            // one fork branching off mid-ray
            if (i == SEGMENTS / 2 && random.nextBoolean()) {
                walk(pos, yaw + (random.nextDouble() - 0.5) * 1.5,
                        pitch + (random.nextDouble() - 0.5) * 0.8, length * FORK_LENGTH, random, out);
            }
        }
    }
}
