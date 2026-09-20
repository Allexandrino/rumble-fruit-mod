package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

// vacuum fake: shadows the real ElectroBolts in tests (the real one would spawn
// entity classes that need a live game). records structured bolt calls
public final class ElectroBolts {
    public static class BoltCall {
        public final String kind;
        public final double x;
        public final double y;
        public final double z;

        public BoltCall(String kind, double x, double y, double z) {
            this.kind = kind;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static final List<BoltCall> STRIKES = new ArrayList<>();

    private ElectroBolts() {
    }

    public static void strike(ServerLevel level, double x, double y, double z, ServerPlayer cause) {
        STRIKES.add(new BoltCall("strike", x, y, z));
    }

    public static void visual(ServerLevel level, double x, double y, double z, ServerPlayer cause) {
        STRIKES.add(new BoltCall("visual", x, y, z));
    }

    public static void visualHoly(ServerLevel level, double x, double y, double z) {
        STRIKES.add(new BoltCall("holy", x, y, z));
    }

    public static void clear() {
        STRIKES.clear();
    }
}
