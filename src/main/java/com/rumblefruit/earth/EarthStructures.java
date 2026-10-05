package com.rumblefruit.earth;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

// unique multi-block landmarks of the ancient world at real scale
// (1 block = 1 meter): colosseum 189x155 m, parthenon 70x31 m,
// pharos lighthouse ~75 m, sphinx 73 m, theaters, basilicas, pylon
// temples, aqueduct arcade, triumphal arches, obelisks, harbor moles,
// baths and villas. everything is built per-column so chunk borders
// never matter
public final class EarthStructures {

    private static final BlockState STONE_BRICKS = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState CRACKED = Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
    private static final BlockState BRICK_SLAB = Blocks.STONE_BRICK_SLAB.defaultBlockState();
    private static final BlockState QUARTZ = Blocks.QUARTZ_BLOCK.defaultBlockState();
    private static final BlockState SMOOTH_QUARTZ = Blocks.SMOOTH_QUARTZ.defaultBlockState();
    private static final BlockState QUARTZ_PILLAR = Blocks.QUARTZ_PILLAR.defaultBlockState();
    private static final BlockState QUARTZ_SLAB = Blocks.QUARTZ_SLAB.defaultBlockState();
    private static final BlockState SANDSTONE = Blocks.SANDSTONE.defaultBlockState();
    private static final BlockState SMOOTH_SAND = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
    private static final BlockState CUT_SAND = Blocks.CUT_SANDSTONE.defaultBlockState();
    private static final BlockState GOLD = Blocks.GOLD_BLOCK.defaultBlockState();
    private static final BlockState BLUE = Blocks.BLUE_TERRACOTTA.defaultBlockState();
    private static final BlockState YELLOW = Blocks.YELLOW_TERRACOTTA.defaultBlockState();
    private static final BlockState RED = Blocks.RED_TERRACOTTA.defaultBlockState();
    private static final BlockState BRICKS = Blocks.BRICKS.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState LANTERN = Blocks.LANTERN.defaultBlockState();
    private static final BlockState SEA_LANTERN = Blocks.SEA_LANTERN.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState LEAVES = Blocks.OAK_LEAVES.defaultBlockState();
    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();
    private static final BlockState FLOWER1 = Blocks.POPPY.defaultBlockState();
    private static final BlockState FLOWER2 = Blocks.DANDELION.defaultBlockState();
    private static final BlockState PLANKS = Blocks.OAK_PLANKS.defaultBlockState();

    private static final int T_COLOSSEUM = 1, T_TEMPLE_GR = 2, T_TEMPLE_RO = 3,
            T_PYLON = 4, T_BASILICA = 5, T_PHAROS = 6, T_AQUEDUCT = 7,
            T_ARCH = 8, T_OBELISKS = 9, T_THEATER = 10, T_SPHINX = 11,
            T_MASTABAS = 12, T_PIER = 13, T_BATHS = 14, T_VILLA = 15;

    private static final class Struct {
        int type, x, z, p0, p1, p2, palette;
        int minX, maxX, minZ, maxZ;
    }

    private static volatile List<Struct> structs;

    private EarthStructures() {}

    private static int hash(int a, int b, int c) {
        int h = a * 73428767 + b * 912271 + c * 334343;
        h = (h ^ (h >> 13)) * 1274126177;
        h ^= h >> 16;
        return h & 0x7FFFFFFF;
    }

    private static void add(List<Struct> l, int type, int x, int z,
                            int p0, int p1, int p2, int palette, int rx, int rz) {
        Struct s = new Struct();
        s.type = type; s.x = x; s.z = z;
        s.p0 = p0; s.p1 = p1; s.p2 = p2; s.palette = palette;
        s.minX = x - rx; s.maxX = x + rx; s.minZ = z - rz; s.maxZ = z + rz;
        l.add(s);
    }

    // направление к морю: минимум рельефа на разрешении радиус+300
    private static int seaDir(EarthCities.City c) {
        int best = 0;
        int bestH = Integer.MAX_VALUE;
        for (int deg = 0; deg < 360; deg += 45) {
            double a = Math.toRadians(deg);
            int x = c.cx() + (int) Math.round(Math.cos(a) * (c.radius() + 300));
            int z = c.cz() + (int) Math.round(Math.sin(a) * (c.radius() + 300));
            int h = EarthData.worldHeight(x, z);
            if (h < bestH) { bestH = h; best = deg; }
        }
        return best;
    }

    private static List<Struct> structs() {
        if (structs == null) {
            synchronized (EarthStructures.class) {
                if (structs == null) {
                    List<Struct> l = new ArrayList<>();
                    for (EarthCities.City c : EarthCities.cities()) {
                        place(l, c);
                    }
                    // некрополь Гизы: сфинкс (73 м) восточнее Хефрена + мастабы
                    add(l, T_SPHINX, EarthCities.GIZA_X - 150, EarthCities.GIZA_Z + 550, 0, 0, 0, 2, 60, 20);
                    add(l, T_MASTABAS, EarthCities.GIZA_X + 250, EarthCities.GIZA_Z + 500, 0, 0, 0, 2, 350, 250);
                    structs = l;
                }
            }
        }
        return structs;
    }

    private static void place(List<Struct> l, EarthCities.City c) {
        int cx = c.cx(), cz = c.cz(), r = c.radius();
        switch (c.id()) {
            case "rome" -> {
                add(l, T_COLOSSEUM, cx + r / 2, cz - r / 3, 0, 0, 0, 0, 148, 122);
                add(l, T_TEMPLE_RO, cx - r / 3, cz + r / 4, 0, 0, 0, 0, 16, 11);
                add(l, T_BATHS, cx - r / 3, cz - r / 3, 0, 0, 0, 0, 17, 12);
                for (int g = 0; g < 4; g++) {
                    double a = Math.toRadians(g * 90);
                    add(l, T_ARCH, cx + (int) Math.round(Math.cos(a) * (r + 10)),
                            cz + (int) Math.round(Math.sin(a) * (r + 10)), g, 0, 0, 0, 10, 10);
                }
                add(l, T_AQUEDUCT, cx + r + 14, cz, 1500, 0, 0, 0, 1510, 3);
            }
            case "athens" -> {
                add(l, T_TEMPLE_GR, cx - r / 3, cz - r / 4, 1, 0, 0, 1, 76, 36); // Парфенон ×2
                add(l, T_THEATER, cx + r / 3, cz + r / 3, 60, 0, 0, 1, 64, 64);
                add(l, T_ARCH, cx - r - 10, cz, 3, 0, 0, 1, 10, 10);
            }
            case "alexandria" -> {
                int dir = seaDir(c);
                double a = Math.toRadians(dir);
                add(l, T_PHAROS, cx + (int) Math.round(Math.cos(a) * (r + 120)),
                        cz + (int) Math.round(Math.sin(a) * (r + 120)), 0, 0, 0, 2, 16, 16);
                add(l, T_PYLON, cx - r / 3, cz + r / 4, 1, 0, 0, 2, 34, 22);
                add(l, T_OBELISKS, cx + r / 4, cz + r / 3, 0, 0, 0, 2, 14, 8);
                add(l, T_PIER, cx, cz, dir, 200, r, 2, r + 300, r + 300);
            }
            case "carthage" -> {
                add(l, T_BATHS, cx - r / 3, cz - r / 4, 0, 0, 0, 0, 17, 12);
                add(l, T_TEMPLE_RO, cx + r / 3, cz + r / 4, 0, 0, 0, 0, 16, 11);
                add(l, T_PIER, cx, cz, seaDir(c), 200, r, 0, r + 300, r + 300);
            }
            case "byzantium" -> {
                add(l, T_BASILICA, cx - r / 4, cz - r / 4, 1, 0, 0, 3, 24, 18);
                add(l, T_PIER, cx, cz, seaDir(c), 200, r, 3, r + 300, r + 300);
                add(l, T_ARCH, cx - r - 10, cz, 3, 0, 0, 3, 10, 10);
            }
            case "cairo" -> {
                add(l, T_PYLON, cx - r / 3, cz + r / 4, 1, 0, 0, 2, 34, 22);
                add(l, T_OBELISKS, cx + r / 4, cz + r / 4, 0, 0, 0, 2, 14, 8);
            }
            default -> {
                switch (c.palette()) {
                    case 1 -> { // греческие города
                        add(l, T_TEMPLE_GR, cx - r / 4, cz - r / 4, 0, 0, 0, 1, 18, 12);
                        add(l, T_THEATER, cx + r / 4, cz + r / 4, 45, 0, 0, 1, 50, 50);
                    }
                    case 2 -> add(l, T_OBELISKS, cx + r / 4, cz + r / 4, 0, 0, 0, 2, 14, 8);
                    case 3 -> add(l, T_BASILICA, cx - r / 4, cz - r / 4, 0, 0, 0, 3, 16, 12);
                    default -> add(l, T_TEMPLE_RO, cx - r / 4, cz - r / 4, 0, 0, 0, 0, 16, 11);
                }
            }
        }
        // виллы с садами — у всех городов, позиции из хэша
        for (int v = 0; v < 2; v++) {
            int off = r / 3 + hash(cx, cz, v) % Math.max(60, r / 2);
            double a = Math.toRadians(hash(cz, v, cx) % 360);
            add(l, T_VILLA, cx + (int) Math.round(Math.cos(a) * off),
                    cz + (int) Math.round(Math.sin(a) * off), v, 0, 0, c.palette(), 16, 13);
        }
    }

    // перекрытие блока поверхности (мощение площадей у структур)
    public static BlockState surfaceTop(int x, int z) {
        for (Struct s : structs()) {
            if (x < s.minX || x > s.maxX || z < s.minZ || z > s.maxZ) continue;
            if (claimsFloor(s, x - s.x, z - s.z)) {
                return (hash(x, z, 11) & 7) == 0 ? CRACKED : STONE_BRICKS;
            }
        }
        return null;
    }

    private static boolean claimsFloor(Struct s, int lx, int lz) {
        return switch (s.type) {
            case T_COLOSSEUM -> ellipse(lx, lz, 146, 120);
            case T_TEMPLE_GR -> Math.abs(lx) <= (s.p0 == 1 ? 38 : 15) && Math.abs(lz) <= (s.p0 == 1 ? 18 : 10);
            case T_TEMPLE_RO -> Math.abs(lx) <= 15 && Math.abs(lz) <= 10;
            case T_PYLON -> Math.abs(lx) <= 33 && Math.abs(lz) <= 21;
            case T_BASILICA -> Math.abs(lx) <= (s.p0 == 1 ? 23 : 15) && Math.abs(lz) <= (s.p0 == 1 ? 17 : 11);
            case T_THEATER -> lx * lx + lz * lz <= (s.p0 + 3) * (s.p0 + 3);
            case T_BATHS -> Math.abs(lx) <= 16 && Math.abs(lz) <= 11;
            case T_VILLA -> Math.abs(lx) <= 15 && Math.abs(lz) <= 12;
            default -> false;
        };
    }

    private static boolean ellipse(int lx, int lz, double rx, double rz) {
        return (lx * lx) / (rx * rx) + (lz * lz) / (rz * rz) <= 1.0;
    }

    // отладка: какие структуры покрывают колонну
    public static String debugAt(int x, int z) {
        StringBuilder sb = new StringBuilder();
        for (Struct s : structs()) {
            if (x >= s.minX && x <= s.maxX && z >= s.minZ && z <= s.maxZ) {
                sb.append("t=").append(s.type).append('@').append(s.x).append(',').append(s.z).append(' ');
            }
        }
        return sb.length() == 0 ? "нет структур" : sb.toString();
    }

    // вызывается из EarthCities.buildAbove до городской застройки;
    // true — колонна принадлежит структуре и обработана
    public static boolean build(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                int x, int z, int h) {
        for (Struct s : structs()) {
            if (x < s.minX || x > s.maxX || z < s.minZ || z > s.maxZ) continue;
            if (buildStruct(chunk, pos, s, x, z, h)) return true;
        }
        return false;
    }

    private static void put(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                            int x, int y, int z, BlockState st) {
        chunk.setBlockState(pos.set(x, y, z), st, false);
    }

    private static void fill(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                             int x, int z, int y0, int y1, BlockState st) {
        for (int y = y0; y <= y1; y++) put(chunk, pos, x, y, z, st);
    }

    private static boolean buildStruct(ChunkAccess chunk, BlockPos.MutableBlockPos pos,
                                       Struct s, int x, int z, int h) {
        int lx = x - s.x, lz = z - s.z;
        return switch (s.type) {
            case T_COLOSSEUM -> colosseum(chunk, pos, s, x, z, h, lx, lz);
            case T_TEMPLE_GR -> templeGreek(chunk, pos, s, x, z, h, lx, lz);
            case T_TEMPLE_RO -> templeRoman(chunk, pos, s, x, z, h, lx, lz);
            case T_PYLON -> pylonTemple(chunk, pos, s, x, z, h, lx, lz);
            case T_BASILICA -> basilica(chunk, pos, s, x, z, h, lx, lz);
            case T_PHAROS -> pharos(chunk, pos, s, x, z, h, lx, lz);
            case T_AQUEDUCT -> aqueduct(chunk, pos, s, x, z, h, lx, lz);
            case T_ARCH -> arch(chunk, pos, s, x, z, h, lx, lz);
            case T_OBELISKS -> obelisks(chunk, pos, s, x, z, h, lx, lz);
            case T_THEATER -> theater(chunk, pos, s, x, z, h, lx, lz);
            case T_SPHINX -> sphinx(chunk, pos, s, x, z, h, lx, lz);
            case T_MASTABAS -> mastabas(chunk, pos, s, x, z, h, lx, lz);
            case T_PIER -> pier(chunk, pos, s, x, z, h, lx, lz);
            case T_BATHS -> baths(chunk, pos, s, x, z, h, lx, lz);
            case T_VILLA -> villa(chunk, pos, s, x, z, h, lx, lz);
            default -> false;
        };
    }

    // ---- Колизей: эллипс 189x155 м, арена, три яруса арок, развалины ----

    private static boolean colosseum(ChunkAccess c, BlockPos.MutableBlockPos p,
                                     Struct s, int x, int z, int h, int lx, int lz) {
        if (!ellipse(lx, lz, 143, 117)) return false;
        double rr = Math.sqrt((lx * lx) / (143.0 * 143.0) + (lz * lz) / (117.0 * 117.0));
        if (rr <= 0.45) {
            // арена: песок с барьером-подиумом
            if (rr > 0.42) fill(c, p, x, z, h + 1, h + 4, STONE_BRICKS);
            else if (h >= EarthData.SEA_LEVEL) put(c, p, x, h, z, SAND);
            return true;
        }
        if (rr <= 0.8) {
            // зрительные ярусы: кольца-ступени к центру
            int tier = (int) ((0.8 - rr) / 0.35 * 16.0);
            int top = h + 1 + tier;
            fill(c, p, x, z, h + 1, top, tier % 2 == 0 ? STONE_BRICKS : SMOOTH_SAND);
            put(c, p, x, top + 1, z, BRICK_SLAB);
            return true;
        }
        // внешняя стена: 80 арочных пролётов, 4 яруса (реальные 48 м → 20 бл.)
        double ang = Math.atan2(lz / 117.0, lx / 143.0);
        int seg = (int) Math.floor((ang + Math.PI) / (Math.PI / 60));
        boolean pillar = (seg % 2) == 0;
        boolean ruined = hash(x >> 2, z >> 2, 77) % 5 == 0; // верх местами обвален
        BlockState stone = hash(x, z, 78) % 4 == 0 ? CRACKED : STONE_BRICKS;
        if (pillar) {
            fill(c, p, x, z, h + 1, h + (ruined ? 13 : 24), stone);
            if (!ruined) put(c, p, x, h + 25, z, BRICK_SLAB);
        } else {
            fill(c, p, x, z, h + 1, h + 2, stone);           // цоколь
            put(c, p, x, h + 8, z, stone);                   // перемычка 1 яруса
            put(c, p, x, h + 14, z, stone);                  // перемычка 2 яруса
            if (!ruined) {
                put(c, p, x, h + 19, z, stone);              // перемычка 3 яруса
                fill(c, p, x, z, h + 22, h + 24, stone);     // аттик
            }
        }
        return true;
    }

    // ---- греческий храм-периптер: Парфенон в удвоенном масштабе
    // (140x62 м, колонны 3x3 «круглые», шаг 9 м) / малый 26x13 м ----

    private static boolean templeGreek(ChunkAccess c, BlockPos.MutableBlockPos p,
                                       Struct s, int x, int z, int h, int lx, int lz) {
        boolean big = s.p0 == 1;
        int hx = big ? 70 : 13, hz = big ? 31 : 6;
        if (Math.abs(lx) > hx + 3 || Math.abs(lz) > hz + 3) return false;
        // три ступени стилобата
        if (Math.abs(lx) <= hx + 3 && Math.abs(lz) <= hz + 3) put(c, p, x, h + 1, z, QUARTZ_SLAB);
        if (Math.abs(lx) <= hx + 2 && Math.abs(lz) <= hz + 2) put(c, p, x, h + 2, z, QUARTZ_SLAB);
        if (Math.abs(lx) <= hx + 1 && Math.abs(lz) <= hz + 1) put(c, p, x, h + 3, z, SMOOTH_QUARTZ);
        if (Math.abs(lx) > hx || Math.abs(lz) > hz) return true;
        int colH = big ? 21 : 10;
        // «круглые» колонны 3x3 по периметру (шаг 9 м — реальные 4.3 м ×2)
        boolean colRing = (Math.abs(lx) == hx || Math.abs(lz) == hz);
        if (!big && colRing) {
            boolean colAt = Math.abs(lx) == hx ? Math.floorMod(lz, 5) == 0 : Math.floorMod(lx, 5) == 0;
            if (colAt) {
                fill(c, p, x, z, h + 4, h + 3 + colH, QUARTZ_PILLAR);
                put(c, p, x, h + 4 + colH, z, QUARTZ_SLAB);
                return true;
            }
        }
        if (big) {
            // пояс колонн: кластеры 3x3 вокруг центров кратных 9
            boolean nearRing = Math.abs(Math.abs(lx) - hx) <= 1 || Math.abs(Math.abs(lz) - hz) <= 1;
            if (nearRing) {
                int dcx = Math.abs(Math.floorMod(lx + 4, 9) - 4);
                int dcz = Math.abs(Math.floorMod(lz + 4, 9) - 4);
                boolean inCluster = (Math.abs(lx) >= hx - 1 && dcz <= 1)
                        || (Math.abs(lz) >= hz - 1 && dcx <= 1);
                boolean cornerCol = Math.abs(lx) >= hx - 1 && Math.abs(lz) >= hz - 1;
                if (inCluster || cornerCol) {
                    fill(c, p, x, z, h + 4, h + 3 + colH, QUARTZ_PILLAR);
                    if (dcx == 0 || dcz == 0) put(c, p, x, h + 4 + colH, z, QUARTZ_SLAB);
                    return true;
                }
                return true; // промежутки между колоннами — воздух
            }
            // архитрав и фронтон над поясом
            if (colRing) {
                put(c, p, x, h + 5 + colH, z, QUARTZ);
                if (Math.abs(lx) == hx) {
                    for (int i = 0; i < 10; i++) {
                        if (Math.abs(lz) <= hz - i * 3) put(c, p, x, h + 6 + colH + i, z, QUARTZ);
                    }
                }
                return true;
            }
        } else if (colRing) {
            put(c, p, x, h + 5 + colH, z, QUARTZ);
            if (Math.abs(lx) == hx) {
                for (int i = 0; i < 3; i++) {
                    if (Math.abs(lz) <= hz - i) put(c, p, x, h + 6 + colH + i, z, QUARTZ);
                }
            }
            return true;
        }
        // целла со статуей (у Парфенона — Афина 12 м)
        int cx0 = big ? hx - 12 : hx - 5, cz0 = big ? hz - 10 : hz - 3;
        boolean cella = Math.abs(lx) <= cx0 && Math.abs(lz) <= cz0;
        if (cella) {
            boolean wall = Math.abs(lx) == cx0 || Math.abs(lz) == cz0;
            boolean door = lx == cx0 && Math.abs(lz) <= 2;
            if (wall && !door) fill(c, p, x, z, h + 4, h + 4 + colH - 4, SMOOTH_QUARTZ);
            if (wall) put(c, p, x, h + 5 + colH - 4, z, QUARTZ_SLAB);
            if (lx == 0 && lz == 0) {
                fill(c, p, x, z, h + 4, h + (big ? 14 : 7), QUARTZ_PILLAR);
                put(c, p, x, h + (big ? 15 : 8), z, GOLD);
                EarthCities.spawnVillager(c, x, h + 4, z, "cleric"); // жрец храма
            }
        }
        return true;
    }

    // ---- римский подиумный храм: Мезон-Карре 26x13 м ----

    private static boolean templeRoman(ChunkAccess c, BlockPos.MutableBlockPos p,
                                       Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 14 || Math.abs(lz) > 9) return false;
        // лестница с востока
        if (lx == 14 && Math.abs(lz) <= 2) {
            put(c, p, x, h + 1, z, STONE_BRICKS);
            return true;
        }
        // подиум 3 м
        if (Math.abs(lx) <= 13 && Math.abs(lz) <= 8) fill(c, p, x, z, h + 1, h + 3, STONE_BRICKS);
        // портик: колонны 9 м
        boolean front = lx >= 6 && lx <= 13 && (Math.abs(lz) == 8 || (lx == 13 || lx == 9) && Math.abs(lz) <= 8);
        if (front && Math.floorMod(lx - lz, 3) == 0) {
            fill(c, p, x, z, h + 4, h + 12, QUARTZ_PILLAR);
            put(c, p, x, h + 13, z, RED);
            return true;
        }
        // целла
        if (lx <= 5 && Math.abs(lz) <= 6) {
            boolean wall = lx == 5 || lx == -13 + 8 || Math.abs(lz) == 6;
            if (wall) fill(c, p, x, z, h + 4, h + 10, SMOOTH_QUARTZ);
            if (lx == -1 && lz == 0) {
                fill(c, p, x, z, h + 4, h + 6, QUARTZ_PILLAR);
                put(c, p, x, h + 7, z, GOLD);
            }
        }
        // двускатная черепичная крыша
        if (Math.abs(lx) <= 13 && Math.abs(lz) <= 8) {
            int roofY = h + 11 + Math.max(0, 5 - Math.abs(lz));
            put(c, p, x, roofY, z, RED);
            if (Math.abs(lz) <= 1) put(c, p, x, roofY + 1, z, RED);
        }
        return true;
    }

    // ---- египетский пилонный храм: башни-пилоны 20 м, двор, гипостиль ----

    private static boolean pylonTemple(ChunkAccess c, BlockPos.MutableBlockPos p,
                                       Struct s, int x, int z, int h, int lx, int lz) {
        boolean big = s.p0 == 1;
        int sx = big ? 32 : 20, sz = big ? 20 : 13;
        if (Math.abs(lx) > sx || Math.abs(lz) > sz) return false;
        int px0 = -sx + 2, px1 = -sx + 8; // пилоны у западного входа
        boolean pylon = lx >= px0 && lx <= px1 && Math.abs(lz) >= 4 && Math.abs(lz) <= sz - 2;
        if (pylon) {
            int height = 20 - (lx - px0); // сужение кверху
            for (int y = 1; y <= height; y++) {
                BlockState st = (y == height - 1) ? YELLOW
                        : (y % 4 == 0 ? SMOOTH_SAND : SANDSTONE);
                put(c, p, x, h + y, z, st);
            }
            put(c, p, x, h + height + 1, z, GOLD);
            return true;
        }
        // флагштоки перед пилонами
        if (lx == px0 - 2 && (Math.abs(lz) == 7 || Math.abs(lz) == 12)) {
            fill(c, p, x, z, h + 1, h + 20, LOG);
            put(c, p, x, h + 21, z, YELLOW);
            put(c, p, x, h + 20, z, BLUE);
            return true;
        }
        // гипостильный зал: колонны с синими капителями
        if (lx >= -sx / 4 && lx <= sx / 2 && Math.abs(lz) <= sz - 4) {
            if (Math.floorMod(lx, 4) == 0 && Math.floorMod(lz, 4) == 0 && !(lx == 0 && lz == 0)) {
                fill(c, p, x, z, h + 1, h + 10, SANDSTONE);
                put(c, p, x, h + 11, z, BLUE);
                put(c, p, x, h + 12, z, SMOOTH_SAND);
            }
            put(c, p, x, h + 13, z, SMOOTH_SAND); // плиты перекрытия
            return true;
        }
        // святилище
        if (lx >= sx - 8 && Math.abs(lz) <= 5) {
            boolean wall = lx == sx - 8 || lx == sx || Math.abs(lz) == 5;
            if (wall) fill(c, p, x, z, h + 1, h + 8, CUT_SAND);
            if (lx == sx - 4 && lz == 0) {
                put(c, p, x, h + 1, z, SMOOTH_SAND);
                put(c, p, x, h + 2, z, GOLD);
            }
            if (wall) put(c, p, x, h + 9, z, SMOOTH_SAND);
            return true;
        }
        return true;
    }

    // ---- византийская базилика: неф, апсида, купол ----

    private static boolean basilica(ChunkAccess c, BlockPos.MutableBlockPos p,
                                    Struct s, int x, int z, int h, int lx, int lz) {
        boolean big = s.p0 == 1;
        int sx = big ? 22 : 14, sz = big ? 16 : 10;
        if (Math.abs(lx) > sx || Math.abs(lz) > sz) return false;
        boolean wall = Math.abs(lx) == sx || Math.abs(lz) == sz;
        if (wall) {
            boolean window = Math.floorMod(lx + lz, 4) == 1;
            int wallH = big ? 12 : 9;
            for (int y = 1; y <= wallH; y++) {
                if (window && y >= wallH - 4 && y <= wallH - 2) continue;
                put(c, p, x, h + y, z, (y % 3 == 0) ? SMOOTH_SAND : STONE_BRICKS);
            }
            put(c, p, x, h + wallH + 1, z, BRICK_SLAB);
            return true;
        }
        // колонны нефа
        if (Math.abs(lx) <= sx - 3 && Math.abs(lz) == sz / 2 && Math.floorMod(lx, 5) == 0) {
            fill(c, p, x, z, h + 1, h + 8, QUARTZ_PILLAR);
            put(c, p, x, h + 9, z, QUARTZ_SLAB);
            return true;
        }
        // купол над средокрестием
        double rr = Math.sqrt(lx * lx + lz * lz);
        int domeR = big ? 11 : 7;
        if (rr <= domeR) {
            int dy = (big ? 13 : 10) + (int) Math.round((big ? 8 : 5) * (1 - rr / domeR));
            put(c, p, x, h + dy, z, rr < 1.5 ? GOLD : SMOOTH_SAND);
            return true;
        }
        if (Math.abs(lx) <= sx - 1 && Math.abs(lz) <= sz - 1) {
            put(c, p, x, h + (big ? 13 : 10), z, RED);
        }
        // алтарь
        if (lx == sx - 3 && lz == 0) {
            put(c, p, x, h + 1, z, SMOOTH_QUARTZ);
            put(c, p, x, h + 2, z, GOLD);
        }
        return true;
    }

    // ---- маяк Фарос ~75 м: квадрат 22 м → восьмигранник → цилиндр ----

    private static boolean pharos(ChunkAccess c, BlockPos.MutableBlockPos p,
                                  Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 12 || Math.abs(lz) > 12) return false;
        int base = Math.max(h, EarthData.SEA_LEVEL + 1);
        // цилиндр 56..73 + свет (первым — меньший след)
        if (lx * lx + lz * lz <= 12) {
            fill(c, p, x, z, base + 56, base + 73, QUARTZ);
            if (lx * lx + lz * lz <= 2) {
                put(c, p, x, base + 74, z, SEA_LANTERN);
                put(c, p, x, base + 75, z, GOLD);
            }
            return true;
        }
        // восьмигранник 31..55
        if (Math.abs(lx) <= 7 && Math.abs(lz) <= 7 && Math.abs(lx) + Math.abs(lz) <= 10) {
            boolean edge = Math.abs(lx) + Math.abs(lz) >= 9 || Math.abs(lx) == 7 || Math.abs(lz) == 7;
            if (edge) fill(c, p, x, z, base + 31, base + 55, SMOOTH_QUARTZ);
            return true;
        }
        // цоколь 22x22 м, 30 м высотой, окна, дверь
        if (Math.abs(lx) <= 11 && Math.abs(lz) <= 11) {
            boolean edge = Math.abs(lx) == 11 || Math.abs(lz) == 11;
            if (edge) {
                boolean window = (Math.floorMod(lx + lz, 5) == 0);
                for (int y = 1; y <= 30; y++) {
                    if (window && y % 6 == 0) continue;
                    if (lz == 11 && Math.abs(lx) <= 1 && y <= 3) continue; // дверь
                    put(c, p, x, base + y, z, y % 8 == 0 ? QUARTZ : STONE_BRICKS);
                }
            }
            return true;
        }
        return false;
    }

    // ---- акведук: аркада с водяным лотком по сглаженному профилю ----

    private static boolean aqueduct(ChunkAccess c, BlockPos.MutableBlockPos p,
                                    Struct s, int x, int z, int h, int lx, int lz) {
        if (lx < 0 || lx > s.p0 || Math.abs(lz) > 2) return false;
        int t1 = EarthData.worldHeight(x - 48, z);
        int t2 = EarthData.worldHeight(x + 48, z);
        int channelY = Math.max((h + t1 + t2) / 3, h) + 12;
        boolean pillar = Math.floorMod(lx, 9) <= 1;
        if (Math.abs(lz) <= 1) {
            fill(c, p, x, z, h + 1, channelY - 2, STONE_BRICKS);
            if (lz == 0) {
                put(c, p, x, channelY - 1, z, STONE_BRICKS);
                put(c, p, x, channelY, z, WATER);
            } else {
                put(c, p, x, channelY - 1, z, STONE_BRICKS);
            }
            return true;
        }
        if (pillar) {
            fill(c, p, x, z, h + 1, channelY - 1, STONE_BRICKS);
        } else {
            put(c, p, x, channelY - 4, z, BRICK_SLAB);
            fill(c, p, x, z, channelY - 2, channelY, STONE_BRICKS);
        }
        return true;
    }

    // ---- триумфальная арка на воротах (15 м) ----

    private static boolean arch(ChunkAccess c, BlockPos.MutableBlockPos p,
                                Struct s, int x, int z, int h, int lx, int lz) {
        boolean swap = s.p0 % 2 == 1;
        int ax = swap ? lz : lx, az = swap ? lx : lz;
        if (Math.abs(ax) > 5 || Math.abs(az) > 2) return false;
        boolean pylon = Math.abs(ax) >= 3;
        if (pylon) {
            fill(c, p, x, z, h + 1, h + 11, STONE_BRICKS);
            put(c, p, x, h + 12, z, QUARTZ_SLAB);
        } else {
            put(c, p, x, h + 10, z, QUARTZ);
            put(c, p, x, h + 11, z, STONE_BRICKS);
        }
        fill(c, p, x, z, h + 12, h + 14, STONE_BRICKS); // аттик
        if (pylon && az == 0) {
            put(c, p, x, h + 15, z, QUARTZ_PILLAR);
            put(c, p, x, h + 16, z, GOLD);
        }
        return true;
    }

    // ---- пара обелисков 25 м с золотыми пирамидионами ----

    private static boolean obelisks(ChunkAccess c, BlockPos.MutableBlockPos p,
                                    Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 12 || Math.abs(lz) > 5) return false;
        for (int ox : new int[]{-8, 8}) {
            int dx = lx - ox, dz = lz;
            if (Math.abs(dx) > 2 || Math.abs(dz) > 2) continue;
            fill(c, p, x, z, h + 1, h + 2, CUT_SAND); // постамент 5x5
            if (Math.abs(dx) > 1 || Math.abs(dz) > 1) continue;
            if (dx == 0 && dz == 0) {
                for (int y = 3; y <= 26; y++) {
                    put(c, p, x, h + y, z, y % 4 == 0 ? SMOOTH_SAND : SANDSTONE);
                }
                put(c, p, x, h + 27, z, GOLD);
            }
            return true;
        }
        return false;
    }

    // ---- греческий театр: ярусы, орхестра, скена (радиус из p0) ----

    private static boolean theater(ChunkAccess c, BlockPos.MutableBlockPos p,
                                   Struct s, int x, int z, int h, int lx, int lz) {
        int R = s.p0;
        double rr = Math.sqrt(lx * lx + lz * lz);
        if (rr > R) return false;
        int orch = R / 6;
        if (rr <= orch) return true; // орхестра
        if (lz >= 0) {
            int tier = (int) ((rr - orch) / 2.5);
            fill(c, p, x, z, h + 1, h + tier, tier % 2 == 0 ? STONE_BRICKS : SMOOTH_QUARTZ);
            put(c, p, x, h + tier + 1, z, QUARTZ_SLAB);
            return true;
        }
        // скена: стена с колоннами за орхестрой
        if (lz <= -orch - 1 && Math.abs(lx) <= orch + 3) {
            if (Math.abs(lx) == orch + 3 || lz <= -orch - 3) {
                fill(c, p, x, z, h + 1, h + 7, SMOOTH_QUARTZ);
            } else if (Math.floorMod(lx, 4) == 0) {
                fill(c, p, x, z, h + 1, h + 6, QUARTZ_PILLAR);
            }
            return true;
        }
        return true;
    }

    // ---- сфинкс у Гизы: 73 м, тело льва, голова с немесом ----

    private static boolean sphinx(ChunkAccess c, BlockPos.MutableBlockPos p,
                                  Struct s, int x, int z, int h, int lx, int lz) {
        if (lx < -10 || lx > 55 || Math.abs(lz) > 8) return false;
        // тело льва 12 м высотой
        if (lx <= 30 && Math.abs(lz) <= 5) fill(c, p, x, z, h + 1, h + 12, SANDSTONE);
        // вытянутые лапы вперёд
        if (lx > 30 && lx <= 48 && Math.abs(lz) <= 5 && (Math.abs(lz) >= 3 || lx <= 38)) {
            fill(c, p, x, z, h + 1, h + 4, SMOOTH_SAND);
        }
        // голова 10 м с немесом
        if (lx >= 24 && lx <= 31 && Math.abs(lz) <= 3) {
            for (int y = 13; y <= 21; y++) {
                BlockState st = (y == 15 && Math.abs(lz) >= 2) ? BLUE
                        : (y == 21 ? GOLD : SMOOTH_SAND);
                put(c, p, x, h + y, z, st);
            }
        }
        return true;
    }

    // ---- поле мастаб: ряды гробниц 12x8 м ----

    private static boolean mastabas(ChunkAccess c, BlockPos.MutableBlockPos p,
                                    Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 340 || Math.abs(lz) > 240) return false;
        int gx = Math.floorMod(lx, 26), gz = Math.floorMod(lz, 20);
        if (gx > 12 || gz > 8) return false; // улицы между гробницами
        int cellX = Math.floorDiv(lx, 26), cellZ = Math.floorDiv(lz, 20);
        if (hash(cellX, cellZ, 900) % 4 == 0) return false;
        boolean edge = gx == 0 || gx == 12 || gz == 0 || gz == 8;
        int hh = 3 + hash(cellX, cellZ, 901) % 4;
        fill(c, p, x, z, h + 1, h + hh, edge ? CUT_SAND : SANDSTONE);
        put(c, p, x, h + hh + 1, z, SMOOTH_SAND);
        return true;
    }

    // торговое судно: корпус 11x5 м, нос и корма приподняты, мачта с реей
    private static void buildShip(ChunkAccess c, BlockPos.MutableBlockPos p,
                                  int x, int z, int deck, int lx, int lz, int dx, int dz) {
        // корабль стоит бортом к молу: корпус по перпендикуляру
        int ax = -dz * lx + dx * lz;  // вдоль корпуса
        int az = dx * lx + dz * lz;   // поперёк
        if (az < -3 || az > 3 || ax < -6 || ax > 6) return;
        if (az >= 1) { // только с морской стороны мола
            // корпус: палуба и борта
            if (Math.abs(ax) <= 5 && az <= 2) {
                put(c, p, x, deck, z, PLANKS);
            }
            boolean side = az == 2 || Math.abs(ax) == 5;
            if (side) {
                int rise = Math.abs(ax) >= 4 ? 2 : 1; // нос/корма выше
                fill(c, p, x, z, deck + 1, deck + rise, PLANKS);
            }
            // мачта с реей
            if (ax == 0 && az == 1) {
                fill(c, p, x, z, deck + 1, deck + 8, Blocks.OAK_LOG.defaultBlockState());
                put(c, p, x, deck + 9, z, Blocks.WHITE_WOOL.defaultBlockState());
            }
            if (Math.abs(ax) <= 3 && az == 1 && ax != 0) {
                put(c, p, x, deck + 8, z, Blocks.OAK_LOG.defaultBlockState()); // рея
            }
        }
    }

    // ---- гавань: молы в море с фонарями (длина из p1, радиус из p2) ----

    private static boolean pier(ChunkAccess c, BlockPos.MutableBlockPos p,
                                Struct s, int x, int z, int h, int lx, int lz) {
        double a = Math.toRadians(s.p0 & 0xFFFF);
        int dx = (int) Math.round(Math.cos(a)), dz = (int) Math.round(Math.sin(a));
        int len = s.p1 > 0 ? s.p1 : 200;
        int r = s.p2 > 0 ? s.p2 : 400; // радиус города из p2
        for (int off : new int[]{-30, 30}) {
            int px = -dz * off, pz = dx * off;
            for (int t = 0; t <= len; t++) {
                int bx = (int) Math.round(dx * (r * 0.8 + t)) + px;
                int bz = (int) Math.round(dz * (r * 0.8 + t)) + pz;
                if (Math.abs(lx - bx) > 2 || Math.abs(lz - bz) > 2) continue;
                int deck = EarthData.SEA_LEVEL + 2;
                if (h <= deck - 1) fill(c, p, x, z, h + 1, deck, STONE_BRICKS);
                put(c, p, x, deck, z, SMOOTH_SAND);
                if (Math.abs(lx - bx) == 2 && t % 25 == 0) {
                    put(c, p, x, deck + 1, z, STONE_BRICKS);
                    put(c, p, x, deck + 2, z, LANTERN);
                }
                // рыбак у начала мола
                if (t == 0 && lx == bx && lz == bz) {
                    EarthCities.spawnVillager(c, x, deck + 1, z, "fisherman");
                }
                // торговое судно у конца мола
                if (t >= len - 2) {
                    buildShip(c, p, x, z, deck, lx - bx, lz - bz, dx, dz);
                }
                return true;
            }
        }
        return false;
    }

    // ---- римские термы: зал 32x22 м с купальней и сводом ----

    private static boolean baths(ChunkAccess c, BlockPos.MutableBlockPos p,
                                 Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 15 || Math.abs(lz) > 10) return false;
        boolean wall = Math.abs(lx) == 15 || Math.abs(lz) == 10;
        if (wall) {
            boolean window = Math.floorMod(lx + lz, 4) == 2;
            boolean door = lx == -15 && Math.abs(lz) <= 1;
            for (int y = 1; y <= 9; y++) {
                if (door && y <= 3) continue;
                if (window && y >= 5 && y <= 7) continue;
                put(c, p, x, h + y, z, y % 3 == 0 ? BRICKS : STONE_BRICKS);
            }
            return true;
        }
        int vault = 10 + (int) Math.round(4 * (1 - Math.abs(lz) / 10.0));
        put(c, p, x, h + vault, z, BRICKS);
        // купальня 12x7 м
        if (lx >= -8 && lx <= 3 && Math.abs(lz) <= 3) {
            put(c, p, x, h, z, SMOOTH_QUARTZ);
            if (lx >= -7 && lx <= 2 && Math.abs(lz) <= 2) put(c, p, x, h + 1, z, WATER);
            return true;
        }
        if (Math.floorMod(lx, 5) == 0 && Math.abs(lz) == 7) {
            fill(c, p, x, z, h + 1, h + 8, QUARTZ_PILLAR);
        }
        return true;
    }

    // ---- вилла с садом: 30x24 м, двор, бассейн, изгородь, дерево ----

    private static boolean villa(ChunkAccess c, BlockPos.MutableBlockPos p,
                                 Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 14 || Math.abs(lz) > 11) return false;
        boolean wall = Math.abs(lx) == 14 || Math.abs(lz) == 11;
        if (wall) {
            boolean door = lx == 14 && Math.abs(lz) <= 1;
            boolean window = Math.floorMod(lx * 3 + lz, 5) == 1;
            for (int y = 1; y <= 6; y++) {
                if (door && y <= 2) continue;
                if (window && y == 3) continue;
                put(c, p, x, h + y, z, s.palette == 2 ? SANDSTONE : SMOOTH_QUARTZ);
            }
            put(c, p, x, h + 7, z, RED);
            return true;
        }
        // жилая часть (западная половина)
        if (lx <= -3) {
            boolean innerWall = lx == -3 || Math.abs(lz) >= 9;
            if (innerWall) fill(c, p, x, z, h + 1, h + 5, s.palette == 2 ? SANDSTONE : SMOOTH_QUARTZ);
            put(c, p, x, h + 6, z, PLANKS);
            return true;
        }
        // сад
        if (lx >= -1 && lx <= 12 && Math.abs(lz) <= 9) {
            if (lx >= 3 && lx <= 7 && Math.abs(lz) <= 2) {
                put(c, p, x, h, z, SMOOTH_QUARTZ);
                if (lx >= 4 && lx <= 6 && Math.abs(lz) <= 1) put(c, p, x, h + 1, z, WATER);
                return true;
            }
            if (Math.abs(lz) == 8 && Math.floorMod(lx, 2) == 0) {
                fill(c, p, x, z, h + 1, h + 2, LEAVES);
                return true;
            }
            if (Math.floorMod(lx + lz * 2, 9) == 0) {
                put(c, p, x, h + 1, z, hash(x, z, 33) % 2 == 0 ? FLOWER1 : FLOWER2);
                return true;
            }
            if (lx == 10 && lz == 6) {
                fill(c, p, x, z, h + 1, h + 5, LOG);
                for (int ddy = 5; ddy <= 7; ddy++) put(c, p, x, h + ddy, z, LEAVES);
                return true;
            }
        }
        return true;
    }
}
