package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// the visual layer of the R ultimate, verified against golden references
// captured with a fixed random seed (fx emission is fully deterministic)
class ReleaseSkillFxTest {
    private ServerPlayer caster;
    private ServerLevel level;

    @BeforeEach
    void setUp() {
        ReleaseSkill.RANDOM = new Random(42);
        ElectroBolts.clear();
        caster = new ServerPlayer();
        level = new ServerLevel();
        caster.setServerLevel(level);
        caster.setPos(0.0, 64.0, 0.0);
        level.setGameTime(0);
        RumblePowerData.grant(caster);
    }

    @Test
    void firstTickEmitsGoldenRayPoints() {
        // when the release starts and one tick passes
        ReleaseSkill.begin(caster);
        ReleaseSkill.tick(caster);
        // then the ray from the offhand follows the golden coordinates
        double[][] golden = {
                {-0.654030, 65.831741, -0.534379},
                {-0.928660, 66.226032, -1.107422},
                {-1.050690, 66.743621, -1.633230},
        };
        for (int i = 0; i < golden.length; i++) {
            var p = level.particles.get(i * 2); // each ray point emits spark + rod
            assertEquals(golden[i][0], p.x, 1.0E-5);
            assertEquals(golden[i][1], p.y, 1.0E-5);
            assertEquals(golden[i][2], p.z, 1.0E-5);
        }
    }

    @Test
    void firstTickEmitsExactShow() {
        // when the release starts and one tick passes
        ReleaseSkill.begin(caster);
        ReleaseSkill.tick(caster);
        // then exactly the golden number of effects fired
        assertEquals(76, level.particles.size());
        assertEquals(2, level.sounds.size());
    }

    @Test
    void firstTickGoldenSpreadsAndSounds() {
        // when the release starts and one tick passes
        ReleaseSkill.begin(caster);
        ReleaseSkill.tick(caster);
        // then spreads/velocities match the golden capture
        var rayPoint = level.particles.get(0);
        assertEquals(0.03, rayPoint.dx, 1.0E-9); // tight spark spread
        assertEquals(0.0, rayPoint.speed, 1.0E-9);
        var helixPoint = level.particles.get(70);
        assertEquals(0.05, helixPoint.dx, 1.0E-9);
        assertEquals(0.02, helixPoint.speed, 1.0E-9);
        // and the charge-up sounds carry the golden volume and pitch
        assertEquals("warden_sonic_charge@3.0@0.6", level.sounds.get(0));
        assertEquals("beacon_power_select@2.0@0.5", level.sounds.get(1));
    }

    @Test
    void coatingThickensWhileCharging() {
        // when the caster charges to tick 40
        ReleaseSkill.begin(caster);
        for (int t = 0; t < 40; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the show has grown far beyond the opening tick
        assertEquals(10247, level.particles.size());
        assertEquals(12, level.sounds.size());
    }

    @Test
    void blastTickEmitsGoldenShow() {
        // given a full charge
        ReleaseSkill.begin(caster);
        for (int t = 0; t < 60; t++) {
            ReleaseSkill.tick(caster);
        }
        level.particles.clear();
        level.sounds.clear();
        // when the blast fires
        ReleaseSkill.tick(caster);
        // then exactly the golden blast: 1572 particles, 4 thunder sounds
        assertEquals(1572, level.particles.size());
        assertEquals(4, level.sounds.size());
        // opening with the triple flash at the caster's chest height
        for (int i = 0; i < 3; i++) {
            var flash = level.particles.get(i);
            assertEquals(0.0, flash.x, 1.0E-9);
            assertEquals(65.0, flash.y, 1.0E-9);
            assertEquals(0.0, flash.z, 1.0E-9);
            assertEquals(1, flash.count);
        }
    }

    @Test
    void skyBoltsFeedTheChargeEveryFourTicks() {
        // when 8 ticks of charge pass
        ReleaseSkill.begin(caster);
        for (int t = 0; t < 8; t++) {
            ReleaseSkill.tick(caster);
        }
        // then two sky bolts have struck into the caster (t=4 and t=8)
        assertEquals(2, ElectroBolts.STRIKES.size());
    }

    // rolling hash over every emitted particle field — any mutated coordinate,
    // spread, speed or count breaks the stream and the hash
    private static long hash(long h, double v) {
        return h * 31 + Double.doubleToLongBits(v);
    }

    // fresh caster + world + seed per probe, exactly like the golden capture
    // (caster faces yaw 37 so no trig factor degenerates to 0 or 1)
    private long probeHash(int ticks) {
        ReleaseSkill.RANDOM = new Random(42);
        ElectroBolts.clear();
        ServerPlayer probe = new ServerPlayer();
        ServerLevel probeLevel = new ServerLevel();
        probe.setServerLevel(probeLevel);
        probe.setPos(0.0, 64.0, 0.0);
        probe.setYRot(37.0F);
        RumblePowerData.grant(probe);
        ReleaseSkill.begin(probe);
        long h = 7;
        for (int t = 0; t < ticks; t++) {
            probeLevel.particles.clear();
            probeLevel.sounds.clear();
            ReleaseSkill.tick(probe);
            for (var p : probeLevel.particles) {
                h = hash(h, p.x);
                h = hash(h, p.y);
                h = hash(h, p.z);
                h = hash(h, p.count);
                h = hash(h, p.dx);
                h = hash(h, p.dy);
                h = hash(h, p.dz);
                h = hash(h, p.speed);
            }
            for (String s : probeLevel.sounds) {
                h = h * 31 + s.hashCode();
            }
        }
        for (String b : ElectroBolts.STRIKES) {
            h = h * 31 + b.hashCode();
        }
        return h;
    }

    @Test
    void chargeStreamMatchesGoldenHashes() {
        // every phase boundary of the ascension, hashed against the golden capture
        long[][] golden = {
                {1, 1830378927751236466L},
                {4, 3295771452895037328L},
                {8, -5473444450046889394L},
                {10, 4155257181818601588L},
                {15, -987783029939817272L},
                {30, -1231204668925986699L},
                {45, -6502395068137776905L},
                {60, 5203240350432331406L},
        };
        for (long[] g : golden) {
            assertEquals(g[1], probeHash((int) g[0]), "stream hash diverged at tick " + g[0]);
        }
    }

    @Test
    void blastStreamMatchesGoldenHash() {
        // given a full charge (caster rotated so every trig factor counts)
        ReleaseSkill.RANDOM = new Random(42);
        ElectroBolts.clear();
        caster.setYRot(37.0F);
        ReleaseSkill.begin(caster);
        for (int t = 0; t < 60; t++) {
            ReleaseSkill.tick(caster);
        }
        level.particles.clear();
        level.sounds.clear();
        // when the blast fires
        ReleaseSkill.tick(caster);
        // then the whole stream matches the golden capture
        long h = 7;
        for (var p : level.particles) {
            h = hash(h, p.x);
            h = hash(h, p.y);
            h = hash(h, p.z);
            h = hash(h, p.count);
            h = hash(h, p.dx);
            h = hash(h, p.dy);
            h = hash(h, p.dz);
            h = hash(h, p.speed);
        }
        for (String s : level.sounds) {
            h = h * 31 + s.hashCode();
        }
        for (String b : ElectroBolts.STRIKES) {
            h = h * 31 + b.hashCode();
        }
        assertEquals(8670506311394982401L, h);
    }
}
