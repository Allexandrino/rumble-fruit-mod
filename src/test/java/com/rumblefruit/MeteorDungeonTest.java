package com.rumblefruit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// the meteor dungeon: built once deep underground, the core drags you in with
// a fruit in hand, the Cube Titan wakes, and only its death lets you out
class MeteorDungeonTest {
    private ServerLevel level;
    private ServerPlayer player;
    private int fruitGranted;
    private List<BlockPos> bossSpawns;

    @BeforeEach
    void setUp() {
        MeteorDungeon.reset();
        fruitGranted = 0;
        bossSpawns = new ArrayList<>();
        MeteorDungeon.FRUIT_GRANT = p -> fruitGranted++;
        MeteorDungeon.BOSS_SPAWNER = (lvl, at) -> bossSpawns.add(at);
        level = new ServerLevel();
        player = new ServerPlayer();
        player.setServerLevel(level);
        player.setPos(5.0, 70.0, 5.0);
    }

    @Test
    void vaultIsBuiltOnceDeepUnderground() {
        // when the dungeon is located
        BlockPos c = MeteorDungeon.getOrCreate(level);
        // then it sits at y=-50, far from spawn
        assertEquals(-50, c.getY());
        assertEquals(2000, c.getX());
        // the arena cross is gold, the walls are deepslate with an open middle
        assertEquals(Blocks.GOLD_BLOCK, level.getBlockState(c).getBlock());
        assertEquals(Blocks.DEEPSLATE_BRICKS, level.getBlockState(c.offset(12, 1, 5)).getBlock());
        assertTrue(level.getBlockState(c.offset(0, 3, 0)).isAir());
        // and it is never rebuilt
        assertEquals(c, MeteorDungeon.getOrCreate(level));
    }

    @Test
    void enterDragsYouInWithFruitAndWakesTheTitan() {
        // when the player right-clicks the meteor core
        MeteorDungeon.enter(level, player);
        // then they are inside the vault, fruit in hand, titan awake at the dais
        BlockPos c = MeteorDungeon.getOrCreate(level);
        assertEquals(c.getX() + 0.5, player.getX(), 1.0E-9);
        assertEquals(c.getY() + 1.0, player.getY(), 1.0E-9);
        assertEquals(1, fruitGranted);
        assertEquals(1, bossSpawns.size());
        assertEquals(c, bossSpawns.get(0));
        // and the way home is remembered
        var home = MeteorDungeon.returnPos(player.getUUID());
        assertEquals(5.0, home.x, 1.0E-9);
        assertEquals(70.0, home.y, 1.0E-9);
        assertEquals(5.0, home.z, 1.0E-9);
    }

    @Test
    void titanIsNotDuplicatedOnReentry() {
        // given the titan already guards the room
        MeteorDungeon.enter(level, player);
        // when the player enters again
        MeteorDungeon.enter(level, player);
        // then no second titan wakes
        assertEquals(1, bossSpawns.size());
    }

    @Test
    void releaseAllSendsEveryoneHomeAndRearmsTheVault() {
        // given a player inside the dungeon
        MeteorDungeon.enter(level, player);
        // when the titan falls
        MeteorDungeon.releaseAll(level);
        // then the player is back where they touched the core
        assertEquals(5.0, player.getX(), 1.0E-9);
        assertEquals(70.0, player.getY(), 1.0E-9);
        assertTrue(player.messages.contains("rumblefruit.dungeon_cleared"));
        assertNull(MeteorDungeon.returnPos(player.getUUID()));
        // and the vault re-arms for the next visit
        MeteorDungeon.enter(level, player);
        assertEquals(2, bossSpawns.size());
    }

    @Test
    void strangersOutsideAreNotReleased() {
        // given a bystander far away from the vault
        ServerPlayer bystander = new ServerPlayer();
        bystander.setServerLevel(level);
        bystander.setPos(100.0, 70.0, 100.0);
        MeteorDungeon.enter(level, player);
        // when the titan falls
        MeteorDungeon.releaseAll(level);
        // then the bystander never moved
        assertEquals(100.0, bystander.getX(), 1.0E-9);
        assertEquals(70.0, bystander.getY(), 1.0E-9);
    }
}
