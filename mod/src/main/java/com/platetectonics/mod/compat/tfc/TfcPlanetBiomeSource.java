package com.platetectonics.mod.compat.tfc;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.platetectonics.geosim.world.TerrainSampler;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * TFC biomes from the simulated terrain (D-17). TFC biomes describe landforms, not climate: TFC takes
 * temperature, rain and forest type from ChunkData, so the choice here only depends on depth, height, local
 * relief and ice. Phase 4 uses a small set; volcanic, karst and glacial landforms follow with the region
 * refinement and rock columns.
 */
public final class TfcPlanetBiomeSource extends BiomeSource {
    public static final MapCodec<TfcPlanetBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            RegistryOps.retrieveGetter(Registries.BIOME)
    ).apply(i, i.stable(TfcPlanetBiomeSource::new)));

    private final Map<String, Holder<Biome>> biomes = new LinkedHashMap<>();
    private volatile TerrainSampler sampler;

    public TfcPlanetBiomeSource(HolderGetter<Biome> lookup) {
        for (String id : new String[]{"deep_ocean", "ocean", "shore", "lowlands", "plains", "rolling_hills", "hills",
                "highlands", "plateau", "mountains", "old_mountains", "ice_sheet"}) {
            biomes.put(id, lookup.getOrThrow(ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("tfc", id))));
        }
    }

    void attach(TerrainSampler sampler) {
        this.sampler = sampler;
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return biomes.values().stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler climate) {
        TerrainSampler s = sampler;
        if (s == null) {
            throw new IllegalStateException("PlateTectonics planet is not loaded yet");
        }
        double bx = QuartPos.toBlock(x) + 2;
        double bz = QuartPos.toBlock(z) + 2;
        return biomes.get(landform(s, bx, bz));
    }

    static String landform(TerrainSampler s, double bx, double bz) {
        double e = s.elevationM(bx, bz);
        if (e < -1_800.0) {
            return "deep_ocean";
        }
        if (e < -15.0) {
            return "ocean";
        }
        if (e < 6.0 && s.baseElevationM(bx, bz) < 60.0) {
            return "shore";
        }
        if (s.iceM(bx, bz) > 1.0) {
            return "ice_sheet";
        }
        double relief = s.reliefM(bx, bz);
        if (e > 2_400.0) {
            return relief > 1_200.0 ? "mountains" : "plateau";
        }
        if (e > 1_100.0) {
            if (relief > 900.0) {
                // ranges that stopped rising wear down: low erosion means an old range
                int cell = s.cellIndex(bx, bz);
                return s.world().erosionMPerMyr[cell] < 30.0 ? "old_mountains" : "mountains";
            }
            return "highlands";
        }
        if (e > 350.0) {
            return relief > 450.0 ? "hills" : "rolling_hills";
        }
        if (e < 40.0 && s.aridity(bx, bz) > 1.0) {
            return "lowlands";
        }
        return "plains";
    }
}
