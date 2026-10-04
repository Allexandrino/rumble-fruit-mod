package com.rumblefruit.earth;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

// ancient-Mediterranean data for the earth dimension: a high-resolution
// crop of the NASA SRTM heightmap (srtm_ramp2 21600x10800, brightness 12.5 =
// sea level, Everest ~ 219), Natural Earth 110m borders renamed to ancient
// states and a curated list of ancient cities/landmarks.
// scale: 1 degree = 600 blocks; x=0 is Greenwich, z=0 is the equator,
// north is -z (vanilla convention). only the Mediterranean frame
// (lon -10..45, lat 28..48) carries real terrain — beyond it lies open ocean
public final class EarthData {
    // крупный масштаб: 1 градус = 600 блоков, ~18 км на пиксель карты
    public static final double BLOCKS_PER_DEGREE = 600.0;
    public static final int SEA_LEVEL = 63;

    // Mediterranean frame in degrees
    public static final double LON_MIN = -10.0, LON_MAX = 45.0;
    public static final double LAT_MIN = 28.0, LAT_MAX = 48.0;

    // heightmap calibration for srtm_ramp2: sea level ~ 12.5,
    // Everest (8848 m) ~ 219 → ~42.7 m per brightness step
    private static final double SEA_BRIGHTNESS = 12.5;
    private static final double METERS_PER_STEP = 8848.0 / (219.0 - SEA_BRIGHTNESS);
    private static final double METERS_PER_BLOCK = 16.0; // огромные горы

    // вертикальный предел мира — 319; выше 270 мягкое сжатие к потолку,
    // чтобы вершины не срезались в плоские плато
    private static final int Y_MIN = -60;
    private static final double Y_SOFT = 270.0;
    private static final double Y_CAP = 318.0;

    private static volatile int[] heightPixels; // grayscale crop
    private static volatile int hmW, hmH;

    private static volatile List<Country> countries;
    private static volatile List<Place> places;

    public record Country(String name, String nameRu, List<double[][]> polygons) {}
    public record Place(String id, String name, String nameRu, double lat, double lon, boolean city) {}

    private EarthData() {}

    // ---- coordinates ----

    public static double lonFromBlock(int x) {
        double lon = x / BLOCKS_PER_DEGREE;
        lon = lon % 360.0;
        if (lon < -180.0) lon += 360.0;
        if (lon >= 180.0) lon -= 360.0;
        return lon;
    }

    public static double latFromBlock(int z) {
        double lat = -z / BLOCKS_PER_DEGREE;
        return Math.max(-85.0, Math.min(85.0, lat));
    }

    public static int blockFromLon(double lon) {
        return (int) Math.round(lon * BLOCKS_PER_DEGREE);
    }

    public static int blockFromLat(double lat) {
        return (int) Math.round(-lat * BLOCKS_PER_DEGREE);
    }

    // ---- heightmap ----

    private static void ensureHeightmap() {
        if (heightPixels != null) return;
        synchronized (EarthData.class) {
            if (heightPixels != null) return;
            try (InputStream in = EarthData.class.getResourceAsStream("/data/rumblefruit/earth/heightmap_med.png")) {
                BufferedImage img = ImageIO.read(in);
                int w = img.getWidth(), h = img.getHeight();
                int[] px = new int[w * h];
                // Raster.getSample — сырые значения: getRGB гонит grayscale
                // через гамму цветового профиля и искажает высоты
                java.awt.image.Raster raster = img.getRaster();
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        px[y * w + x] = raster.getSample(x, y, 0) & 0xFF;
                    }
                }
                hmW = w;
                hmH = h;
                heightPixels = px;
            } catch (Exception e) {
                throw new IllegalStateException("rumblefruit: failed to load earth heightmap", e);
            }
        }
    }

    // bilinear sample of the heightmap inside the Mediterranean frame;
    // returns -1 outside the frame (open ocean)
    private static double brightness(double lon, double lat) {
        if (lon < LON_MIN || lon > LON_MAX || lat < LAT_MIN || lat > LAT_MAX) {
            return -1.0;
        }
        ensureHeightmap();
        double u = (lon - LON_MIN) / (LON_MAX - LON_MIN) * hmW;
        double v = (LAT_MAX - lat) / (LAT_MAX - LAT_MIN) * hmH;
        u = Math.max(0, Math.min(hmW - 1.001, u));
        v = Math.max(0, Math.min(hmH - 1.001, v));
        int x0 = (int) u, y0 = (int) v;
        double fx = u - x0, fy = v - y0;
        int[] px = heightPixels;
        double p00 = px[y0 * hmW + x0];
        double p10 = px[y0 * hmW + Math.min(x0 + 1, hmW - 1)];
        double p01 = px[Math.min(y0 + 1, hmH - 1) * hmW + x0];
        double p11 = px[Math.min(y0 + 1, hmH - 1) * hmW + Math.min(x0 + 1, hmW - 1)];
        return (p00 * (1 - fx) + p10 * fx) * (1 - fy) + (p01 * (1 - fx) + p11 * fx) * fy;
    }

    // deterministic smooth value noise in [-1, 1] over cells of `cell` blocks —
    // breaks up the blocky terracing of the raw heightmap
    private static double smoothNoise(int x, int z, int cell) {
        double fx = (double) x / cell;
        double fz = (double) z / cell;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = fx - x0, tz = fz - z0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double n00 = nhash(x0, z0);
        double n10 = nhash(x0 + 1, z0);
        double n01 = nhash(x0, z0 + 1);
        double n11 = nhash(x0 + 1, z0 + 1);
        return (n00 * (1 - tx) + n10 * tx) * (1 - tz) + (n01 * (1 - tx) + n11 * tx) * tz;
    }

    private static double nhash(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h ^= h >> 16;
        return ((h & 0xFFFF) / 32767.5) - 1.0;
    }

    // surface height in blocks for a world column
    public static int surfaceHeight(int x, int z) {
        double lon = lonFromBlock(x);
        double lat = latFromBlock(z);
        double b = brightness(lon, lat);
        double y;
        if (b < 0) {
            // beyond the map frame: deep open ocean
            y = SEA_LEVEL - 45 + smoothNoise(x, z, 256) * 6.0;
        } else if (b >= SEA_BRIGHTNESS) {
            double meters = (b - SEA_BRIGHTNESS) * METERS_PER_STEP;
            y = SEA_LEVEL + meters / METERS_PER_BLOCK;
            // smoothing noise fades in with altitude so coastlines stay exact
            double ramp = Math.min(1.0, (y - SEA_LEVEL) / 15.0);
            y += ramp * (smoothNoise(x, z, 8) * 1.5 + smoothNoise(x, z, 96) * 4.0);
            // острые хребты на высокогорье — ломают однотипные склоны
            double ridgeRamp = Math.min(1.0, Math.max(0.0, (y - SEA_LEVEL) / 60.0));
            double ridged = 1.0 - Math.abs(smoothNoise(x, z, 64));
            y += ridgeRamp * ridged * ridged * 22.0;
        } else {
            // Mediterranean seafloor: shallow shelves, deeper basins
            y = SEA_LEVEL - 18 + smoothNoise(x, z, 256) * 10.0 + smoothNoise(x, z, 32) * 2.0;
            if (y > SEA_LEVEL - 4) y = SEA_LEVEL - 4;
        }
        // мягкое сжатие к потолку мира: вершины не срезаются в плато
        if (y > Y_SOFT) {
            y = Y_SOFT + (Y_CAP - Y_SOFT) * (1.0 - Math.exp(-(y - Y_SOFT) / 48.0));
        }
        int h = (int) Math.round(y);
        return Math.max(Y_MIN, Math.min(319, h));
    }

    // ---- countries ----

    private static void ensureCountries() {
        if (countries != null) return;
        synchronized (EarthData.class) {
            if (countries != null) return;
            try (InputStream in = EarthData.class.getResourceAsStream("/data/rumblefruit/earth/countries.geojson")) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                List<Country> list = new ArrayList<>();
                for (JsonElement el : root.getAsJsonArray("features")) {
                    JsonObject f = el.getAsJsonObject();
                    JsonObject props = f.getAsJsonObject("properties");
                    String nameRu = props.has("NAME_RU") && !props.get("NAME_RU").isJsonNull()
                            ? props.get("NAME_RU").getAsString() : null;
                    String name = props.has("NAME_EN") && !props.get("NAME_EN").isJsonNull()
                            ? props.get("NAME_EN").getAsString() : props.get("NAME").getAsString();
                    if (nameRu == null || nameRu.isEmpty()) nameRu = name;
                    JsonObject geom = f.getAsJsonObject("geometry");
                    String type = geom.get("type").getAsString();
                    JsonArray coords = geom.getAsJsonArray("coordinates");
                    List<double[][]> polys = new ArrayList<>();
                    if ("Polygon".equals(type)) {
                        polys.add(readPolygon(coords));
                    } else if ("MultiPolygon".equals(type)) {
                        for (JsonElement p : coords) {
                            polys.add(readPolygon(p.getAsJsonArray()));
                        }
                    }
                    list.add(new Country(name, nameRu, polys));
                }
                countries = list;
            } catch (Exception e) {
                throw new IllegalStateException("rumblefruit: failed to load country borders", e);
            }
        }
    }

    // polygon as flat rings [lon0,lat0,lon1,lat1,...]; ring 0 = outer boundary,
    // the rest are holes — even-odd fill over all rings handles them
    private static double[][] readPolygon(JsonArray rings) {
        double[][] all = new double[rings.size()][];
        for (int i = 0; i < rings.size(); i++) {
            JsonArray ring = rings.get(i).getAsJsonArray();
            double[] flat = new double[ring.size() * 2];
            for (int j = 0; j < ring.size(); j++) {
                JsonArray ll = ring.get(j).getAsJsonArray();
                flat[j * 2] = ll.get(0).getAsDouble();
                flat[j * 2 + 1] = ll.get(1).getAsDouble();
            }
            all[i] = flat;
        }
        return all;
    }

    public static String countryAt(int x, int z, boolean russian) {
        ensureCountries();
        double lon = lonFromBlock(x);
        double lat = latFromBlock(z);
        for (Country c : countries) {
            for (double[][] poly : c.polygons()) {
                if (pointInPolygon(lon, lat, poly)) {
                    return russian ? c.nameRu() : c.name();
                }
            }
        }
        return null; // international waters
    }

    private static boolean pointInPolygon(double lon, double lat, double[][] rings) {
        boolean inside = false;
        for (double[] ring : rings) {
            int n = ring.length / 2;
            for (int i = 0, j = n - 1; i < n; j = i++) {
                double xi = ring[i * 2], yi = ring[i * 2 + 1];
                double xj = ring[j * 2], yj = ring[j * 2 + 1];
                if (((yi > lat) != (yj > lat))
                        && (lon < (xj - xi) * (lat - yi) / (yj - yi) + xi)) {
                    inside = !inside;
                }
            }
        }
        return inside;
    }

    // ---- cities & landmarks ----

    private static void ensurePlaces() {
        if (places != null) return;
        synchronized (EarthData.class) {
            if (places != null) return;
            try (InputStream in = EarthData.class.getResourceAsStream("/data/rumblefruit/earth/cities.json")) {
                JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                List<Place> list = new ArrayList<>();
                for (String section : new String[]{"cities", "landmarks"}) {
                    boolean isCity = "cities".equals(section);
                    for (JsonElement el : root.getAsJsonArray(section)) {
                        JsonObject o = el.getAsJsonObject();
                        list.add(new Place(
                                o.get("id").getAsString(),
                                o.get("name").getAsString(),
                                o.get("name_ru").getAsString(),
                                o.get("lat").getAsDouble(),
                                o.get("lon").getAsDouble(),
                                isCity));
                    }
                }
                places = list;
            } catch (Exception e) {
                throw new IllegalStateException("rumblefruit: failed to load city list", e);
            }
        }
    }

    public static List<Place> places() {
        ensurePlaces();
        return places;
    }

    public static Place findPlace(String id) {
        ensurePlaces();
        for (Place p : places) {
            if (p.id().equalsIgnoreCase(id)) return p;
        }
        return null;
    }

    // nearest known place within maxBlocks of the column, or null
    public static Place nearestPlace(int x, int z, double maxBlocks) {
        ensurePlaces();
        Place best = null;
        double bestDist = maxBlocks;
        for (Place p : places) {
            double dx = blockFromLon(p.lon()) - x;
            double dz = blockFromLat(p.lat()) - z;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    // final terrain: raw heightmap → city plateaus → road corridors.
    // everything that asks "how high is the world here" must use this
    public static int worldHeight(int x, int z) {
        double h = EarthCities.terrain(x, z, surfaceHeight(x, z));
        h = EarthRoads.terrain(x, z, h);
        int r = (int) Math.round(h);
        return Math.max(Y_MIN, Math.min(319, r));
    }
}
