package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

// vacuum fake: shadows the real ElectroBolts in tests (the real one would spawn
// entity classes that need a live game). records every bolt instead
public final class ElectroBolts {
    public static final List<String> STRIKES = new ArrayList<>();

    private ElectroBolts() {
    }

    public static void strike(ServerLevel level, double x, double y, double z, ServerPlayer cause) {
        STRIKES.add("strike");
    }

    public static void visual(ServerLevel level, double x, double y, double z, ServerPlayer cause) {
        STRIKES.add("visual");
    }

    public static void visualHoly(ServerLevel level, double x, double y, double z) {
        STRIKES.add("holy");
    }

    public static void clear() {
        STRIKES.clear();
    }
}
