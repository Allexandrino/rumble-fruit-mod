package com.rumblefruit.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimCurvesTest {
    @Test
    void phaseClampsBelow() {
        assertEquals(0.0F, AnimCurves.phase(-1.0F, 0.2F, 0.8F));
        assertEquals(0.0F, AnimCurves.phase(0.2F, 0.2F, 0.8F));
    }

    @Test
    void phaseClampsAbove() {
        assertEquals(1.0F, AnimCurves.phase(0.8F, 0.2F, 0.8F));
        assertEquals(1.0F, AnimCurves.phase(99.0F, 0.2F, 0.8F));
    }

    @Test
    void phaseIsLinearInside() {
        assertEquals(0.5F, AnimCurves.phase(0.5F, 0.2F, 0.8F), 1.0E-6F);
        assertEquals(0.25F, AnimCurves.phase(0.35F, 0.2F, 0.8F), 1.0E-6F);
    }

    @Test
    void easeInOutEndpoints() {
        assertEquals(0.0F, AnimCurves.easeInOut(0.0F));
        assertEquals(1.0F, AnimCurves.easeInOut(1.0F));
        assertEquals(0.5F, AnimCurves.easeInOut(0.5F), 1.0E-6F);
    }

    @Test
    void easeInOutIsMonotonic() {
        float prev = -1.0F;
        for (int i = 0; i <= 20; i++) {
            float v = AnimCurves.easeInOut(i / 20.0F);
            assertTrue(v >= prev, "must be monotonic");
            prev = v;
        }
    }

    @Test
    void lerpWorks() {
        assertEquals(6.0F, AnimCurves.lerp(2.0F, 10.0F, 0.5F), 1.0E-6F);
        assertEquals(2.0F, AnimCurves.lerp(2.0F, 10.0F, 0.0F), 1.0E-6F);
        assertEquals(10.0F, AnimCurves.lerp(2.0F, 10.0F, 1.0F), 1.0E-6F);
        assertEquals(6.0, AnimCurves.lerp(2.0, 10.0, 0.5), 1.0E-9);
    }

    @Test
    void clampWorks() {
        assertEquals(2.0F, AnimCurves.clamp(5.0F, -2.0F, 2.0F));
        assertEquals(-2.0F, AnimCurves.clamp(-5.0F, -2.0F, 2.0F));
        assertEquals(1.0F, AnimCurves.clamp(1.0F, -2.0F, 2.0F));
    }
}
