package com.platetectonics.geosim.grid;

/**
 * Regular grid on the cylinder planet: column index {@code i} wraps east-west,
 * row index {@code j} runs from the north pole (0) to the south pole (height - 1) and clamps.
 * Cells are stored row-major: {@code index = j * width + i}.
 */
public final class CylinderGrid {
    private final int width;
    private final int height;
    private final double cellKm;

    public CylinderGrid(int width, int height, double cellKm) {
        if (width < 1 || height < 1 || !(cellKm > 0)) {
            throw new IllegalArgumentException("invalid grid " + width + "x" + height + " @ " + cellKm + " km");
        }
        this.width = width;
        this.height = height;
        this.cellKm = cellKm;
    }

    /** Grid covering a planet of the given circumference with square cells (height = width / 2). */
    public static CylinderGrid forPlanet(double circumferenceKm, int width) {
        if (width % 2 != 0) {
            throw new IllegalArgumentException("width must be even so the pole-to-pole extent is half of it");
        }
        return new CylinderGrid(width, width / 2, circumferenceKm / width);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int size() {
        return width * height;
    }

    public double cellKm() {
        return cellKm;
    }

    public double cellAreaKm2() {
        return cellKm * cellKm;
    }

    public int wrapI(int i) {
        return Math.floorMod(i, width);
    }

    public int clampJ(int j) {
        return Math.max(0, Math.min(height - 1, j));
    }

    public int index(int i, int j) {
        return clampJ(j) * width + wrapI(i);
    }

    public int iOf(int index) {
        return index % width;
    }

    public int jOf(int index) {
        return index / width;
    }

    /** Latitude of a row centre in degrees (+north), linear from 90 at the top edge to -90 at the bottom edge. */
    public double latitudeDeg(int j) {
        return 90.0 - 180.0 * (j + 0.5) / height;
    }

    /**
     * The 4 edge neighbours (east, west, north, south) of a cell. At the poles the out-of-range
     * neighbour clamps to the cell itself.
     */
    public void neighbors4(int index, int[] out) {
        int i = iOf(index);
        int j = jOf(index);
        out[0] = index(i + 1, j);
        out[1] = index(i - 1, j);
        out[2] = index(i, j - 1);
        out[3] = index(i, j + 1);
    }
}
