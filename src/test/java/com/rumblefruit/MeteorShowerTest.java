package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// meteor shower on a fake server: the 20-minute schedule, the fall, the crater
// and the meteor core left smouldering in it
class MeteorShowerTest {
    private ServerPlayer player;
    private ServerLevel level;

    @BeforeEach
    void setUp() {
        MeteorShower.clear();
        MeteorShower.RANDOM = new Random(42);
        player = new ServerPlayer();
        level = new ServerLevel();
        player.setServerLevel(level);
        player.setPos(100.0, 64.0, -50.0);
        level.setGameTime(0);
    }

    @Test
    void scheduleFiresEveryTwentyMinutes() {
        // given a fresh world clock
        // then nothing falls before the deadline
        assertFalse(MeteorShower.tickSchedule(level));
        level.setGameTime(23999);
        assertFalse(MeteorShower.tickSchedule(level));
        // when the clock strikes 20 minutes
        level.setGameTime(24000);
        assertTrue(MeteorShower.tickSchedule(level));
        // then a meteor is falling above the player
        assertTrue(level.freshEntities.stream().anyMatch(e -> e instanceof FallingBlockEntity));
        // and the next one is exactly 20 minutes away, not sooner
        assertFalse(MeteorShower.tickSchedule(level));
        level.setGameTime(47999);
        assertFalse(MeteorShower.tickSchedule(level));
        level.setGameTime(48000);
        assertTrue(MeteorShower.tickSchedule(level));
    }

    @Test
    void countdownCountsDownToTheNextFall() {
        // given the schedule is armed at t=0
        MeteorShower.tickSchedule(level);
        // then the countdown reads 20 minutes and shrinks with the clock
        assertEquals(24000, MeteorShower.remainingTicks(0));
        assertEquals(23000, MeteorShower.remainingTicks(1000));
        // when the meteor falls at t=24000 the countdown restarts
        level.setGameTime(24000);
        MeteorShower.tickSchedule(level);
        assertEquals(24000, MeteorShower.remainingTicks(24000));
        assertEquals(19000, MeteorShower.remainingTicks(29000));
    }

    @Test
    void meteorFallsFromHighAboveThePlayer() {
        // when a meteor spawns
        FallingBlockEntity meteor = MeteorShower.spawnMeteor(level, player);
        // then it appears 55 blocks overhead, within the spawn spread sideways
        assertEquals(119.0, meteor.getY(), 1.0E-9);
        assertTrue(Math.abs(meteor.getX() - 100.0) <= 40.0);
        assertTrue(Math.abs(meteor.getZ() - (-50.0)) <= 40.0);
        // and it dives fast, still airborne, as a magma block
        assertEquals(-1.5, meteor.getDeltaMovement().y, 1.0E-9);
        assertFalse(meteor.onGround());
        assertEquals(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK, meteor.blockState.getBlock());
        assertTrue(level.freshEntities.contains(meteor));
        // the sky tear is force-flagged — visible from kilometers away
        assertTrue(level.particles.stream().anyMatch(p -> p.longDistance));
    }

    @Test
    void meteorBurnsATrailWhileFalling() {
        // given a meteor mid-flight
        MeteorShower.spawnMeteor(level, player);
        level.particles.clear();
        // when a tick passes
        MeteorShower.tickMeteors();
        // then the burning trail was emitted (sparks + glow)
        assertEquals(2, level.particles.size());
        // and nothing has exploded yet
        assertTrue(level.explosions.isEmpty());
    }

    @Test
    void impactBlastsACraterAndPlantsTheCore() {
        // given a meteor that just touched down
        FallingBlockEntity meteor = MeteorShower.spawnMeteor(level, player);
        meteor.setOnGround(true);
        // when a tick passes
        MeteorShower.tickMeteors();
        // then a small crater was blown at the impact point
        assertEquals(1, level.explosions.size());
        assertEquals(meteor.getX(), level.explosions.get(0).x, 1.0E-9);
        assertEquals(3.0F, level.explosions.get(0).radius, 1.0E-9);
        // and the meteor core smoulders where it hit
        var pos = net.minecraft.core.BlockPos.containing(meteor.getX(), meteor.getY(), meteor.getZ());
        assertEquals(ModBlocks.METEOR_CORE.get(), level.getBlockState(pos).getBlock());
        // the impact flash stabs the sky for everyone far away
        assertTrue(level.particles.stream().anyMatch(p -> p.longDistance && p.y > meteor.getY() + 100.0));
        // the meteor is spent: further ticks do nothing
        int particleCount = level.particles.size();
        MeteorShower.tickMeteors();
        assertEquals(1, level.explosions.size());
        assertEquals(particleCount, level.particles.size());
    }

    @Test
    void discardedMeteorStillImpacts() {
        // given a meteor that turned into a block on landing (no longer alive)
        FallingBlockEntity meteor = MeteorShower.spawnMeteor(level, player);
        meteor.discard();
        // when a tick passes
        MeteorShower.tickMeteors();
        // then the impact still happens — the core survives the crash
        assertEquals(1, level.explosions.size());
        var pos = net.minecraft.core.BlockPos.containing(meteor.getX(), meteor.getY(), meteor.getZ());
        assertEquals(ModBlocks.METEOR_CORE.get(), level.getBlockState(pos).getBlock());
    }

    @Test
    void nothingHappensWithoutMeteors() {
        // when ticks pass with a clear sky
        MeteorShower.tickMeteors();
        MeteorShower.tickMeteors();
        // then nothing falls, nothing explodes, nothing glows
        assertTrue(level.explosions.isEmpty());
        assertTrue(level.particles.isEmpty());
    }
}
