package com.platetectonics.geosim.tectonics;

/**
 * A rigid plate. Velocities in km/Myr; x points east, y points south (increasing grid row).
 * <p>
 * Each plate has its own raster (D-42): every cell it owns keeps a place (u, v) in the plate's frame
 * ({@link Lithosphere}), and the pose puts that place on the world at
 * {@code (originI + x, originJ + y)}, with {@code (x, y) = frame.forward(u, v)} and the column wrapped.
 */
public final class Plate {
    public final int id;
    boolean alive = true;

    double vx;
    double vy;
    double driftX;
    double driftY;
    /** Spin about a vertical axis through the centre of area, rad/Myr; positive turns east toward south. */
    double omega;
    /** Random part of the spin target (slow mantle drift), rad/Myr. */
    double spinDrift;

    /** Angle of the plate's raster on the world. */
    final PlateFrame frame = new PlateFrame();
    /** World cell of the frame origin: column (0..W-1) and row. */
    int originI;
    int originJ;
    /** Sub-cell position of the frame origin carried between substeps, in cells. */
    double accX;
    double accY;
    /** Total turn since the plate (or the plate it split from) formed, rad; and its value when marked. */
    double turned;
    double turnedMark;

    double forceX;
    double forceY;
    double torque;

    // statistics refreshed by PlateSimulation.refreshStats()
    int area;
    int continentalArea;
    double centroidI;
    double centroidJ;
    float maxOceanAge;
    double lastRiftTime;

    // spin statistics refreshed by PlateSimulation.refreshSpin(), in cells
    /** Centre of area in the plate's frame. */
    double pivotU;
    double pivotV;
    /** Centre of area on the world (column unwrapped near originI). */
    double pivotX;
    double pivotY;
    /** Sum of squared distances from the centre of area. */
    double inertia;
    /** The same, each cell weighted by its basal drag (1 + continentalDrag for continent). */
    double spinDrag;
    /** Farthest reach from the centre of area (corner of the frame's bounding box). */
    double radius;
    /** False while the plate is too wide to turn on the cylinder. */
    boolean canSpin;

    Plate(int id, double vx, double vy, double time) {
        this.id = id;
        this.vx = vx;
        this.vy = vy;
        this.lastRiftTime = time;
    }

    public boolean alive() {
        return alive;
    }

    public double vxKmPerMyr() {
        return vx;
    }

    public double vyKmPerMyr() {
        return vy;
    }

    /** Spin, degrees per Myr; positive is clockwise on a north-up map. */
    public double spinDegPerMyr() {
        return Math.toDegrees(omega);
    }

    /** Turn since {@link PlateSimulation#markTurns()} (inherited through splits), degrees; clockwise positive. */
    public double turnSinceMarkDeg() {
        return Math.toDegrees(turned - turnedMark);
    }

    /** Centre of area on the world as of the last step's start, cells (column may lie outside 0..W-1). */
    public double centreI() {
        return pivotX;
    }

    public double centreJ() {
        return pivotY;
    }

    public int area() {
        return area;
    }

    public int continentalArea() {
        return continentalArea;
    }

    public double continentalFraction() {
        return area == 0 ? 0.0 : (double) continentalArea / area;
    }
}
