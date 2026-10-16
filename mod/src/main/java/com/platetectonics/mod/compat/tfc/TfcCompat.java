package com.platetectonics.mod.compat.tfc;

import com.mojang.serialization.MapCodec;
import com.platetectonics.mod.PlateTectonicsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers the TFC generator. Only called when TFC is loaded, so no TFC class loads without it. */
public final class TfcCompat {
    private TfcCompat() {
    }

    public static void register(IEventBus modBus) {
        DeferredRegister<MapCodec<? extends ChunkGenerator>> generators =
                DeferredRegister.create(Registries.CHUNK_GENERATOR, PlateTectonicsMod.MOD_ID);
        DeferredRegister<MapCodec<? extends BiomeSource>> biomeSources =
                DeferredRegister.create(Registries.BIOME_SOURCE, PlateTectonicsMod.MOD_ID);
        generators.register("planet_tfc", () -> TfcPlanetChunkGenerator.CODEC);
        biomeSources.register("planet_tfc", () -> TfcPlanetBiomeSource.CODEC);
        generators.register(modBus);
        biomeSources.register(modBus);
    }
}
