package com.rumblefruit.earth;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.List;
import java.util.stream.Stream;

// biome assignment for the earth dimension: climate bands by real latitude
// (polar/taiga/temperate/subtropic desert belt/tropics) plus a deterministic
// moisture noise for jungle/savanna/desert splits; oceans by the heightmap
public class EarthBiomeSource extends BiomeSource {
    public static final MapCodec<EarthBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Biome.CODEC.listOf().fieldOf("biomes").forGetter(s -> s.biomes))
                    .apply(instance, EarthBiomeSource::new));

    // order matters: 0 ocean, 1 frozen_ocean, 2 beach, 3 desert, 4 savanna,
    // 5 plains, 6 forest, 7 jungle, 8 taiga, 9 snowy_plains
    private final List<Holder<Biome>> biomes;

    public EarthBiomeSource(List<Holder<Biome>> biomes) {
        this.biomes = biomes;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
        int bx = QuartPos.toBlock(quartX);
        int bz = QuartPos.toBlock(quartZ);
        double lat = EarthData.latFromBlock(bz);
        double absLat = Math.abs(lat);
        int h = EarthData.surfaceHeight(bx, bz);

        if (h < EarthData.SEA_LEVEL) {
            return biomes.get(absLat > 58 ? 1 : 0); // frozen_ocean / ocean
        }
        if (h <= EarthData.SEA_LEVEL + 2 && absLat < 55) {
            return biomes.get(2); // beach
        }
        if (absLat > 66) {
            return biomes.get(9); // snowy_plains
        }
        if (absLat > 52) {
            return biomes.get(8); // taiga
        }
        double m = moisture(bx >> 5, bz >> 5); // coarse cells, ~32 blocks
        if (absLat < 23.5) {
            if (m > 0.62) return biomes.get(7); // jungle
            if (m > 0.38) return biomes.get(4); // savanna
            return biomes.get(3); // desert
        }
        if (absLat < 38 && m < 0.42) {
            return biomes.get(3); // subtropical desert belt (Sahara, Gobi, ...)
        }
        return m > 0.55 ? biomes.get(6) : biomes.get(5); // forest / plains
    }

    // deterministic value noise in [0,1), smoothed over neighbouring cells
    private static double moisture(int cx, int cz) {
        double sum = 0;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                sum += hash(cx + dx, cz + dz);
            }
        }
        return sum / 4.0;
    }

    private static double hash(int x, int z) {
        int h = x * 73428767 ^ z * 912271 ^ 0x5DEECE66;
        h = (h ^ (h >> 13)) * 1274126177;
        h ^= h >> 16;
        return (h & 0xFFFF) / 65535.0;
    }
}
