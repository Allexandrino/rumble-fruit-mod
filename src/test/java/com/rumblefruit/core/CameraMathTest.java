package com.rumblefruit.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraMathTest {
    @Test
    void yawCardinals() {
        assertEquals(0.0F, CameraMath.lookYaw(new Vec(0, 0, 5)), 1.0E-4F);   // south
        assertEquals(-90.0F, CameraMath.lookYaw(new Vec(5, 0, 0)), 1.0E-4F); // east
        assertEquals(90.0F, CameraMath.lookYaw(new Vec(-5, 0, 0)), 1.0E-4F); // west
        assertEquals(180.0F, Math.abs(CameraMath.lookYaw(new Vec(0, 0, -5))), 1.0E-4F); // north
    }

    @Test
    void pitchLevelAndDown() {
        assertEquals(0.0F, CameraMath.lookPitch(new Vec(3, 0, 4)), 1.0E-4F);
        assertTrue(CameraMath.lookPitch(new Vec(0, -5, 3)) > 0.0F); // target below -> look down
        assertTrue(CameraMath.lookPitch(new Vec(0, 5, 3)) < 0.0F);  // target above -> look up
        assertEquals(90.0F, CameraMath.lookPitch(new Vec(0, -5, 0)), 1.0E-4F);
    }

    @Test
    void bankRollClamps() {
        // way too fast sideways -> clamped at the limit
        assertEquals(7.0F, CameraMath.bankRoll(new Vec(-50, 0, 0), 0.0F), 1.0E-4F);
        assertEquals(-7.0F, CameraMath.bankRoll(new Vec(50, 0, 0), 0.0F), 1.0E-4F);
        // still -> no roll
        assertEquals(0.0F, CameraMath.bankRoll(new Vec(0, 0, 0), 45.0F), 1.0E-4F);
        // diagonal: both axes contribute, signs must stay coherent
        assertEquals((float) (-(0.1 * Math.cos(Math.toRadians(45)) + 0.1 * Math.sin(Math.toRadians(45))) * 14.0),
                CameraMath.bankRoll(new Vec(0.1, 0, 0.1), 45.0F), 1.0E-4F);
    }

    @Test
    void glideMovesToward() {
        Vec from = new Vec(0, 0, 0);
        Vec to = new Vec(10, 0, 0);
        Vec mid = CameraMath.glide(from, to, 0.5);
        assertEquals(5.0, mid.x(), 1.0E-9);
    }
}
