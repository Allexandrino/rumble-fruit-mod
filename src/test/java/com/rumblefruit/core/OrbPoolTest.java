package com.rumblefruit.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrbPoolTest {
    @Test
    void startsFull() {
        assertEquals(OrbPool.MAX_ORBS, new OrbPool().getOrbs());
    }

    @Test
    void consumeTakesWhatIsThere() {
        OrbPool pool = new OrbPool();
        assertEquals(2, pool.consume(2, 0));
        assertEquals(2, pool.getOrbs());
    }

    @Test
    void consumeCapsAtAvailable() {
        OrbPool pool = new OrbPool();
        assertEquals(4, pool.consume(99, 0));
        assertEquals(0, pool.getOrbs());
        assertEquals(0, pool.consume(1, 0)); // nothing left
    }

    @Test
    void noRechargeBeforeTime() {
        OrbPool pool = new OrbPool();
        pool.consume(1, 0);
        assertFalse(pool.tick(OrbPool.RECHARGE_TICKS - 1));
        assertEquals(3, pool.getOrbs());
    }

    @Test
    void rechargesAfterDelay() {
        OrbPool pool = new OrbPool();
        pool.consume(1, 0);
        assertTrue(pool.tick(OrbPool.RECHARGE_TICKS));
        assertEquals(4, pool.getOrbs());
    }

    @Test
    void rechargeChainUntilFull() {
        OrbPool pool = new OrbPool();
        pool.consume(3, 0);
        assertTrue(pool.tick(100));
        assertEquals(2, pool.getOrbs());
        assertTrue(pool.tick(200));
        assertTrue(pool.tick(300));
        assertEquals(4, pool.getOrbs());
        assertFalse(pool.tick(400)); // full: nothing scheduled
    }

    @Test
    void fullPoolSchedulesNothing() {
        OrbPool pool = new OrbPool();
        assertFalse(pool.tick(1000));
    }

    @Test
    void emptyConsumeOnFullPoolSchedulesNothing() {
        OrbPool pool = new OrbPool();
        pool.consume(0, 0); // nothing taken, pool still full
        assertFalse(pool.tick(OrbPool.RECHARGE_TICKS));
    }

    @Test
    void rechargeSchedulesFromNow() {
        OrbPool pool = new OrbPool();
        pool.consume(3, 0);
        assertTrue(pool.tick(100));
        // next recharge is 100 ticks after THIS one, not immediately
        assertFalse(pool.tick(150));
        assertTrue(pool.tick(200));
    }

    @Test
    void rechargeAtTickZeroWorks() {
        // schedule so that the recharge lands exactly on tick 0
        OrbPool pool = new OrbPool();
        pool.consume(1, -OrbPool.RECHARGE_TICKS);
        assertTrue(pool.tick(0));
        assertEquals(OrbPool.MAX_ORBS, pool.getOrbs());
    }

    @Test
    void consumingTwiceNeverReschedules() {
        // given a recharge scheduled for tick 0
        OrbPool pool = new OrbPool();
        pool.consume(1, -OrbPool.RECHARGE_TICKS);
        // when another orb is consumed while the schedule is pending
        pool.consume(1, -50);
        // then the existing schedule is kept — tick 0 recharges as planned
        assertTrue(pool.tick(0));
    }
}
