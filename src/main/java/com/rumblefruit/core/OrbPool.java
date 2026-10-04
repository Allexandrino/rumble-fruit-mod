package com.rumblefruit.core;

// orb charge pool (blox fruits): N orbs, consume for skills, one recharges
// every `rechargeTicks` while not full. pure state machine, no minecraft.
public class OrbPool {
    public static final int MAX_ORBS = 4;
    public static final long RECHARGE_TICKS = 100; // 5 seconds

    private final int maxOrbs;
    private final long rechargeTicks;
    private int orbs;
    private long nextRechargeTick = -1;

    public OrbPool() {
        this(MAX_ORBS, RECHARGE_TICKS);
    }

    public OrbPool(int maxOrbs, long rechargeTicks) {
        this.maxOrbs = maxOrbs;
        this.rechargeTicks = rechargeTicks;
        this.orbs = maxOrbs;
    }

    public int getOrbs() {
        return orbs;
    }

    // consume up to `amount`; returns how many were actually taken
    public int consume(int amount, long now) {
        int taken = Math.min(amount, orbs);
        orbs -= taken;
        if (orbs < maxOrbs && nextRechargeTick < 0) {
            nextRechargeTick = now + rechargeTicks;
        }
        return taken;
    }

    // advance time; returns true if an orb recharged this tick
    public boolean tick(long now) {
        if (nextRechargeTick < 0 || now < nextRechargeTick) {
            return false;
        }
        orbs = Math.min(maxOrbs, orbs + 1);
        nextRechargeTick = orbs < maxOrbs ? now + rechargeTicks : -1;
        return true;
    }
}
