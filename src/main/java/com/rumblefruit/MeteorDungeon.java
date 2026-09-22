package com.rumblefruit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// the meteor dungeon: a deepslate vault deep underground, built once per
// world. the meteor core drags you in; the Cube Titan guards the exit —
// beat it and everyone inside is teleported back to where they came from
public class MeteorDungeon {

    private MeteorDungeon() {
    }

    private static final int ROOM = 12;  // half-size of the square room
    private static final int HEIGHT = 10;
    private static BlockPos center;
    private static boolean bossAwake;
    private static final Map<UUID, Vec3> RETURN = new ConcurrentHashMap<>();

    // test seams: how the fruit is handed over and how the titan is summoned
    interface FruitGrant {
        void give(ServerPlayer player);
    }

    interface BossSpawner {
        void spawn(ServerLevel level, BlockPos at);
    }

    static FruitGrant FRUIT_GRANT = player -> player.getInventory().add(
            new net.minecraft.world.item.ItemStack(RumbleFruitMod.ELECTRO_APPLE.get()));
    static BossSpawner BOSS_SPAWNER = (level, at) -> {
        CubeTitanEntity boss = new CubeTitanEntity(ModEntities.CUBE_TITAN.get(), level);
        boss.setPos(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5);
        level.addFreshEntity(boss);
    };

    // where the vault sits: deep under a far corner of the world
    public static BlockPos getOrCreate(ServerLevel level) {
        if (center == null) {
            BlockPos spawn = level.getSharedSpawnPos();
            center = new BlockPos(spawn.getX() + 2000, -50, spawn.getZ() + 2000);
            build(level, center);
        }
        return center;
    }

    // the meteor core drags the player in: fruit in hand, titan awake
    public static void enter(ServerLevel level, ServerPlayer player) {
        BlockPos c = getOrCreate(level);
        RETURN.put(player.getUUID(), player.position());
        FRUIT_GRANT.give(player);
        player.teleportTo(c.getX() + 0.5, c.getY() + 1.0, c.getZ() - (ROOM - 3) + 0.5);
        // wake the titan only if none is guarding the room already
        if (!bossAwake) {
            BOSS_SPAWNER.spawn(level, c);
            bossAwake = true;
        }
        level.playSound(null, c, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);
        level.sendParticles(ModParticles.ELECTRO_GLOW.get(),
                c.getX() + 0.5, c.getY() + 2.0, c.getZ() + 0.5, 100, 6.0, 3.0, 6.0, 0.1);
    }

    // the titan is down: everyone inside the vault is flung back out
    public static void releaseAll(ServerLevel level) {
        if (center == null) {
            return;
        }
        bossAwake = false;
        Vec3 roomCenter = new Vec3(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        for (ServerPlayer player : level.players()) {
            if (player.position().distanceTo(roomCenter) > ROOM * 4) {
                continue;
            }
            Vec3 back = RETURN.remove(player.getUUID());
            if (back != null) {
                player.teleportTo(back.x, back.y, back.z);
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "rumblefruit.dungeon_cleared").withStyle(net.minecraft.ChatFormatting.GOLD), false);
        }
    }

    // test hooks
    static void reset() {
        center = null;
        bossAwake = false;
        RETURN.clear();
    }

    static Vec3 returnPos(UUID playerId) {
        return RETURN.get(playerId);
    }

    private static void set(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        level.setBlock(pos, block.defaultBlockState(), 3);
    }

    private static void build(ServerLevel level, BlockPos c) {
        for (int dx = -ROOM; dx <= ROOM; dx++) {
            for (int dz = -ROOM; dz <= ROOM; dz++) {
                for (int dy = 0; dy <= HEIGHT; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    boolean wall = dx == -ROOM || dx == ROOM || dz == -ROOM || dz == ROOM;
                    if (dy == 0) {
                        // floor: deepslate tiles with a gold arena cross
                        set(level, p, (Math.abs(dx) <= 1 || Math.abs(dz) <= 1)
                                ? Blocks.GOLD_BLOCK : Blocks.DEEPSLATE_TILES);
                    } else if (dy == HEIGHT) {
                        set(level, p, Blocks.DEEPSLATE_BRICKS);
                    } else if (wall) {
                        // walls: deepslate bricks with crying-obsidian ribs
                        set(level, p, dy % 3 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.DEEPSLATE_BRICKS);
                    } else {
                        set(level, p, Blocks.AIR);
                    }
                }
            }
        }
        // inner pillars with glowstone crowns
        int[][] pillars = {{8, 8}, {-8, 8}, {8, -8}, {-8, -8}};
        for (int[] pillar : pillars) {
            for (int dy = 1; dy < HEIGHT - 1; dy++) {
                set(level, c.offset(pillar[0], dy, pillar[1]),
                        dy % 3 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.DEEPSLATE_BRICKS);
            }
            set(level, c.offset(pillar[0], HEIGHT - 1, pillar[1]), Blocks.GLOWSTONE);
        }
        // glowstone chandeliers: a full ceiling grid so the vault reads clearly
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                set(level, c.offset(i * 5, HEIGHT - 1, j * 5), Blocks.GLOWSTONE);
            }
        }
        // glowstone ring lighting the arena floor
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8.0;
            set(level, c.offset((int) Math.round(Math.cos(a) * 7.0), 0,
                    (int) Math.round(Math.sin(a) * 7.0)), Blocks.GLOWSTONE);
        }
        // the titan's dais: a gold ring in the middle
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8.0;
            set(level, c.offset((int) Math.round(Math.cos(a) * 4.0), 0,
                    (int) Math.round(Math.sin(a) * 4.0)), Blocks.GOLD_BLOCK);
        }
    }
}
