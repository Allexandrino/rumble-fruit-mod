package com.rumblefruit.earth;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// roman roads: every city links to its two nearest neighbours with a
// mostly-straight engineered road (slight waypoint jitter), a smoothed
// elevation profile that cuts through hills, and solid causeways across
// shallow straits. deep-sea links (islands) are not built.
// terrain() is queried per column through a 16-block spatial grid
public final class EarthRoads {

    private static final class Seg {
        double x0, z0, x1, z1, y0, y1;
        double minX, maxX, minZ, maxZ;
    }

    private static volatile boolean ready = false;
    private static final Map<Long, List<Seg>> GRID = new HashMap<>();

    private static final double HALF_WIDTH = 3.0;   // полное сглаживание
    private static final double BLEND = 7.0;        // переход к рельефу

    private EarthRoads() {}

    private static int hash(int a, int b, int c) {
        int h = a * 73428767 + b * 912271 + c * 334343;
        h = (h ^ (h >> 13)) * 1274126177;
        h ^= h >> 16;
        return h & 0x7FFFFFFF;
    }

    private static long cellKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static double terrainAt(int x, int z) {
        return EarthCities.terrain(x, z, EarthData.surfaceHeight(x, z));
    }

    private static void ensure() {
        if (ready) return;
        synchronized (EarthRoads.class) {
            if (ready) return;
            List<EarthCities.City> cs = EarthCities.cities();
            Set<Long> pairs = new HashSet<>();
            for (int i = 0; i < cs.size(); i++) {
                // две ближайшие соседки
                int b1 = -1, b2 = -1;
                double d1 = Double.MAX_VALUE, d2 = Double.MAX_VALUE;
                for (int j = 0; j < cs.size(); j++) {
                    if (i == j) continue;
                    double d = Math.hypot(cs.get(i).cx() - cs.get(j).cx(),
                            cs.get(i).cz() - cs.get(j).cz());
                    if (d < d1) { d2 = d1; b2 = b1; d1 = d; b1 = j; }
                    else if (d < d2) { d2 = d; b2 = j; }
                }
                for (int j : new int[]{b1, b2}) {
                    if (j < 0) continue;
                    pairs.add((long) Math.min(i, j) << 32 | Math.max(i, j));
                }
            }
            for (long p : pairs) {
                buildRoad(cs.get((int) (p >> 32)), cs.get((int) p));
            }
            ready = true;
        }
    }

    // straight line with deterministic waypoint jitter, sampled every 16
    // blocks, smoothed profile, deep-water rejection
    private static void buildRoad(EarthCities.City a, EarthCities.City b) {
        double ax = a.cx(), az = a.cz(), bx = b.cx(), bz = b.cz();
        double len = Math.hypot(bx - ax, bz - az);
        if (len < 50) return;
        // вейпоинты каждые ~200 блоков с небольшим смещением
        int wp = (int) Math.max(1, Math.round(len / 200.0));
        double[] px = new double[wp + 1];
        double[] pz = new double[wp + 1];
        double nx = -(bz - az) / len, nz = (bx - ax) / len; // перпендикуляр
        for (int i = 0; i <= wp; i++) {
            double t = (double) i / wp;
            double off = (i == 0 || i == wp) ? 0
                    : (hash(a.cx(), b.cx(), i) % 100 - 50) / 50.0 * 45.0;
            px[i] = ax + (bx - ax) * t + nx * off;
            pz[i] = az + (bz - az) * t + nz * off;
        }
        // ресэмплинг каждые 16 блоков
        int steps = (int) Math.max(2, Math.round(len / 16.0));
        double[] sx = new double[steps + 1];
        double[] sz = new double[steps + 1];
        double[] sh = new double[steps + 1];
        int segIdx = 0;
        double segT = 0;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps * wp;
            int k = Math.min((int) t, wp - 1);
            double ft = t - k;
            sx[i] = px[k] + (px[k + 1] - px[k]) * ft;
            sz[i] = pz[k] + (pz[k + 1] - pz[k]) * ft;
            sh[i] = terrainAt((int) Math.round(sx[i]), (int) Math.round(sz[i]));
        }
        // отказ от дорог через глубокое море (больше ~240 блоков глубины)
        int deepRun = 0;
        for (double h : sh) {
            deepRun = h < EarthData.SEA_LEVEL - 15 ? deepRun + 1 : 0;
            if (deepRun > 15) return;
        }
        // сглаживание профиля (окно ±4 сэмпла ≈ ±64 блока)
        double[] ry = new double[steps + 1];
        for (int i = 0; i <= steps; i++) {
            double sum = 0;
            int n = 0;
            for (int k = Math.max(0, i - 4); k <= Math.min(steps, i + 4); k++) {
                sum += sh[k];
                n++;
            }
            ry[i] = sum / n;
        }
        // дамбовые мосты через мелкие проливы
        for (int i = 0; i <= steps; i++) {
            if (sh[i] < EarthData.SEA_LEVEL - 2) {
                ry[i] = Math.max(ry[i], EarthData.SEA_LEVEL + 1);
            }
        }
        // сегменты в пространственную сетку
        for (int i = 0; i < steps; i++) {
            Seg s = new Seg();
            s.x0 = sx[i]; s.z0 = sz[i]; s.x1 = sx[i + 1]; s.z1 = sz[i + 1];
            s.y0 = ry[i]; s.y1 = ry[i + 1];
            s.minX = Math.min(s.x0, s.x1) - 10; s.maxX = Math.max(s.x0, s.x1) + 10;
            s.minZ = Math.min(s.z0, s.z1) - 10; s.maxZ = Math.max(s.z0, s.z1) + 10;
            int cx0 = (int) Math.floor(s.minX / 16), cx1 = (int) Math.floor(s.maxX / 16);
            int cz0 = (int) Math.floor(s.minZ / 16), cz1 = (int) Math.floor(s.maxZ / 16);
            for (int cx = cx0; cx <= cx1; cx++) {
                for (int cz = cz0; cz <= cz1; cz++) {
                    GRID.computeIfAbsent(cellKey(cx, cz), k -> new ArrayList<>()).add(s);
                }
            }
        }
    }

    // ближайший сегмент дороги к колонне; null если дальше 10 блоков
    private static Seg nearest(int x, int z, double[] outDistY) {
        List<Seg> list = GRID.get(cellKey(x >> 4, z >> 4));
        if (list == null) return null;
        Seg best = null;
        double bestD = 10.0, bestY = 0;
        for (Seg s : list) {
            if (x < s.minX || x > s.maxX || z < s.minZ || z > s.maxZ) continue;
            double dx = s.x1 - s.x0, dz = s.z1 - s.z0;
            double len2 = dx * dx + dz * dz;
            double t = len2 == 0 ? 0
                    : ((x - s.x0) * dx + (z - s.z0) * dz) / len2;
            t = Math.max(0, Math.min(1, t));
            double qx = s.x0 + dx * t, qz = s.z0 + dz * t;
            double d = Math.hypot(x - qx, z - qz);
            if (d < bestD) {
                bestD = d;
                best = s;
                bestY = s.y0 + (s.y1 - s.y0) * t;
            }
        }
        if (best == null) return null;
        outDistY[0] = bestD;
        outDistY[1] = bestY;
        return best;
    }

    // высота рельефа с учётом дорожного коридора
    public static double terrain(int x, int z, double base) {
        ensure();
        double[] dy = new double[2];
        if (nearest(x, z, dy) == null) return base;
        double d = dy[0], roadY = dy[1];
        // над водой — дамба уровнем чуть выше моря
        double target = base < EarthData.SEA_LEVEL - 2
                ? Math.max(roadY, EarthData.SEA_LEVEL + 1) : roadY;
        if (d <= HALF_WIDTH) return target;
        double s = (d - HALF_WIDTH) / BLEND;
        s = s * s * (3 - 2 * s);
        return target + (base - target) * s;
    }

    // покрытие дороги: каменные плиты в центре, гравий по краям
    public static BlockState surfaceTop(int x, int z) {
        ensure();
        double[] dy = new double[2];
        if (nearest(x, z, dy) == null) return null;
        double d = dy[0];
        if (d <= 1.6) {
            return (hash(x, z, 3) & 5) == 0
                    ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                    : Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (d <= 2.8) {
            return Blocks.GRAVEL.defaultBlockState();
        }
        return null;
    }
}
