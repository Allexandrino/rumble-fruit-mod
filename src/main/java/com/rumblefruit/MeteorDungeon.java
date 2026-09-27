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

    private static final int ROOM = 100; // half-size of the cavern (200x200)
    private static final int HEIGHT = 60; // the 50-block colossus must fit
    private static BlockPos center;
    private static boolean bossAwake;

    // the eight guardian crystals burn on the cavern walls at these offsets
    // from the dungeon heart; while any stands, the throne takes no damage
    public static final int[][] CRYSTAL_OFFSETS = {
            {ROOM - 1, 8, 30}, {ROOM - 1, 12, -45}, {-ROOM + 1, 10, 55}, {-ROOM + 1, 6, -25},
            {35, 9, ROOM - 1}, {-50, 13, ROOM - 1}, {25, 7, -ROOM + 1}, {-40, 11, -ROOM + 1}
    };

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

    // test seam: is a living exorcist already reigning in the realm?
    // (survives JVM restarts, unlike the static bossAwake flag)
    interface BossProbe {
        boolean alive(ServerLevel realm, BlockPos center);
    }

    interface RealmOpener {
        ServerLevel open(ServerLevel from);
    }

    interface Teleporter {
        void teleport(ServerPlayer player, ServerLevel dest, double x, double y, double z);
    }

    static FruitGrant FRUIT_GRANT = player -> {
        // the vault feeds you a random elemental fruit
        var fruits = java.util.List.of(
                RumbleFruitMod.ELECTRO_APPLE, RumbleFruitMod.INFERNO_FRUIT, RumbleFruitMod.VOID_FRUIT,
                RumbleFruitMod.FROST_FRUIT, RumbleFruitMod.NATURE_FRUIT);
        player.getInventory().add(new net.minecraft.world.item.ItemStack(
                fruits.get(new java.util.Random().nextInt(fruits.size())).get()));
    };
    static BossSpawner BOSS_SPAWNER = (level, at) -> {
        FallenExorcistEntity boss = new FallenExorcistEntity(ModEntities.FALLEN_EXORCIST.get(), level);
        // the throne is a pit: the exorcist rises from the abyss shaft
        boss.setPos(at.getX() + 0.5, at.getY() - 13.0, at.getZ() + 0.5);
        boss.setHome(at); // the pit remembers its hall — crystal wards survive restarts
        level.addFreshEntity(boss);
    };
    static BossProbe BOSS_PROBE = (realm, c) -> !realm.getEntitiesOfClass(FallenExorcistEntity.class,
            new net.minecraft.world.phys.AABB(c).inflate(ROOM * 2.0 + 10.0), e -> true).isEmpty();
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
        if (!bossAwake && !BOSS_PROBE.alive(realm, c)) {
            BOSS_SPAWNER.spawn(realm, c);
            bossAwake = true;
        } else if (!bossAwake) {
            // the old exorcist survived a restart — re-adopt its reign
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

    // test hook: simulates a JVM restart — the world persists, statics do not
    static void forgetBoss() {
        bossAwake = false;
    }

    static Vec3 returnPos(UUID playerId) {
        ReturnPoint point = RETURN.get(playerId);
        return point == null ? null : point.pos();
    }

    private static void set(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        level.setBlock(pos, block.defaultBlockState(), 3);
    }

    private static void build(ServerLevel realm, BlockPos c) {
        // the Cavern of the Exorcist: a colossal 200x200 cave — rough floor,
        // stalactites overhead, lava veins and glowing moss patches for light
        for (int dx = -ROOM; dx <= ROOM; dx++) {
            for (int dz = -ROOM; dz <= ROOM; dz++) {
                for (int dy = 0; dy <= HEIGHT; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    boolean wall = dx == -ROOM || dx == ROOM || dz == -ROOM || dz == ROOM;
                    int noise = Math.floorMod(dx * 31 + dz * 17 + dy * 13, 97);
                    if (dy == 0) {
                        // floor: blackstone with bumps and lava veins
                        if (Math.abs(dx) <= 2 || Math.abs(dz) <= 2) {
                            set(realm, p, Blocks.GOLD_BLOCK); // the arena cross
                        } else if (noise % 23 == 0) {
                            set(realm, p, Blocks.MAGMA_BLOCK); // glowing veins
                        } else {
                            set(realm, p, Blocks.BLACKSTONE);
                        }
                    } else if (dy == HEIGHT) {
                        set(realm, p, Blocks.BLACKSTONE);
                    } else if (wall) {
                        set(realm, p, dy % 4 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.BLACKSTONE);
                    } else {
                        set(realm, p, Blocks.AIR);
                    }
                }
            }
        }
        // floor bumps: scattered rubble mounds (never over the pit)
        for (int i = 0; i < 220; i++) {
            int bx = (i * 37) % (ROOM * 2) - ROOM;
            int bz = (i * 53) % (ROOM * 2) - ROOM;
            if (Math.abs(bx) <= 3 || Math.abs(bz) <= 3 || bx * bx + bz * bz <= 16 * 16) {
                continue; // keep the arena cross walkable and the pit clear
            }
            set(realm, c.offset(bx, 1, bz), Blocks.BLACKSTONE);
        }
        // stalactites hanging from the ceiling
        for (int i = 0; i < 160; i++) {
            int sx = (i * 41) % (ROOM * 2 - 8) - ROOM + 4;
            int sz = (i * 59) % (ROOM * 2 - 8) - ROOM + 4;
            int len = 3 + (i * 7) % 9;
            for (int dy = 0; dy < len; dy++) {
                set(realm, c.offset(sx, HEIGHT - 1 - dy, sz), Blocks.BLACKSTONE);
            }
        }
        // light: glowstone patches in a loose grid + glowing veins in the floor
        for (int i = -6; i <= 6; i++) {
            for (int j = -6; j <= 6; j++) {
                if ((i + j) % 2 == 0) {
                    set(realm, c.offset(i * 16, 0, j * 16), Blocks.GLOWSTONE);
                }
            }
        }
        // grand pillars holding the cavern roof
        int[][] pillars = {{50, 50}, {-50, 50}, {50, -50}, {-50, -50}, {70, 0}, {-70, 0}, {0, 70}, {0, -70}};
        for (int[] pillar : pillars) {
            for (int dy = 1; dy < HEIGHT - 1; dy++) {
                set(realm, c.offset(pillar[0], dy, pillar[1]),
                        dy % 5 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.BLACKSTONE);
            }
            set(realm, c.offset(pillar[0], HEIGHT - 1, pillar[1]), Blocks.GLOWSTONE);
        }
        // the abyss pit at the heart: a shaft into darkness, bedrock at the
        // bottom, crying-obsidian veins in the walls — the exorcist looms
        // out of it like the Icon of Sin. radius 15: the colossus's hitbox
        // (20 wide) must not brush the walls
        for (int dx = -15; dx <= 15; dx++) {
            for (int dz = -15; dz <= 15; dz++) {
                int r2 = dx * dx + dz * dz;
                if (r2 > 15 * 15) {
                    continue;
                }
                for (int dy = 0; dy <= 14; dy++) {
                    set(realm, c.offset(dx, -dy, dz), Blocks.AIR);
                }
                set(realm, c.offset(dx, -15, dz), Blocks.BEDROCK);
                if (r2 > 13 * 13) {
                    for (int dy = 2; dy <= 14; dy += 4) {
                        set(realm, c.offset(dx, -dy, dz), Blocks.CRYING_OBSIDIAN);
                    }
                }
            }
        }
        // the ritual ring around the pit rim
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI / 18.0;
            set(realm, c.offset((int) Math.round(Math.cos(a) * 17.0), 0,
                    (int) Math.round(Math.sin(a) * 17.0)), Blocks.GOLD_BLOCK);
        }
        // guardian crystals burn on the walls: while any stands, the throne
        // takes no damage. break all eight.
        for (int[] crystal : CRYSTAL_OFFSETS) {
            set(realm, c.offset(crystal[0], crystal[1], crystal[2]), ModBlocks.GUARDIAN_CRYSTAL.get());
        }
        GuardianCrystals.arm(CRYSTAL_OFFSETS.length);
    }
}
