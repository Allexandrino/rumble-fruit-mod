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
        return 384;
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
                int h = EarthData.surfaceHeight(x, z);
                double lat = Math.abs(EarthData.latFromBlock(z));
                boolean underwater = h < EarthData.SEA_LEVEL;
                BlockState top;
                BlockState under;
                if (underwater) {
                    top = ((x * 31 + z * 17) & 3) == 0 ? GRAVEL : SAND;
                    under = SAND;
                } else if (lat > 66) {
                    top = SNOW;
                    under = DIRT;
                } else if (h <= EarthData.SEA_LEVEL + 2) {
                    top = SAND;
                    under = SANDSTONE;
                } else {
                    top = GRASS;
                    under = DIRT;
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
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int h = EarthData.surfaceHeight(x, z);
        return Math.max(h + 1, EarthData.SEA_LEVEL + 1);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int h = EarthData.surfaceHeight(x, z);
        BlockState[] column = new BlockState[level.getHeight()];
        int minY = level.getMinBuildHeight();
        for (int y = minY; y <= Math.max(h, EarthData.SEA_LEVEL); y++) {
            if (y <= h) {
                column[y - minY] = y < 0 ? DEEPSLATE : STONE;
            } else {
                column[y - minY] = WATER;
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
