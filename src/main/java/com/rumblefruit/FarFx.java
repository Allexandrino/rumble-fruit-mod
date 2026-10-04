package com.rumblefruit;

import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

// far-visible effects: force-flagged particle packets reach every player up to
// 512 blocks out (vanilla limit is 32) — sky columns and flashes read as
// beacons from kilometers away
public final class FarFx {

    private FarFx() {
    }

    public static void send(ServerLevel level, SimpleParticleType type,
                            double x, double y, double z, int count,
                            double dx, double dy, double dz, double speed) {
        for (ServerPlayer viewer : level.players()) {
            level.sendParticles(viewer, type, true, x, y, z, count, dx, dy, dz, speed);
        }
    }

    // a straight pillar of light from y up to y+height
    public static void column(ServerLevel level, SimpleParticleType type,
                              double x, double y, double z,
                              double height, double step, int count, double spread) {
        for (double dy = 0.0; dy < height; dy += step) {
            send(level, type, x, y + dy, z, count, spread, 0.0, spread, 0.0);
        }
    }

    // a ring of pillars around a point
    public static void crown(ServerLevel level, SimpleParticleType type,
                             double x, double y, double z, double radius, int pillars,
                             double height, double step) {
        for (int i = 0; i < pillars; i++) {
            double angle = i * Math.PI * 2.0 / pillars;
            column(level, type, x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius,
                    height, step, 2, 0.4);
        }
    }
}
