package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

// vacuum fake: shadows the real ElectroBolts in tests (the real one would spawn
// entity classes that need a live game). records every bolt with its position
public final class ElectroBolts {
    public static final List<String> STRIKES = new ArrayList<>();

    private ElectroBolts() {
    }

    private static String at(String kind, double x, double y, double z) {
        return kind + "@" + x + "," + y + "," + z;
    }

    public static void strike(ServerLevel level, double x, double y, double z, ServerPlayer cause) {
        STRIKES.add(at("strike", x, y, z));
    }

    public static void visual(ServerLevel level, double x, double y, double z, ServerPlayer cause) {
        STRIKES.add(at("visual", x, y, z));
    }

    public static void visualHoly(ServerLevel level, double x, double y, double z) {
        STRIKES.add(at("holy", x, y, z));
    }

    public static void clear() {
        STRIKES.clear();
    }
}
