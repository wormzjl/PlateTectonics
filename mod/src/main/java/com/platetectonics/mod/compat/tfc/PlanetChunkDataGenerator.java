package com.platetectonics.mod.compat.tfc;

import com.platetectonics.geosim.climate.Biome;
import com.platetectonics.geosim.world.MasterWorld;
import com.platetectonics.geosim.world.TerrainSampler;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataGenerator;
import net.dries007.tfc.world.chunkdata.ChunkRockDataCache;
import net.dries007.tfc.world.chunkdata.ForestType;
import net.dries007.tfc.world.chunkdata.LerpFloatLayer;
import net.dries007.tfc.world.settings.RockSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * TFC's per-chunk data from the simulated planet (docs/design/08-minecraft-integration.md#chunk-generators):
 * climate corners for TFC's climate model (seasons, crops, snow, colours) and the rock at any point, which
 * TFC asks for during generation and afterwards (veins, collapses, prospecting), so it is a pure function of
 * position: thread-safe and the same forever.
 * <p>
 * Phase 4 rock model: sediment-thick cells show sedimentary layers to their sediment depth, ocean crust
 * basalt over gabbro, continents granite, gneiss and schist where deeply exhumed and volcanic or low-grade
 * rock where not. Lateral patches about 300 blocks across vary the choice. The real stratigraphic column
 * comes in Phase 6.
 */
public final class PlanetChunkDataGenerator implements ChunkDataGenerator {
    private static final List<String> SEDIMENTARY = List.of("shale", "limestone", "claystone", "conglomerate",
            "dolomite", "shale", "chalk", "chert");
    private static final List<String> DEEP_CONTINENTAL = List.of("granite", "gneiss", "schist", "diorite", "granite",
            "marble", "quartzite", "gneiss");
    private static final List<String> SHALLOW_CONTINENTAL = List.of("andesite", "rhyolite", "phyllite", "slate", "dacite",
            "granite", "quartzite", "diorite");

    private final TerrainSampler sampler;
    private final Map<String, RockSettings> rocks = new HashMap<>();
    private final RockSettings fallback;

    public PlanetChunkDataGenerator(TerrainSampler sampler, java.util.Collection<RockSettings> rockSettings) {
        this.sampler = sampler;
        for (RockSettings r : rockSettings) {
            String path = BuiltInRegistries.BLOCK.getKey(r.raw()).getPath(); // rock/raw/granite
            rocks.put(path.substring(path.lastIndexOf('/') + 1), r);
        }
        if (rocks.isEmpty()) {
            throw new IllegalStateException("TFC rock_layer_settings lists no rocks");
        }
        this.fallback = rocks.getOrDefault("granite", rocks.values().iterator().next());
    }

    TerrainSampler sampler() {
        return sampler;
    }

    // ------------------------------------------------------------------ climate (PARTIAL)

    @Override
    public ChunkData generate(ChunkData data) {
        if (data.status() != ChunkData.Status.EMPTY) {
            return data;
        }
        ChunkPos pos = data.getPos();
        int x0 = pos.getMinBlockX();
        int z0 = pos.getMinBlockZ();
        float[] rain = new float[4];
        float[] temp = new float[4];
        for (int k = 0; k < 4; k++) {
            int x = x0 + (k & 1) * 16;
            int z = z0 + (k >> 1) * 16;
            double e = sampler.elevationM(x, z);
            rain[k] = tfcRainfall(sampler.precipitationMm(x, z));
            temp[k] = (float) seaLevelTemperature(x, z, e);
        }
        // TFC corners: value00 (x0, z0), value01 (x0, z1), value10 (x1, z0), value11 (x1, z1)
        LerpFloatLayer rainfall = new LerpFloatLayer(rain[0], rain[2], rain[1], rain[3]);
        LerpFloatLayer temperature = new LerpFloatLayer(temp[0], temp[2], temp[1], temp[3]);
        LerpFloatLayer variance = new LerpFloatLayer(0f, 0f, 0f, 0f);
        data.generatePartial(rainfall, variance, rainfall, temperature, forest(x0 + 8, z0 + 8));
        return data;
    }

    /** TFC rainfall runs 0..500; Earth's wettest forests (2,500 mm/yr and more) map to the top. */
    static float tfcRainfall(double precipitationMm) {
        return (float) Math.max(0.0, Math.min(500.0, precipitationMm * 0.2));
    }

    /** TFC applies its own lapse rate from y, so it wants the temperature reduced to sea level. */
    private double seaLevelTemperature(int x, int z, double elevationM) {
        return sampler.temperatureC(x, z, elevationM) + 6.5 / 1000.0 * Math.max(0.0, elevationM);
    }

    private ForestType forest(int x, int z) {
        Biome b = sampler.biome(x, z);
        return switch (b) {
            case TROPICAL_RAINFOREST, TEMPERATE_RAINFOREST -> ForestType.PRIMARY_DIVERSE;
            case TEMPERATE_FOREST, TROPICAL_SEASONAL_FOREST -> ForestType.SECONDARY_DIVERSE;
            case BOREAL_FOREST -> ForestType.PRIMARY_MONOCULTURE;
            case SAVANNA -> ForestType.SAVANNA_DIVERSE;
            case TEMPERATE_GRASSLAND, COLD_STEPPE -> ForestType.SPARSE;
            case TUNDRA -> ForestType.SHRUBLAND;
            default -> ForestType.GRASSLAND;
        };
    }

    // ------------------------------------------------------------------ rock

    @Override
    public RockSettings generateRock(int x, int y, int z, int surfaceY, ChunkRockDataCache cache) {
        MasterWorld m = sampler.world();
        int cell = sampler.cellIndex(x, z);
        double blocksPerKm = 1000.0 / sampler.spec().metersPerBlock();
        int depth = Math.max(0, surfaceY - y);
        int sediment = (int) Math.min(80.0, m.sedimentKm[cell] * blocksPerKm);
        int patch = patch(x, z);
        if (depth < sediment) {
            // beds 6-13 blocks thick, the sequence different in each patch
            int bed = (depth + (patch & 7)) / (6 + (patch >>> 3 & 7));
            return rock(SEDIMENTARY.get(Math.floorMod(patch + bed * 3, SEDIMENTARY.size())));
        }
        int below = depth - sediment;
        if (m.crustType[cell] == 0) {
            return rock(below < 40 ? "basalt" : "gabbro");
        }
        boolean exhumed = m.exhumedKm[cell] > 4.0 || below > 60;
        List<String> suite = exhumed ? DEEP_CONTINENTAL : SHALLOW_CONTINENTAL;
        return rock(suite.get(Math.floorMod(patch >>> 6, suite.size())));
    }

    private RockSettings rock(String name) {
        return rocks.getOrDefault(name, fallback);
    }

    /** A hash for irregular patches about 300 blocks across (boundaries wiggle by a few dozen blocks). */
    private static int patch(int x, int z) {
        double wx = x + 40.0 * Math.sin(z / 97.0) + 17.0 * Math.sin(z / 23.0);
        double wz = z + 40.0 * Math.sin(x / 89.0) + 17.0 * Math.sin(x / 29.0);
        long px = (long) Math.floor(wx / 300.0);
        long pz = (long) Math.floor(wz / 300.0);
        long h = px * 0x9E3779B97F4A7C15L ^ pz * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (int) (h & 0x7FFFFFFF);
    }

    @Override
    public void displayDebugInfo(List<String> tooltip, BlockPos pos, int surfaceY) {
        tooltip.add(String.format(Locale.ROOT, "PlateTectonics rock: %s",
                BuiltInRegistries.BLOCK.getKey(generateRock(pos.getX(), pos.getY(), pos.getZ(), surfaceY, null).raw())));
    }
}
