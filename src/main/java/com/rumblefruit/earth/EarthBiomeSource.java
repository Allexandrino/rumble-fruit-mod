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

// biome assignment for the ancient-Mediterranean dimension: altitude bands
// (beach → plains/forest → grove → stony peaks → jagged snow peaks) plus the
// North-African desert belt; moisture noise splits forest/plains and
// desert/savanna
public class EarthBiomeSource extends BiomeSource {
    public static final MapCodec<EarthBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(Biome.CODEC.listOf().fieldOf("biomes").forGetter(s -> s.biomes))
                    .apply(instance, EarthBiomeSource::new));

    // order matters: 0 ocean, 1 beach, 2 desert, 3 savanna, 4 plains,
    // 5 forest, 6 grove, 7 stony_peaks, 8 jagged_peaks
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
        int h = EarthData.surfaceHeight(bx, bz);

        if (h < EarthData.SEA_LEVEL) {
            return biomes.get(0); // ocean
        }
        if (h <= EarthData.SEA_LEVEL + 2) {
            return biomes.get(1); // beach
        }
        if (h >= 230) {
            return biomes.get(8); // jagged_peaks — высокий снег
        }
        if (h >= 165) {
            return biomes.get(7); // stony_peaks
        }
        if (h >= 115) {
            return biomes.get(6); // grove
        }
        double m = moisture(bx >> 5, bz >> 5); // coarse cells, ~32 blocks
        if (lat < 33.5) {
            // север Африки и Аравия: пустыня/саванна
            return m < 0.55 ? biomes.get(2) : biomes.get(3);
        }
        return m > 0.5 ? biomes.get(5) : biomes.get(4); // forest / plains
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
