package com.rumblefruit.earth;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.core.BlockPos;

import java.util.List;

// populated countryside: roadside villages (well, unique small houses,
// wheat fields, vineyards, pens, lamps) and roman milestone pillars.
// spots are collected while roads are built; everything is per-column
public final class EarthVillages {

    private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState PLANKS = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState FENCE = Blocks.OAK_FENCE.defaultBlockState();
    private static final BlockState SLAB = Blocks.OAK_SLAB.defaultBlockState();
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState();
    private static final BlockState RED = Blocks.RED_TERRACOTTA.defaultBlockState();
    private static final BlockState SANDSTONE = Blocks.SANDSTONE.defaultBlockState();
    private static final BlockState BRICKS = Blocks.BRICKS.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState FARMLAND = Blocks.FARMLAND.defaultBlockState();
    private static final BlockState WHEAT = Blocks.WHEAT.defaultBlockState();
    private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
    private static final BlockState HAY = Blocks.HAY_BLOCK.defaultBlockState();
    private static final BlockState DIRT_PATH = Blocks.DIRT_PATH.defaultBlockState();
    private static final BlockState SMOOTH_SAND = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
    private static final BlockState QUARTZ = Blocks.QUARTZ_BLOCK.defaultBlockState();

    private static final int R = 70; // радиус деревни

    private EarthVillages() {}

    private static int hash(int a, int b, int c) {
        int h = a * 73428767 + b * 912271 + c * 334343;
        h = (h ^ (h >> 13)) * 1274126177;
        h ^= h >> 16;
        return h & 0x7FFFFFFF;
    }

    // деревня рядом с колонной или null
    private static int[] villageAt(int x, int z) {
        List<int[]> spots = EarthRoads.VILLAGE_SPOTS;
        for (int[] s : spots) {
            if (Math.abs(x - s[0]) <= R && Math.abs(z - s[1]) <= R) return s;
        }
        return null;
    }

    // дома деревни: позиции и размеры из хэша деревни
    // возвращает [x0, z0, w, d] или null
    private static int[] houseAt(int[] v, int x, int z) {
        int n = 4 + hash(v[0], v[1], 1) % 4;
        for (int i = 0; i < n; i++) {
            int hx = v[0] - 45 + hash(v[0], i, 11) % 90;
            int hz = v[1] - 45 + hash(v[1], i, 13) % 90;
            // не ближе 12 м к колодцу
            if (Math.abs(hx - v[0]) < 12 && Math.abs(hz - v[1]) < 12) continue;
            int w = 7 + hash(hx, hz, 21) % 4;
            int d = 6 + hash(hx, hz, 22) % 4;
            if (x >= hx && x < hx + w && z >= hz && z < hz + d) {
                return new int[]{hx, hz, w, d};
            }
        }
        return null;
    }

    // поле рядом с колонной или null: [x0, z0, w, d, виноград?]
    private static int[] fieldAt(int[] v, int x, int z) {
        int nf = 2 + hash(v[0], v[1], 31) % 3;
        for (int i = 0; i < nf; i++) {
            int fx = v[0] - R + hash(v[0], i, 41) % (2 * R - 30);
            int fz = v[1] - R + hash(v[1], i, 43) % (2 * R - 30);
            int w = 16 + hash(fx, fz, 45) % 14;
            int d = 12 + hash(fx, fz, 46) % 12;
            if (x >= fx && x < fx + w && z >= fz && z < fz + d) {
                return new int[]{fx, fz, w, d, hash(v[0], i, 47) % 3};
            }
        }
        return null;
    }

    // загон для скота
    private static boolean penAt(int[] v, int x, int z) {
        int px = v[0] + 20 - hash(v[0], v[1], 51) % 40;
        int pz = v[1] + 20 - hash(v[0], v[1], 53) % 40;
        return x >= px && x < px + 10 && z >= pz && z < pz + 8;
    }

    // ---- поверхность: грядки, тропинки ----

    public static BlockState surfaceTop(int x, int z) {
        int[] v = villageAt(x, z);
        if (v == null) return null;
        int[] f = fieldAt(v, x, z);
        if (f != null) {
            // ирригационные канавы каждые 4 ряда
            if (Math.floorMod(x - f[0], 4) == 0) return WATER;
            return f[4] == 0 ? FARMLAND : DIRT_PATH;
        }
        // тропинка к колодцу
        if (Math.abs(x - v[0]) <= 1 || Math.abs(z - v[1]) <= 1) return DIRT_PATH;
        return null;
    }

    // ---- постройки над рельефом ----

    public static boolean build(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                int x, int z, int h) {
        // мильные столбы: каменная колонна 4 м с плитой
        for (int[] m : EarthRoads.MILE_STONES) {
            if (x == m[0] && z == m[1]) {
                for (int y = h + 1; y <= h + 4; y++)
                    chunk.setBlockState(pos.set(x, y, z), SMOOTH_SAND, false);
                chunk.setBlockState(pos.set(x, h + 5, z), QUARTZ, false);
                return false; // не прерываем: столб стоит у обочины
            }
        }
        int[] v = villageAt(x, z);
        if (v == null) return false;

        // колодец в центре
        if (Math.abs(x - v[0]) <= 1 && Math.abs(z - v[1]) <= 1) {
            boolean ring = Math.abs(x - v[0]) == 1 || Math.abs(z - v[1]) == 1;
            chunk.setBlockState(pos.set(x, h + 1, z), ring ? STONE_BRICKS : WATER, false);
            if (ring && Math.floorMod(x + z, 2) == 0) {
                chunk.setBlockState(pos.set(x, h + 2, z), FENCE, false);
            }
            return true;
        }

        // дома
        int[] hs = houseAt(v, x, z);
        if (hs != null) {
            buildVillageHouse(chunk, pos, v, x, z, h, hs);
            return true;
        }

        // загон: ограда и сено
        if (penAt(v, x, z)) {
            int px = v[0] + 20 - hash(v[0], v[1], 51) % 40;
            int pz = v[1] + 20 - hash(v[0], v[1], 53) % 40;
            boolean edge = x == px || x == px + 9 || z == pz || z == pz + 7;
            boolean gate = z == pz + 7 && (x == px + 4 || x == px + 5);
            if (edge && !gate) chunk.setBlockState(pos.set(x, h + 1, z), FENCE, false);
            if (!edge && Math.floorMod(x * 5 + z * 3, 17) == 0) {
                chunk.setBlockState(pos.set(x, h + 1, z), HAY, false);
            }
            return true;
        }

        // поля: пшеница / виноград / оливы
        int[] f = fieldAt(v, x, z);
        if (f != null) {
            if (f[4] == 0) { // пшеница
                if (Math.floorMod(x - f[0], 4) != 0 && Math.floorMod(x + z, 3) != 0) {
                    chunk.setBlockState(pos.set(x, h + 1, z), WHEAT, false);
                }
            } else if (f[4] == 1) { // виноград: ряды шпалер
                if (Math.floorMod(x - f[0], 3) == 0 && Math.floorMod(z - f[1], 2) == 0) {
                    chunk.setBlockState(pos.set(x, h + 1, z), FENCE, false);
                    chunk.setBlockState(pos.set(x, h + 2, z), LEAVES, false);
                }
            } else { // оливковая роща
                if (Math.floorMod(x - f[0], 5) == 0 && Math.floorMod(z - f[1], 5) == 0) {
                    for (int y = h + 1; y <= h + 3; y++)
                        chunk.setBlockState(pos.set(x, y, z), LOG, false);
                }
                if (Math.floorMod(x - f[0], 5) <= 2 && Math.floorMod(z - f[1], 5) <= 2) {
                    chunk.setBlockState(pos.set(x, h + 4, z), LEAVES, false);
                }
            }
            return true;
        }

        // фонари у колодца
        if (Math.abs(x - v[0]) == 3 && Math.abs(z - v[1]) == 3) {
            chunk.setBlockState(pos.set(x, h + 1, z), FENCE, false);
            chunk.setBlockState(pos.set(x, h + 2, z), LANTERN, false);
            return true;
        }
        return false;
    }

    // маленький сельский дом: стены 4 м, дверь, окна, двускатная/плоская крыша
    private static void buildVillageHouse(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                          int[] v, int x, int z, int h, int[] hs) {
        int x0 = hs[0], z0 = hs[1], x1 = hs[0] + hs[2] - 1, z1 = hs[1] + hs[3] - 1;
        int variant = hash(x0, z0, 61);
        boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
        BlockState wallBlock = switch (variant % 3) {
            case 0 -> BRICKS;
            case 1 -> SANDSTONE;
            default -> STONE_BRICKS;
        };
        if (wall) {
            int mz = (z0 + z1) / 2;
            boolean door = x == x1 && z == mz;
            boolean window = !door && Math.floorMod(x + z, 3) == 1;
            for (int y = h + 1; y <= h + 4; y++) {
                if (door && y <= h + 2) continue;
                if (window && y == h + 2) continue;
                chunk.setBlockState(pos.set(x, y, z), wallBlock, false);
            }
        }
        // крыша: двускатная красная или плоская
        if ((variant >> 3) % 2 == 0) {
            int rise = Math.max(0, hs[3] / 2 - Math.abs(z - (z0 + z1) / 2));
            chunk.setBlockState(pos.set(x, h + 5 + rise, z), RED, false);
        } else {
            chunk.setBlockState(pos.set(x, h + 5, z), SLAB, false);
        }
        // сеновал внутри
        if (!wall && Math.floorMod(x * 7 + z * 3, 19) == 0) {
            chunk.setBlockState(pos.set(x, h + 1, z), HAY, false);
        }
    }
}
