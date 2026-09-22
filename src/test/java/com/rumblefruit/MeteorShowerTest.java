package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// meteor shower end-to-end on a fake server: a blazing block falls from high
// above the player, burns a trail, blasts a crater and drops a lightning fruit
class MeteorShowerTest {
    private ServerPlayer player;
    private ServerLevel level;
    private List<double[]> fruitDrops;

    @BeforeEach
    void setUp() {
        MeteorShower.clear();
        MeteorShower.RANDOM = new Random(42);
        fruitDrops = new ArrayList<>();
        MeteorShower.FRUIT_DROP = (lvl, x, y, z) -> fruitDrops.add(new double[]{x, y, z});
        player = new ServerPlayer();
        level = new ServerLevel();
        player.setServerLevel(level);
        player.setPos(100.0, 64.0, -50.0);
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
        // the world actually got the entity
        assertTrue(level.freshEntities.contains(meteor));
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
        assertTrue(fruitDrops.isEmpty());
    }

    @Test
    void impactBlastsACraterAndDropsTheFruit() {
        // given a meteor that just touched down
        FallingBlockEntity meteor = MeteorShower.spawnMeteor(level, player);
        meteor.setOnGround(true);
        level.particles.clear();
        // when a tick passes
        MeteorShower.tickMeteors();
        // then a small crater was blown at the impact point
        assertEquals(1, level.explosions.size());
        assertEquals(meteor.getX(), level.explosions.get(0).x, 1.0E-9);
        assertEquals(meteor.getY(), level.explosions.get(0).y, 1.0E-9);
        assertEquals(meteor.getZ(), level.explosions.get(0).z, 1.0E-9);
        assertEquals(3.0F, level.explosions.get(0).radius, 1.0E-9);
        // and the fruit pod smoulders one block above the crater
        assertEquals(1, fruitDrops.size());
        assertEquals(meteor.getX(), fruitDrops.get(0)[0], 1.0E-9);
        assertEquals(meteor.getY() + 1.0, fruitDrops.get(0)[1], 1.0E-9);
        assertEquals(meteor.getZ(), fruitDrops.get(0)[2], 1.0E-9);
        // the meteor is spent: further ticks do nothing
        MeteorShower.tickMeteors();
        assertEquals(1, level.explosions.size());
        assertEquals(1, fruitDrops.size());
    }

    @Test
    void discardedMeteorStillImpacts() {
        // given a meteor that turned into a block on landing (no longer alive)
        FallingBlockEntity meteor = MeteorShower.spawnMeteor(level, player);
        meteor.discard();
        // when a tick passes
        MeteorShower.tickMeteors();
        // then the impact still happens — the fruit survives the crash
        assertEquals(1, level.explosions.size());
        assertEquals(1, fruitDrops.size());
    }

    @Test
    void nothingHappensWithoutMeteors() {
        // when ticks pass with a clear sky
        MeteorShower.tickMeteors();
        MeteorShower.tickMeteors();
        // then nothing falls, nothing explodes, nothing drops
        assertTrue(level.explosions.isEmpty());
        assertTrue(fruitDrops.isEmpty());
        assertTrue(level.particles.isEmpty());
    }
}
