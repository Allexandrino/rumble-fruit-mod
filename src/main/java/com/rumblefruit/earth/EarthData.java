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

// real-Earth data for the earth dimension: a 2048x1024 equirectangular
// heightmap (brightness 143 = sea level, NASA/GEBCO topography+bathymetry),
// Natural Earth 110m country borders and a curated list of cities/landmarks.
// scale: 1 degree = 111 blocks (1 block ~ 1 km); x=0 is Greenwich, z=0 is
// the equator, north is -z (vanilla convention)
public final class EarthData {
    public static final double BLOCKS_PER_DEGREE = 111.0;
    public static final int SEA_LEVEL = 63;

    private static volatile int[] heightPixels; // 2048*1024 grayscale
    private static final int HM_W = 2048, HM_H = 1024;

    private static volatile List<Country> countries;
    private static volatile List<Place> places;

    public record Country(String name, String nameRu, List<double[][]> polygons) {}
    public record Place(String id, String name, String nameRu, double lat, double lon) {}

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
            try (InputStream in = EarthData.class.getResourceAsStream("/data/rumblefruit/earth/heightmap.png")) {
                BufferedImage img = ImageIO.read(in);
                int[] px = new int[HM_W * HM_H];
                for (int y = 0; y < HM_H; y++) {
                    for (int x = 0; x < HM_W; x++) {
                        px[y * HM_W + x] = img.getRGB(x, y) & 0xFF;
                    }
                }
                heightPixels = px;
            } catch (Exception e) {
                throw new IllegalStateException("rumblefruit: failed to load earth heightmap", e);
            }
        }
    }

    // bilinear sample of the heightmap; lon in [-180,180), lat in [-90,90]
    private static double brightness(double lon, double lat) {
        ensureHeightmap();
        double u = (lon + 180.0) / 360.0 * HM_W;
        double v = (90.0 - lat) / 180.0 * HM_H;
        u = Math.max(0, Math.min(HM_W - 1.001, u));
        v = Math.max(0, Math.min(HM_H - 1.001, v));
        int x0 = (int) u, y0 = (int) v;
        double fx = u - x0, fy = v - y0;
        int[] px = heightPixels;
        double p00 = px[y0 * HM_W + x0];
        double p10 = px[y0 * HM_W + Math.min(x0 + 1, HM_W - 1)];
        double p01 = px[Math.min(y0 + 1, HM_H - 1) * HM_W + x0];
        double p11 = px[Math.min(y0 + 1, HM_H - 1) * HM_W + Math.min(x0 + 1, HM_W - 1)];
        return (p00 * (1 - fx) + p10 * fx) * (1 - fy) + (p01 * (1 - fx) + p11 * fx) * fy;
    }

    // surface height in blocks for a world column
    public static int surfaceHeight(int x, int z) {
        double lon = lonFromBlock(x);
        double lat = latFromBlock(z);
        double b = brightness(lon, lat);
        double meters;
        if (b >= 143.0) {
            meters = (b - 143.0) / 112.0 * 8900.0;   // 255 ~ Everest
        } else {
            meters = -(143.0 - b) / 143.0 * 11000.0; // 0 ~ Mariana trench
        }
        int y = (int) Math.round(SEA_LEVEL + meters / 100.0); // 1 block ~ 100 m
        return Math.max(-40, Math.min(200, y));
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
                    for (JsonElement el : root.getAsJsonArray(section)) {
                        JsonObject o = el.getAsJsonObject();
                        list.add(new Place(
                                o.get("id").getAsString(),
                                o.get("name").getAsString(),
                                o.get("name_ru").getAsString(),
                                o.get("lat").getAsDouble(),
                                o.get("lon").getAsDouble()));
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
}
