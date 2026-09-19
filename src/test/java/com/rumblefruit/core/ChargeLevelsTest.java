package com.rumblefruit.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChargeLevelsTest {
    @Test
    void tapIsLevelOne() {
        assertEquals(1, ChargeLevels.level(0));
        assertEquals(1, ChargeLevels.level(4));
    }

    @Test
    void shortHoldIsLevelTwo() {
        assertEquals(2, ChargeLevels.level(5));
        assertEquals(2, ChargeLevels.level(14));
    }

    @Test
    void mediumHoldIsLevelThree() {
        assertEquals(3, ChargeLevels.level(15));
        assertEquals(3, ChargeLevels.level(29));
    }

    @Test
    void longHoldIsLevelFour() {
        assertEquals(4, ChargeLevels.level(30));
        assertEquals(4, ChargeLevels.level(999));
    }
}
