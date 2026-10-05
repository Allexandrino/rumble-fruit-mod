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
    private static final BlockState RED_TERRACOTTA = Blocks.RED_TERRACOTTA.defaultBlockState();
    private static final BlockState TRAVERTINE =
            com.rumblefruit.ModBlocks.TRAVERTINE.get().defaultBlockState();
    private static final BlockState MARBLE =
            com.rumblefruit.ModBlocks.MARBLE.get().defaultBlockState();
    private static final BlockState MOSAIC =
            com.rumblefruit.ModBlocks.MOSAIC_TILE.get().defaultBlockState();
    private static final BlockState ROMAN_TILE =
            com.rumblefruit.ModBlocks.ROMAN_ROOF_TILE.get().defaultBlockState();
    private static final BlockState MARBLE_SLAB_LINE = QUARTZ_SLAB;

    // пирамиды Гизы в реальных размерах: Хеопс 230×139 м, Хефрен 215×136 м,
    // Микерин 105×65 м; Хефрен в 400 м к ЮЗ, Микерин в 830 м
    public static final int GIZA_X = 3455907, GIZA_Z = -3327691;
    private static final int[][] PYRAMIDS = {
            {GIZA_X, GIZA_Z, 115, 139},
            {GIZA_X - 400, GIZA_Z + 400, 107, 136},
            {GIZA_X - 830, GIZA_Z + 830, 52, 65},
    };
    private static final int GIZA_FLAT_RADIUS = 1200;
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
                            case "rome" -> 1000;
                            case "alexandria", "byzantium", "carthage", "cairo" -> 700;
                            default -> 400;
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

    // районная раскладка (Помпеи/Остия): кварталы 24-32 м, переулки 2 м,
    // обычные улицы 4 м, редкие проспекты 6 м
    // → [ширина улицы, размер квартала по x, по z]
    private static int[] districtLayout(City c, int lx, int lz) {
        int distX = Math.floorDiv(lx, 96), distZ = Math.floorDiv(lz, 96);
        int wide = hash(distX, distZ, c.cx() + 63) % 5 == 0 ? 4   // проспект 5 м
                : (hash(distX, distZ, c.cz() + 64) % 3 == 0 ? 1 : 2); // переулок 2 м / улица 3 м
        int csX = hash(distX, distZ, c.cx() + 61) % 2 == 0 ? 32 : 24;
        int csZ = hash(distX, distZ, c.cz() + 62) % 2 == 0 ? 32 : 24;
        return new int[]{wide, csX, csZ};
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
            boolean gate = Math.abs(lx) <= 6 || Math.abs(lz) <= 6
                    || EarthRoads.isRoad(x, z);
            return gate ? KIND_STREET : KIND_WALL;
        }
        if (d > r - 14) return KIND_NONE;
        // форум в центре (площадь масштабируется с городом)
        if (Math.abs(lx) <= Math.max(16, r / 20) && Math.abs(lz) <= Math.max(16, r / 28))
            return KIND_FORUM;
        // районы 96x96 м: у каждого свой ритм — кварталы 24/32 м,
        // переулки 2 м, улицы 4 м, проспекты 6 м (как в Помпеях)
        int[] dl = districtLayout(c, lx, lz);
        int wide = dl[0], csX = dl[1], csZ = dl[2];
        int sx = Math.floorMod(lx, csX);
        int sz = Math.floorMod(lz, csZ);
        if (sx <= wide || sz <= wide) return KIND_STREET;
        // кварталы, дом по спецификации из хэша лота — каждый уникален
        int cellX = Math.floorDiv(lx - wide - 1, csX);
        int cellZ = Math.floorDiv(lz - wide - 1, csZ);
        int bx = Math.floorMod(lx - wide - 1, csX);
        int bz = Math.floorMod(lz - wide - 1, csZ);
        int[] hs = houseSpec(c, cellX, cellZ, csX - wide - 1, csZ - wide - 1);
        if (hs[8] > 0) return KIND_LOT; // двор: колодец/сад/рынок/мастерская
        if (!inHouse(hs, bx, bz)) return KIND_LOT;
        if (out != null) {
            out[0] = cellX;
            out[1] = cellZ;
            out[2] = bx;
            out[3] = bz;
        }
        return isHouseWall(hs, bx, bz) ? KIND_HOUSE_WALL : KIND_HOUSE_IN;
    }

    // колонна внутри пятна дома (с учётом формы: прямоугольник/Г/атриум/портик)
    private static boolean inHouse(int[] s, int bx, int bz) {
        boolean in = bx >= s[0] && bx < s[0] + s[2] && bz >= s[1] && bz < s[1] + s[3];
        if (in && s[10] == 1) {
            // Г-образный: срезанный угол 6x6
            int cutX = (s[9] & 1) == 0 ? s[0] + s[2] - 6 : s[0];
            int cutZ = (s[9] & 2) == 0 ? s[1] + s[3] - 6 : s[1];
            if (bx >= cutX && bx < cutX + 6 && bz >= cutZ && bz < cutZ + 6) return false;
        }
        return in;
    }

    private static boolean isHouseWall(int[] s, int bx, int bz) {
        // граница пятна: сосед по любой стороне уже вне дома
        return !inHouse(s, bx - 1, bz) || !inHouse(s, bx + 1, bz)
                || !inHouse(s, bx, bz - 1) || !inHouse(s, bx, bz + 1);
    }

    // уникальная спецификация дома из хэша квартала:
    // [0-1] смещение, [2-3] ширина/глубина, [4] этажи, [5] тип крыши,
    // [6] сторона двери, [7] шаг окон, [8] тип двора (0 = жилой дом),
    // [9] декоративный вариант, [10] форма: прямоугольник/Г/атриум/портик
    private static int[] houseSpec(City c, int cellX, int cellZ, int ux, int uz) {
        int h1 = hash(c.cx(), cellX * 3 + 11, cellZ * 7 + 5);
        int h2 = hash(c.cz(), cellX ^ 991, cellZ ^ 517);
        int[] s = new int[11];
        s[0] = 1 + h1 % 3;                 // ox
        s[1] = 1 + (h1 >> 4) % 3;          // oz
        s[2] = Math.min(10 + (h1 >> 8) % 8, ux - s[0] - 1);   // w
        s[3] = Math.min(10 + (h1 >> 12) % 6, uz - s[1] - 1);  // d
        s[4] = 1 + (h2 & 1);               // этажи
        s[5] = (h2 >> 2) % 4;              // крыша: плоская/парапет/двускат/купол
        s[6] = (h2 >> 4) % 4;              // дверь: юг/север/восток/запад
        s[7] = 2 + (h2 >> 6) % 3;          // шаг окон
        s[8] = (h1 >> 16) % 5 == 0 ? 1 + (h2 >> 8) % 5 : 0; // каждый пятый — двор
        s[9] = (h2 >> 11) % 4;             // вариант отделки
        s[10] = s[2] >= 13 ? (h2 >> 13) % 4 : 0; // форма (большие дома)
        return s;
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
        BlockState structFloor = EarthStructures.surfaceTop(x, z);
        if (structFloor != null) return structFloor;
        BlockState villageFloor = EarthVillages.surfaceTop(x, z);
        if (villageFloor != null) return villageFloor;
        City c = cityAt(x, z);
        if (c == null) return null;
        int kind = kind(c, x, z, null);
        return switch (kind) {
            case KIND_STREET -> {
                int lx = x - c.cx(), lz = z - c.cz();
                int[] dl = districtLayout(c, lx, lz);
                boolean center = Math.floorMod(lx, dl[1]) == 1 || Math.floorMod(lz, dl[2]) == 1;
                if (center) yield STONE_BRICKS;
                yield ((hash(x, z, 7) & 3) == 0) ? GRAVEL : DIRT_PATH;
            }
            case KIND_FORUM -> {
                // мозаичные вставки по камню
                if ((hash(x, z, 13) & 15) == 0) yield MOSAIC;
                yield ((hash(x, z, 13) & 7) == 0) ? CRACKED_BRICKS : STONE_BRICKS;
            }
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

        // уникальные структуры (колизей, храмы, маяк, акведук и др.)
        if (EarthStructures.build(chunk, pos, x, z, h)) return;

        // деревни, фермы и мильные столбы вдоль дорог
        if (EarthVillages.build(chunk, pos, x, z, h)) return;

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
            buildHouse(chunk, pos, c, x, z, h, out, kind == KIND_HOUSE_WALL);
            return;
        }

        if (kind == KIND_STREET) {
            // уличные фонари каждые ~48 м вдоль центра улицы
            int[] dl = districtLayout(c, lx, lz);
            boolean centerLine = Math.floorMod(lx, dl[1]) == 1 || Math.floorMod(lz, dl[2]) == 1;
            if (centerLine && Math.floorMod(lx + lz, 48) == 0) {
                chunk.setBlockState(pos.set(x, h + 1, z), Blocks.OAK_FENCE.defaultBlockState(), false);
                chunk.setBlockState(pos.set(x, h + 2, z), Blocks.OAK_FENCE.defaultBlockState(), false);
                chunk.setBlockState(pos.set(x, h + 3, z), Blocks.LANTERN.defaultBlockState(), false);
                return;
            }
            // балконы вторых этажей нависают над улицей (как в Помпеях)
            int[][] nb = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] dxy : nb) {
                int[] nout = new int[4];
                if (kind(c, x + dxy[0], z + dxy[1], nout) == KIND_HOUSE_WALL) {
                    int[] nhs = houseSpec(c, nout[0], nout[1], dl[1] - dl[0] - 1, dl[2] - dl[0] - 1);
                    if (nhs[4] == 2 && hash(x, z, 91) % 2 == 0) {
                        chunk.setBlockState(pos.set(x, h + 5, z), Blocks.OAK_SLAB.defaultBlockState(), false);
                        return;
                    }
                }
            }
        }

        if (kind == KIND_LOT) {
            int[] dl = districtLayout(c, lx, lz);
            int wide = dl[0], csX = dl[1], csZ = dl[2];
            int cellX = Math.floorDiv(lx - wide - 1, csX);
            int cellZ = Math.floorDiv(lz - wide - 1, csZ);
            int[] hs = houseSpec(c, cellX, cellZ, csX - wide - 1, csZ - wide - 1);
            if (hs[8] > 0) {
                buildCourtyard(chunk, pos, c, x, z, h, hs,
                        Math.floorMod(lx - wide - 1, csX), Math.floorMod(lz - wide - 1, csZ),
                        (csX - wide - 1) / 2, (csZ - wide - 1) / 2);
            }
        }
    }

    // уникальный старинный дом: материалы по культуре и этажу, формы
    // (прямоугольник/Г/атриум с бассейном/портик), лавка у двери, жилец
    private static void buildHouse(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                   City c, int x, int z, int h, int[] out, boolean wall) {
        int cellX = out[0], cellZ = out[1], bx = out[2], bz = out[3];
        int[] dl = districtLayout(c, x - c.cx(), z - c.cz());
        int[] hs = houseSpec(c, cellX, cellZ, dl[1] - dl[0] - 1, dl[2] - dl[0] - 1);
        int wallH = hs[4] * 4;
        int x0 = hs[0], z0 = hs[1], x1 = hs[0] + hs[2] - 1, z1 = hs[1] + hs[3] - 1;
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;

        boolean door = switch (hs[6]) {
            case 0 -> bz == z1 && (bx == mx || (hs[2] >= 13 && bx == mx + 1));
            case 1 -> bz == z0 && (bx == mx || (hs[2] >= 13 && bx == mx + 1));
            case 2 -> bx == x1 && (bz == mz || (hs[3] >= 13 && bz == mz + 1));
            default -> bx == x0 && (bz == mz || (hs[3] >= 13 && bz == mz + 1));
        };
        boolean doorSide = switch (hs[6]) {
            case 0 -> bz == z1;
            case 1 -> bz == z0;
            case 2 -> bx == x1;
            default -> bx == x0;
        };

        if (wall) {
            // портик: передний фасад заменён колоннадой
            boolean portico = hs[10] == 3 && doorSide && !door
                    && Math.floorMod(bx + bz, 2) == 0;
            for (int y = h + 1; y <= h + wallH; y++) {
                if (door && y <= h + 2) continue;
                if (portico) {
                    if (y == h + wallH) {
                        chunk.setBlockState(pos.set(x, y, z), archBlock(c), false);
                    } else {
                        chunk.setBlockState(pos.set(x, y, z), columnBlock(c), false);
                    }
                    continue;
                }
                boolean winCol = !doorSide && Math.floorMod(bx + bz, hs[7]) == 1;
                // окно: проём + подоконник — читается как окно, не дыра
                if (winCol && (y == h + 2 || (hs[4] == 2 && y == h + 6))) continue;
                if (winCol && (y == h + 1 || (hs[4] == 2 && y == h + 5))) {
                    chunk.setBlockState(pos.set(x, y, z), archBlock(c), false);
                    continue;
                }
                chunk.setBlockState(pos.set(x, y, z), ancientWall(c, hs, x, z, y - h), false);
            }
        }

        // крыша — старинные материалы по культуре
        int ry = h + wallH + 1;
        BlockState roofMat = roofAncient(c, hs);
        switch (hs[5]) {
            case 0 -> chunk.setBlockState(pos.set(x, ry, z), roofMat, false);
            case 1 -> { // парапет
                chunk.setBlockState(pos.set(x, ry, z), roofMat, false);
                if (wall) chunk.setBlockState(pos.set(x, ry + 1, z), ancientWall(c, hs, x, z, wallH), false);
            }
            case 2 -> { // двускатная черепица: конёк вдоль длинной оси
                chunk.setBlockState(pos.set(x, ry, z), roofMat, false); // сплошной настил
                boolean ridgeX = hs[2] >= hs[3];
                int rise = ridgeX ? Math.max(0, hs[3] / 2 - Math.abs(bz - mz))
                        : Math.max(0, hs[2] / 2 - Math.abs(bx - mx));
                if (rise > 0) chunk.setBlockState(pos.set(x, ry + rise, z), roofMat, false);
                // фронтоны: торцевые стены закрывают торцы конька
                boolean gableEnd = ridgeX ? (bx == x0 || bx == x1) : (bz == z0 || bz == z1);
                if (gableEnd) {
                    for (int y = ry + 1; y <= ry + rise; y++) {
                        chunk.setBlockState(pos.set(x, y, z), ancientWall(c, hs, x, z, wallH), false);
                    }
                }
            }
            default -> { // ступенчатый купол
                chunk.setBlockState(pos.set(x, ry, z), roofMat, false); // сплошной настил
                int dr = Math.min(hs[2], hs[3]) / 2;
                int dd = Math.max(Math.abs(bx - mx), Math.abs(bz - mz));
                int rise = Math.max(0, dr - dd);
                if (rise > 0) chunk.setBlockState(pos.set(x, ry + rise, z), roofMat, false);
            }
        }

        // атриум: бассейн-имплювий с колоннами по углам
        if (!wall && hs[10] == 2) {
            if (Math.abs(bx - mx) <= 1 && Math.abs(bz - mz) <= 1) {
                boolean rim = Math.abs(bx - mx) == 1 || Math.abs(bz - mz) == 1;
                chunk.setBlockState(pos.set(x, h + 1, z),
                        rim ? SMOOTH_QUARTZ : Blocks.WATER.defaultBlockState(), false);
                return;
            }
            if (Math.abs(bx - mx) == 2 && Math.abs(bz - mz) == 2) {
                for (int y = h + 1; y <= h + 4; y++)
                    chunk.setBlockState(pos.set(x, y, z), columnBlock(c), false);
                return;
            }
        }

        // лавка у двери: прилавок, тент, товар, бочка
        if (hs[9] == 3) {
            buildShop(chunk, pos, c, x, z, h, hs, bx, bz, door, doorSide, mx, mz);
        }

        // мебель у внутренних колонн
        if (!wall) {
            int fh = hash(x, z, cellX + cellZ);
            if (fh % 13 == 0) { // ложе
                BlockState wool = switch (hs[9]) {
                    case 1 -> Blocks.RED_WOOL.defaultBlockState();
                    case 2 -> Blocks.BLUE_WOOL.defaultBlockState();
                    default -> Blocks.WHITE_WOOL.defaultBlockState();
                };
                chunk.setBlockState(pos.set(x, h + 1, z), wool, false);
            } else if (fh % 13 == 5) { // стол
                chunk.setBlockState(pos.set(x, h + 1, z), Blocks.OAK_FENCE.defaultBlockState(), false);
                chunk.setBlockState(pos.set(x, h + 2, z), Blocks.OAK_SLAB.defaultBlockState(), false);
            } else if (fh % 17 == 9) { // сундук
                chunk.setBlockState(pos.set(x, h + 1, z), Blocks.CHEST.defaultBlockState(), false);
            } else if (fh % 19 == 11) { // амфора
                chunk.setBlockState(pos.set(x, h + 1, z), Blocks.DECORATED_POT.defaultBlockState(), false);
            }
        }

        // жилец в центре дома: каждый третий
        if (bx == mx && bz == mz && hash(cellX, cellZ, 777) % 3 == 0) {
            String prof = hs[9] == 3
                    ? switch (hash(cellX, cellZ, 778) % 3) {
                        case 0 -> "mason"; case 1 -> "butcher"; default -> "leatherworker"; }
                    : switch (hash(cellX, cellZ, 779) % 3) {
                        case 0 -> "librarian"; case 1 -> "cleric"; default -> "nitwit"; };
            spawnVillager(chunk, x, h + 1, z, prof);
        }
    }

    // прилавок с тентом и товаром перед дверью
    private static void buildShop(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                  City c, int x, int z, int h, int[] hs,
                                  int bx, int bz, boolean door, boolean doorSide, int mx, int mz) {
        if (!doorSide) return;
        int goods = hash(mx, mz, 881) % 4;
        if (door) {
            // прилавок в проёме и товар на нём
            chunk.setBlockState(pos.set(x, h + 1, z), Blocks.OAK_SLAB.defaultBlockState(), false);
            BlockState good = switch (goods) {
                case 0 -> Blocks.MELON.defaultBlockState();
                case 1 -> Blocks.HAY_BLOCK.defaultBlockState();
                case 2 -> Blocks.PUMPKIN.defaultBlockState();
                default -> Blocks.BARREL.defaultBlockState();
            };
            chunk.setBlockState(pos.set(x, h + 2, z), good, false);
            // тент над лавкой
            chunk.setBlockState(pos.set(x, h + 4, z),
                    goods % 2 == 0 ? Blocks.RED_WOOL.defaultBlockState()
                            : Blocks.YELLOW_WOOL.defaultBlockState(), false);
        }
    }

    // старинные материалы стен: низ каменный, верх по культуре
    private static BlockState ancientWall(City c, int[] hs, int x, int z, int dy) {
        boolean corner = (hash(x, z, 61) & 3) == 0;
        return switch (c.palette()) {
            case 1 -> dy <= 2 ? SANDSTONE
                    : (dy == 3 ? MARBLE_SLAB_LINE : Blocks.WHITE_TERRACOTTA.defaultBlockState());
            case 2 -> dy <= 1 ? SANDSTONE : Blocks.MUD_BRICKS.defaultBlockState();
            case 3 -> (dy % 4 == 0) ? RED_TERRACOTTA
                    : ((hash(x, z, 62) & 3) == 0 ? MOSSY_BRICKS : STONE_BRICKS);
            default -> {
                if (dy <= 2) yield TRAVERTINE;
                if (hs[9] >= 2 && corner) yield Blocks.SPRUCE_LOG.defaultBlockState();
                yield hs[9] >= 2 ? Blocks.WHITE_TERRACOTTA.defaultBlockState() : BRICKS;
            }
        };
    }

    private static BlockState columnBlock(City c) {
        return switch (c.palette()) {
            case 1 -> QUARTZ_PILLAR;
            case 2 -> SANDSTONE;
            default -> Blocks.SPRUCE_LOG.defaultBlockState();
        };
    }

    private static BlockState archBlock(City c) {
        return switch (c.palette()) {
            case 1 -> QUARTZ_SLAB;
            case 2 -> SMOOTH_SANDSTONE;
            default -> Blocks.OAK_SLAB.defaultBlockState();
        };
    }

    private static BlockState roofAncient(City c, int[] hs) {
        if (hs[9] == 0) return Blocks.SPRUCE_SLAB.defaultBlockState(); // доски для бедных
        return switch (c.palette()) {
            case 1 -> BRICK_SLAB;
            case 2 -> Blocks.MUD_BRICK_SLAB.defaultBlockState();
            case 3 -> RED_TERRACOTTA;
            default -> ROMAN_TILE; // римская черепица
        };
    }

    // сущность через NBT проточанка (жители, скот, птица)
    static void spawnEntity(ChunkAccess chunk, int x, int y, int z, String id,
                            net.minecraft.nbt.CompoundTag extra) {
        if (!(chunk instanceof net.minecraft.world.level.chunk.ProtoChunk proto)) return;
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", id);
        net.minecraft.nbt.ListTag posTag = new net.minecraft.nbt.ListTag();
        posTag.add(net.minecraft.nbt.DoubleTag.valueOf(x + 0.5));
        posTag.add(net.minecraft.nbt.DoubleTag.valueOf(y));
        posTag.add(net.minecraft.nbt.DoubleTag.valueOf(z + 0.5));
        tag.put("Pos", posTag);
        tag.putBoolean("PersistenceRequired", true);
        if (extra != null) tag.put("VillagerData", extra);
        proto.addEntity(tag);
    }

    // жилец-деревенщина с профессией через NBT проточанка
    static void spawnVillager(ChunkAccess chunk, int x, int y, int z, String profession) {
        net.minecraft.nbt.CompoundTag vd = new net.minecraft.nbt.CompoundTag();
        vd.putString("profession", "minecraft:" + profession);
        vd.putString("type", "minecraft:plains");
        vd.putInt("level", 2);
        spawnEntity(chunk, x, y, z, "minecraft:villager", vd);
    }

    // двор вместо дома: колодец, сад, рынок, мастерская, олива
    private static void buildCourtyard(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                       City c, int x, int z, int h, int[] hs,
                                       int bx, int bz, int cx0, int cz0) {
        // хозяин двора с профессией
        if (bx == cx0 && bz == cz0) {
            String prof = switch (hs[8]) {
                case 2 -> "farmer";
                case 3 -> (hash(x, z, 91) & 1) == 0 ? "butcher" : "leatherworker";
                case 4 -> switch (hash(x, z, 92) % 3) {
                    case 0 -> "toolsmith"; case 1 -> "weaponsmith"; default -> "armorer"; };
                default -> "shepherd";
            };
            if (hs[8] != 1) spawnVillager(chunk, x, h + 1, z, prof);
        }
        switch (hs[8]) {
            case 1 -> { // колодец с навесом
                if (Math.abs(bx - cx0) <= 1 && Math.abs(bz - cz0) <= 1) {
                    boolean ring = Math.abs(bx - cx0) == 1 || Math.abs(bz - cz0) == 1;
                    chunk.setBlockState(pos.set(x, h + 1, z),
                            ring ? STONE_BRICKS : Blocks.WATER.defaultBlockState(), false);
                }
                if ((bx == cx0 - 1 || bx == cx0 + 1) && bz == cz0 - 1) {
                    for (int y = h + 2; y <= h + 4; y++)
                        chunk.setBlockState(pos.set(x, y, z), Blocks.OAK_FENCE.defaultBlockState(), false);
                }
                if (Math.abs(bx - cx0) <= 1 && bz >= cz0 - 1 && bz <= cz0) {
                    chunk.setBlockState(pos.set(x, h + 5, z), Blocks.OAK_SLAB.defaultBlockState(), false);
                }
            }
            case 2 -> { // сад: цветы и изгородь
                if ((bx == 4 || bx == 19 || bz == 4 || bz == 19) && Math.floorMod(bx + bz, 2) == 0) {
                    chunk.setBlockState(pos.set(x, h + 1, z), Blocks.OAK_LEAVES.defaultBlockState(), false);
                } else if (Math.floorMod(bx * 3 + bz * 7, 11) == 0) {
                    chunk.setBlockState(pos.set(x, h + 1, z),
                            hash(x, z, 44) % 2 == 0 ? Blocks.POPPY.defaultBlockState()
                                    : Blocks.DANDELION.defaultBlockState(), false);
                }
            }
            case 3 -> { // рыночный лоток с тентом
                if (Math.abs(bx - cx0) <= 2 && bz == cz0) {
                    chunk.setBlockState(pos.set(x, h + 1, z), Blocks.OAK_PLANKS.defaultBlockState(), false);
                }
                if ((Math.abs(bx - cx0) == 2) && (bz == cz0 - 1 || bz == cz0 + 1)) {
                    for (int y = h + 1; y <= h + 3; y++)
                        chunk.setBlockState(pos.set(x, y, z), Blocks.OAK_FENCE.defaultBlockState(), false);
                }
                if (Math.abs(bx - cx0) <= 2 && Math.abs(bz - cz0) <= 1) {
                    chunk.setBlockState(pos.set(x, h + 4, z),
                            hs[9] % 2 == 0 ? Blocks.RED_WOOL.defaultBlockState()
                                    : Blocks.YELLOW_WOOL.defaultBlockState(), false);
                }
            }
            case 4 -> { // мастерская
                if (bx == cx0 && bz == cz0)
                    chunk.setBlockState(pos.set(x, h + 1, z), Blocks.ANVIL.defaultBlockState(), false);
                if (bx == cx0 + 2 && bz == cz0 - 1)
                    chunk.setBlockState(pos.set(x, h + 1, z), Blocks.CAULDRON.defaultBlockState(), false);
                if (bx == cx0 - 2 && bz == cz0 + 1)
                    chunk.setBlockState(pos.set(x, h + 1, z), Blocks.STONECUTTER.defaultBlockState(), false);
                if (Math.floorMod(bx + bz, 9) == 0)
                    chunk.setBlockState(pos.set(x, h + 1, z), Blocks.HAY_BLOCK.defaultBlockState(), false);
            }
            default -> { // олива / пальма по культуре
                if (bx == cx0 && bz == cz0) {
                    BlockState log = c.palette() == 2 ? Blocks.JUNGLE_LOG.defaultBlockState()
                            : Blocks.OAK_LOG.defaultBlockState();
                    for (int y = h + 1; y <= h + 4; y++)
                        chunk.setBlockState(pos.set(x, y, z), log, false);
                }
                if (Math.abs(bx - cx0) <= 2 && Math.abs(bz - cz0) <= 2
                        && !(bx == cx0 && bz == cz0)) {
                    int ly = h + 4 + (Math.abs(bx - cx0) + Math.abs(bz - cz0) <= 1 ? 1 : 0);
                    chunk.setBlockState(pos.set(x, ly, z), Blocks.OAK_LEAVES.defaultBlockState(), false);
                }
            }
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
