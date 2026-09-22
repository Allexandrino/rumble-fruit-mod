package com.rumblefruit;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// the Cube Titan's arsenal: all 25 attacks must actually DO something —
// particles, bolts, explosions, damage or effects — and the index wraps
class CubeTitanAttacksTest {
    private ServerLevel level;
    private LivingEntity boss;
    private LivingEntity target;

    @BeforeEach
    void setUp() {
        ElectroBolts.clear();
        CubeTitanAttacks.RANDOM = new Random(42);
        level = new ServerLevel();
        boss = new LivingEntity();
        boss.setPos(0.0, 64.0, 0.0);
        target = new LivingEntity();
        target.setPos(3.0, 64.0, 3.0);
    }

    private boolean somethingHappened() {
        return !level.particles.isEmpty() || !level.sounds.isEmpty() || !level.explosions.isEmpty()
                || !ElectroBolts.STRIKES.isEmpty() || !target.damageLog.isEmpty()
                || !target.effectLog.isEmpty() || !boss.effectLog.isEmpty()
                || !level.freshEntities.isEmpty();
    }

    @Test
    void everyAttackDoesSomething() {
        for (int i = 0; i < CubeTitanAttacks.COUNT; i++) {
            setUp();
            // when attack i fires
            CubeTitanAttacks.perform(i, level, boss, target);
            // then the world noticed
            assertTrue(somethingHappened(), "attack " + i + " was a dud");
        }
    }

    @Test
    void attackIndexWrapsAroundTheArsenal() {
        // when attack 25 fires (one past the end)
        CubeTitanAttacks.perform(25, level, boss, target);
        // then it is attack 0 again: a single crushing bolt plus 15 damage
        assertEquals(1, ElectroBolts.STRIKES.size());
        assertEquals(1, target.damageLog.size());
        assertEquals(15.0F, target.damageLog.get(0), 1.0E-9);
    }

    @Test
    void groundSlamKnocksTheCloseTargetAway() {
        // when the floor detonates next to the prey
        CubeTitanAttacks.perform(9, level, boss, target);
        // then it takes 20 damage and is hurled away and up
        assertEquals(1, target.damageLog.size());
        assertEquals(20.0F, target.damageLog.get(0), 1.0E-9);
        assertTrue(target.getDeltaMovement().y > 0.5);
        assertTrue(target.getDeltaMovement().length() > 1.0);
    }

    @Test
    void blinkStrikeMaterialisesTheTitanOnThePrey() {
        // when the titan blinks
        CubeTitanAttacks.perform(11, level, boss, target);
        // then it stands on top of the prey
        assertEquals(4.5, boss.getX(), 1.0E-9);
        assertEquals(4.5, boss.getZ(), 1.0E-9);
    }

    @Test
    void theCageSlowsThePrey() {
        // when the lightning cage closes
        CubeTitanAttacks.perform(15, level, boss, target);
        // then the prey is slowed and zapped
        assertEquals(1, target.effectLog.size());
        assertEquals(1, target.damageLog.size());
        assertEquals(12.0F, target.damageLog.get(0), 1.0E-9);
    }

    @Test
    void miniMeteorDropsABurningCube() {
        // when the mini meteor fires
        CubeTitanAttacks.perform(14, level, boss, target);
        // then a falling magma block dives at the prey from above
        assertEquals(1, level.freshEntities.size());
        var mini = (net.minecraft.world.entity.item.FallingBlockEntity) level.freshEntities.get(0);
        assertEquals(79.0, mini.getY(), 1.0E-9);
        assertEquals(-1.8, mini.getDeltaMovement().y, 1.0E-9);
    }
}
