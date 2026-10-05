package com.rumblefruit.earth;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;

// the earth dimension generator: terrain follows the real heightmap
// (continents, ocean floors, mountain ranges), biomes come from
// EarthBiomeSource; biome decoration (trees, ores, grass) is inherited
// from vanilla biomes via the default ChunkGenerator pipeline
public class EarthChunkGenerator extends ChunkGenerator {
    public static final MapCodec<EarthChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource))
                    .apply(instance, EarthChunkGenerator::new));

    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState SANDSTONE = Blocks.SANDSTONE.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState SNOW = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState COARSE_DIRT = Blocks.COARSE_DIRT.defaultBlockState();
    private static final BlockState PODZOL = Blocks.PODZOL.defaultBlockState();
    private static final BlockState TERRACOTTA = Blocks.TERRACOTTA.defaultBlockState();
    private static final BlockState RED_SAND = Blocks.RED_SAND.defaultBlockState();
    private static final BlockState RED_SANDSTONE = Blocks.RED_SANDSTONE.defaultBlockState();

    // лёгкий детерминированный шум для пятен поверхности
    private static double patchNoise(int x, int z, int cell) {
        double fx = (double) x / cell, fz = (double) z / cell;
        int x0 = (int) Math.floor(fx), z0 = (int) Math.floor(fz);
        double tx = fx - x0, tz = fz - z0;
        tx = tx * tx * (3 - 2 * tx);
        tz = tz * tz * (3 - 2 * tz);
        double n00 = phash(x0, z0), n10 = phash(x0 + 1, z0);
        double n01 = phash(x0, z0 + 1), n11 = phash(x0 + 1, z0 + 1);
        return (n00 * (1 - tx) + n10 * tx) * (1 - tz) + (n01 * (1 - tx) + n11 * tx) * tz;
    }

    private static double phash(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h ^= h >> 16;
        return ((h & 0xFFFF) / 32767.5) - 1.0;
    }

    public EarthChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState random, BiomeManager biomes,
                             StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step) {
        // real terrain needs no caves carved blind — heightmap rules
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures,
                             RandomState random, ChunkAccess chunk) {
        // the surface is laid down in fillFromNoise directly
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // biome-driven spawning still applies later; nothing extra here
    }

    @Override
    public int getGenDepth() {
        return 1024;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState random,
                                                        StructureManager structures, ChunkAccess chunk) {
        int minY = chunk.getMinBuildHeight();
        int x0 = chunk.getPos().getMinBlockX();
        int z0 = chunk.getPos().getMinBlockZ();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = x0 + dx, z = z0 + dz;
                int h = EarthData.worldHeight(x, z);
                double lat = EarthData.latFromBlock(z);
                boolean underwater = h < EarthData.SEA_LEVEL;
                BlockState top;
                BlockState under;
                if (underwater) {
                    top = ((x * 31 + z * 17) & 3) == 0 ? GRAVEL : SAND;
                    under = SAND;
                } else if (h >= 480) {
                    top = SNOW;   // вечные снега (реальная снеговая линия ~2500 м)
                    under = STONE;
                } else if (h >= 330) {
                    // скалистый высокогорный пояс с гравийными осыпями
                    double scree = patchNoise(x, z, 24);
                    top = scree > 0.3 ? GRAVEL : STONE;
                    under = STONE;
                } else if (h <= EarthData.SEA_LEVEL + 2) {
                    top = SAND;   // beach
                    under = SANDSTONE;
                } else if (lat < 33.5 && h < 130) {
                    // североафриканская пустыня: песок, пятна красного песка,
                    // у подножий — обожжённая глина
                    double dune = patchNoise(x, z, 48);
                    if (h > 100 && dune > 0.25) {
                        top = TERRACOTTA;
                        under = RED_SANDSTONE;
                    } else if (dune > 0.45) {
                        top = RED_SAND;
                        under = RED_SANDSTONE;
                    } else {
                        top = SAND;
                        under = SANDSTONE;
                    }
                } else {
                    // средиземноморье: луга, сухая коштила, лесная подстилка,
                    // каменистые холмы; на крутых склонах — скальные выходы
                    int sx1 = EarthData.surfaceHeight(x + 8, z) - EarthData.surfaceHeight(x - 8, z);
                    int sz1 = EarthData.surfaceHeight(x, z + 8) - EarthData.surfaceHeight(x, z - 8);
                    double patch = patchNoise(x, z, 32);
                    if (Math.abs(sx1) + Math.abs(sz1) > 10) {
                        top = STONE;   // утёс на крутом склоне
                        under = STONE;
                    } else if (h > 120 && patch > 0.2) {
                        top = STONE;
                        under = STONE;
                    } else if (patch > 0.38) {
                        top = COARSE_DIRT;
                        under = DIRT;
                    } else if (patch < -0.42) {
                        top = PODZOL;
                        under = DIRT;
                    } else {
                        top = GRASS;
                        under = DIRT;
                    }
                }
                // улицы городов и римские дороги перекрывают поверхность
                BlockState cityTop = EarthCities.surfaceTop(x, z, h);
                if (cityTop != null) {
                    top = cityTop;
                } else {
                    BlockState roadTop = EarthRoads.surfaceTop(x, z);
                    if (roadTop != null) top = roadTop;
                }
                for (int y = minY; y <= h; y++) {
                    BlockState state;
                    if (y == h) {
                        state = top;
                    } else if (y > h - 4) {
                        state = under;
                    } else {
                        state = y < 0 ? DEEPSLATE : STONE;
                    }
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                }
                if (underwater) {
                    for (int y = h + 1; y <= EarthData.SEA_LEVEL; y++) {
                        chunk.setBlockState(pos.set(x, y, z), WATER, false);
                    }
                }
                // стены, дома, форум, пирамиды
                EarthCities.buildAbove(chunk, pos, x, z, h);
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int h = EarthData.worldHeight(x, z);
        return Math.max(h + 1, EarthData.SEA_LEVEL + 1);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        // every cell must be non-null: vanilla structure placement (e.g.
        // ruined portals) walks the column calling BlockState#isAir, and a
        // null above the terrain NPEs the chunk generator
        int h = EarthData.worldHeight(x, z);
        int minY = level.getMinBuildHeight();
        BlockState[] column = new BlockState[level.getHeight()];
        for (int i = 0; i < column.length; i++) {
            int y = minY + i;
            if (y <= h) {
                column[i] = y < 0 ? DEEPSLATE : STONE;
            } else if (y <= EarthData.SEA_LEVEL) {
                column[i] = WATER;
            } else {
                column[i] = AIR;
            }
        }
        return new NoiseColumn(minY, column);
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getSeaLevel() {
        return EarthData.SEA_LEVEL;
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) {
        return 80;
    }

    @Override
    public void addDebugScreenInfo(List<String> text, RandomState random, BlockPos pos) {
        double lon = EarthData.lonFromBlock(pos.getX());
        double lat = EarthData.latFromBlock(pos.getZ());
        text.add("Earth: lat=" + String.format("%.2f", lat) + " lon=" + String.format("%.2f", lon));
    }
}
