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

    // ---- сельская местность: хутора, рощи, руины — по всей карте ----

    // хутор в ячейке 700 м: [x, z] или null
    private static int[] farmsteadAt(int x, int z, int h) {
        if (h < EarthData.SEA_LEVEL + 2 || h > 250) return null;
        int cellX = Math.floorDiv(x, 700), cellZ = Math.floorDiv(z, 700);
        int r = hash(cellX, cellZ, 701) % 100;
        if (r >= 38) return null;
        int fx = cellX * 700 + 200 + hash(cellX, cellZ, 702) % 300;
        int fz = cellZ * 700 + 200 + hash(cellX, cellZ, 703) % 300;
        if (Math.abs(x - fx) > 40 || Math.abs(z - fz) > 40) return null;
        if (EarthCities.insideCity(fx, fz)) return null;
        if (villageAt(fx, fz) != null) return null;
        return new int[]{fx, fz};
    }

    // руины: редкие обломки колонн в ячейке 900 м
    private static int[] ruinAt(int x, int z) {
        int cellX = Math.floorDiv(x, 900), cellZ = Math.floorDiv(z, 900);
        if (hash(cellX, cellZ, 801) % 100 >= 12) return null;
        int rx = cellX * 900 + 300 + hash(cellX, cellZ, 802) % 300;
        int rz = cellZ * 900 + 300 + hash(cellX, cellZ, 803) % 300;
        if (Math.abs(x - rx) > 8 || Math.abs(z - rz) > 8) return null;
        if (EarthCities.insideCity(rx, rz)) return null;
        return new int[]{rx, rz};
    }

    // оливковые рощи и виноградники пятнами в средиземноморском поясе
    private static double groveNoise(int x, int z) {
        double fx = (double) x / 240, fz = (double) z / 240;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = fx - x0, tz = fz - z0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double n00 = nhash(x0, z0), n10 = nhash(x0 + 1, z0);
        double n01 = nhash(x0, z0 + 1), n11 = nhash(x0 + 1, z0 + 1);
        return (n00 * (1 - tx) + n10 * tx) * (1 - tz) + (n01 * (1 - tx) + n11 * tx) * tz;
    }

    private static double nhash(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h ^= h >> 16;
        return ((h & 0xFFFF) / 32767.5) - 1.0;
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
        if (v != null) {
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
        // поле хутора
        int[] fs = farmsteadAt(x, z, 64);
        if (fs != null) {
            int dx = x - fs[0], dz = z - fs[1];
            if (dx >= 7 && dx <= 24 && Math.abs(dz) <= 4) {
                if (Math.floorMod(dx - 7, 4) == 0) return WATER;
                return FARMLAND;
            }
        }
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

        // руины: обломки колонн и щебень
        int[] ruin = ruinAt(x, z);
        if (ruin != null) {
            int rdx = x - ruin[0], rdz = z - ruin[1];
            if (Math.floorMod(rdx * 7 + rdz * 11, 13) == 0) {
                int hh = 2 + hash(rdx, rdz, 810) % 5;
                for (int y = h + 1; y <= h + hh; y++)
                    chunk.setBlockState(pos.set(x, y, z), QUARTZ, false);
            } else if (Math.floorMod(rdx * 5 + rdz * 3, 7) == 0) {
                chunk.setBlockState(pos.set(x, h + 1, z), Blocks.QUARTZ_SLAB.defaultBlockState(), false);
            }
            return true;
        }

        // хутор: домик и полоса поля
        int[] fs = farmsteadAt(x, z, h);
        if (fs != null && buildFarmstead(chunk, pos, fs, x, z, h)) return true;

        // деревня раньше рощ: поселения важнее деревьев
        int[] v = villageAt(x, z);
        if (v != null && buildVillage(chunk, pos, v, x, z, h)) return true;

        // рощи и виноградники пятнами пояса Средиземноморья
        double lat = EarthData.latFromBlock(z);
        if (lat > 33.5 && lat < 46.5 && h > EarthData.SEA_LEVEL + 3 && h < 200
                && !EarthCities.insideCity(x, z)) {
            double gn = groveNoise(x, z);
            if (gn > 0.35) {
                int gx = Math.floorMod(x, 6), gz = Math.floorMod(z, 6);
                if (gn > 0.55) { // виноград шпалерами
                    if (gz == 0 && gx % 3 == 0) {
                        chunk.setBlockState(pos.set(x, h + 1, z), FENCE, false);
                        chunk.setBlockState(pos.set(x, h + 2, z), LEAVES, false);
                        return true;
                    }
                } else { // оливы
                    if (gx == 0 && gz == 0) {
                        for (int y = h + 1; y <= h + 3; y++)
                            chunk.setBlockState(pos.set(x, y, z), LOG, false);
                        return true;
                    }
                    if (gx <= 2 && gz <= 2) {
                        chunk.setBlockState(pos.set(x, h + 4, z), LEAVES, false);
                        return true;
                    }
                }
            }
            // пинии-зонтики пятнами (Италия/Греция)
            double pn = groveNoise(x + 5000, z - 3000);
            if (pn > 0.62 && h < 150) {
                int px = Math.floorMod(x, 9), pz = Math.floorMod(z, 9);
                if (px == 0 && pz == 0) { // ствол
                    for (int y = h + 1; y <= h + 5; y++)
                        chunk.setBlockState(pos.set(x, y, z), LOG, false);
                    return true;
                }
                // зонтичная крона
                if (px <= 4 && pz <= 4) {
                    chunk.setBlockState(pos.set(x, h + 6, z), LEAVES, false);
                    if (px >= 1 && px <= 3 && pz >= 1 && pz <= 3)
                        chunk.setBlockState(pos.set(x, h + 7, z), LEAVES, false);
                    return true;
                }
            }
        }
        // пальмы у египетского/африканского побережья
        if (lat < 33.5 && h > EarthData.SEA_LEVEL + 1 && h < 90 && !EarthCities.insideCity(x, z)) {
            double pn = groveNoise(x - 7000, z + 9000);
            if (pn > 0.5) {
                int px = Math.floorMod(x, 7), pz = Math.floorMod(z, 7);
                if (px == 0 && pz == 0) {
                    for (int y = h + 1; y <= h + 6; y++)
                        chunk.setBlockState(pos.set(x, y, z), Blocks.JUNGLE_LOG.defaultBlockState(), false);
                    return true;
                }
                if ((px <= 2 && pz == 0) || (pz <= 2 && px == 0)) { // веер листьев
                    chunk.setBlockState(pos.set(x, h + 7, z), LEAVES, false);
                    return true;
                }
            }
        }
        // кипарисовые аллеи вдоль римских дорог
        if (h > EarthData.SEA_LEVEL + 1 && !EarthCities.insideCity(x, z)) {
            double rd = EarthRoads.roadDistance(x, z);
            if (rd > 5.5 && rd < 8.5 && Math.floorMod(x * 3 + z * 5, 18) == 0) {
                for (int y = h + 1; y <= h + 7; y++)
                    chunk.setBlockState(pos.set(x, y, z), Blocks.SPRUCE_LOG.defaultBlockState(), false);
                for (int y = h + 3; y <= h + 8; y++)
                    chunk.setBlockState(pos.set(x, y, z), Blocks.SPRUCE_LEAVES.defaultBlockState(), false);
                return true;
            }
        }
        return false;
    }

    // деревня: колодец, дома, загон, поля, фонари
    private static boolean buildVillage(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                        int[] v, int x, int z, int h) {

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

        // загон: ограда, сено и скот
        if (penAt(v, x, z)) {
            int px = v[0] + 20 - hash(v[0], v[1], 51) % 40;
            int pz = v[1] + 20 - hash(v[0], v[1], 53) % 40;
            boolean edge = x == px || x == px + 9 || z == pz || z == pz + 7;
            boolean gate = z == pz + 7 && (x == px + 4 || x == px + 5);
            if (edge && !gate) chunk.setBlockState(pos.set(x, h + 1, z), FENCE, false);
            if (!edge && Math.floorMod(x * 5 + z * 3, 17) == 0) {
                chunk.setBlockState(pos.set(x, h + 1, z), HAY, false);
            }
            // скот в загоне
            if (x == px + 3 && z == pz + 3) {
                EarthCities.spawnEntity(chunk, x, h + 1, z,
                        hash(v[0], v[1], 66) % 2 == 0 ? "minecraft:sheep" : "minecraft:cow", null);
            }
            if (x == px + 6 && z == pz + 4) {
                EarthCities.spawnEntity(chunk, x, h + 1, z, "minecraft:pig", null);
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

    // хутор: дом 9x7 с двускатной крышей и полоса поля 18x9 рядом
    private static boolean buildFarmstead(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                          int[] fs, int x, int z, int h) {
        int dx = x - fs[0], dz = z - fs[1];
        // дом
        if (dx >= -4 && dx <= 4 && dz >= -3 && dz <= 3) {
            boolean wall = dx == -4 || dx == 4 || dz == -3 || dz == 3;
            BlockState wb = hash(fs[0], fs[1], 71) % 2 == 0 ? SANDSTONE : BRICKS;
            if (wall) {
                boolean door = dx == 4 && dz == 0;
                boolean window = !door && Math.floorMod(dx + dz, 3) == 1;
                for (int y = h + 1; y <= h + 4; y++) {
                    if (door && y <= h + 2) continue;
                    if (window && y == h + 2) continue;
                    chunk.setBlockState(pos.set(x, y, z), wb, false);
                }
            }
            int rise = Math.max(0, 3 - Math.abs(dz));
            chunk.setBlockState(pos.set(x, h + 5 + rise, z),
                    hash(fs[0], fs[1], 72) % 3 == 0 ? HAY : RED, false);
            if (dx == 0 && dz == 0 && hash(fs[0], fs[1], 73) % 2 == 0) {
                EarthCities.spawnVillager(chunk, x, h + 1, z, "farmer");
            }
            return true;
        }
        // поле рядом
        if (dx >= 7 && dx <= 24 && Math.abs(dz) <= 4) {
            if (Math.floorMod(dx - 7, 4) == 0) {
                chunk.setBlockState(pos.set(x, h, z), WATER, false);
            } else if (Math.floorMod(dx + dz, 3) != 0) {
                chunk.setBlockState(pos.set(x, h + 1, z), WHEAT, false);
            }
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
        // жилец почти в каждом доме: фермер/пастух/лучник
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
        if (x == mx && z == mz && hash(x0, z0, 63) % 5 != 4) {
            String prof = switch (hash(x0, z0, 64) % 3) {
                case 0 -> "farmer"; case 1 -> "shepherd"; default -> "fletcher"; };
            EarthCities.spawnVillager(chunk, x, h + 1, z, prof);
        }
        // куры у домов
        if (x == x0 + 1 && z == z0 + 1 && hash(x0, z0, 65) % 2 == 0) {
            EarthCities.spawnEntity(chunk, x, h + 1, z, "minecraft:chicken", null);
        }
        // дворовый кот у части домов
        if (x == x1 - 1 && z == z1 - 1 && hash(x0, z0, 67) % 3 == 0) {
            EarthCities.spawnEntity(chunk, x, h + 1, z, "minecraft:cat", null);
        }
    }
}
