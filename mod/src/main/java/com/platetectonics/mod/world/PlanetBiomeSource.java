package com.platetectonics.mod.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.platetectonics.geosim.world.TerrainSampler;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;

import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Vanilla biomes from the simulated climate (D-17, vanilla mode): each block column is classified by
 * {@link com.platetectonics.geosim.climate.Biome} and mapped to the closest vanilla biome, with oceans split
 * by water temperature and mountains by height. The biome holders come from the registry when the world is
 * loaded, so the JSON only needs {@code {"type": "platetectonics:planet"}}.
 */
public final class PlanetBiomeSource extends BiomeSource {
    public static final MapCodec<PlanetBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(i, i.stable(PlanetBiomeSource::new)));

    private final Map<com.platetectonics.geosim.climate.Biome, Holder<Biome>> land =
            new EnumMap<>(com.platetectonics.geosim.climate.Biome.class);
    private final Holder<Biome> warmOcean;
    private final Holder<Biome> lukewarmOcean;
    private final Holder<Biome> ocean;
    private final Holder<Biome> coldOcean;
    private final Holder<Biome> frozenOcean;
    private final Holder<Biome> deepLukewarmOcean;
    private final Holder<Biome> deepOcean;
    private final Holder<Biome> deepColdOcean;
    private final Holder<Biome> deepFrozenOcean;
    private final Holder<Biome> beach;
    private final Holder<Biome> snowyBeach;
    private final Holder<Biome> stonyPeaks;
    private final Holder<Biome> jaggedPeaks;
    private final Holder<Biome> frozenPeaks;
    private final Holder<Biome> snowySlopes;
    private volatile TerrainSampler sampler;

    public PlanetBiomeSource(HolderGetter<Biome> biomes) {
        warmOcean = biomes.getOrThrow(Biomes.WARM_OCEAN);
        lukewarmOcean = biomes.getOrThrow(Biomes.LUKEWARM_OCEAN);
        ocean = biomes.getOrThrow(Biomes.OCEAN);
        coldOcean = biomes.getOrThrow(Biomes.COLD_OCEAN);
        frozenOcean = biomes.getOrThrow(Biomes.FROZEN_OCEAN);
        deepLukewarmOcean = biomes.getOrThrow(Biomes.DEEP_LUKEWARM_OCEAN);
        deepOcean = biomes.getOrThrow(Biomes.DEEP_OCEAN);
        deepColdOcean = biomes.getOrThrow(Biomes.DEEP_COLD_OCEAN);
        deepFrozenOcean = biomes.getOrThrow(Biomes.DEEP_FROZEN_OCEAN);
        beach = biomes.getOrThrow(Biomes.BEACH);
        snowyBeach = biomes.getOrThrow(Biomes.SNOWY_BEACH);
        stonyPeaks = biomes.getOrThrow(Biomes.STONY_PEAKS);
        jaggedPeaks = biomes.getOrThrow(Biomes.JAGGED_PEAKS);
        frozenPeaks = biomes.getOrThrow(Biomes.FROZEN_PEAKS);
        snowySlopes = biomes.getOrThrow(Biomes.SNOWY_SLOPES);
        put(biomes, com.platetectonics.geosim.climate.Biome.ICE_SHEET, Biomes.SNOWY_SLOPES);
        put(biomes, com.platetectonics.geosim.climate.Biome.POLAR_DESERT, Biomes.SNOWY_PLAINS);
        put(biomes, com.platetectonics.geosim.climate.Biome.TUNDRA, Biomes.SNOWY_PLAINS);
        put(biomes, com.platetectonics.geosim.climate.Biome.BOREAL_FOREST, Biomes.TAIGA);
        put(biomes, com.platetectonics.geosim.climate.Biome.COLD_STEPPE, Biomes.WINDSWEPT_HILLS);
        put(biomes, com.platetectonics.geosim.climate.Biome.COLD_DESERT, Biomes.DESERT);
        put(biomes, com.platetectonics.geosim.climate.Biome.TEMPERATE_GRASSLAND, Biomes.PLAINS);
        put(biomes, com.platetectonics.geosim.climate.Biome.TEMPERATE_FOREST, Biomes.FOREST);
        put(biomes, com.platetectonics.geosim.climate.Biome.TEMPERATE_RAINFOREST, Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        put(biomes, com.platetectonics.geosim.climate.Biome.HOT_DESERT, Biomes.DESERT);
        put(biomes, com.platetectonics.geosim.climate.Biome.SAVANNA, Biomes.SAVANNA);
        put(biomes, com.platetectonics.geosim.climate.Biome.TROPICAL_SEASONAL_FOREST, Biomes.SPARSE_JUNGLE);
        put(biomes, com.platetectonics.geosim.climate.Biome.TROPICAL_RAINFOREST, Biomes.JUNGLE);
    }

    private void put(HolderGetter<Biome> biomes, com.platetectonics.geosim.climate.Biome b, ResourceKey<Biome> key) {
        land.put(b, biomes.getOrThrow(key));
    }

    /** Called once the planet is loaded; until then no biome can be answered. */
    void attach(TerrainSampler sampler) {
        this.sampler = sampler;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.concat(land.values().stream(), Stream.of(warmOcean, lukewarmOcean, ocean, coldOcean, frozenOcean,
                deepLukewarmOcean, deepOcean, deepColdOcean, deepFrozenOcean, beach, snowyBeach, stonyPeaks, jaggedPeaks,
                frozenPeaks, snowySlopes)).distinct();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler climate) {
        TerrainSampler s = sampler;
        if (s == null) {
            throw new IllegalStateException("PlateTectonics planet is not loaded yet");
        }
        double bx = QuartPos.toBlock(x) + 2;
        double bz = QuartPos.toBlock(z) + 2;
        double e = s.elevationM(bx, bz);
        double t = s.temperatureC(bx, bz, e);
        return pick(s, bx, bz, e, t);
    }

    /** The biome for a column with detailed elevation {@code e} and temperature {@code t}. */
    Holder<Biome> pick(TerrainSampler s, double bx, double bz, double e, double t) {
        if (e < 0.0) {
            boolean deep = e < -200.0;
            if (t < -2.0) {
                return deep ? deepFrozenOcean : frozenOcean;
            }
            if (t < 8.0) {
                return deep ? deepColdOcean : coldOcean;
            }
            if (t < 18.0) {
                return deep ? deepOcean : ocean;
            }
            if (t < 24.0) {
                return deep ? deepLukewarmOcean : lukewarmOcean;
            }
            return deep ? deepLukewarmOcean : warmOcean;
        }
        if (e < 4.0 * s.spec().metersPerBlock() / 30.0 && s.baseElevationM(bx, bz) < 60.0) {
            return t < 0.0 ? snowyBeach : beach;
        }
        com.platetectonics.geosim.climate.Biome b = com.platetectonics.geosim.climate.Biome.classify(e, t,
                s.precipitationMm(bx, bz), s.aridity(bx, bz), s.iceM(bx, bz));
        if (b != com.platetectonics.geosim.climate.Biome.ICE_SHEET && e > 2_600.0) {
            if (t < -6.0) {
                return frozenPeaks;
            }
            if (t < 2.0) {
                return e > 3_800.0 ? jaggedPeaks : snowySlopes;
            }
            return stonyPeaks;
        }
        return land.get(b);
    }
}
