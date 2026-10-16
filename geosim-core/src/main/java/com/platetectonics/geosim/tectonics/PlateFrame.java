package com.platetectonics.geosim.tectonics;

/**
 * The exact integer rotation that places a plate's raster on the world (D-42). A turn by {@code theta} is k
 * quarter turns, which are exact, then the rest phi in [-45°, 45°] as three shears that each move whole rows or
 * columns by whole cells (Paeth 1986):
 * <pre>
 *   x1 = u + round(-tan(phi/2) * v)
 *   y  = v + round(sin(phi) * x1)
 *   x  = x1 + round(-tan(phi/2) * y)
 * </pre>
 * Each shear is undone exactly by subtracting the same rounded shift, so the map is one-to-one at any angle and
 * has an exact inverse: two cells never land on one place and none is skipped. A cell lands within about 1.5
 * cells of where an exact rotation would put it; the error depends only on the angle and the cell, so it never
 * builds up while a plate keeps turning.
 * <p>
 * Coordinates are grid cells, x east and y south (the row index). Positive angles turn x toward y, which is
 * clockwise on a north-up map. Points are packed into a long: x in the high half, y in the low half.
 */
final class PlateFrame {
    private static final double QUARTER = Math.PI / 2.0;

    private double theta;
    private int quarter;
    /** -tan(phi / 2). */
    private double shear;
    /** sin(phi). */
    private double lift;
    private double cos = 1.0;
    private double sin;
    private boolean identity = true;

    /** Sets the angle, rad; kept in [-pi, pi). Uses {@link StrictMath}, so every machine gets the same shifts. */
    void set(double theta) {
        double t = theta - 2.0 * Math.PI * Math.floor((theta + Math.PI) / (2.0 * Math.PI));
        this.theta = t;
        if (t == 0.0) {
            quarter = 0;
            shear = 0.0;
            lift = 0.0;
            cos = 1.0;
            sin = 0.0;
            identity = true;
            return;
        }
        int k = (int) Math.round(t / QUARTER);
        double phi = t - k * QUARTER;
        quarter = Math.floorMod(k, 4);
        shear = -StrictMath.tan(phi / 2.0);
        lift = StrictMath.sin(phi);
        cos = StrictMath.cos(t);
        sin = StrictMath.sin(t);
        identity = false;
    }

    double theta() {
        return theta;
    }

    boolean identity() {
        return identity;
    }

    /** Exact rotation of a real offset, x part (for centres of area, not cells). */
    double rotX(double u, double v) {
        return identity ? u : cos * u - sin * v;
    }

    /** Exact rotation of a real offset, y part. */
    double rotY(double u, double v) {
        return identity ? v : sin * u + cos * v;
    }

    /** World offset (x, y) of plate cell (u, v), packed. */
    long forward(int u, int v) {
        if (identity) {
            return pack(u, v);
        }
        int a;
        int b;
        switch (quarter) {
            case 0 -> {
                a = u;
                b = v;
            }
            case 1 -> {
                a = -v;
                b = u;
            }
            case 2 -> {
                a = -u;
                b = -v;
            }
            default -> {
                a = v;
                b = -u;
            }
        }
        int x1 = a + round(shear * b);
        int y = b + round(lift * x1);
        int x = x1 + round(shear * y);
        return pack(x, y);
    }

    /** Plate cell (u, v) that lands on world offset (x, y), packed: the exact inverse of {@link #forward}. */
    long inverse(int x, int y) {
        if (identity) {
            return pack(x, y);
        }
        int x1 = x - round(shear * y);
        int b = y - round(lift * x1);
        int a = x1 - round(shear * b);
        return switch (quarter) {
            case 0 -> pack(a, b);
            case 1 -> pack(b, -a);
            case 2 -> pack(-a, -b);
            default -> pack(-b, a);
        };
    }

    static long pack(int x, int y) {
        return ((long) x << 32) | (y & 0xffffffffL);
    }

    static int x(long packed) {
        return (int) (packed >> 32);
    }

    static int y(long packed) {
        return (int) packed;
    }

    private static int round(double a) {
        return (int) Math.floor(a + 0.5);
    }
}
