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
        assertEquals(0.48, caster.getDeltaMovement().y, 1.0E-9);
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
        far.setPos(50.0, 64.0, 0.0);
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
        // then the near one takes much more
        assertEquals(1, near.damageLog.size());
        assertEquals(1, far.damageLog.size());
        assertTrue(near.damageLog.get(0) > far.damageLog.get(0));
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
        assertEquals(0.48, caster.getDeltaMovement().y, 1.0E-9);
    }

    @Test
    void ascendBoundaryBetweenFastAndHover() {
        // given a running release
        ReleaseSkill.begin(caster);
        // when tick 51 — still climbing fast
        for (int t = 0; t < 51; t++) {
            ReleaseSkill.tick(caster);
        }
        assertEquals(0.48, caster.getDeltaMovement().y, 1.0E-9);
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
        edge.setPos(40.0, 64.0, 0.0);
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
        // then it is hurled east, up and away: (1-3/40)*9 = 8.325
        assertEquals(8.325, east.getDeltaMovement().x, 1.0E-3);
        assertEquals(2.0, east.getDeltaMovement().y, 1.0E-9);
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
}
