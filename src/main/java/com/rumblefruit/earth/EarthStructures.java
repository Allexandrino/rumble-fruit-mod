package com.rumblefruit.earth;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

// unique multi-block landmarks of the ancient world, built per-column so
// chunk borders never matter. every structure is a local-coordinate recipe:
// colosseum and theater (ellipse/rings), peripteros and pylon temples,
// basilica, pharos lighthouse, aqueduct arcade, triumphal arches, obelisks,
// the sphinx and mastaba field at giza, harbor piers, baths and villas
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
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();

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

    // направление к морю: минимум рельефа на разрешении радиус+60
    private static int seaDir(EarthCities.City c) {
        int best = 0;
        int bestH = Integer.MAX_VALUE;
        for (int deg = 0; deg < 360; deg += 45) {
            double a = Math.toRadians(deg);
            int x = c.cx() + (int) Math.round(Math.cos(a) * (c.radius() + 60));
            int z = c.cz() + (int) Math.round(Math.sin(a) * (c.radius() + 60));
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
                    // некрополь Гизы: сфинкс смотрит на восток + поле мастаб
                    add(l, T_SPHINX, EarthCities.GIZA_X + 100, EarthCities.GIZA_Z + 130, 0, 0, 0, 2, 20, 8);
                    // мастабы — на плато у пирамид (не в затопленной долине)
                    add(l, T_MASTABAS, EarthCities.GIZA_X - 40, EarthCities.GIZA_Z + 160, 0, 0, 0, 2, 130, 60);
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
                add(l, T_COLOSSEUM, cx + 95, cz - 55, 0, 0, 0, 0, 40, 32);
                add(l, T_TEMPLE_RO, cx - 70, cz + 55, 0, 0, 0, 0, 16, 11);
                add(l, T_BATHS, cx - 65, cz - 75, 0, 0, 0, 0, 13, 10);
                for (int g = 0; g < 4; g++) {
                    double a = Math.toRadians(g * 90);
                    add(l, T_ARCH, cx + (int) Math.round(Math.cos(a) * (r + 8)),
                            cz + (int) Math.round(Math.sin(a) * (r + 8)), g, 0, 0, 0, 8, 8);
                }
                add(l, T_AQUEDUCT, cx + r + 12, cz, 300, 0, 0, 0, 310, 3);
            }
            case "athens" -> {
                add(l, T_TEMPLE_GR, cx - 32, cz - 28, 1, 0, 0, 1, 17, 12); // Парфенон
                add(l, T_THEATER, cx + 38, cz + 32, 0, 0, 0, 1, 24, 24);
                add(l, T_ARCH, cx - r - 8, cz, 3, 0, 0, 1, 8, 8);
            }
            case "alexandria" -> {
                int dir = seaDir(c);
                double a = Math.toRadians(dir);
                add(l, T_PHAROS, cx + (int) Math.round(Math.cos(a) * (r + 40)),
                        cz + (int) Math.round(Math.sin(a) * (r + 40)), 0, 0, 0, 2, 10, 10);
                add(l, T_PYLON, cx - 45, cz + 40, 0, 0, 0, 2, 18, 12);
                add(l, T_OBELISKS, cx + 40, cz + 45, 0, 0, 0, 2, 10, 6);
                add(l, T_PIER, cx, cz, dir, 0, 0, 2, r + 60, r + 60);
            }
            case "carthage" -> {
                add(l, T_BATHS, cx - 45, cz - 40, 0, 0, 0, 0, 13, 10);
                add(l, T_TEMPLE_RO, cx + 45, cz + 40, 0, 0, 0, 0, 16, 11);
                add(l, T_PIER, cx, cz, seaDir(c), 0, 0, 0, r + 60, r + 60);
            }
            case "byzantium" -> {
                add(l, T_BASILICA, cx - 38, cz - 32, 0, 0, 0, 3, 14, 11);
                add(l, T_PIER, cx, cz, seaDir(c), 0, 0, 3, r + 60, r + 60);
                add(l, T_ARCH, cx - r - 8, cz, 3, 0, 0, 3, 8, 8);
            }
            case "cairo" -> {
                add(l, T_PYLON, cx - 45, cz + 40, 0, 0, 0, 2, 18, 12);
                add(l, T_OBELISKS, cx + 38, cz + 38, 0, 0, 0, 2, 10, 6);
            }
            default -> {
                switch (c.palette()) {
                    case 1 -> { // греческие города
                        add(l, T_TEMPLE_GR, cx - 25, cz - 22, 0, 0, 0, 1, 14, 10);
                        add(l, T_THEATER, cx + 30, cz + 28, 0, 0, 0, 1, 20, 20);
                    }
                    case 2 -> add(l, T_OBELISKS, cx + 28, cz + 28, 0, 0, 0, 2, 10, 6);
                    case 3 -> add(l, T_BASILICA, cx - 28, cz - 24, 0, 0, 0, 3, 14, 11);
                    default -> add(l, T_TEMPLE_RO, cx - 28, cz - 24, 0, 0, 0, 0, 16, 11);
                }
            }
        }
        // виллы с садами — у всех городов, позиции из хэша
        for (int v = 0; v < 2; v++) {
            int off = 45 + hash(cx, cz, v) % Math.max(20, r - 60);
            double a = Math.toRadians(hash(cz, v, cx) % 360);
            add(l, T_VILLA, cx + (int) Math.round(Math.cos(a) * off),
                    cz + (int) Math.round(Math.sin(a) * off), v, 0, 0, c.palette(), 12, 10);
        }
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
            case T_COLOSSEUM -> ellipse(lx, lz, 38, 30);
            case T_TEMPLE_GR -> Math.abs(lx) <= 16 && Math.abs(lz) <= 11;
            case T_TEMPLE_RO -> Math.abs(lx) <= 15 && Math.abs(lz) <= 10;
            case T_PYLON -> Math.abs(lx) <= 17 && Math.abs(lz) <= 11;
            case T_BASILICA -> Math.abs(lx) <= 13 && Math.abs(lz) <= 10;
            case T_THEATER -> lx * lx + lz * lz <= 23 * 23;
            case T_BATHS -> Math.abs(lx) <= 12 && Math.abs(lz) <= 9;
            case T_VILLA -> Math.abs(lx) <= 11 && Math.abs(lz) <= 9;
            default -> false;
        };
    }

    private static boolean ellipse(int lx, int lz, double rx, double rz) {
        return (lx * lx) / (rx * rx) + (lz * lz) / (rz * rz) <= 1.0;
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

    // ---- Колизей: эллипс 76x60, три яруса арок, арена, развалины ----

    private static boolean colosseum(ChunkAccess c, BlockPos.MutableBlockPos p,
                                     Struct s, int x, int z, int h, int lx, int lz) {
        if (!ellipse(lx, lz, 38, 30)) return false;
        double rr = Math.sqrt((lx * lx) / (38.0 * 38.0) + (lz * lz) / (30.0 * 30.0));
        if (rr <= 0.45) {
            // арена: песок с барьером
            if (rr > 0.42) fill(c, p, x, z, h + 1, h + 3, STONE_BRICKS);
            else if (h >= EarthData.SEA_LEVEL) put(c, p, x, h, z, SAND);
            return true;
        }
        if (rr <= 0.75) {
            // зрительные ярусы: кольца-ступени к центру
            int tier = (int) ((0.75 - rr) / 0.30 * 10.0);
            int top = h + 1 + tier;
            fill(c, p, x, z, h + 1, top, tier % 2 == 0 ? STONE_BRICKS : SMOOTH_SAND);
            put(c, p, x, top + 1, z, BRICK_SLAB);
            return true;
        }
        // внешняя стена: пилястры каждые 3 градуса, арки, 3 яруса
        double ang = Math.atan2(lz / 30.0, lx / 38.0);
        int seg = (int) Math.floor((ang + Math.PI) / (Math.PI / 20));
        boolean pillar = (seg % 2) == 0;
        // руины: верхний ярус местами обвален
        boolean ruined = hash(x >> 2, z >> 2, 77) % 5 == 0;
        BlockState stone = hash(x, z, 78) % 4 == 0 ? CRACKED : STONE_BRICKS;
        if (pillar) {
            fill(c, p, x, z, h + 1, h + (ruined ? 9 : 15), stone);
            if (!ruined) put(c, p, x, h + 16, z, BRICK_SLAB);
        } else {
            fill(c, p, x, z, h + 1, h + 2, stone);           // цоколь
            put(c, p, x, h + 6, z, stone);                   // перемычка 1 яруса
            if (!ruined) {
                put(c, p, x, h + 11, z, stone);              // перемычка 2 яруса
                fill(c, p, x, z, h + 14, h + 15, stone);     // аттик
            }
        }
        return true;
    }

    // ---- греческий храм-периптер: стилобат, колонны, фронтон, целла ----

    private static boolean templeGreek(ChunkAccess c, BlockPos.MutableBlockPos p,
                                       Struct s, int x, int z, int h, int lx, int lz) {
        boolean big = s.p0 == 1;
        int hx = big ? 14 : 11, hz = big ? 8 : 7;
        if (Math.abs(lx) > hx + 2 || Math.abs(lz) > hz + 2) return false;
        // три ступени стилобата
        if (Math.abs(lx) <= hx + 2 && Math.abs(lz) <= hz + 2) put(c, p, x, h + 1, z, QUARTZ_SLAB);
        if (Math.abs(lx) <= hx + 1 && Math.abs(lz) <= hz + 1) put(c, p, x, h + 2, z, QUARTZ_SLAB);
        if (Math.abs(lx) <= hx && Math.abs(lz) <= hz) put(c, p, x, h + 3, z, SMOOTH_QUARTZ);
        if (Math.abs(lx) > hx || Math.abs(lz) > hz) return true;
        // колонны по периметру
        boolean colRing = (Math.abs(lx) == hx || Math.abs(lz) == hz);
        boolean colAt = colRing && (Math.abs(lx) == hx
                ? Math.floorMod(lz, 3) == 0 : Math.floorMod(lx, 3) == 0);
        if (colAt) {
            fill(c, p, x, z, h + 4, h + 12, QUARTZ_PILLAR);
            put(c, p, x, h + 13, z, QUARTZ_SLAB);
            return true;
        }
        // архитрав + фронтон (треугольник на торцах)
        if (colRing) {
            put(c, p, x, h + 14, z, QUARTZ);
            if (Math.abs(lx) == hx) {
                for (int i = 0; i < 4; i++) {
                    if (Math.abs(lz) <= hz - i * 2) put(c, p, x, h + 15 + i, z, QUARTZ);
                }
            }
            return true;
        }
        // целла со статуей
        boolean cella = Math.abs(lx) <= hx - 5 && Math.abs(lz) <= hz - 4;
        if (cella) {
            boolean wall = Math.abs(lx) == hx - 5 || Math.abs(lz) == hz - 4;
            boolean door = lx == hx - 5 && Math.abs(lz) <= 1;
            if (wall && !door) fill(c, p, x, z, h + 4, h + 9, SMOOTH_QUARTZ);
            if (wall) put(c, p, x, h + 10, z, QUARTZ_SLAB);
            if (lx == 0 && lz == 0) { // культовая статуя
                fill(c, p, x, z, h + 4, h + 6, QUARTZ_PILLAR);
                put(c, p, x, h + 7, z, GOLD);
            }
        }
        return true;
    }

    // ---- римский подиумный храм: высокий цоколь, лестница, красная крыша ----

    private static boolean templeRoman(ChunkAccess c, BlockPos.MutableBlockPos p,
                                       Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 14 || Math.abs(lz) > 9) return false;
        // подиум 4 высотой, лестница с востока
        if (lx == 14 && Math.abs(lz) <= 2) {
            int step = h + 4 - Math.max(0, 4 - (14 - lx));
            fill(c, p, x, z, h + 1, h + 1, STONE_BRICKS);
            return true;
        }
        if (Math.abs(lx) <= 13 && Math.abs(lz) <= 8) fill(c, p, x, z, h + 1, h + 4, STONE_BRICKS);
        // портик: колонны на фронте и по бокам спереди
        boolean front = lx >= 6 && lx <= 13 && (Math.abs(lz) == 8 || (lx == 13 || lx == 9) && Math.abs(lz) <= 8);
        if (front && Math.floorMod(lx - lz, 3) == 0) {
            fill(c, p, x, z, h + 5, h + 12, QUARTZ_PILLAR);
            put(c, p, x, h + 13, z, RED);
            return true;
        }
        // целла
        if (lx <= 5 && Math.abs(lz) <= 6) {
            boolean wall = lx == 5 || lx == -13 + 8 || Math.abs(lz) == 6;
            if (wall) fill(c, p, x, z, h + 5, h + 11, SMOOTH_QUARTZ);
            if (lx == -1 && lz == 0) {
                fill(c, p, x, z, h + 5, h + 7, QUARTZ_PILLAR);
                put(c, p, x, h + 8, z, GOLD);
            }
        }
        // двускатная черепичная крыша
        if (Math.abs(lx) <= 13 && Math.abs(lz) <= 8) {
            int roofY = h + 12 + Math.max(0, 5 - Math.abs(lz));
            put(c, p, x, roofY, z, RED);
            if (Math.abs(lz) <= 1) put(c, p, x, roofY + 1, z, RED);
        }
        return true;
    }

    // ---- египетский пилонный храм: башни-пилоны, двор, колонны с орнаментом ----

    private static boolean pylonTemple(ChunkAccess c, BlockPos.MutableBlockPos p,
                                       Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 16 || Math.abs(lz) > 10) return false;
        // пилоны: две башни у западного входа, сужаются кверху
        boolean pylon = lx >= -14 && lx <= -10 && Math.abs(lz) >= 3 && Math.abs(lz) <= 9;
        if (pylon) {
            int height = 12 - (Math.abs(lx + 12));
            for (int y = 1; y <= height; y++) {
                BlockState st = (y == height - 1) ? YELLOW
                        : (y % 4 == 0 ? SMOOTH_SAND : SANDSTONE);
                put(c, p, x, h + y, z, st);
            }
            put(c, p, x, h + height + 1, z, GOLD); // позолоченное тяжёлое навершие
            return true;
        }
        // флаги перед пилонами
        if (lx == -15 && (Math.abs(lz) == 5 || Math.abs(lz) == 8)) {
            fill(c, p, x, z, h + 1, h + 14, LOG);
            put(c, p, x, h + 15, z, YELLOW);
            put(c, p, x, h + 14, z, BLUE);
            return true;
        }
        // гипостильный зал: колонны с синими капителями
        if (lx >= -6 && lx <= 6 && Math.abs(lz) <= 7) {
            if (Math.floorMod(lx, 3) == 0 && Math.floorMod(lz, 3) == 0 && !(lx == 0 && lz == 0)) {
                fill(c, p, x, z, h + 1, h + 7, SANDSTONE);
                put(c, p, x, h + 8, z, BLUE);
                put(c, p, x, h + 9, z, SMOOTH_SAND);
            }
            // плиты перекрытия
            put(c, p, x, h + 10, z, SMOOTH_SAND);
            return true;
        }
        // святилище
        if (lx >= 10 && lx <= 15 && Math.abs(lz) <= 4) {
            boolean wall = lx == 10 || lx == 15 || Math.abs(lz) == 4;
            if (wall) fill(c, p, x, z, h + 1, h + 6, CUT_SAND);
            if (lx == 13 && lz == 0) {
                put(c, p, x, h + 1, z, SMOOTH_SAND);
                put(c, p, x, h + 2, z, GOLD);
            }
            if (wall) put(c, p, x, h + 7, z, SMOOTH_SAND);
            return true;
        }
        return true;
    }

    // ---- византийская базилика: неф, апсида, ступенчатый купол ----

    private static boolean basilica(ChunkAccess c, BlockPos.MutableBlockPos p,
                                    Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 12 || Math.abs(lz) > 9) return false;
        boolean apse = lx >= 9 && lx * lx + lz * lz <= 100 && lx * lx / 36.0 + lz * lz / 36.0 <= 1.2;
        boolean wall = Math.abs(lx) == 12 || Math.abs(lz) == 9 || (lx >= 9 && Math.abs(lz) >= 8);
        if (wall) {
            boolean window = Math.floorMod(lx + lz, 4) == 1;
            for (int y = 1; y <= 9; y++) {
                if (window && y >= 5 && y <= 7) continue;
                put(c, p, x, h + y, z, (y % 3 == 0) ? SMOOTH_SAND : STONE_BRICKS);
            }
            put(c, p, x, h + 10, z, BRICK_SLAB);
            return true;
        }
        // колонны нефа
        if (Math.abs(lx) <= 8 && (Math.abs(lz) == 4) && Math.floorMod(lx, 4) == 0) {
            fill(c, p, x, z, h + 1, h + 7, QUARTZ_PILLAR);
            put(c, p, x, h + 8, z, QUARTZ_SLAB);
            return true;
        }
        // купол над средокрестием
        double rr = Math.sqrt(lx * lx + lz * lz);
        if (rr <= 6) {
            int dy = (int) Math.round(11 + 5 * (1 - rr / 6.0));
            put(c, p, x, h + dy, z, rr < 1.5 ? GOLD : SMOOTH_SAND);
            return true;
        }
        // крыша притворов
        if (Math.abs(lx) <= 11 && Math.abs(lz) <= 8) put(c, p, x, h + 10, z, RED);
        // алтарь
        if (lx == 9 && lz == 0) {
            put(c, p, x, h + 1, z, SMOOTH_QUARTZ);
            put(c, p, x, h + 2, z, GOLD);
        }
        return true;
    }

    // ---- маяк Фарос: квадрат → восьмигранник → цилиндр, огонь наверху ----

    private static boolean pharos(ChunkAccess c, BlockPos.MutableBlockPos p,
                                  Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 6 || Math.abs(lz) > 6) return false;
        int base = Math.max(h, EarthData.SEA_LEVEL + 1);
        // сначала верхние ярусы — их след отпечатков меньше, иначе
        // широкие ярусы «съедают» центральные колонны цилиндра
        if (lx * lx + lz * lz <= 6) {
            fill(c, p, x, z, base + 33, base + 42, QUARTZ);
            if (lx * lx + lz * lz <= 2) {
                put(c, p, x, base + 43, z, SEA_LANTERN);
                put(c, p, x, base + 44, z, GOLD);
            }
            return true;
        }
        // восьмигранник 19..32
        if (Math.abs(lx) <= 4 && Math.abs(lz) <= 4 && Math.abs(lx) + Math.abs(lz) <= 6) {
            boolean edge = Math.abs(lx) + Math.abs(lz) >= 5 || Math.abs(lx) == 4 || Math.abs(lz) == 4;
            if (edge) fill(c, p, x, z, base + 19, base + 32, SMOOTH_QUARTZ);
            return true;
        }
        // цоколь 11x11, 18 высоты, окна каждые 5, дверь с юга
        if (Math.abs(lx) <= 5 && Math.abs(lz) <= 5) {
            boolean edge = Math.abs(lx) == 5 || Math.abs(lz) == 5;
            if (edge) {
                boolean window = (Math.floorMod(lx + lz, 4) == 0);
                for (int y = 1; y <= 18; y++) {
                    if (window && y % 5 == 0) continue;
                    if (lz == 5 && Math.abs(lx) <= 1 && y <= 3) continue; // дверь
                    put(c, p, x, base + y, z, y % 6 == 0 ? QUARTZ : STONE_BRICKS);
                }
            }
            return true;
        }
        return false;
    }

    // ---- акведук: аркада с водяным лотком, лоток идёт по сглаженному
    // профилю рельефа +11 блоков — через холмы насыпью, через долы аркадой ----

    private static boolean aqueduct(ChunkAccess c, BlockPos.MutableBlockPos p,
                                    Struct s, int x, int z, int h, int lx, int lz) {
        if (lx < 0 || lx > s.p0 || Math.abs(lz) > 2) return false;
        // сглаженная высота лотка: среднее рельефа ±24 блока по трассе
        int t1 = EarthData.worldHeight(x - 24, z);
        int t2 = EarthData.worldHeight(x + 24, z);
        int channelY = Math.max((h + t1 + t2) / 3, h) + 11;
        boolean pillar = Math.floorMod(lx, 8) <= 1;
        if (Math.abs(lz) <= 1) {
            // несущий хребет: насыпь/аркада до лотка
            fill(c, p, x, z, h + 1, channelY - 2, STONE_BRICKS);
            if (lz == 0) {
                put(c, p, x, channelY - 1, z, STONE_BRICKS);
                put(c, p, x, channelY, z, WATER);
            } else {
                put(c, p, x, channelY - 1, z, STONE_BRICKS);
            }
            return true;
        }
        // боковые грани: опоры и арочные просветы
        if (pillar) {
            fill(c, p, x, z, h + 1, channelY - 1, STONE_BRICKS);
        } else {
            put(c, p, x, channelY - 3, z, BRICK_SLAB); // намёт арки
            fill(c, p, x, z, channelY - 2, channelY, STONE_BRICKS);
        }
        return true;
    }

    // ---- триумфальная арка на воротах ----

    private static boolean arch(ChunkAccess c, BlockPos.MutableBlockPos p,
                                Struct s, int x, int z, int h, int lx, int lz) {
        // арка поворачивается по стороне света (p0: 0..3)
        boolean swap = s.p0 % 2 == 1;
        int ax = swap ? lz : lx, az = swap ? lx : lz;
        if (Math.abs(ax) > 4 || Math.abs(az) > 2) return false;
        boolean pylon = Math.abs(ax) >= 3;
        if (pylon) {
            fill(c, p, x, z, h + 1, h + 9, STONE_BRICKS);
            put(c, p, x, h + 10, z, QUARTZ_SLAB); // капитель
        } else if (Math.abs(ax) <= 2) {
            put(c, p, x, h + 8, z, QUARTZ);       // перемычка проёма
            put(c, p, x, h + 9, z, STONE_BRICKS);
        }
        // аттик и статуи
        fill(c, p, x, z, h + 10, h + 11, STONE_BRICKS);
        if (pylon && az == 0) {
            put(c, p, x, h + 12, z, QUARTZ_PILLAR);
            put(c, p, x, h + 13, z, GOLD);
        }
        return true;
    }

    // ---- пара обелисков с золотыми пирамидионами ----

    private static boolean obelisks(ChunkAccess c, BlockPos.MutableBlockPos p,
                                    Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 8 || Math.abs(lz) > 4) return false;
        for (int ox : new int[]{-5, 5}) {
            int dx = lx - ox, dz = lz;
            if (Math.abs(dx) > 1 || Math.abs(dz) > 1) continue;
            fill(c, p, x, z, h + 1, h + 2, CUT_SAND); // постамент
            if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && !(dx == 0 && dz == 0)) continue;
            // ствол 1x1 с полосами иероглифов
            for (int y = 3; y <= 18; y++) {
                put(c, p, x, h + y, z, y % 3 == 0 ? SMOOTH_SAND : SANDSTONE);
            }
            put(c, p, x, h + 19, z, GOLD);
            return true;
        }
        return false;
    }

    // ---- греческий театр: полукруглые ярусы, орхестра, скена ----

    private static boolean theater(ChunkAccess c, BlockPos.MutableBlockPos p,
                                   Struct s, int x, int z, int h, int lx, int lz) {
        double rr = Math.sqrt(lx * lx + lz * lz);
        if (rr > 22) return false;
        if (rr <= 7) return true; // орхестра — мощёная площадка
        if (lz >= 0) {
            // ярусы: каждое кольцо на блок выше
            int tier = (int) ((rr - 7) / 2.2);
            fill(c, p, x, z, h + 1, h + tier, tier % 2 == 0 ? STONE_BRICKS : SMOOTH_QUARTZ);
            put(c, p, x, h + tier + 1, z, QUARTZ_SLAB);
            return true;
        }
        // скена: стена с колоннами за орхестрой
        if (lz <= -8 && Math.abs(lx) <= 9) {
            if (Math.abs(lx) == 9 || lz <= -10) {
                fill(c, p, x, z, h + 1, h + 6, SMOOTH_QUARTZ);
            } else if (Math.floorMod(lx, 3) == 0) {
                fill(c, p, x, z, h + 1, h + 5, QUARTZ_PILLAR);
            }
            return true;
        }
        return true;
    }

    // ---- сфинкс у Гизы ----

    private static boolean sphinx(ChunkAccess c, BlockPos.MutableBlockPos p,
                                  Struct s, int x, int z, int h, int lx, int lz) {
        if (lx < -4 || lx > 16 || Math.abs(lz) > 3) return false;
        // тело льва
        if (lx <= 10 && Math.abs(lz) <= 2) fill(c, p, x, z, h + 1, h + 4, SANDSTONE);
        // лапы вперёд
        if (lx > 10 && Math.abs(lz) <= 2 && (Math.abs(lz) == 2 || lx <= 13)) {
            fill(c, p, x, z, h + 1, h + 2, SMOOTH_SAND);
        }
        // голова с немесом
        if (lx >= 7 && lx <= 10 && Math.abs(lz) <= 1) {
            for (int y = 5; y <= 8; y++) {
                BlockState st = (y == 6 && Math.abs(lz) == 1) ? BLUE
                        : (y == 8 ? GOLD : SMOOTH_SAND);
                put(c, p, x, h + y, z, st);
            }
        }
        return true;
    }

    // ---- поле мастаб: ряды гробниц между пирамидами ----

    private static boolean mastabas(ChunkAccess c, BlockPos.MutableBlockPos p,
                                    Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 125 || Math.abs(lz) > 65) return false;
        int gx = Math.floorMod(lx, 18), gz = Math.floorMod(lz, 14);
        if (gx > 8 || gz > 5) return false; // улицы между гробницами
        int cellX = Math.floorDiv(lx, 18), cellZ = Math.floorDiv(lz, 14);
        if (hash(cellX, cellZ, 900) % 4 == 0) return false; // пустые участки
        boolean edge = gx == 0 || gx == 8 || gz == 0 || gz == 5;
        int hh = 3 + hash(cellX, cellZ, 901) % 3;
        fill(c, p, x, z, h + 1, h + hh, edge ? CUT_SAND : SANDSTONE);
        put(c, p, x, h + hh + 1, z, SMOOTH_SAND);
        return true;
    }

    // ---- гавань: молы в море с фонарями ----

    private static boolean pier(ChunkAccess c, BlockPos.MutableBlockPos p,
                                Struct s, int x, int z, int h, int lx, int lz) {
        double a = Math.toRadians(s.p0);
        int dx = (int) Math.round(Math.cos(a)), dz = (int) Math.round(Math.sin(a));
        // два мола от края города в сторону моря
        int r = 60;
        for (int off : new int[]{-14, 14}) {
            int px = -dz * off, pz = dx * off; // перпендикуляр
            for (int t = 0; t <= 45; t++) {
                int bx = (int) Math.round(dx * (r * 0.8 + t)) + px;
                int bz = (int) Math.round(dz * (r * 0.8 + t)) + pz;
                if (Math.abs(lx - bx) > 2 || Math.abs(lz - bz) > 2) continue;
                int deck = EarthData.SEA_LEVEL + 2;
                if (h <= deck - 1) fill(c, p, x, z, h + 1, deck, STONE_BRICKS);
                put(c, p, x, deck, z, SMOOTH_SAND);
                if (Math.abs(lx - bx) == 2 && t % 10 == 0) {
                    put(c, p, x, deck + 1, z, STONE_BRICKS);
                    put(c, p, x, deck + 2, z, LANTERN);
                }
                return true;
            }
        }
        return false;
    }

    // ---- римские термы: зал с купальней, свод, окна ----

    private static boolean baths(ChunkAccess c, BlockPos.MutableBlockPos p,
                                 Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 11 || Math.abs(lz) > 8) return false;
        boolean wall = Math.abs(lx) == 11 || Math.abs(lz) == 8;
        if (wall) {
            boolean window = Math.floorMod(lx + lz, 4) == 2;
            boolean door = lx == -11 && Math.abs(lz) <= 1;
            for (int y = 1; y <= 7; y++) {
                if (door && y <= 3) continue;
                if (window && y >= 4 && y <= 6) continue;
                put(c, p, x, h + y, z, y % 3 == 0 ? BRICKS : STONE_BRICKS);
            }
            return true;
        }
        // свод: ступенчатый полуциркуль
        int vault = 8 + (int) Math.round(3 * (1 - Math.abs(lz) / 8.0));
        put(c, p, x, h + vault, z, BRICKS);
        // купальня
        if (lx >= -6 && lx <= 2 && Math.abs(lz) <= 3) {
            put(c, p, x, h, z, SMOOTH_QUARTZ);
            put(c, p, x, h - 0, z, SMOOTH_QUARTZ);
            put(c, p, x, h + 0, z, SMOOTH_QUARTZ);
            if (lx >= -5 && lx <= 1 && Math.abs(lz) <= 2) put(c, p, x, h + 1, z, WATER);
            return true;
        }
        // колонны зала
        if (Math.floorMod(lx, 4) == 0 && Math.abs(lz) == 5) {
            fill(c, p, x, z, h + 1, h + 6, QUARTZ_PILLAR);
        }
        return true;
    }

    // ---- вилла с садом: стены, двор, бассейн, живая изгородь ----

    private static boolean villa(ChunkAccess c, BlockPos.MutableBlockPos p,
                                 Struct s, int x, int z, int h, int lx, int lz) {
        if (Math.abs(lx) > 10 || Math.abs(lz) > 8) return false;
        boolean wall = Math.abs(lx) == 10 || Math.abs(lz) == 8;
        if (wall) {
            boolean door = lx == 10 && Math.abs(lz) <= 1;
            boolean window = Math.floorMod(lx * 3 + lz, 5) == 1;
            for (int y = 1; y <= 5; y++) {
                if (door && y <= 2) continue;
                if (window && y == 3) continue;
                put(c, p, x, h + y, z, s.palette == 2 ? SANDSTONE : SMOOTH_QUARTZ);
            }
            put(c, p, x, h + 6, z, RED);
            return true;
        }
        // жилая часть с плоской крышей (западная половина)
        if (lx <= -2) {
            boolean innerWall = lx == -2 || Math.abs(lz) >= 6;
            if (innerWall) fill(c, p, x, z, h + 1, h + 4, s.palette == 2 ? SANDSTONE : SMOOTH_QUARTZ);
            put(c, p, x, h + 5, z, PLANKS);
            return true;
        }
        // сад: бассейн, изгородь, клумбы, дерево
        if (lx >= 0 && lx <= 8 && Math.abs(lz) <= 6) {
            if (lx >= 2 && lx <= 5 && Math.abs(lz) <= 1) {
                put(c, p, x, h, z, SMOOTH_QUARTZ);
                put(c, p, x, h + 1, z, WATER);
                return true;
            }
            if (Math.abs(lz) == 5 && Math.floorMod(lx, 2) == 0) {
                fill(c, p, x, z, h + 1, h + 2, LEAVES);
                return true;
            }
            if (Math.floorMod(lx + lz * 2, 7) == 0) {
                put(c, p, x, h + 1, z, hash(x, z, 33) % 2 == 0 ? FLOWER1 : FLOWER2);
                return true;
            }
            if (lx == 7 && lz == 4) { // дерево в саду
                fill(c, p, x, z, h + 1, h + 4, LOG);
                for (int ddy = 4; ddy <= 6; ddy++) put(c, p, x, h + ddy, z, LEAVES);
                return true;
            }
        }
        return true;
    }
}
