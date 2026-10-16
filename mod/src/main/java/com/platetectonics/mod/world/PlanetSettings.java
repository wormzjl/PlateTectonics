package com.platetectonics.mod.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.platetectonics.geosim.history.WorldHistory;
import com.platetectonics.geosim.world.MasterWorld;
import com.platetectonics.geosim.world.PlanetSpec;

/**
 * Planet settings stored with the chunk generator in the world preset and level.dat. The seed is the world
 * seed. Defaults follow docs/design/01-world-model.md: 30 m per block, 31.25 km master cells (O-6 open).
 *
 * @param metersPerBlock scale factor S
 * @param gridWidth      master cells around the planet (1,280 = 31.25 km cells, 512 = 78 km for quick worlds)
 * @param ageMyr         elapsed time since formation
 * @param history        "random" or "earth"
 * @param oceanDepthM    ocean water as a mean depth over the planet
 */
public record PlanetSettings(double metersPerBlock, int gridWidth, double ageMyr, String history, double oceanDepthM) {
    public static final MapCodec<PlanetSettings> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.doubleRange(10.0, 100.0).optionalFieldOf("meters_per_block", PlanetSpec.DEFAULT_SCALE)
                    .forGetter(PlanetSettings::metersPerBlock),
            Codec.intRange(64, 2_560).optionalFieldOf("grid_width", 1_280).forGetter(PlanetSettings::gridWidth),
            Codec.doubleRange(100.0, 6_000.0).optionalFieldOf("age_myr", 4_500.0).forGetter(PlanetSettings::ageMyr),
            Codec.STRING.optionalFieldOf("history", "random").forGetter(PlanetSettings::history),
            Codec.doubleRange(500.0, 6_000.0).optionalFieldOf("ocean_depth_m", 2_620.0).forGetter(PlanetSettings::oceanDepthM)
    ).apply(i, PlanetSettings::new));

    public PlanetSpec spec() {
        return PlanetSpec.earth(metersPerBlock);
    }

    public MasterWorld.Settings simulation(long seed) {
        WorldHistory.Preset preset = "earth".equals(history) ? WorldHistory.Preset.EARTH_LIKE : WorldHistory.Preset.RANDOM;
        // the grid needs an even width (height = width / 2)
        return new MasterWorld.Settings(seed, gridWidth & ~1, ageMyr, preset, oceanDepthM);
    }
}
