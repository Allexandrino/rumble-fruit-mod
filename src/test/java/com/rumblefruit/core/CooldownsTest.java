package com.rumblefruit.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CooldownsTest {
    @Test
    void justUsedIsFull() {
        assertEquals(1.0F, Cooldowns.fraction(100, 100, 60), 1.0E-6F);
    }

    @Test
    void halfwayIsHalf() {
        assertEquals(0.5F, Cooldowns.fraction(100, 130, 60), 1.0E-6F);
    }

    @Test
    void finishedIsZero() {
        assertEquals(0.0F, Cooldowns.fraction(100, 160, 60));
        assertEquals(0.0F, Cooldowns.fraction(100, 9999, 60));
    }

    @Test
    void secondsLeftRoundsUp() {
        assertEquals(3, Cooldowns.secondsLeft(0, 0, 60));
        assertEquals(1, Cooldowns.secondsLeft(0, 59, 60));
        assertEquals(0, Cooldowns.secondsLeft(0, 60, 60));
        // non-zero lastUseTick so the subtraction mutants cannot hide
        assertEquals(1, Cooldowns.secondsLeft(100, 159, 60));
        assertEquals(2, Cooldowns.secondsLeft(100, 130, 60));
    }
}
