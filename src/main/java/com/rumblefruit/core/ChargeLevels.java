package com.rumblefruit.core;

// V charge levels from hold time (blox fruits style): tap=1 ... 1.5s+=4
public final class ChargeLevels {
    public static final int MIN_TICKS_PER_LEVEL = 5;   // level 2 at 5 ticks
    public static final int MID_TICKS = 15;            // level 3
    public static final int MAX_TICKS = 30;            // level 4

    private ChargeLevels() {
    }

    public static int level(int heldTicks) {
        if (heldTicks >= MAX_TICKS) {
            return 4;
        }
        if (heldTicks >= MID_TICKS) {
            return 3;
        }
        if (heldTicks >= MIN_TICKS_PER_LEVEL) {
            return 2;
        }
        return 1;
    }
}
