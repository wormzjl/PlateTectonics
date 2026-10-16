package com.platetectonics.mod.compat.tfc;

import com.platetectonics.geosim.history.WorldHistory;
import com.platetectonics.geosim.parallel.Parallel;
import com.platetectonics.geosim.world.MasterWorld;
import com.platetectonics.geosim.world.TerrainSampler;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.settings.RockSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.presets.WorldPreset;

/** GameTest body for the TFC generator; only loaded when TFC is present. */
public final class TfcGameTestChecks {
    private TfcGameTestChecks() {
    }

    public static void run(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldPreset preset = level.registryAccess().registryOrThrow(Registries.WORLD_PRESET).getOrThrow(
                ResourceKey.create(Registries.WORLD_PRESET, ResourceLocation.fromNamespaceAndPath("platetectonics", "planet_tfc")));
        ChunkGenerator generator = preset.createWorldDimensions().dimensions().get(LevelStem.OVERWORLD).generator();
        helper.assertTrue(generator instanceof TfcPlanetChunkGenerator, "planet_tfc preset uses the TFC planet generator");
        TfcPlanetChunkGenerator gen = (TfcPlanetChunkGenerator) generator;
        helper.assertTrue(gen.settings().temperatureScale() == 333_333, "TFC latitude scale follows 30 m/block (D-7)");
        gen.attach(MasterWorld.simulate(new MasterWorld.Settings(777L, 128, 300.0, WorldHistory.Preset.RANDOM, 2_620.0),
                Parallel.SERIAL, null));
        TerrainSampler s = gen.sampler();
        RandomState random = level.getChunkSource().randomState();
        double z = s.spec().equatorZ();
        boolean land = false;
        boolean sea = false;
        for (int x = 0; x < 2_000_000 && !(land && sea); x += 20_011) {
            double e = s.elevationM(x, z);
            if ((e > 50.0 && !land) || (e < -500.0 && !sea)) {
                ChunkPos pos = new ChunkPos(x >> 4, (int) z >> 4);
                ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level,
                        level.registryAccess().registryOrThrow(Registries.BIOME), null);
                gen.fillFromNoise(Blender.empty(), random, level.structureManager(), chunk).join();
                ChunkData data = ChunkData.get(chunk);
                helper.assertTrue(data.status() == ChunkData.Status.FULL, "chunk data is FULL after fill, was " + data.status());
                float t = data.getAverageSeaLevelTemp(pos.getMinBlockX(), pos.getMinBlockZ());
                helper.assertTrue(t > -60f && t < 50f, "sea-level temperature " + t);
                int bx = pos.getMinBlockX() + 3;
                int bz = pos.getMinBlockZ() + 3;
                int top = data.getRockData().getSurfaceHeight()[3 | 3 << 4];
                RockSettings rock = gen.chunkDataGenerator().generateRock(bx, top - 20, bz, top, null);
                helper.assertTrue(chunk.getBlockState(new BlockPos(bx, top - 20, bz)).is(rock.raw()),
                        "rock 20 blocks down is the TFC rock the data generator reports");
                if (e < -500.0) {
                    String fluid = BuiltInRegistries.BLOCK.getKey(chunk.getBlockState(new BlockPos(bx, 62, bz)).getBlock()).toString();
                    helper.assertTrue(fluid.equals("tfc:fluid/salt_water") || fluid.equals("tfc:sea_ice"),
                            "the sea is TFC salt water, got " + fluid);
                }
                land |= e > 50.0;
                sea |= e < -500.0;
            }
        }
        helper.assertTrue(land && sea, "found land and sea along the equator");
        String biome = gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(0), 16, QuartPos.fromBlock((int) z),
                random.sampler()).unwrapKey().orElseThrow().location().getNamespace();
        helper.assertTrue(biome.equals("tfc"), "TFC biomes");
        helper.succeed();
    }
}
