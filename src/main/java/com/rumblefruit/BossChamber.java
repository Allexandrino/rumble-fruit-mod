package com.rumblefruit;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

// the Chambers of the Fallen Exorcist: a floating sky temple, built once per world.
// the boss map teleports the player here; the pedestal at the center starts the fight
public class BossChamber {

    public static BlockPos getOrCreate(ServerLevel level) {
        ChamberData data = ChamberData.get(level);
        if (data.center == null) {
            BlockPos spawn = level.getSharedSpawnPos();
            BlockPos center = new BlockPos(spawn.getX() + 1000, 140, spawn.getZ() + 1000);
            build(level, center);
            data.center = center;
            data.setDirty();
        }
        return data.center;
    }

    private static void set(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.Block block) {
        level.setBlock(pos, block.defaultBlockState(), 3);
    }

    private static void build(ServerLevel level, BlockPos c) {
        int r = 11;
        // floating disc: blackstone with gilded veins + polished X pattern
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > r) {
                    continue;
                }
                BlockPos p = c.offset(dx, 0, dz);
                if (Math.abs(dx) == Math.abs(dz) && d > 2.0) {
                    set(level, p, Blocks.POLISHED_BLACKSTONE);
                } else if ((dx * dx + dz * dz) % 7 == 0) {
                    set(level, p, Blocks.GILDED_BLACKSTONE);
                } else {
                    set(level, p, Blocks.BLACKSTONE);
                }
                // underbelly: obsidian rim, crying obsidian veins
                if (d > 10.0) {
                    set(level, p.below(), Blocks.OBSIDIAN);
                    set(level, p.below(2), Blocks.BLACKSTONE);
                } else if ((dx + dz) % 4 == 0) {
                    set(level, p.below(), Blocks.CRYING_OBSIDIAN);
                } else {
                    set(level, p.below(), Blocks.BLACKSTONE);
                }
            }
        }
        // gold ring on the edge
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI * 2.0 / 48.0;
            set(level, c.offset((int) Math.round(Math.cos(a) * 10.0), 0, (int) Math.round(Math.sin(a) * 10.0)),
                    Blocks.GOLD_BLOCK);
        }
        // glowing amethyst clusters scattered on the surface + hanging below
        int[][] crystals = {{4, 5}, {-6, 3}, {5, -6}, {-3, -5}, {7, 1}, {-8, -2}};
        for (int[] pos : crystals) {
            set(level, c.offset(pos[0], 1, pos[1]), Blocks.AMETHYST_CLUSTER);
            set(level, c.offset(pos[0], -3, pos[1]), Blocks.AMETHYST_CLUSTER);
        }
        // four corner pillars: blackstone with crying-obsidian bands, gold cap, lightning rod
        int[][] corners = {{8, 8}, {-8, 8}, {8, -8}, {-8, -8}};
        for (int[] corner : corners) {
            for (int y = 1; y <= 8; y++) {
                set(level, c.offset(corner[0], y, corner[1]),
                        y == 8 ? Blocks.GOLD_BLOCK : y % 3 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.POLISHED_BLACKSTONE);
            }
            set(level, c.offset(corner[0], 9, corner[1]), Blocks.LIGHTNING_ROD);
            // chain + soul lantern hanging on the inner side of each pillar
            int inwardX = corner[0] > 0 ? -1 : 1;
            int inwardZ = corner[1] > 0 ? -1 : 1;
            set(level, c.offset(corner[0] + inwardX, 7, corner[1] + inwardZ), Blocks.CHAIN);
            set(level, c.offset(corner[0] + inwardX, 6, corner[1] + inwardZ), Blocks.SOUL_LANTERN);
        }
        // red carpet (alastor style) from the north edge to the dais
        for (int z = -9; z <= -2; z++) {
            set(level, c.offset(0, 0, z), Blocks.RED_NETHER_BRICKS);
        }
        // throne at the south end: quartz + gold
        set(level, c.offset(0, 1, 9), Blocks.QUARTZ_STAIRS);
        set(level, c.offset(-1, 1, 9), Blocks.GOLD_BLOCK);
        set(level, c.offset(1, 1, 9), Blocks.GOLD_BLOCK);
        set(level, c.offset(-1, 2, 10), Blocks.QUARTZ_BLOCK);
        set(level, c.offset(0, 2, 10), Blocks.QUARTZ_BLOCK);
        set(level, c.offset(1, 2, 10), Blocks.QUARTZ_BLOCK);
        set(level, c.offset(0, 3, 10), Blocks.GOLD_BLOCK);
        // central dais: 3x3 gold base + the boss pedestal
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                set(level, c.offset(dx, 1, dz), Blocks.GOLD_BLOCK);
            }
        }
        set(level, c.above(2), ModBlocks.BOSS_PEDESTAL.get());
        // eight end-rod candles around the dais
        int[][] candles = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}, {2, 2}, {-2, 2}, {2, -2}, {-2, -2}};
        for (int[] candle : candles) {
            set(level, c.offset(candle[0], 1, candle[1]), Blocks.END_ROD);
        }
    }

    // per-world storage for the chamber location
    private static class ChamberData extends SavedData {
        private BlockPos center;

        private static ChamberData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(
                    new Factory<>(ChamberData::new, ChamberData::load, null), "rumblefruit_chamber");
        }

        private static ChamberData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
            ChamberData data = new ChamberData();
            if (tag.contains("cx")) {
                data.center = new BlockPos(tag.getInt("cx"), tag.getInt("cy"), tag.getInt("cz"));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
            if (center != null) {
                tag.putInt("cx", center.getX());
                tag.putInt("cy", center.getY());
                tag.putInt("cz", center.getZ());
            }
            return tag;
        }
    }
}
