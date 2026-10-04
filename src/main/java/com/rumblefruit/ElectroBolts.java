package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;

// custom electro bolt: jagged glowing arc (ElectroBoltEntity), never sets fire,
// strike damage is magic ("killed by magic")
public final class ElectroBolts {

    private ElectroBolts() {
    }

    public static void strike(ServerLevel level, double x, double y, double z, @Nullable ServerPlayer cause) {
        ElectroBoltEntity.strike(level, x, y, z, cause);
    }

    // pure decoration: flash + thunder, no damage at all
    public static void visual(ServerLevel level, double x, double y, double z, @Nullable ServerPlayer cause) {
        ElectroBoltEntity.visual(level, x, y, z);
    }

    // golden angel variant
    public static void visualHoly(ServerLevel level, double x, double y, double z) {
        ElectroBoltEntity.visual(level, x, y, z, true);
    }
}
