package com.rumblefruit.earth;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;

// procedural ancient cities: every city from cities.json gets a flattened
// plateau, a roman street grid, houses in a cultural style (roman brick,
// greek marble, egyptian sandstone, ottoman stone), a central forum with a
// columned temple and a city wall with four gates.
// the pyramids of Giza stand at their real coordinates.
// everything is per-column deterministic, so chunks stitch seamlessly
public final class EarthCities {

    // palettes: 0 roman, 1 greek, 2 sandstone (egypt/levant), 3 ottoman
    public record City(String id, int cx, int cz, int radius, int palette, int baseY) {}

    private static volatile List<City> cities;

    private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED_BRICKS = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    private static final BlockState BRICKS = Blocks.BRICKS.defaultBlockState();
    private static final BlockState MOSSY_BRICKS = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    private static final BlockState QUARTZ = Blocks.QUARTZ_BLOCK.defaultBlockState();
    private static final BlockState QUARTZ_PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
    private static final BlockState QUARTZ_SLAB = Blocks.QUARTZ_SLAB.defaultBlockState();
    private static final BlockState SANDSTONE = Blocks.SANDSTONE.defaultBlockState();
    private static final BlockState SMOOTH_SANDSTONE = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
    private static final BlockState SANDSTONE_SLAB = Blocks.SANDSTONE_SLAB.defaultBlockState();
    private static final BlockState BRICK_SLAB = Blocks.BRICK_SLAB.defaultBlockState();
    private static final BlockState STONE_BRICK_SLAB = Blocks.STONE_BRICK_SLAB.defaultBlockState();
    private static final BlockState OAK_PLANKS = Blocks.OAK_PLANKS.defaultBlockState();
    private static final BlockState SPRUCE_PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState SMOOTH_QUARTZ = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState DIRT_PATH = Blocks.DIRT_PATH.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();

    // пирамиды Гизы: центр + смещения (в блоках), половина основания, высота
    private static final int GIZA_X = 9300, GIZA_Z = -8994;
    private static final int[][] PYRAMIDS = {
            {GIZA_X, GIZA_Z, 30, 34},
            {GIZA_X + 75, GIZA_Z + 35, 21, 24},
            {GIZA_X - 65, GIZA_Z + 75, 13, 15},
    };
    private static final int GIZA_FLAT_RADIUS = 130;
    private static volatile int gizaBaseY = Integer.MIN_VALUE;

    private EarthCities() {}

    private static int hash(int a, int b, int c) {
        int h = a * 374761393 + b * 668265263 + c * 912271;
        h = (h ^ (h >> 13)) * 1274126177;
        h ^= h >> 16;
        return h & 0x7FFFFFFF;
    }

    public static List<City> cities() {
        if (cities == null) {
            synchronized (EarthCities.class) {
                if (cities == null) {
                    List<City> list = new ArrayList<>();
                    for (EarthData.Place p : EarthData.places()) {
                        if (!p.city()) continue;
                        int cx = EarthData.blockFromLon(p.lon());
                        int cz = EarthData.blockFromLat(p.lat());
                        int radius = switch (p.id()) {
                            case "rome" -> 120;
                            case "alexandria", "byzantium", "carthage", "cairo" -> 90;
                            default -> 60;
                        };
                        int palette = switch (p.id()) {
                            case "athens", "sparta", "thebes", "corinth", "olympia",
                                 "delphi", "ephesus", "pergamum", "syracuse", "thessaloniki" -> 1;
                            case "alexandria", "memphis", "cairo", "jerusalem", "tyre" -> 2;
                            case "byzantium", "edirne", "bursa" -> 3;
                            default -> 0;
                        };
                        int baseY = Math.max(EarthData.SEA_LEVEL + 1,
                                EarthData.surfaceHeight(cx, cz));
                        list.add(new City(p.id(), cx, cz, radius, palette, baseY));
                    }
                    cities = list;
                }
            }
        }
        return cities;
    }

    private static int gizaBase() {
        if (gizaBaseY == Integer.MIN_VALUE) {
            synchronized (EarthCities.class) {
                if (gizaBaseY == Integer.MIN_VALUE) {
                    gizaBaseY = Math.max(EarthData.SEA_LEVEL + 1,
                            EarthData.surfaceHeight(GIZA_X, GIZA_Z));
                }
            }
        }
        return gizaBaseY;
    }

    // ---- terrain ----

    // city plateaus + giza plateau blended into the raw heightmap
    public static double terrain(int x, int z, double base) {
        double dGiza = Math.hypot(x - GIZA_X, z - GIZA_Z);
        if (dGiza < GIZA_FLAT_RADIUS + 40) {
            double t = dGiza <= GIZA_FLAT_RADIUS - 20 ? 1.0
                    : smoothstep((GIZA_FLAT_RADIUS + 40 - dGiza) / 60.0);
            base = base + (gizaBase() - base) * t;
        }
        for (City c : cities()) {
            double d = Math.hypot(x - c.cx(), z - c.cz());
            if (d >= c.radius() + 40) continue;
            double t = d <= c.radius() - 25 ? 1.0
                    : smoothstep((c.radius() + 40 - d) / 65.0);
            base = base + (c.baseY() - base) * t;
        }
        return base;
    }

    private static double smoothstep(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    public static boolean insideCity(int x, int z) {
        for (City c : cities()) {
            double d = Math.hypot(x - c.cx(), z - c.cz());
            if (d < c.radius() + 10) return true;
        }
        return false;
    }

    // ---- layout ----

    private static final int KIND_NONE = 0;
    private static final int KIND_STREET = 1;
    private static final int KIND_FORUM = 2;
    private static final int KIND_WALL = 3;
    private static final int KIND_HOUSE_WALL = 4;
    private static final int KIND_HOUSE_IN = 5;
    private static final int KIND_LOT = 6;

    private static City cityAt(int x, int z) {
        City best = null;
        double bestD = Double.MAX_VALUE;
        for (City c : cities()) {
            double d = Math.hypot(x - c.cx(), z - c.cz());
            if (d < c.radius() + 12 && d < bestD) {
                bestD = d;
                best = c;
            }
        }
        return best;
    }

    // что за городская колонна; параметры дома уходят в out
    // (out[0]=cellX, out[1]=cellZ, out[2]=bx, out[3]=bz)
    private static int kind(City c, int x, int z, int[] out) {
        // пирамиды — без городской застройки (зона каждой пирамиды своя,
        // чтобы не стереть соседний Мемфис)
        for (int[] p : PYRAMIDS) {
            if (Math.max(Math.abs(x - p[0]), Math.abs(z - p[1])) <= p[2] + 8) {
                return KIND_NONE;
            }
        }
        int lx = x - c.cx();
        int lz = z - c.cz();
        double d = Math.hypot(lx, lz);
        int r = c.radius();
        // городская стена с воротами по сторонам света; там, где стену
        // пересекает римская дорога, проезд остаётся свободным
        if (d >= r - 9 && d <= r - 6) {
            boolean gate = Math.abs(lx) <= 4 || Math.abs(lz) <= 4
                    || EarthRoads.isRoad(x, z);
            return gate ? KIND_STREET : KIND_WALL;
        }
        if (d > r - 14) return KIND_NONE;
        // форум в центре
        if (Math.abs(lx) <= 16 && Math.abs(lz) <= 16) return KIND_FORUM;
        // улицы каждые 24 блока, ширина 3
        int sx = Math.floorMod(lx, 24);
        int sz = Math.floorMod(lz, 24);
        if (sx <= 2 || sz <= 2) return KIND_STREET;
        // кварталы 24x24, дом 14x14 внутри
        int cellX = Math.floorDiv(lx - 3, 24);
        int cellZ = Math.floorDiv(lz - 3, 24);
        int bx = Math.floorMod(lx - 3, 24);
        int bz = Math.floorMod(lz - 3, 24);
        if (bx < 4 || bx > 17 || bz < 4 || bz > 17) return KIND_LOT;
        if (hash(c.cx(), cellX, cellZ) % 5 == 0) return KIND_LOT; // пустырь-сад
        if (out != null) {
            out[0] = cellX;
            out[1] = cellZ;
            out[2] = bx;
            out[3] = bz;
        }
        if (bx == 4 || bx == 17 || bz == 4 || bz == 17) return KIND_HOUSE_WALL;
        return KIND_HOUSE_IN;
    }

    private static BlockState wallBlock(City c, int x, int z) {
        int h = hash(x, z, c.palette());
        return switch (c.palette()) {
            case 1 -> QUARTZ;
            case 2 -> (h & 3) == 0 ? SMOOTH_SANDSTONE : SANDSTONE;
            case 3 -> (h & 3) == 0 ? MOSSY_BRICKS : STONE_BRICKS;
            default -> (h & 3) == 0 ? BRICKS : STONE_BRICKS;
        };
    }

    private static BlockState roofBlock(City c) {
        return switch (c.palette()) {
            case 1 -> BRICK_SLAB;
            case 2 -> SANDSTONE_SLAB;
            case 3 -> STONE_BRICK_SLAB;
            default -> BRICK_SLAB;
        };
    }

    private static BlockState floorBlock(City c) {
        return switch (c.palette()) {
            case 1 -> SMOOTH_QUARTZ;
            case 2 -> SMOOTH_SANDSTONE;
            case 3 -> SPRUCE_PLANKS;
            default -> OAK_PLANKS;
        };
    }

    // top block override for the terrain surface inside cities, or null
    public static BlockState surfaceTop(int x, int z, int h) {
        City c = cityAt(x, z);
        if (c == null) return null;
        int kind = kind(c, x, z, null);
        return switch (kind) {
            case KIND_STREET -> {
                int lx = x - c.cx(), lz = z - c.cz();
                boolean center = Math.floorMod(lx, 24) == 1 || Math.floorMod(lz, 24) == 1;
                if (center) yield STONE_BRICKS;
                yield ((hash(x, z, 7) & 3) == 0) ? GRAVEL : DIRT_PATH;
            }
            case KIND_FORUM -> ((hash(x, z, 13) & 7) == 0) ? CRACKED_BRICKS : STONE_BRICKS;
            case KIND_HOUSE_IN -> floorBlock(c);
            default -> null;
        };
    }

    // отладка: что генератор думает про колонну
    public static String debugKind(int x, int z) {
        String road = EarthRoads.debugInfo(x, z);
        City c = cityAt(x, z);
        if (c == null) return "нет города; " + road;
        int lx = x - c.cx(), lz = z - c.cz();
        double d = Math.hypot(lx, lz);
        return c.id() + " d=" + (int) d + " r=" + c.radius()
                + " kind=" + kind(c, x, z, null)
                + " " + road;
    }

    // structures above the terrain: walls, houses, temple, pyramids
    public static void buildAbove(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                  int x, int z, int h) {
        // пирамиды Гизы (от местного рельефа, не от gizaBase —
        // плато Каира частично перекрывает плато Гизы, иначе основание
        // повисало бы в воздухе)
        for (int[] p : PYRAMIDS) {
            int dd = Math.max(Math.abs(x - p[0]), Math.abs(z - p[1]));
            int half = p[2], height = p[3];
            if (dd > half) continue;
            int top = h + height - (int) Math.ceil(dd * (double) height / (half + 1));
            for (int y = h + 1; y <= top; y++) {
                chunk.setBlockState(pos.set(x, y, z),
                        ((x + y + z) & 7) == 0 ? SMOOTH_SANDSTONE : SANDSTONE, false);
            }
            return;
        }

        City c = cityAt(x, z);
        if (c == null) return;
        int[] out = new int[4];
        int kind = kind(c, x, z, out);
        int lx = x - c.cx(), lz = z - c.cz();

        if (kind == KIND_WALL) {
            for (int y = h + 1; y <= h + 6; y++) {
                chunk.setBlockState(pos.set(x, y, z), STONE_BRICKS, false);
            }
            if (((x + z) & 1) == 0) {
                chunk.setBlockState(pos.set(x, h + 7, z), STONE_BRICKS, false);
            }
            return;
        }

        if (kind == KIND_FORUM) {
            buildForum(chunk, pos, c, x, z, h, lx, lz);
            return;
        }

        if (kind == KIND_HOUSE_WALL || kind == KIND_HOUSE_IN) {
            int cellX = out[0], cellZ = out[1], bx = out[2], bz = out[3];
            int wallH = 4 + hash(c.cx() + cellX, cellZ, 5) % 3;
            if (kind == KIND_HOUSE_WALL) {
                // дверь с южной стороны, окна на боковых стенах
                boolean door = bz == 17 && (bx == 10 || bx == 11);
                boolean window = (bx == 4 || bx == 17) && Math.floorMod(bz, 4) == 1 && wallH >= 4;
                for (int y = h + 1; y <= h + wallH; y++) {
                    if (door && y <= h + 2) continue;
                    if (window && y == h + 2) continue;
                    chunk.setBlockState(pos.set(x, y, z), wallBlock(c, x, z), false);
                }
            }
            // плоская крыша над всей коробкой дома
            chunk.setBlockState(pos.set(x, h + wallH + 1, z), roofBlock(c), false);
        }
    }

    // форум: площадь + храм с колоннадой и целлой на северной стороне
    private static void buildForum(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                   City c, int x, int z, int h, int lx, int lz) {
        boolean temple = lx >= -13 && lx <= 13 && lz >= -16 && lz <= -4;
        if (!temple) {
            // центральная колонна-монумент
            if (lx == 0 && lz == 0) {
                for (int y = h + 1; y <= h + 9; y++) {
                    chunk.setBlockState(pos.set(x, y, z), QUARTZ_PILLAR, false);
                }
                chunk.setBlockState(pos.set(x, h + 10, z), QUARTZ_SLAB, false);
            }
            return;
        }
        // ступень храма
        chunk.setBlockState(pos.set(x, h + 1, z), STONE_BRICK_SLAB, false);
        boolean cella = lx >= -6 && lx <= 6 && lz >= -13 && lz <= -7;
        if (cella) {
            boolean wall = lx == -6 || lx == 6 || lz == -13 || lz == -7;
            boolean door = lz == -7 && (lx == -1 || lx == 0);
            if (wall) {
                for (int y = h + 2; y <= h + 6; y++) {
                    if (door && y <= h + 3) continue;
                    chunk.setBlockState(pos.set(x, y, z), QUARTZ, false);
                }
            }
            chunk.setBlockState(pos.set(x, h + 7, z), QUARTZ_SLAB, false);
            return;
        }
        // колоннада по периметру храма
        boolean edge = lx == -13 || lx == 13 || lz == -16 || lz == -4;
        if (edge && Math.floorMod(lx + lz, 3) == 0) {
            for (int y = h + 2; y <= h + 8; y++) {
                chunk.setBlockState(pos.set(x, y, z), QUARTZ_PILLAR, false);
            }
            chunk.setBlockState(pos.set(x, h + 9, z), QUARTZ_SLAB, false);
        }
    }
}
