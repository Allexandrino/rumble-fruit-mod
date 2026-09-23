package com.rumblefruit;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// the meteor dungeon: the Fallen Exorcist's colossal chambers in the exorcist
// realm dimension, built once per world. the meteor core drags you in; the
// 50-block Fallen Exorcist guards the exit — beat it and everyone inside is
// teleported back to where they came from
public class MeteorDungeon {

    private MeteorDungeon() {
    }

    private static final int ROOM = 40;  // half-size of the square hall
    private static final int HEIGHT = 60; // the 50-block colossus must fit
    private static BlockPos center;
    private static boolean bossAwake;

    private record ReturnPoint(ServerLevel level, Vec3 pos) {
    }

    private static final Map<UUID, ReturnPoint> RETURN = new ConcurrentHashMap<>();

    // test seams: fruit handover, titan summon, realm access, cross-dim travel
    interface FruitGrant {
        void give(ServerPlayer player);
    }

    interface BossSpawner {
        void spawn(ServerLevel level, BlockPos at);
    }

    interface RealmOpener {
        ServerLevel open(ServerLevel from);
    }

    interface Teleporter {
        void teleport(ServerPlayer player, ServerLevel dest, double x, double y, double z);
    }

    static FruitGrant FRUIT_GRANT = player -> player.getInventory().add(
            new net.minecraft.world.item.ItemStack(RumbleFruitMod.ELECTRO_APPLE.get()));
    static BossSpawner BOSS_SPAWNER = (level, at) -> {
        FallenExorcistEntity boss = new FallenExorcistEntity(ModEntities.FALLEN_EXORCIST.get(), level);
        boss.setPos(at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5);
        level.addFreshEntity(boss);
    };
    static RealmOpener REALM_OPENER = from -> {
        ServerLevel realm = from.getServer().getLevel(MeteorRealm.KEY);
        getOrCreate(realm);
        return realm;
    };
    static Teleporter TELEPORTER = (player, dest, x, y, z) ->
            player.teleportTo(dest, x, y, z, player.getYRot(), player.getXRot());

    // the chambers sit at the heart of the void realm
    public static BlockPos getOrCreate(ServerLevel realm) {
        if (center == null) {
            center = new BlockPos(0, 10, 0);
            build(realm, center);
        }
        return center;
    }

    // the meteor core drags the player in: fruit in hand, exorcist awake
    public static void enter(ServerLevel level, ServerPlayer player) {
        ServerLevel realm = REALM_OPENER.open(level);
        BlockPos c = getOrCreate(realm);
        RETURN.put(player.getUUID(), new ReturnPoint(level, player.position()));
        FRUIT_GRANT.give(player);
        TELEPORTER.teleport(player, realm,
                c.getX() + 0.5, c.getY() + 1.0, c.getZ() - (ROOM - 6) + 0.5);
        if (!bossAwake) {
            BOSS_SPAWNER.spawn(realm, c);
            bossAwake = true;
        }
        realm.playSound(null, c, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,
                net.minecraft.sounds.SoundSource.HOSTILE, 2.0F, 0.6F);
        realm.sendParticles(ModParticles.ELECTRO_GLOW.get(),
                c.getX() + 0.5, c.getY() + 2.0, c.getZ() + 0.5, 100, 10.0, 5.0, 10.0, 0.1);
    }

    // the exorcist is down: everyone inside the chambers is flung back out
    public static void releaseAll(ServerLevel realm) {
        if (center == null) {
            return;
        }
        bossAwake = false;
        Vec3 roomCenter = new Vec3(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 0.5);
        for (ServerPlayer player : realm.players()) {
            if (player.position().distanceTo(roomCenter) > ROOM * 2) {
                continue;
            }
            ReturnPoint back = RETURN.remove(player.getUUID());
            if (back != null) {
                TELEPORTER.teleport(player, back.level(), back.pos().x, back.pos().y, back.pos().z);
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
        ReturnPoint point = RETURN.get(playerId);
        return point == null ? null : point.pos();
    }

    private static void set(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        level.setBlock(pos, block.defaultBlockState(), 3);
    }

    private static void build(ServerLevel realm, BlockPos c) {
        for (int dx = -ROOM; dx <= ROOM; dx++) {
            for (int dz = -ROOM; dz <= ROOM; dz++) {
                for (int dy = 0; dy <= HEIGHT; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    boolean wall = dx == -ROOM || dx == ROOM || dz == -ROOM || dz == ROOM;
                    if (dy == 0) {
                        // floor: blackstone with a wide gold arena cross
                        set(realm, p, (Math.abs(dx) <= 2 || Math.abs(dz) <= 2)
                                ? Blocks.GOLD_BLOCK : Blocks.BLACKSTONE);
                    } else if (dy == HEIGHT) {
                        set(realm, p, Blocks.BLACKSTONE);
                    } else if (wall) {
                        // walls: blackstone with crying-obsidian ribs and a gold crown
                        set(realm, p, dy == HEIGHT - 1 ? Blocks.GOLD_BLOCK
                                : dy % 3 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.BLACKSTONE);
                    } else {
                        set(realm, p, Blocks.AIR);
                    }
                }
            }
        }
        // light: a glowstone grid in the floor so the whole hall reads
        for (int i = -4; i <= 4; i++) {
            for (int j = -4; j <= 4; j++) {
                if (i % 2 == 0 && j % 2 == 0) {
                    set(realm, c.offset(i * 8, 0, j * 8), Blocks.GLOWSTONE);
                }
            }
        }
        // grand pillars with crying ribs and gold capitals
        int[][] pillars = {{24, 24}, {-24, 24}, {24, -24}, {-24, -24}};
        for (int[] pillar : pillars) {
            for (int dy = 1; dy < HEIGHT - 1; dy++) {
                set(realm, c.offset(pillar[0], dy, pillar[1]),
                        dy % 4 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.BLACKSTONE);
            }
            set(realm, c.offset(pillar[0], HEIGHT - 1, pillar[1]), Blocks.GLOWSTONE);
        }
        // the exorcist's dais: a gold ring around a glowstone heart
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12.0;
            set(realm, c.offset((int) Math.round(Math.cos(a) * 6.0), 0,
                    (int) Math.round(Math.sin(a) * 6.0)), Blocks.GOLD_BLOCK);
        }
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12.0;
            set(realm, c.offset((int) Math.round(Math.cos(a) * 9.0), 0,
                    (int) Math.round(Math.sin(a) * 9.0)), Blocks.GLOWSTONE);
        }
    }
}
