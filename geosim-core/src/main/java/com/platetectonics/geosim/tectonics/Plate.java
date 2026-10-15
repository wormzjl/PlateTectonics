package com.platetectonics.geosim.tectonics;

/** A rigid plate. Velocities in km/Myr; x points east, y points south (increasing grid row). */
public final class Plate {
    public final int id;
    boolean alive = true;

    double vx;
    double vy;
    double driftX;
    double driftY;
    /** Sub-cell displacement carried between substeps, in cells. */
    double accX;
    double accY;
    int shiftX;
    int shiftY;

    double forceX;
    double forceY;

    // statistics refreshed by PlateSimulation.refreshStats()
    int area;
    int continentalArea;
    double centroidI;
    double centroidJ;
    float maxOceanAge;
    double lastRiftTime;

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

    public int area() {
        return area;
    }

    public double continentalFraction() {
        return area == 0 ? 0.0 : (double) continentalArea / area;
    }
}
