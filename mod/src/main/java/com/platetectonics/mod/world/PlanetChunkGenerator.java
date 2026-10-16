package com.platetectonics.mod.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.platetectonics.geosim.climate.Biome;
import com.platetectonics.geosim.world.MasterWorld;
import com.platetectonics.geosim.world.PlanetSpec;
import com.platetectonics.geosim.world.TerrainSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * Terrain from the simulated planet with vanilla blocks ({@code platetectonics:planet}, docs/design/08-minecraft-integration.md).
 * Phase 4 fills each column straight from {@link TerrainSampler}: bedrock, deepslate and stone up to the
 * rock surface, a cover chosen from climate, slope and water depth, ice sheets as packed ice, and water to
 * sea level. Caves, river channels and strata come in later phases.
 */
public class PlanetChunkGenerator extends ChunkGenerator {
    public static final MapCodec<PlanetChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(g -> g.biomeSource),
            PlanetSettings.CODEC.fieldOf("planet").forGetter(PlanetChunkGenerator::planetSettings)
    ).apply(i, i.stable(PlanetChunkGenerator::new)));

    protected static final int SEA_LEVEL = PlanetSpec.SEA_LEVEL_Y;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState WATER = Blocks.WATER.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState BEDROCK = Blocks.BEDROCK.defaultBlockState();
    private static final BlockState PACKED_ICE = Blocks.PACKED_ICE.defaultBlockState();
    private static final BlockState ICE = Blocks.ICE.defaultBlockState();
    private static final BlockState SNOW_BLOCK = Blocks.SNOW_BLOCK.defaultBlockState();
    private static final BlockState SNOW = Blocks.SNOW.defaultBlockState();
    private static final BlockState GRASS = Blocks.GRASS_BLOCK.defaultBlockState();
    private static final BlockState SNOWY_GRASS = GRASS.setValue(SnowyDirtBlock.SNOWY, true);
    private static final BlockState DIRT = Blocks.DIRT.defaultBlockState();
    private static final BlockState COARSE_DIRT = Blocks.COARSE_DIRT.defaultBlockState();
    private static final BlockState PODZOL = Blocks.PODZOL.defaultBlockState();
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState SANDSTONE = Blocks.SANDSTONE.defaultBlockState();
    private static final BlockState GRAVEL = Blocks.GRAVEL.defaultBlockState();
    private static final BlockState CLAY = Blocks.CLAY.defaultBlockState();

    private final PlanetSettings settings;
    private volatile TerrainSampler sampler;

    public PlanetChunkGenerator(BiomeSource biomeSource, PlanetSettings settings) {
        super(biomeSource);
        this.settings = settings;
    }

    public PlanetSettings planetSettings() {
        return settings;
    }

    /** Hands the loaded planet to the generator and its biome source (server start, before any chunk). */
    public void attach(MasterWorld world) {
        TerrainSampler s = new TerrainSampler(world, settings.spec());
        if (biomeSource instanceof PlanetBiomeSource p) {
            p.attach(s);
        }
        this.sampler = s;
    }

    public boolean attached() {
        return sampler != null;
    }

    public TerrainSampler sampler() {
        TerrainSampler s = sampler;
        if (s == null) {
            throw new IllegalStateException("PlateTectonics planet is not loaded yet");
        }
        return s;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // ------------------------------------------------------------------ columns

    /** Heights of one column: rock surface, top of an ice sheet (= rock if none) and climate. */
    public record Column(double elevationM, double temperatureC, int rockTop, int iceTop) {
        public boolean underwater() {
            return rockTop < SEA_LEVEL - 1;
        }
    }

    protected Column column(TerrainSampler s, int x, int z, int minY, int maxY) {
        double e = s.elevationM(x, z);
        double t = s.temperatureC(x, z, e);
        int rock = clamp((int) Math.floor(s.surfaceY(e)), minY + 1, maxY);
        int ice = rock;
        if (e > 0.0) {
            double iceM = s.iceM(x, z);
            if (iceM > 1.0) {
                ice = clamp(rock + (int) Math.round(Math.min(iceM, 4_000.0) / s.spec().metersPerBlock()), rock, maxY);
            }
        }
        return new Column(e, t, rock, ice);
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : Math.min(v, hi);
    }

    // ------------------------------------------------------------------ fill

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
                                                        StructureManager structureManager, ChunkAccess chunk) {
        TerrainSampler s = sampler();
        ChunkPos pos = chunk.getPos();
        int x0 = pos.getMinBlockX();
        int z0 = pos.getMinBlockZ();
        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight() - 1;
        // one ring beyond the chunk for slopes
        Column[] cols = new Column[18 * 18];
        for (int dz = -1; dz <= 16; dz++) {
            for (int dx = -1; dx <= 16; dx++) {
                cols[(dz + 1) * 18 + dx + 1] = column(s, x0 + dx, z0 + dz, minY, maxY);
            }
        }
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int c = (lz + 1) * 18 + lx + 1;
                Column col = cols[c];
                int slope = Math.max(Math.max(Math.abs(cols[c - 1].rockTop - col.rockTop), Math.abs(cols[c + 1].rockTop - col.rockTop)),
                        Math.max(Math.abs(cols[c - 18].rockTop - col.rockTop), Math.abs(cols[c + 18].rockTop - col.rockTop)));
                fillColumn(chunk, s, x0 + lx, z0 + lz, lx, lz, col, slope, minY, oceanFloor, worldSurface);
            }
        }
        onFilled(chunk, cols);
        return CompletableFuture.completedFuture(chunk);
    }

    /** Called after a chunk's columns are filled; {@code cols} is 18 x 18 with a one-column ring. */
    protected void onFilled(ChunkAccess chunk, Column[] cols) {
    }

    /** Sea water (TFC: salt water). */
    protected BlockState seaWater() {
        return WATER;
    }

    /** Frozen sea surface. */
    protected BlockState seaIce() {
        return ICE;
    }

    private void fillColumn(ChunkAccess chunk, TerrainSampler s, int x, int z, int lx, int lz, Column col, int slope,
                            int minY, Heightmap oceanFloor, Heightmap worldSurface) {
        Cover cover = cover(s, x, z, col, slope);
        int top = col.rockTop();
        int coverBottom = top - cover.depth + 1;
        int underBottom = cover.under == null ? coverBottom : coverBottom - cover.underDepth;
        for (int y = minY; y <= top; y++) {
            BlockState state = y == top ? cover.top
                    : y >= coverBottom ? cover.filler
                    : y >= underBottom ? cover.under
                    : baseRock(x, y, z, minY, top);
            set(chunk, lx, y, lz, state);
        }
        BlockState solid = cover.top;
        int solidTop = top;
        for (int y = top + 1; y <= col.iceTop(); y++) {
            solid = y == col.iceTop() ? SNOW_BLOCK : PACKED_ICE;
            solidTop = y;
            set(chunk, lx, y, lz, solid);
        }
        oceanFloor.update(lx, solidTop, lz, solid);
        worldSurface.update(lx, solidTop, lz, solid);
        if (col.underwater()) {
            boolean frozen = col.temperatureC() < -2.0;
            for (int y = top + 1; y < SEA_LEVEL; y++) {
                set(chunk, lx, y, lz, frozen && y == SEA_LEVEL - 1 ? seaIce() : seaWater());
            }
            worldSurface.update(lx, SEA_LEVEL - 1, lz, seaWater());
        } else if (cover.snowLayer && solidTop + 1 < chunk.getMaxBuildHeight()) {
            set(chunk, lx, solidTop + 1, lz, SNOW);
            worldSurface.update(lx, solidTop + 1, lz, SNOW);
        }
    }

    /**
     * Rock at a point below the surface ({@code surfaceY} = top of the rock column): stone above y = 0,
     * deepslate below with a dithered boundary, a ragged bedrock floor.
     */
    protected BlockState baseRock(int x, int y, int z, int minY, int surfaceY) {
        if (y < minY + 5 && (y == minY || hash(x, y, z) % 5 < minY + 5 - y)) {
            return BEDROCK;
        }
        if (y < 0 || (y < 8 && hash(x, y, z) % 8 >= y)) {
            return DEEPSLATE;
        }
        return STONE;
    }

    protected static int hash(int x, int y, int z) {
        long h = x * 0x9E3779B97F4A7C15L ^ y * 0xC2B2AE3D27D4EB4FL ^ z * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (int) (h & 0x7FFFFFFF);
    }

    private static void set(ChunkAccess chunk, int lx, int y, int lz, BlockState state) {
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
        section.setBlockState(lx, y & 15, lz, state, false);
    }

    // ------------------------------------------------------------------ surface cover

    /** Top block, the filler under it ({@code depth} blocks including the top), an optional deeper layer. */
    public record Cover(BlockState top, BlockState filler, int depth, BlockState under, int underDepth, boolean snowLayer) {
        public static Cover of(BlockState top, BlockState filler, int depth) {
            return new Cover(top, filler, depth, null, 0, false);
        }
    }

    protected Cover cover(TerrainSampler s, int x, int z, Column col, int slope) {
        double t = col.temperatureC();
        double e = col.elevationM();
        if (col.underwater()) {
            int depth = SEA_LEVEL - 1 - col.rockTop();
            if (depth > 30 || t < 4.0) {
                return Cover.of(GRAVEL, GRAVEL, 3);
            }
            if (depth > 12 && hash(x, 0, z) % 7 == 0) {
                return Cover.of(CLAY, CLAY, 2);
            }
            return new Cover(SAND, SAND, 4, SANDSTONE, 2, false);
        }
        if (col.iceTop() > col.rockTop()) {
            return Cover.of(GRAVEL, GRAVEL, 2);
        }
        boolean beach = col.rockTop() <= SEA_LEVEL + 1 && s.baseElevationM(x, z) < 60.0;
        if (beach) {
            return new Cover(SAND, SAND, 4, SANDSTONE, 3, t < -1.0);
        }
        if (slope >= 4) {
            return new Cover(STONE, STONE, 1, null, 0, false);
        }
        boolean snow = t < -1.0;
        if (e > 2_600.0) {
            return t < -3.0 ? new Cover(SNOW_BLOCK, SNOW_BLOCK, 2, null, 0, false) : Cover.of(STONE, STONE, 1);
        }
        Biome b = Biome.classify(e, t, s.precipitationMm(x, z), s.aridity(x, z), 0.0);
        BlockState grass = snow ? SNOWY_GRASS : GRASS;
        return switch (b) {
            case HOT_DESERT, COLD_DESERT -> slope >= 3 ? Cover.of(SANDSTONE, SANDSTONE, 2)
                    : new Cover(SAND, SAND, 4, SANDSTONE, 4, false);
            case POLAR_DESERT -> new Cover(GRAVEL, GRAVEL, 2, null, 0, snow);
            case COLD_STEPPE, SAVANNA -> new Cover(hash(x, 1, z) % 5 == 0 ? COARSE_DIRT : grass, DIRT, 3, null, 0, snow);
            case BOREAL_FOREST -> new Cover(hash(x, 2, z) % 3 == 0 ? PODZOL : grass, DIRT, 4, null, 0, snow);
            case TUNDRA -> new Cover(grass, DIRT, 2, null, 0, snow);
            default -> slope >= 3 ? new Cover(COARSE_DIRT, DIRT, 2, null, 0, snow) : new Cover(grass, DIRT, 4, null, 0, snow);
        };
    }

    // ------------------------------------------------------------------ queries without a chunk

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        TerrainSampler s = sampler();
        int minY = level.getMinBuildHeight();
        Column col = column(s, x, z, minY, level.getMaxBuildHeight() - 1);
        if (col.underwater() && type.isOpaque().test(seaWater())) {
            return SEA_LEVEL;
        }
        return col.iceTop() + 1;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor height, RandomState random) {
        TerrainSampler s = sampler();
        int minY = height.getMinBuildHeight();
        Column col = column(s, x, z, minY, height.getMaxBuildHeight() - 1);
        BlockState[] states = new BlockState[height.getHeight()];
        for (int k = 0; k < states.length; k++) {
            int y = minY + k;
            states[k] = y <= col.rockTop() ? baseRock(x, y, z, minY, col.rockTop())
                    : y <= col.iceTop() ? PACKED_ICE
                    : col.underwater() && y < SEA_LEVEL ? seaWater() : AIR;
        }
        return new NoiseColumn(minY, states);
    }

    // ------------------------------------------------------------------ everything else

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        // the cover is placed with the column in fillFromNoise
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager,
                             StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // caves come with the region refinement (Phase 5)
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        ChunkPos chunkPos = level.getCenter();
        Holder<net.minecraft.world.level.biome.Biome> biome =
                level.getBiome(chunkPos.getWorldPosition().atY(level.getMaxBuildHeight() - 1));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(level.getSeed(), chunkPos.getMinBlockX(), chunkPos.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(level, biome, chunkPos, random);
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        TerrainSampler s = sampler;
        if (s == null) {
            return;
        }
        double e = s.elevationM(pos.getX(), pos.getZ());
        int cell = s.cellIndex(pos.getX(), pos.getZ());
        MasterWorld m = s.world();
        info.add(String.format(Locale.ROOT, "Planet: lat %.2f, %.0f m (cell %.0f m), %.1f C, %.0f mm/yr, %s",
                s.latitudeDeg(pos.getZ()), e, m.elevationM[cell], s.temperatureC(pos.getX(), pos.getZ(), e),
                s.precipitationMm(pos.getX(), pos.getZ()), m.biomeAt(cell).name().toLowerCase(Locale.ROOT)));
        info.add(String.format(Locale.ROOT, "Planet: plate %d, %s crust %.0f km formed %.0f Ma, sediment %.2f km",
                m.plate[cell], m.crustType[cell] == 1 ? "continental" : "oceanic", m.crustKm[cell],
                m.settings.ageMyr() - m.crustFormedMyr[cell], m.sedimentKm[cell]));
    }
}
