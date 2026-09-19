package com.rumblefruit.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RayPolylineTest {
    private static final double[][] GOLDEN = {
            {1.548585, 2.324136, 3.322792},
            {1.984551, 2.682545, 3.760605},
            {2.419122, 2.995992, 4.232943},
            {2.875871, 3.393853, 4.611481},
            {3.420454, 3.761340, 4.891816},
            {3.972151, 4.024027, 5.261721},
            {4.432895, 4.320586, 5.719947},
            {4.868863, 4.611004, 6.205532},
            {5.362129, 4.873870, 6.650274},
            {5.921866, 4.976523, 7.081968},
            {6.514051, 4.980979, 7.481345},
            {7.130310, 4.941696, 7.840352},
            {7.707813, 4.956823, 8.260430},
            {8.382738, 4.986661, 8.492355},
    };

    @Test
    void goldenMasterSeed42() {
        // given a fixed seed the ray is fully deterministic
        List<Vec> points = RayPolyline.generate(new Vec(1.0, 2.0, 3.0), 0.7, 0.4, 10.0, new Random(42));
        assertEquals(GOLDEN.length, points.size());
        for (int i = 0; i < GOLDEN.length; i++) {
            assertEquals(GOLDEN[i][0], points.get(i).x(), 1.0E-5, "x at " + i);
            assertEquals(GOLDEN[i][1], points.get(i).y(), 1.0E-5, "y at " + i);
            assertEquals(GOLDEN[i][2], points.get(i).z(), 1.0E-5, "z at " + i);
        }
    }

    @Test
    void forkAddsSegments() {
        // seed 0 forks once at the middle: main ray + fork
        assertEquals(2 * RayPolyline.SEGMENTS,
                RayPolyline.generate(new Vec(0, 0, 0), 0.0, 0.0, 10.0, new Random(0)).size());
        // seed 5 never forks
        assertEquals(RayPolyline.SEGMENTS,
                RayPolyline.generate(new Vec(0, 0, 0), 0.0, 0.0, 10.0, new Random(5)).size());
    }

    @Test
    void forkGoldenMaster() {
        // the fork ray points must match exactly (kills fork-angle mutants)
        List<Vec> points = RayPolyline.generate(new Vec(0, 0, 0), 0.3, 0.2, 10.0, new Random(0));
        double[][] fork = {
                {7.010856, 1.008248, 1.274574},
                {7.240402, 1.161132, 1.199957},
                {7.431760, 1.330956, 1.072778},
        };
        for (int i = 0; i < fork.length; i++) {
            Vec p = points.get(14 + i);
            assertEquals(fork[i][0], p.x(), 1.0E-5);
            assertEquals(fork[i][1], p.y(), 1.0E-5);
            assertEquals(fork[i][2], p.z(), 1.0E-5);
        }
    }

    @Test
    void startsNearOrigin() {
        List<Vec> points = RayPolyline.generate(new Vec(5, 5, 5), 1.0, 0.5, 8.0, new Random(1));
        assertTrue(points.get(0).distanceTo(new Vec(5, 5, 5)) < 1.0);
    }

    @Test
    void staysWithinReasonableReach() {
        List<Vec> points = RayPolyline.generate(new Vec(0, 0, 0), 0.3, 0.2, 12.0, new Random(7));
        double limit = 12.0 * (1.0 + RayPolyline.FORK_LENGTH) + 1.0;
        for (Vec p : points) {
            assertTrue(p.length() <= limit, "point escaped: " + p);
        }
    }

    @Test
    void deterministicPerSeed() {
        List<Vec> a = RayPolyline.generate(new Vec(0, 0, 0), 0.0, 0.0, 10.0, new Random(123));
        List<Vec> b = RayPolyline.generate(new Vec(0, 0, 0), 0.0, 0.0, 10.0, new Random(123));
        assertEquals(a, b);
    }

    @Test
    void differentSeedsDiffer() {
        List<Vec> a = RayPolyline.generate(new Vec(0, 0, 0), 0.0, 0.0, 10.0, new Random(1));
        List<Vec> b = RayPolyline.generate(new Vec(0, 0, 0), 0.0, 0.0, 10.0, new Random(2));
        assertTrue(!a.equals(b));
    }
}
