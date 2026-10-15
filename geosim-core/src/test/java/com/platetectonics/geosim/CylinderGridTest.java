package com.platetectonics.geosim;

import com.platetectonics.geosim.grid.CylinderGrid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CylinderGridTest {
    private final CylinderGrid grid = new CylinderGrid(8, 4, 10.0);

    @Test
    void columnsWrapEastWest() {
        assertEquals(grid.index(0, 1), grid.index(8, 1));
        assertEquals(grid.index(7, 1), grid.index(-1, 1));
        assertEquals(grid.index(3, 2), grid.index(3 + 8 * 5, 2));
    }

    @Test
    void rowsClampAtPoles() {
        assertEquals(grid.index(2, 0), grid.index(2, -3));
        assertEquals(grid.index(2, 3), grid.index(2, 9));
    }

    @Test
    void neighboursWrapAcrossTheSeam() {
        int[] n = new int[4];
        grid.neighbors4(grid.index(0, 1), n);
        assertEquals(grid.index(1, 1), n[0]); // east
        assertEquals(grid.index(7, 1), n[1]); // west wraps to the last column
        assertEquals(grid.index(0, 0), n[2]); // north
        assertEquals(grid.index(0, 2), n[3]); // south
    }

    @Test
    void planetGridHasSquareCells() {
        CylinderGrid g = CylinderGrid.forPlanet(40_000.0, 2560);
        assertEquals(1280, g.height());
        assertEquals(15.625, g.cellKm(), 1e-12);
        assertEquals(90.0 - 180.0 * 0.5 / 1280, g.latitudeDeg(0), 1e-12);
    }
}
