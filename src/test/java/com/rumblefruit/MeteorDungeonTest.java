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
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// the exorcist chambers: a colossal hall built once in the void realm, the
// core drags you across dimensions with a fruit in hand, the Fallen Exorcist
// wakes, and only its death lets you out
class MeteorDungeonTest {
    private ServerLevel level;
    private ServerLevel realm;
    private ServerPlayer player;
    private int fruitGranted;
    private List<BlockPos> bossSpawns;

    private record TpCall(ServerPlayer player, ServerLevel dest, double x, double y, double z) {
    }

    private List<TpCall> teleports;

    @BeforeEach
    void setUp() {
        MeteorDungeon.reset();
        fruitGranted = 0;
        bossSpawns = new ArrayList<>();
        teleports = new ArrayList<>();
        MeteorDungeon.FRUIT_GRANT = p -> fruitGranted++;
        MeteorDungeon.BOSS_SPAWNER = (lvl, at) -> bossSpawns.add(at);
        level = new ServerLevel();
        realm = new ServerLevel();
        MeteorDungeon.REALM_OPENER = from -> {
            MeteorDungeon.getOrCreate(realm);
            return realm;
        };
        MeteorDungeon.TELEPORTER = (p, dest, x, y, z) -> {
            teleports.add(new TpCall(p, dest, x, y, z));
            p.setPos(x, y, z);
        };
        player = new ServerPlayer();
        player.setServerLevel(level);
        player.setPos(5.0, 70.0, 5.0);
    }

    @Test
    void chambersAreBuiltOnceInTheVoidRealm() {
        // when the chambers are located
        BlockPos c = MeteorDungeon.getOrCreate(realm);
        // then they sit at the heart of the void, colossal (walls at ±40, 60 high)
        assertArrayEquals(new int[]{0, 10, 0}, new int[]{c.getX(), c.getY(), c.getZ()});
        assertEquals(Blocks.GLOWSTONE, realm.getBlockState(c).getBlock()); // the glowing heart
        assertEquals(Blocks.GOLD_BLOCK, realm.getBlockState(c.offset(2, 0, 0)).getBlock()); // arena cross
        assertEquals(Blocks.BLACKSTONE, realm.getBlockState(c.offset(40, 1, 5)).getBlock());
        assertEquals(Blocks.BLACKSTONE, realm.getBlockState(c.offset(0, 60, 0)).getBlock()); // ceiling
        assertTrue(realm.getBlockState(c.offset(0, 3, 0)).isAir());
        // and they are never rebuilt
        assertEquals(c, MeteorDungeon.getOrCreate(realm));
    }

    @Test
    void enterDragsYouAcrossDimensionsWithFruitAndWakesTheExorcist() {
        // when the player right-clicks the meteor core in the overworld
        MeteorDungeon.enter(level, player);
        // then they cross into the realm, near the entrance of the hall
        assertEquals(1, teleports.size());
        assertEquals(realm, teleports.get(0).dest());
        assertEquals(0.5, teleports.get(0).x(), 1.0E-9);
        assertEquals(11.0, teleports.get(0).y(), 1.0E-9);
        // fruit in hand, exorcist awake at the dais
        assertEquals(1, fruitGranted);
        assertEquals(1, bossSpawns.size());
        assertArrayEquals(new int[]{0, 10, 0}, new int[]{bossSpawns.get(0).getX(),
                bossSpawns.get(0).getY(), bossSpawns.get(0).getZ()});
        // and the way home is remembered
        var home = MeteorDungeon.returnPos(player.getUUID());
        assertEquals(5.0, home.x, 1.0E-9);
        assertEquals(70.0, home.y, 1.0E-9);
    }

    @Test
    void exorcistIsNotDuplicatedOnReentry() {
        // given the exorcist already reigns over the hall
        MeteorDungeon.enter(level, player);
        // when the player enters again
        MeteorDungeon.enter(level, player);
        // then no second exorcist wakes
        assertEquals(1, bossSpawns.size());
    }

    @Test
    void releaseAllSendsEveryoneHomeAcrossDimensionsAndRearms() {
        // given a player inside the chambers
        realm.players().add(player);
        MeteorDungeon.enter(level, player);
        // when the exorcist falls
        MeteorDungeon.releaseAll(realm);
        // then the player crosses back to where they touched the core
        assertEquals(2, teleports.size());
        assertEquals(level, teleports.get(1).dest());
        assertEquals(5.0, teleports.get(1).x(), 1.0E-9);
        assertEquals(70.0, teleports.get(1).y(), 1.0E-9);
        assertTrue(player.messages.contains("rumblefruit.dungeon_cleared"));
        assertNull(MeteorDungeon.returnPos(player.getUUID()));
        // and the vault re-arms for the next visit
        MeteorDungeon.enter(level, player);
        assertEquals(2, bossSpawns.size());
    }

    @Test
    void strangersOutsideAreNotReleased() {
        // given a bystander in the realm but far outside the hall
        ServerPlayer bystander = new ServerPlayer();
        bystander.setServerLevel(realm);
        bystander.setPos(100.0, 70.0, 100.0);
        MeteorDungeon.enter(level, player);
        // when the exorcist falls
        MeteorDungeon.releaseAll(realm);
        // then the bystander never moved
        assertEquals(100.0, bystander.getX(), 1.0E-9);
        assertEquals(70.0, bystander.getY(), 1.0E-9);
    }
}
