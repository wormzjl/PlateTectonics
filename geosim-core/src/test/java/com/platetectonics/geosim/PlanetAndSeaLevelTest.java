package com.platetectonics.geosim;

import com.platetectonics.geosim.field.FloatField;
import com.platetectonics.geosim.grid.CylinderGrid;
import com.platetectonics.geosim.terrain.ProtoTerrain;
import com.platetectonics.geosim.terrain.SeaLevel;
import com.platetectonics.geosim.world.PlanetSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanetAndSeaLevelTest {
    private final PlanetSpec earth = PlanetSpec.earth(30.0);

    @Test
    void earthAtThirtyMetresPerBlock() {
        assertEquals(1_333_333.33, earth.blocksAround(), 0.01);
        assertEquals(333_333.33, earth.poleToEquatorBlocks(), 0.01);
    }

    @Test
    void latitudeIsLinearAndAlignedWithTfc() {
        assertEquals(90.0, earth.latitudeDeg(earth.northPoleZ()), 1e-9);
        assertEquals(0.0, earth.latitudeDeg(earth.equatorZ()), 1e-9);
        assertEquals(-90.0, earth.latitudeDeg(earth.southPoleZ()), 1e-9);
        assertEquals(45.0, earth.latitudeDeg(0.0), 1e-9);
    }

    @Test
    void verticalMappingMatchesTheDesign() {
        assertEquals(63.0, earth.elevationToY(0), 1e-9);
        assertEquals(63.0 + 1000.0 / 30.0, earth.elevationToY(1000), 1e-9);
        assertEquals(-37.0, earth.elevationToY(-3000), 1e-9);
        assertEquals(-60.0, earth.elevationToY(-11_000), 1e-9);
        assertEquals(-60.0, earth.elevationToY(-20_000), 1e-9);
        double everest = earth.elevationToY(8_848);
        assertTrue(everest > 296 && everest < 316, "Everest at y=" + everest);
        double prev = Double.NEGATIVE_INFINITY;
        for (int e = -12_000; e <= 12_000; e += 50) {
            double y = earth.elevationToY(e);
            assertTrue(y >= prev, "mapping must be monotonic at " + e);
            assertTrue(y >= -60 && y <= 316);
            prev = y;
        }
    }

    @Test
    void flatFloorFillsToTheRequestedDepth() {
        CylinderGrid g = new CylinderGrid(16, 8, 100.0);
        FloatField flat = new FloatField(g);
        assertEquals(2620.0, SeaLevel.fillToVolume(flat, 2620.0), 1e-6);
    }

    @Test
    void protoTerrainIsDeterministicAndHasOceans() {
        CylinderGrid g = CylinderGrid.forPlanet(40_000.0, 256);
        FloatField a = ProtoTerrain.generate(g, 12345L);
        FloatField b = ProtoTerrain.generate(g, 12345L);
        for (int k = 0; k < g.size(); k++) {
            assertEquals(a.get(k), b.get(k));
        }
        double sea = SeaLevel.fillToVolume(a, SeaLevel.EARTH_MEAN_OCEAN_DEPTH_M);
        double oceanFraction = a.fractionBelow(sea);
        assertTrue(oceanFraction > 0.3 && oceanFraction < 0.98, "ocean fraction " + oceanFraction);
    }
}
