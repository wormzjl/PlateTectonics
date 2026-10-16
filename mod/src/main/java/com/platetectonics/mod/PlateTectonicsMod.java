package com.platetectonics.mod;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.platetectonics.geosim.GeoSim;
import com.platetectonics.geosim.world.PlanetSpec;
import com.platetectonics.mod.world.PlanetBiomeSource;
import com.platetectonics.mod.world.PlanetChunkGenerator;
import com.platetectonics.mod.world.PlanetLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(PlateTectonicsMod.MOD_ID)
public final class PlateTectonicsMod {
    public static final String MOD_ID = "platetectonics";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, MOD_ID);
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
            DeferredRegister.create(Registries.BIOME_SOURCE, MOD_ID);

    static {
        CHUNK_GENERATORS.register("planet", () -> PlanetChunkGenerator.CODEC);
        BIOME_SOURCES.register("planet", () -> PlanetBiomeSource.CODEC);
    }

    public PlateTectonicsMod(IEventBus modBus, ModContainer container) {
        CHUNK_GENERATORS.register(modBus);
        BIOME_SOURCES.register(modBus);
        if (ModList.get().isLoaded("tfc")) {
            com.platetectonics.mod.compat.tfc.TfcCompat.register(modBus);
            LOGGER.info("PlateTectonics: TFC found, the planet_tfc world type is available");
        }
        NeoForge.EVENT_BUS.addListener(PlanetLoader::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(PlanetLoader::onCreateSpawn);
        PlanetSpec earth = PlanetSpec.earth(PlanetSpec.DEFAULT_SCALE);
        LOGGER.info("PlateTectonics loaded (geosim {}): Earth-scale planet at {} m/block, {} blocks around",
                GeoSim.SIM_VERSION, earth.metersPerBlock(), Math.round(earth.blocksAround()));
    }
}
