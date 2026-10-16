package com.platetectonics.mod.test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.platetectonics.geosim.parallel.Parallel;
import com.platetectonics.geosim.world.MasterWorld;
import com.platetectonics.geosim.world.TerrainSampler;
import com.platetectonics.mod.PlateTectonicsMod;
import com.platetectonics.mod.world.PlanetChunkGenerator;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.Set;

/**
 * In-game checks of the planet generator, run by {@code gradlew :mod:runGameTestServer}: the preset JSON
 * decodes, a small simulated planet fills real chunks whose heights agree with the height queries used by
 * structures, oceans hold water to sea level, and the biome source answers everywhere.
 */
@GameTestHolder(PlateTectonicsMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PlanetGameTests {
    private PlanetGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void planetFillsChunks(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        JsonElement json = JsonParser.parseString("{\"type\": \"platetectonics:planet\", "
                + "\"biome_source\": {\"type\": \"platetectonics:planet\"}, "
                + "\"planet\": {\"grid_width\": 128, \"age_myr\": 300}}");
        ChunkGenerator decoded = ChunkGenerator.CODEC.parse(ops, json).getOrThrow();
        helper.assertTrue(decoded instanceof PlanetChunkGenerator, "preset JSON decodes to the planet generator");
        PlanetChunkGenerator gen = (PlanetChunkGenerator) decoded;
        gen.attach(MasterWorld.simulate(gen.planetSettings().simulation(12_345L), Parallel.SERIAL, null));
        TerrainSampler s = gen.sampler();
        RandomState random = level.getChunkSource().randomState();

        // walk along the equator until one land chunk and one ocean chunk have been checked
        double z = s.spec().equatorZ();
        boolean land = false;
        boolean sea = false;
        for (int x = 0; x < 2_000_000 && !(land && sea); x += 20_011) {
            double e = s.elevationM(x, z);
            if ((e > 50.0 && !land) || (e < -500.0 && !sea)) {
                checkChunk(helper, gen, level, random, new ChunkPos(x >> 4, (int) z >> 4), e > 0.0);
                land |= e > 50.0;
                sea |= e < -500.0;
            }
        }
        helper.assertTrue(land && sea, "found land and sea along the equator");

        Set<Holder<Biome>> seen = new HashSet<>();
        double around = s.spec().blocksAround();
        for (int k = 0; k < 400; k++) {
            int bx = (int) (around * (k % 20) / 20.0);
            int bz = (int) (s.spec().northPoleZ() + s.spec().poleToPoleBlocks() * ((k / 20) + 0.5) / 20.0);
            seen.add(gen.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(bx), 16, QuartPos.fromBlock(bz),
                    random.sampler()));
        }
        helper.assertTrue(seen.size() >= 5, "a planet has many biomes, got " + seen.size());
        helper.succeed();
    }

    /** The TFC generator from the real planet_tfc preset; passes trivially without TFC. */
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void tfcPlanetFillsChunks(GameTestHelper helper) {
        if (!net.neoforged.fml.ModList.get().isLoaded("tfc")) {
            helper.succeed();
            return;
        }
        com.platetectonics.mod.compat.tfc.TfcGameTestChecks.run(helper);
    }

    private static void checkChunk(GameTestHelper helper, PlanetChunkGenerator gen, ServerLevel level, RandomState random,
                                   ChunkPos pos, boolean land) {
        ProtoChunk chunk = new ProtoChunk(pos, UpgradeData.EMPTY, level,
                level.registryAccess().registryOrThrow(Registries.BIOME), null);
        gen.fillFromNoise(Blender.empty(), random, level.structureManager(), chunk).join();
        for (int lz = 0; lz < 16; lz += 5) {
            for (int lx = 0; lx < 16; lx += 5) {
                int x = pos.getMinBlockX() + lx;
                int z = pos.getMinBlockZ() + lz;
                int expected = gen.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, chunk, random);
                int actual = chunk.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, lx, lz) + 1;
                helper.assertTrue(expected == actual, "floor at " + x + "," + z + ": query " + expected + ", chunk " + actual);
                if (!land) {
                    helper.assertTrue(chunk.getBlockState(new net.minecraft.core.BlockPos(x, gen.getSeaLevel() - 1, z))
                            .is(Blocks.WATER), "ocean is filled to sea level at " + x + "," + z);
                }
                helper.assertTrue(chunk.getBlockState(new net.minecraft.core.BlockPos(x, chunk.getMinBuildHeight(), z))
                        .is(Blocks.BEDROCK), "bedrock floor");
            }
        }
    }
}
