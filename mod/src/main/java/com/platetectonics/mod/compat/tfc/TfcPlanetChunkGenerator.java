package com.platetectonics.mod.compat.tfc;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.platetectonics.geosim.climate.Biome;
import com.platetectonics.geosim.world.MasterWorld;
import com.platetectonics.geosim.world.TerrainSampler;
import com.platetectonics.mod.world.PlanetChunkGenerator;
import com.platetectonics.mod.world.PlanetLoader;
import com.platetectonics.mod.world.PlanetSettings;
import net.dries007.tfc.mixin.accessor.ChunkMapAccessor;
import net.dries007.tfc.world.ChunkGeneratorExtension;
import net.dries007.tfc.world.RandomStateExtension;
import net.dries007.tfc.world.chunkdata.ChunkData;
import net.dries007.tfc.world.chunkdata.ChunkDataGenerator;
import net.dries007.tfc.world.settings.RockSettings;
import net.dries007.tfc.world.settings.Settings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

import java.util.function.UnaryOperator;

/**
 * The planet with TFC blocks and systems ({@code platetectonics:planet_tfc}; registered only when TFC is
 * loaded, A-11). Implements TFC's {@link ChunkGeneratorExtension} so TFC's climate model, seasons, crops,
 * snow, prospecting, veins and features work on our terrain (docs/design/08-minecraft-integration.md):
 * ChunkData climate comes from the simulated climate, rock from {@link PlanetChunkDataGenerator}, and the
 * surface uses TFC soils, sands and salt water.
 */
public final class TfcPlanetChunkGenerator extends PlanetChunkGenerator implements ChunkGeneratorExtension {
    public static final MapCodec<TfcPlanetChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource),
            PlanetSettings.CODEC.fieldOf("planet").forGetter(PlanetChunkGenerator::planetSettings),
            Settings.CODEC.codec().fieldOf("tfc_settings").forGetter(TfcPlanetChunkGenerator::settings)
    ).apply(i, i.stable(TfcPlanetChunkGenerator::new)));

    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState SNOW_BLOCK = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final Aquifer.FluidStatus NO_FLUID = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2,
            Blocks.AIR.defaultBlockState());

    private static final String[] SOIL_ORDERS = {"entisol", "aridisol", "oxisol", "podzol", "mollisol", "alfisol"};

    private final BlockState saltWater;
    private final BlockState seaIce;
    private final java.util.Map<String, BlockState> grassBlocks = new java.util.HashMap<>();
    private final java.util.Map<String, BlockState> dirtBlocks = new java.util.HashMap<>();
    private Settings tfcSettings;
    private volatile PlanetChunkDataGenerator data;

    public TfcPlanetChunkGenerator(BiomeSource biomeSource, PlanetSettings planet, Settings tfc) {
        super(biomeSource, planet);
        // TFC's latitude (D-7): the equator at z = scale / 2, a pole every scale blocks from it
        int scale = (int) Math.round(planet.spec().poleToEquatorBlocks());
        this.tfcSettings = new Settings(tfc.flatBedrock(), tfc.spawnDistance(), tfc.spawnCenterX(), tfc.spawnCenterZ(),
                scale, tfc.temperatureConstant(), tfc.rainfallScale(), tfc.rainfallConstant(), tfc.rockLayerSettings(),
                tfc.continentalness(), tfc.grassDensity(), tfc.finiteContinents());
        this.saltWater = block("fluid/salt_water");
        this.seaIce = block("sea_ice");
        for (String soil : SOIL_ORDERS) {
            grassBlocks.put(soil, block("grass/" + soil));
            dirtBlocks.put(soil, block("dirt/" + soil));
        }
    }

    private static BlockState block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("tfc", path)).defaultBlockState();
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void attach(MasterWorld world) {
        super.attach(world);
        TerrainSampler s = sampler();
        if (getBiomeSource() instanceof TfcPlanetBiomeSource b) {
            b.attach(s);
        }
        data = new PlanetChunkDataGenerator(s, tfcSettings.rockLayerSettings().getRocks());
    }

    // ------------------------------------------------------------------ ChunkGeneratorExtension

    @Override
    public Settings settings() {
        return tfcSettings;
    }

    @Override
    public void applySettings(UnaryOperator<Settings> settings) {
        tfcSettings = settings.apply(tfcSettings);
    }

    @Override
    public ChunkDataGenerator chunkDataGenerator() {
        PlanetChunkDataGenerator d = data;
        if (d == null) {
            throw new IllegalStateException("PlateTectonics planet is not loaded yet");
        }
        return d;
    }

    @Override
    public Aquifer getOrCreateAquifer(ChunkAccess chunk) {
        // no carved caves yet, so nothing for an aquifer to fill
        return Aquifer.createDisabled((x, y, z) -> NO_FLUID);
    }

    @Override
    public BlockPos findSpawnBiome(RandomSource random) {
        BlockPos spawn = PlanetLoader.findSpawn(sampler());
        return spawn != null ? spawn : BlockPos.ZERO;
    }

    @Override
    public void initRandomState(ChunkMap chunkMap, ServerLevel level) {
        // what TFC's own generator does: lets climate-dependent structure placement find this generator
        ((RandomStateExtension) (Object) ((ChunkMapAccessor) chunkMap).accessor$getRandomState())
                .tfc$setChunkGeneratorExtension(this);
    }

    // ------------------------------------------------------------------ chunk data and blocks

    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState,
                                 StructureManager structureManager, ChunkAccess chunk,
                                 StructureTemplateManager structureTemplateManager) {
        chunkDataGenerator().generate(chunk); // climate before structures, as TFC does
        super.createStructures(registryAccess, structureState, structureManager, chunk, structureTemplateManager);
    }

    @Override
    protected void onFilled(ChunkAccess chunk, Column[] cols) {
        ChunkData d = chunkDataGenerator().generate(chunk);
        if (d.status() != ChunkData.Status.PARTIAL) {
            return;
        }
        int[] surface = new int[256];
        int[] aquifer = new int[16];
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                surface[lx | lz << 4] = cols[(lz + 1) * 18 + lx + 1].rockTop();
            }
        }
        for (int k = 0; k < 16; k++) {
            int top = surface[(k & 3) * 4 + 2 | ((k >> 2) * 4 + 2) << 4];
            aquifer[k] = Math.min(top, SEA_LEVEL);
        }
        d.generateFull(surface, aquifer);
    }

    @Override
    protected BlockState baseRock(int x, int y, int z, int minY, int surfaceY) {
        BlockState bedrockOrNull = super.baseRock(x, y, z, minY, surfaceY);
        if (bedrockOrNull == BEDROCK) {
            return BEDROCK;
        }
        return data.generateRock(x, y, z, surfaceY, null).raw().defaultBlockState();
    }

    @Override
    protected BlockState seaWater() {
        return saltWater;
    }

    @Override
    protected BlockState seaIce() {
        return seaIce;
    }

    @Override
    protected Cover cover(TerrainSampler s, int x, int z, Column col, int slope) {
        RockSettings rock = data.generateRock(x, col.rockTop(), z, col.rockTop(), null);
        BlockState raw = rock.raw().defaultBlockState();
        BlockState sand = rock.sand().defaultBlockState();
        BlockState gravel = rock.gravel().defaultBlockState();
        BlockState sandstone = rock.sandstone().defaultBlockState();
        double t = col.temperatureC();
        double e = col.elevationM();
        if (col.underwater()) {
            int depth = SEA_LEVEL - 1 - col.rockTop();
            return depth > 30 || t < 4.0 ? Cover.of(gravel, gravel, 3) : new Cover(sand, sand, 4, sandstone, 2, false);
        }
        if (col.iceTop() > col.rockTop()) {
            return Cover.of(gravel, gravel, 2);
        }
        if (col.rockTop() <= SEA_LEVEL + 1 && s.baseElevationM(x, z) < 60.0) {
            return new Cover(sand, sand, 4, sandstone, 3, t < -1.0);
        }
        if (slope >= 4) {
            return Cover.of(raw, raw, 1);
        }
        boolean snow = t < -1.0;
        if (e > 2_600.0) {
            return t < -3.0 ? new Cover(SNOW_BLOCK, SNOW_BLOCK, 2, null, 0, false) : Cover.of(raw, gravel, 2);
        }
        double precip = s.precipitationMm(x, z);
        double aridity = s.aridity(x, z);
        Biome b = Biome.classify(e, t, precip, aridity, 0.0);
        if (b == Biome.HOT_DESERT || b == Biome.COLD_DESERT) {
            return slope >= 3 ? Cover.of(sandstone, sandstone, 2) : new Cover(sand, sand, 4, sandstone, 4, false);
        }
        if (b == Biome.POLAR_DESERT) {
            return new Cover(gravel, gravel, 2, null, 0, snow);
        }
        String soil = soilOrder(t, precip, aridity, slope);
        BlockState grass = grassBlocks.get(soil);
        BlockState dirt = dirtBlocks.get(soil);
        return new Cover(slope >= 3 ? dirt : grass, dirt, slope >= 3 ? 2 : 4, null, 0, snow);
    }

    /** Soil order (TFC 4) from climate: dry aridisols, tropical oxisols, boreal podzols, grassland mollisols. */
    static String soilOrder(double tempC, double precipMm, double aridity, int slope) {
        if (slope >= 3) {
            return "entisol";
        }
        if (aridity < 0.3) {
            return "aridisol";
        }
        if (tempC > 20.0 && precipMm > 1_500.0) {
            return "oxisol";
        }
        if (tempC < 5.0 && precipMm > 400.0) {
            return "podzol";
        }
        if (aridity < 0.8) {
            return "mollisol";
        }
        return "alfisol";
    }
}
