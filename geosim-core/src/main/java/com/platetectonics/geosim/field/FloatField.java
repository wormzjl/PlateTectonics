package com.platetectonics.geosim.field;

import com.platetectonics.geosim.grid.CylinderGrid;

/** One float per grid cell, stored row-major like {@link CylinderGrid#index}. */
public final class FloatField {
    private final CylinderGrid grid;
    private final float[] data;

    public FloatField(CylinderGrid grid) {
        this.grid = grid;
        this.data = new float[grid.size()];
    }

    public CylinderGrid grid() {
        return grid;
    }

    /** The backing array (row-major), for tight loops. */
    public float[] data() {
        return data;
    }

    public float get(int index) {
        return data[index];
    }

    public float get(int i, int j) {
        return data[grid.index(i, j)];
    }

    public void set(int index, float value) {
        data[index] = value;
    }

    public void set(int i, int j, float value) {
        data[grid.index(i, j)] = value;
    }

    public float min() {
        float m = Float.POSITIVE_INFINITY;
        for (float v : data) {
            m = Math.min(m, v);
        }
        return m;
    }

    public float max() {
        float m = Float.NEGATIVE_INFINITY;
        for (float v : data) {
            m = Math.max(m, v);
        }
        return m;
    }

    /** Fraction of cells strictly below the given value. */
    public double fractionBelow(double value) {
        int n = 0;
        for (float v : data) {
            if (v < value) {
                n++;
            }
        }
        return (double) n / data.length;
    }
}
