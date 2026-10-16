package com.platetectonics.mod.world;

import com.mojang.logging.LogUtils;
import com.platetectonics.geosim.climate.Biome;
import com.platetectonics.geosim.parallel.Parallel;
import com.platetectonics.geosim.world.MasterWorld;
import com.platetectonics.geosim.world.MasterWorldIO;
import com.platetectonics.geosim.world.TerrainSampler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Loads the planet before any level exists, or simulates and saves it on a world's first start
 * ({@code <world>/data/platetectonics/master.ptw}), and picks a spawn on land.
 */
public final class PlanetLoader {
    private static final Logger LOGGER = LogUtils.getLogger();

    private PlanetLoader() {
    }

    public static Path masterFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("platetectonics").resolve("master.ptw");
    }

    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        long seed = server.getWorldData().worldGenOptions().seed();
        for (Map.Entry<net.minecraft.resources.ResourceKey<LevelStem>, LevelStem> e
                : server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).entrySet()) {
            ChunkGenerator generator = e.getValue().generator();
            if (generator instanceof PlanetChunkGenerator planet) {
                planet.attach(load(server, planet.planetSettings(), seed));
                LOGGER.info("PlateTectonics planet attached to {}", e.getKey().location());
            }
        }
    }

    private static MasterWorld load(MinecraftServer server, PlanetSettings settings, long seed) {
        Path file = masterFile(server);
        MasterWorld.Settings wanted = settings.simulation(seed);
        if (Files.exists(file)) {
            try {
                MasterWorld m = MasterWorldIO.read(file);
                if (!m.settings.equals(wanted)) {
                    LOGGER.warn("PlateTectonics: {} was made with [{}], the world settings say [{}]; keeping the saved planet",
                            file, m.settings.describe(), wanted.describe());
                }
                return m;
            } catch (IOException ex) {
                throw new IllegalStateException("PlateTectonics: cannot read " + file + ": " + ex.getMessage(), ex);
            }
        }
        int threads = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
        LOGGER.info("PlateTectonics: simulating a new planet [{}] on {} threads", wanted.describe(), threads);
        long t0 = System.nanoTime();
        int[] lastTenth = {-1};
        MasterWorld m;
        try (Parallel parallel = Parallel.of(threads)) {
            m = MasterWorld.simulate(wanted, parallel, done -> {
                int tenth = (int) (done * 10.0);
                if (tenth != lastTenth[0]) {
                    lastTenth[0] = tenth;
                    LOGGER.info("PlateTectonics: planet {}% simulated", tenth * 10);
                }
            });
        }
        LOGGER.info("PlateTectonics: planet simulated in {} s", (System.nanoTime() - t0) / 1_000_000_000L);
        try {
            MasterWorldIO.write(m, file);
        } catch (IOException ex) {
            throw new IllegalStateException("PlateTectonics: cannot save " + file + ": " + ex.getMessage(), ex);
        }
        return m;
    }

    /**
     * First spawn: the land cell nearest the world origin with a mild climate (spiralling out over the master
     * grid), then the block at its centre if that is dry land.
     */
    public static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(level.getChunkSource().getGenerator() instanceof PlanetChunkGenerator planet) || !planet.attached()) {
            return;
        }
        BlockPos spawn = findSpawn(planet.sampler());
        if (spawn != null) {
            event.getSettings().setSpawn(spawn, 0.0F);
            LOGGER.info("PlateTectonics: spawn at {} {} {}", spawn.getX(), spawn.getY(), spawn.getZ());
            event.setCanceled(true);
        }
    }

    /** The mild, dry-land block nearest the world origin (spiralling out over master cells), or null. */
    public static BlockPos findSpawn(TerrainSampler s) {
        MasterWorld m = s.world();
        int w = m.grid.width();
        int h = m.grid.height();
        double blocksPerCell = m.grid.cellKm() * 1000.0 / s.spec().metersPerBlock();
        int i0 = (int) Math.floor(s.u(0));
        int j0 = (int) Math.floor(s.v(0));
        for (int r = 0; r < Math.max(w, h); r++) {
            for (int dj = -r; dj <= r; dj++) {
                for (int di = -r; di <= r; di++) {
                    if (Math.max(Math.abs(di), Math.abs(dj)) != r) {
                        continue;
                    }
                    int j = j0 + dj;
                    if (j < 0 || j >= h) {
                        continue;
                    }
                    int idx = m.grid.index(i0 + di, j);
                    Biome b = m.biomeAt(idx);
                    if (m.elevationM[idx] < 20.0 || m.temperatureC[idx] < 4.0 || m.temperatureC[idx] > 26.0
                            || b == Biome.ICE_SHEET || b == Biome.HOT_DESERT) {
                        continue;
                    }
                    int x = (int) Math.round((i0 + di + 0.5) * blocksPerCell);
                    int z = (int) Math.round(s.spec().northPoleZ() + (j + 0.5) * blocksPerCell);
                    double e = s.elevationM(x, z);
                    if (e < 2.0) {
                        continue;
                    }
                    return new BlockPos(x, (int) Math.floor(s.surfaceY(e)) + 1, z);
                }
            }
        }
        return null;
    }
}
