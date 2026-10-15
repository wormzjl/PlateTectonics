package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.grid.CylinderGrid;

/**
 * World raster of the lithosphere. Front arrays hold the current state; back arrays are written
 * during plate motion and swapped in afterwards.
 */
public final class Lithosphere {
    public static final byte OCEANIC = 0;
    public static final byte CONTINENTAL = 1;

    /** Transient marks, cleared every step and carried along with the crust while it moves. */
    public static final byte MARK_TRENCH = 1;
    public static final byte MARK_RIDGE = 2;
    public static final byte MARK_ARC = 4;
    public static final byte MARK_COLLISION = 8;
    public static final byte MARK_PLUME = 16;

    final CylinderGrid grid;
    final int n;

    int[] owner;
    /** Crust thickness, km. */
    float[] thick;
    /** Oceanic: Myr since the crust formed at a ridge. Continental: unused (0). */
    float[] oceanAge;
    /** Time (Myr since planet formation) the crust became what it is: ridge time or continent formation. */
    float[] formedAt;
    byte[] type;
    byte[] marks;
    /**
     * Excess mantle lithosphere under the crust, km, relative to a normal ~85 km keel: positive where
     * collision thickened it (cold, dense, pulls the surface down), negative where it was removed and hot
     * asthenosphere sits high (holds the surface up). Cratonic keels are buoyant and count as zero.
     */
    float[] keel;
    /** Eclogitized lower crust, km: still part of {@link #thick}, but denser than the mantle. */
    float[] eclogite;

    int[] owner2;
    float[] thick2;
    float[] oceanAge2;
    float[] formedAt2;
    byte[] type2;
    byte[] marks2;
    float[] keel2;
    float[] eclogite2;

    Lithosphere(CylinderGrid grid) {
        this.grid = grid;
        this.n = grid.size();
        owner = new int[n];
        thick = new float[n];
        oceanAge = new float[n];
        formedAt = new float[n];
        type = new byte[n];
        marks = new byte[n];
        owner2 = new int[n];
        thick2 = new float[n];
        oceanAge2 = new float[n];
        formedAt2 = new float[n];
        type2 = new byte[n];
        marks2 = new byte[n];
        keel = new float[n];
        keel2 = new float[n];
        eclogite = new float[n];
        eclogite2 = new float[n];
    }

    void swap() {
        int[] oi = owner; owner = owner2; owner2 = oi;
        float[] f = thick; thick = thick2; thick2 = f;
        f = oceanAge; oceanAge = oceanAge2; oceanAge2 = f;
        f = formedAt; formedAt = formedAt2; formedAt2 = f;
        byte[] b = type; type = type2; type2 = b;
        b = marks; marks = marks2; marks2 = b;
        f = keel; keel = keel2; keel2 = f;
        f = eclogite; eclogite = eclogite2; eclogite2 = f;
    }

    /** Copies front cell {@code from} into back cell {@code to}. */
    void copyToBack(int from, int to) {
        owner2[to] = owner[from];
        thick2[to] = thick[from];
        oceanAge2[to] = oceanAge[from];
        formedAt2[to] = formedAt[from];
        type2[to] = type[from];
        marks2[to] = marks[from];
        keel2[to] = keel[from];
        eclogite2[to] = eclogite[from];
    }

    public CylinderGrid grid() {
        return grid;
    }

    public int owner(int index) {
        return owner[index];
    }

    public float thicknessKm(int index) {
        return thick[index];
    }

    public float oceanAgeMyr(int index) {
        return oceanAge[index];
    }

    public float formedAtMyr(int index) {
        return formedAt[index];
    }

    public boolean isContinental(int index) {
        return type[index] == CONTINENTAL;
    }

    /** Continents, island arcs and oceanic plateaus: too light to subduct. */
    boolean isBuoyant(int index, double buoyantCrustKm) {
        return type[index] == CONTINENTAL || thick[index] >= buoyantCrustKm;
    }

    boolean isBuoyantBack(int index, double buoyantCrustKm) {
        return type2[index] == CONTINENTAL || thick2[index] >= buoyantCrustKm;
    }

    public byte marks(int index) {
        return marks[index];
    }

    public float keelKm(int index) {
        return keel[index];
    }

    public float eclogiteKm(int index) {
        return eclogite[index];
    }
}
