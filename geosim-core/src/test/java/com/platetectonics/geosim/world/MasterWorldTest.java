package com.platetectonics.geosim.world;

import com.platetectonics.geosim.history.WorldHistory;
import com.platetectonics.geosim.parallel.Parallel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasterWorldTest {
    private static MasterWorld small() {
        return MasterWorld.simulate(new MasterWorld.Settings(5L, 128, 300.0, WorldHistory.Preset.RANDOM, 2_620.0),
                Parallel.SERIAL, null);
    }

    @Test
    void savesAndLoadsEveryField(@TempDir Path dir) throws Exception {
        MasterWorld m = small();
        Path file = dir.resolve("master.ptw");
        MasterWorldIO.write(m, file);
        MasterWorld r = MasterWorldIO.read(file);
        assertEquals(m.settings, r.settings);
        assertEquals(m.simVersion, r.simVersion);
        assertEquals(m.grid.width(), r.grid.width());
        assertEquals(m.grid.cellKm(), r.grid.cellKm());
        assertArrayEquals(m.elevationM, r.elevationM);
        assertArrayEquals(m.precipitationMm, r.precipitationMm);
        assertArrayEquals(m.biome, r.biome);
        assertArrayEquals(m.plate, r.plate);
        assertArrayEquals(m.crustFormedMyr, r.crustFormedMyr);
    }

    @Test
    void samplerMatchesCellsAndWraps() {
        MasterWorld m = small();
        PlanetSpec spec = PlanetSpec.earth(30.0);
        TerrainSampler s = new TerrainSampler(m, spec);
        double blocksPerCell = m.grid.cellKm() * 1000.0 / spec.metersPerBlock();
        // at a cell centre the smooth surface is the cell's own elevation
        int i = 17;
        int j = 30;
        double x = (i + 0.5) * blocksPerCell;
        double z = spec.northPoleZ() + (j + 0.5) * blocksPerCell;
        assertEquals(m.elevationM[j * m.grid.width() + i], s.baseElevationM(x, z), 1e-3);
        // one trip around the planet lands on the same terrain
        double around = spec.blocksAround();
        for (double px : new double[]{0.0, 12_345.0, -777.0}) {
            assertEquals(s.elevationM(px, z), s.elevationM(px + around, z), 1e-6 * around);
        }
        // the surface is continuous between neighbouring blocks (no seams at cell borders)
        double edge = (i + 1) * blocksPerCell;
        assertTrue(Math.abs(s.elevationM(edge - 0.5, z) - s.elevationM(edge + 0.5, z)) < 50.0);
    }
}
