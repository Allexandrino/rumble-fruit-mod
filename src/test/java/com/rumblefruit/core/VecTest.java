package com.rumblefruit.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VecTest {
    @Test
    void addSubtract() {
        Vec a = new Vec(1, 2, 3);
        assertEquals(new Vec(4, 6, 8), a.add(new Vec(3, 4, 5)));
        assertEquals(new Vec(4, 6, 8), a.add(3, 4, 5));
        assertEquals(new Vec(-2, -2, -2), a.subtract(new Vec(3, 4, 5)));
    }

    @Test
    void scaleDotLength() {
        Vec a = new Vec(3, 0, 4);
        assertEquals(5.0, a.length(), 1.0E-9);
        assertEquals(new Vec(6, 0, 8), a.scale(2.0));
        // operands deliberately avoid 0 and 1 so math mutants cannot hide
        assertEquals(61.0, new Vec(2, 3, 5).dot(new Vec(4, 6, 7)), 1.0E-9);
    }

    @Test
    void normalizeUnit() {
        Vec n = new Vec(0, 3, 4).normalize();
        assertEquals(1.0, n.length(), 1.0E-9);
        assertEquals(0.6, n.y(), 1.0E-9);
    }

    @Test
    void normalizeZeroIsSafe() {
        assertEquals(new Vec(0, 0, 0), new Vec(0, 0, 0).normalize());
    }

    @Test
    void distanceAndLerp() {
        Vec a = new Vec(0, 0, 0);
        Vec b = new Vec(10, 0, 0);
        assertEquals(10.0, a.distanceTo(b), 1.0E-9);
        // non-zero start so subtraction mutants cannot hide
        assertEquals(new Vec(6, 0, 0), new Vec(2, 0, 0).lerp(b, 0.5));
        // all three components non-trivial so per-axis mutants cannot hide
        assertEquals(new Vec(4.0, 5.25, 6.5), new Vec(2, 3, 4).lerp(new Vec(10, 12, 14), 0.25));
    }
}
