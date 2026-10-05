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
        int h = EarthData.worldHeight(bx, bz);

        if (h >= EarthData.SEA_LEVEL - 1 && EarthCities.insideCity(bx, bz)) {
            return biomes.get(4); // plains — никаких деревьев посреди улиц
        }
        if (h < EarthData.SEA_LEVEL) {
            return biomes.get(0); // ocean
        }
        if (h <= EarthData.SEA_LEVEL + 2) {
            return biomes.get(1); // beach
        }
        if (h >= 480) {
            return biomes.get(8); // jagged_peaks — снеговая линия ~2500 м
        }
        if (h >= 330) {
            return biomes.get(7); // stony_peaks — скальный пояс
        }
        double m = moisture(bx >> 5, bz >> 5); // coarse cells, ~32 blocks
        if (h >= 200) {
            // лесной пояс ~1200 м: рощи и продуваемые холмы
            return m > 0.45 ? biomes.get(6) : biomes.get(11);
        }
        if (lat < 33.5) {
            // север Африки и Аравия: пустыня/саванна
            return m < 0.55 ? biomes.get(2) : biomes.get(3);
        }
        // средиземноморская равнина: цветочные леса, дубравы, берёзы, луга
        if (m > 0.82) return biomes.get(9);  // flower_forest
        if (m > 0.58) return biomes.get(5);  // forest
        if (m > 0.42) return biomes.get(10); // birch_forest
        return biomes.get(4);                // plains
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
