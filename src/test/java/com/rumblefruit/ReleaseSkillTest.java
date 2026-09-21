package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// the R ultimate end-to-end on a fake server: ascension pushes the caster up,
// detonation hurts everything in range, blows a crater, strips the fruit
class ReleaseSkillTest {
    private ServerPlayer caster;
    private ServerLevel level;

    @BeforeEach
    void setUp() {
        net.neoforged.neoforge.network.PacketDistributor.clear();
        ElectroBolts.clear();
        caster = new ServerPlayer();
        level = new ServerLevel();
        caster.setServerLevel(level);
        caster.setPos(0.0, 64.0, 0.0);
        level.setGameTime(0);
        // the caster must actually own the fruit
        RumblePowerData.grant(caster);
        net.neoforged.neoforge.network.PacketDistributor.clear();
    }

    @Test
    void beginActivatesAndProtectsTheCaster() {
        // given a caster with the fruit
        int effectsBefore = caster.effectLog.size();
        // when the release starts
        ReleaseSkill.begin(caster);
        // then the sequence is running, the caster is protected, clients see the pose
        assertTrue(ReleaseSkill.isActive(caster.getUUID()));
        assertEquals(2, caster.effectLog.size() - effectsBefore); // resistance + glowing
        assertEquals(1, net.neoforged.neoforge.network.PacketDistributor.SENT.size());
    }

    @Test
    void ascensionLiftsGentlyThenFastThenHovers() {
        // given a running release
        ReleaseSkill.begin(caster);
        // when the first ticks pass, the lift is gentle
        for (int i = 0; i < 5; i++) {
            ReleaseSkill.tick(caster);
        }
        assertEquals(0.12, caster.getDeltaMovement().y, 1.0E-9);
        // when mid-charge, the climb is fast
        for (int i = 0; i < 25; i++) {
            ReleaseSkill.tick(caster);
        }
        assertEquals(0.72, caster.getDeltaMovement().y, 1.0E-9);
        // when the apex is reached, the caster hovers
        for (int i = 0; i < 25; i++) {
            ReleaseSkill.tick(caster);
        }
        assertEquals(0.0, caster.getDeltaMovement().y, 1.0E-9);
        // and bolts fed the charge all the way up
        assertFalse(ElectroBolts.STRIKES.isEmpty());
    }

    @Test
    void detonationNukesAndSpendsTheFruit() {
        // given a victim in the blast zone
        LivingEntity victim = new LivingEntity();
        victim.setPos(3.0, 64.0, 3.0);
        level.setQueryResult(java.util.List.of(victim));
        // when the charge completes
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the victim took nuke-tier magic damage, hurled away and blinded
        assertEquals(1, victim.damageLog.size());
        assertTrue(victim.damageLog.get(0) > 900.0F);
        assertTrue(victim.getDeltaMovement().x > 0.0);
        assertFalse(victim.effectLog.isEmpty());
        // a crater is blown open, the caster is thrown down into it
        assertFalse(level.explosions.isEmpty());
        assertTrue(caster.getDeltaMovement().y < 0.0);
        // the fruit is spent and the knockout went out to clients
        assertFalse(RumblePowerData.hasPower(caster));
        assertTrue(caster.messages.contains("rumblefruit.release_spent"));
        // the caster never hurt himself
        assertTrue(caster.damageLog.isEmpty());
    }

    @Test
    void detonationSkipsOutOfRange() {
        // given a victim far beyond the blast radius
        LivingEntity far = new LivingEntity();
        far.setPos(130.0, 64.0, 0.0);
        level.setQueryResult(java.util.List.of(far));
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then nothing touches it
        assertTrue(far.damageLog.isEmpty());
    }

    @Test
    void detonationDamageFallsOffWithDistance() {
        // given two victims, near and far
        LivingEntity near = new LivingEntity();
        near.setPos(2.0, 64.0, 0.0);
        LivingEntity far = new LivingEntity();
        far.setPos(38.0, 64.0, 0.0);
        level.setQueryResult(java.util.List.of(near, far));
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the near one takes much more — exactly 1000*(1-2/187.5)
        assertEquals(1, near.damageLog.size());
        assertEquals(1, far.damageLog.size());
        assertTrue(near.damageLog.get(0) > far.damageLog.get(0));
        assertEquals(989.33F, near.damageLog.get(0), 0.1F);
    }

    @Test
    void sequenceEndsAfterTheBlast() {
        // given a full run
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 61; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the release is over and ticks are no-ops
        assertFalse(ReleaseSkill.isActive(caster.getUUID()));
        double yBefore = caster.getDeltaMovement().y;
        ReleaseSkill.tick(caster);
        assertEquals(yBefore, caster.getDeltaMovement().y, 1.0E-9);
    }

    @Test
    void stillAscendingAtTheLastChargeTick() {
        // given a release mid-charge
        ReleaseSkill.begin(caster);
        // when exactly 60 ticks pass (the whole charge)
        for (int t = 0; t < 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then it has NOT detonated yet — the blast waits for tick 61
        assertTrue(ReleaseSkill.isActive(caster.getUUID()));
        assertTrue(RumblePowerData.hasPower(caster));
        // when one more tick passes
        ReleaseSkill.tick(caster);
        // then the fruit is spent
        assertFalse(RumblePowerData.hasPower(caster));
    }

    @Test
    void ascendBoundaryBetweenGentleAndFast() {
        // given a running release
        ReleaseSkill.begin(caster);
        // when tick 7 — still the gentle phase
        for (int t = 0; t < 7; t++) {
            ReleaseSkill.tick(caster);
        }
        assertEquals(0.12, caster.getDeltaMovement().y, 1.0E-9);
        // when tick 8 — the fast climb kicks in
        ReleaseSkill.tick(caster);
        assertEquals(0.72, caster.getDeltaMovement().y, 1.0E-9);
    }

    @Test
    void ascendBoundaryBetweenFastAndHover() {
        // given a running release
        ReleaseSkill.begin(caster);
        // when tick 51 — still climbing fast
        for (int t = 0; t < 51; t++) {
            ReleaseSkill.tick(caster);
        }
        assertEquals(0.72, caster.getDeltaMovement().y, 1.0E-9);
        // when tick 52 — hovering
        ReleaseSkill.tick(caster);
        assertEquals(0.0, caster.getDeltaMovement().y, 1.0E-9);
    }

    @Test
    void ascendDampsHorizontalDrift() {
        // given a caster drifting sideways
        caster.setDeltaMovement(new net.minecraft.world.phys.Vec3(1.0, 0.0, 1.0));
        ReleaseSkill.begin(caster);
        // when a tick passes
        ReleaseSkill.tick(caster);
        // then horizontal drift is damped to 40%
        assertEquals(0.4, caster.getDeltaMovement().x, 1.0E-9);
        assertEquals(0.4, caster.getDeltaMovement().z, 1.0E-9);
    }

    @Test
    void casterIsImmuneToHisOwnBlast() {
        // given the caster himself inside the entity query
        LivingEntity victim = new LivingEntity();
        victim.setPos(2.0, 64.0, 0.0);
        level.setQueryResult(java.util.List.of(caster, victim));
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then only the victim is hurt
        assertTrue(caster.damageLog.isEmpty());
        assertEquals(1, victim.damageLog.size());
    }

    @Test
    void blastRadiusBoundaryStillHits() {
        // given a victim exactly at the blast radius edge
        LivingEntity edge = new LivingEntity();
        edge.setPos(125.0, 64.0, 0.0);
        level.setQueryResult(java.util.List.of(edge));
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the edge case still takes damage
        assertEquals(1, edge.damageLog.size());
    }

    @Test
    void knockbackFliesAwayFromTheCaster() {
        // given a victim to the east
        LivingEntity east = new LivingEntity();
        east.setPos(3.0, 64.0, 0.0);
        level.setQueryResult(java.util.List.of(east));
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then it is hurled east, up and away: (1-3/125)*12 = 11.712
        assertEquals(11.712, east.getDeltaMovement().x, 1.0E-3);
        assertEquals(2.5, east.getDeltaMovement().y, 1.0E-9);
        assertEquals(0.0, east.getDeltaMovement().z, 1.0E-9);
    }

    @Test
    void craterExplodesAtGroundLevel() {
        // given solid ground 4 blocks below the detonation
        level.setClipResult(new net.minecraft.world.phys.BlockHitResult(
                new net.minecraft.world.phys.Vec3(0.0, 60.0, 0.0),
                net.minecraft.world.phys.HitResult.Type.BLOCK));
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the crater charges detonate just above the ground, not in the sky
        assertEquals(61.0, level.explosions.get(0).y, 1.0E-9);
        assertEquals(63.0, level.explosions.get(1).y, 1.0E-9);
    }

    @Test
    void craterIsWitherStormScale() {
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the whole area is torn out: 2 core + 4 rings of 8 + 1 final
        assertEquals(35, level.explosions.size());
        // every blast of all four rings lands exactly on its circle
        double[] ringR = {16.0, 32.0, 48.0, 64.0};
        double[] ringPhase = {0.0, Math.PI / 8.0, 0.0, Math.PI / 8.0};
        for (int k = 0; k < 4; k++) {
            for (int i = 0; i < 8; i++) {
                double angle = ringPhase[k] + i * Math.PI / 4.0;
                var blast = level.explosions.get(2 + k * 8 + i);
                assertEquals(Math.cos(angle) * ringR[k], blast.x, 1.0E-9);
                assertEquals(65.0, blast.y, 1.0E-9);
                assertEquals(Math.sin(angle) * ringR[k], blast.z, 1.0E-9);
            }
        }
    }

    @Test
    void withoutGroundTheCraterCentresOnTheCaster() {
        // given open sky below (clip misses)
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the crater forms around the caster position
        assertEquals(65.0, level.explosions.get(0).y, 1.0E-9);
    }

    @Test
    void wingsFoldWhenTheFruitBurnsOut() {
        // given the angel transformation active
        WingsData.setActive(caster, true);
        assertTrue(WingsData.isActive(caster.getUUID()));
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the wings are gone too
        assertFalse(WingsData.isActive(caster.getUUID()));
    }

    @Test
    void stanceResetsWhenTheFruitBurnsOut() {
        // given the sword stance active
        StanceData.cycle(caster);
        assertEquals(StanceData.SWORD, StanceData.get(caster.getUUID()));
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the stance drops back to fists
        assertEquals(StanceData.FISTS, StanceData.get(caster.getUUID()));
    }

    @Test
    void knockoutPoseGoesToClients() {
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then clients got both the ascension and the knockout pose
        boolean knockout = net.neoforged.neoforge.network.PacketDistributor.SENT.stream()
                .anyMatch(p -> p instanceof CombatAnimPacket cap && cap.combo() == 20);
        assertTrue(knockout);
    }

    @Test
    void beginPlaysTheChargeUpSounds() {
        // when the release starts
        ReleaseSkill.begin(caster);
        // then both charge-up sounds play
        assertEquals(2, level.sounds.size());
    }

    @Test
    void blastThenCarvesTheMountainAway() {
        // when the blast goes off
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the sphere dissolves outward for 8 more ticks
        for (int t = 0; t < 8; t++) {
            ReleaseSkill.tick(caster);
        }
        var center = net.minecraft.core.BlockPos.containing(caster.position());
        // the core and the deep shell edge are plain air now
        assertTrue(level.getBlockState(center).isAir());
        assertTrue(level.getBlockState(center.offset(20, 10, 20)).isAir());
    }

    @Test
    void blastEmitsVisualEffects() {
        // given a release mid-charge
        ReleaseSkill.begin(caster);
        for (int t = 0; t < 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // when the charge completes — the blast fires its show
        level.particles.clear();
        level.sounds.clear();
        ReleaseSkill.tick(caster);
        // then particles and thunder actually went out
        assertFalse(level.particles.isEmpty());
        assertFalse(level.sounds.isEmpty());
    }

    @Test
    void bedrockSurvivesTheCarve() {
        // given indestructible ground
        level.setDefaultBlock(net.minecraft.world.level.block.Blocks.BEDROCK);
        // when the blast and the carve complete
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 68; t++) {
            ReleaseSkill.tick(caster);
        }
        // then bedrock is untouched
        var center = net.minecraft.core.BlockPos.containing(caster.position());
        assertFalse(level.getBlockState(center).isAir());
    }

    @Test
    void noCarveWithoutBlast() {
        // when ticks pass without the ultimate
        ReleaseSkill.tick(caster);
        // then nothing is carved
        var center = net.minecraft.core.BlockPos.containing(caster.position());
        assertFalse(level.getBlockState(center).isAir());
    }

    @Test
    void carveStopsAfterAllShells() {
        // when the blast and the carve complete
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 68; t++) {
            ReleaseSkill.tick(caster);
        }
        int afterCarve = level.particles.size();
        // then further ticks emit nothing (the sphere is gone)
        ReleaseSkill.tick(caster);
        assertEquals(afterCarve, level.particles.size());
    }

    @Test
    void carveShellBoundary() {
        // when the blast went off and ONE carve tick ran (shell 0: radius 0..8)
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 61; t++) {
            ReleaseSkill.tick(caster);
        }
        var center = net.minecraft.core.BlockPos.containing(caster.position());
        // then the shell edge block is carved but one beyond it is not yet
        assertTrue(level.getBlockState(center.offset(2, 0, 0)).isAir());
        assertTrue(level.getBlockState(center.offset(8, 0, 0)).isAir()); // exact shell edge
        assertFalse(level.getBlockState(center.offset(9, 0, 0)).isAir());
    }

    @Test
    void carveShellGlowTracesTheExactSphereEdge() {
        // given the blast went off and the rng is reseeded for a deterministic carve
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        ReleaseSkill.RANDOM = new java.util.Random(42);
        level.particles.clear();
        // when the first carve shell (radius 8) dissolves
        ReleaseSkill.tick(caster);
        // then 40 glow points trace the exact shell edge, in rng order
        assertEquals(40, level.particles.size());
        java.util.Random expected = new java.util.Random(42);
        for (int i = 0; i < 40; i++) {
            double theta = expected.nextDouble() * Math.PI * 2.0;
            double phi = expected.nextDouble() * Math.PI;
            var p = level.particles.get(i);
            assertEquals(Math.sin(phi) * Math.cos(theta) * 8.0, p.x, 1.0E-9);
            assertEquals(64.0 + Math.cos(phi) * 8.0, p.y, 1.0E-9);
            assertEquals(Math.sin(phi) * Math.sin(theta) * 8.0, p.z, 1.0E-9);
            assertEquals(2, p.count);
        }
    }

    @Test
    void landingGracePinsFallDistanceToZero() {
        // given the blast went off and the caster is plummeting into the crater
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        caster.fallDistance = 12.0F;
        // when a tick passes inside the grace window
        ReleaseSkill.tick(caster);
        // then the fall distance is pinned to zero — the slam cannot hurt
        assertEquals(0.0F, caster.fallDistance, 1.0E-9);
    }

    @Test
    void landingGraceExpires() {
        // given the blast went off long ago (past the 10s grace window)
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        level.setGameTime(500);
        caster.fallDistance = 12.0F;
        // when a tick passes after the grace expired
        ReleaseSkill.tick(caster);
        // then normal fall physics apply again
        assertEquals(12.0F, caster.fallDistance, 1.0E-9);
    }

    @Test
    void descentIsSlowMotionCapped() {
        // given the blast went off and the caster is airborne
        caster.setOnGround(false);
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // then the dive starts near-weightless
        assertEquals(-0.05, caster.getDeltaMovement().y, 1.0E-9);
        // when the descent stretches on, it eases into a gentle dive and stops there
        for (int t = 0; t < 40; t++) {
            ReleaseSkill.tick(caster);
        }
        assertEquals(-0.30, caster.getDeltaMovement().y, 1.0E-9);
        // and even a hundred ticks later it never gets faster than the slow-mo cap
        for (int t = 0; t < 100; t++) {
            ReleaseSkill.tick(caster);
        }
        assertEquals(-0.30, caster.getDeltaMovement().y, 1.0E-9);
    }

    @Test
    void touchdownSwitchesToTheLyingPose() {
        // given the caster mid slow-mo fall
        caster.setOnGround(false);
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        net.neoforged.neoforge.network.PacketDistributor.clear();
        ReleaseSkill.tick(caster);
        // while airborne no touchdown packet goes out
        assertTrue(net.neoforged.neoforge.network.PacketDistributor.SENT.stream()
                .noneMatch(p -> p instanceof CombatAnimPacket cap && cap.combo() == 21));
        // when the caster hits the ground
        caster.setOnGround(true);
        ReleaseSkill.tick(caster);
        // then clients switch from the falling pose to the lying one
        assertTrue(net.neoforged.neoforge.network.PacketDistributor.SENT.stream()
                .anyMatch(p -> p instanceof CombatAnimPacket cap && cap.combo() == 21));
    }

    @Test
    void fallDistanceStaysZeroThroughTheWholeDive() {
        // given the caster mid slow-mo fall
        caster.setOnGround(false);
        ReleaseSkill.begin(caster);
        for (int t = 0; t <= 60; t++) {
            ReleaseSkill.tick(caster);
        }
        // when the fall stretches on
        for (int t = 0; t < 20; t++) {
            caster.fallDistance = 9.0F; // physics keeps piling it up
            ReleaseSkill.tick(caster);
            // then the slow-mo handler pins it to zero every single tick
            assertEquals(0.0F, caster.fallDistance, 1.0E-9);
        }
    }
}
