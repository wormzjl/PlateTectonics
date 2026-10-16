package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.events.GeoEvent;
import com.platetectonics.geosim.noise.PeriodicNoise;
import com.platetectonics.geosim.rng.Rng;
import com.platetectonics.geosim.rng.Streams;

import java.util.Map;
import java.util.TreeMap;

/**
 * Changes the plate layout once per step: sustained continental collisions weld plates together,
 * mantle plumes thicken crust and can rift plates apart (favouring long-lived supercontinents),
 * old ocean breaks off passive margins to start subducting, tiny plates are absorbed, the largest
 * plate breaks up if too few remain, and ocean trapped inside a plate closes ({@link BasinClosure}).
 */
final class PlateReorganizer {
    private PlateReorganizer() {
    }

    static void reorganize(PlateSimulation sim, double dt) {
        mergeCollidedPlates(sim, dt);
        plumes(sim, dt);
        marginSplits(sim, dt);
        absorbTinyPlates(sim);
        keepMinimumPlates(sim);
        BasinClosure.close(sim, dt, sim.time + dt);
        sim.refreshStats();
    }

    // ------------------------------------------------------------------ collisions weld plates

    private static void mergeCollidedPlates(PlateSimulation sim, double dt) {
        sim.collisionMemory.scale(Math.exp(-dt / sim.cfg.collisionMergeTimescaleMyr()));
        sim.collisionMemory.addAll(sim.stepCollisions);
        for (Map.Entry<Long, Double> e : sim.collisionMemory.sorted().entrySet()) {
            Plate a = sim.plates.get(PairCounter.first(e.getKey()));
            Plate b = sim.plates.get(PairCounter.second(e.getKey()));
            if (!a.alive || !b.alive) {
                continue;
            }
            Plate small = a.area <= b.area ? a : b;
            Plate big = small == a ? b : a;
            // overlap area (cells) > convergence distance x contact length ~ sqrt(area), in cells
            double threshold = sim.cfg.collisionMergeDistanceKm() / sim.grid.cellKm() * Math.sqrt(small.area);
            if (e.getValue() > threshold) {
                absorb(sim, small, big);
                sim.events.add(GeoEvent.Type.COLLISION_MERGE, sim.time, -1, big.id, small.id, small.area);
            }
        }
    }

    /**
     * Relabels every cell of {@code from} as {@code into}, at the place in {@code into}'s raster that lands on
     * it (D-42); velocities and spins combine by area.
     */
    static void absorb(PlateSimulation sim, Plate from, Plate into) {
        Lithosphere L = sim.lith;
        int[] owner = L.owner;
        int w = sim.grid.width();
        // from's own frame keeps its cells in one piece; the two origins are a whole number of columns apart,
        // taken across the date line the short way between the plates' centres
        // whole-number sums: exact in any order, so the worker threads add them up
        long[] sums = new long[6]; // from u, v, cells; into u, v, cells
        sim.parallel.cells(owner.length, (lo, hi) -> {
            long[] part = new long[6];
            for (int idx = lo; idx < hi; idx++) {
                int k = owner[idx] == from.id ? 0 : owner[idx] == into.id ? 3 : -1;
                if (k >= 0) {
                    part[k] += L.u[idx];
                    part[k + 1] += L.v[idx];
                    part[k + 2]++;
                }
            }
            synchronized (sums) {
                for (int k = 0; k < 6; k++) {
                    sums[k] += part[k];
                }
            }
        });
        double fromCentre = sums[2] == 0 ? 0.0 : from.frame.rotX((double) sums[0] / sums[2], (double) sums[1] / sums[2]);
        double intoCentre = sums[5] == 0 ? 0.0 : into.frame.rotX((double) sums[3] / sums[5], (double) sums[4] / sums[5]);
        int shift = from.originI - into.originI;
        int di = shift - w * (int) Math.floor((shift + fromCentre - intoCentre) / w + 0.5);
        int dj = from.originJ - into.originJ;
        sim.parallel.cells(owner.length, (lo, hi) -> {
            for (int idx = lo; idx < hi; idx++) {
                if (owner[idx] == from.id) {
                    owner[idx] = into.id;
                    long at = from.frame.forward(L.u[idx], L.v[idx]);
                    long uv = into.frame.inverse(di + PlateFrame.x(at), dj + PlateFrame.y(at));
                    L.u[idx] = PlateFrame.x(uv);
                    L.v[idx] = PlateFrame.y(uv);
                }
            }
        });
        double total = Math.max(1.0, from.area + into.area);
        into.vx = (into.vx * into.area + from.vx * from.area) / total;
        into.vy = (into.vy * into.area + from.vy * from.area) / total;
        into.omega = (into.omega * into.area + from.omega * from.area) / total;
        into.spinDrift = (into.spinDrift * into.area + from.spinDrift * from.area) / total;
        into.area += from.area;
        into.continentalArea += from.continentalArea;
        from.area = 0;
        from.alive = false;
        sim.collisionMemory.forget(from.id);
    }

    // ------------------------------------------------------------------ plumes and rifts

    private static void plumes(PlateSimulation sim, double dt) {
        double expected = sim.cfg.plumeRatePerMyr() * dt;
        int count = poisson(expected, Rng.unit(sim.seed, Streams.PLUME, sim.stepIndex, -1));
        long totalContinent = 0;
        for (Plate p : sim.plates) {
            if (p.alive) {
                totalContinent += p.continentalArea;
            }
        }
        for (int k = 0; k < count; k++) {
            int cell = pickPlumeCell(sim, k, totalContinent);
            Plate plate = sim.plates.get(sim.lith.owner[cell]);
            boolean supercontinent = isInsulatingSupercontinent(sim, plate, totalContinent);
            applyLargeIgneousProvince(sim, cell);
            sim.events.add(GeoEvent.Type.PLUME, sim.time, cell, plate.id, -1, sim.cfg.plumeRadiusKm());

            double riftChance = supercontinent ? 0.7 : (plate.continentalFraction() > 0.3 ? 0.15 : 0.05);
            if (sim.alivePlateCount() < sim.cfg.maxPlates()
                    && Rng.unit(sim.seed, Streams.RIFT, sim.stepIndex, k) < riftChance) {
                double angle = Rng.unit(sim.seed, Streams.RIFT, sim.stepIndex, 100 + k) * Math.PI;
                Plate created = split(sim, plate, cell % sim.grid.width(), cell / sim.grid.width(), angle,
                        sim.cfg.riftSeparationKmPerMyr(), Streams.RIFT * 1000 + sim.stepIndex);
                if (created != null) {
                    sim.events.add(GeoEvent.Type.RIFT, sim.time, cell, plate.id, created.id, created.area);
                }
            }
        }
    }

    private static boolean isInsulatingSupercontinent(PlateSimulation sim, Plate p, long totalContinent) {
        return totalContinent > 0
                && p.continentalArea >= sim.cfg.supercontinentFraction() * totalContinent
                && sim.time - p.lastRiftTime > sim.cfg.supercontinentRiftDelayMyr();
    }

    /** Plumes rise anywhere, but long-lived supercontinents trap mantle heat and attract them. */
    private static int pickPlumeCell(PlateSimulation sim, int plumeIndex, long totalContinent) {
        int n = sim.grid.size();
        int best = 0;
        double bestKey = -1.0;
        for (int c = 0; c < 16; c++) {
            long sub = plumeIndex * 64L + c;
            int cell = (int) (Rng.unit(sim.seed, Streams.PLUME, sim.stepIndex, sub) * n);
            Plate p = sim.plates.get(sim.lith.owner[cell]);
            double weight = 1.0;
            if (sim.lith.type[cell] == Lithosphere.CONTINENTAL && isInsulatingSupercontinent(sim, p, totalContinent)) {
                weight = 6.0;
            }
            // weighted choice via exponential race: key = u^(1/weight)
            double u = Rng.unit(sim.seed, Streams.PLUME + 1, sim.stepIndex, sub);
            double key = Math.pow(u, 1.0 / weight);
            if (key > bestKey) {
                bestKey = key;
                best = cell;
            }
        }
        return best;
    }

    /** Flood basalts thicken the crust around the plume head; on ocean floor this makes a plateau. */
    private static void applyLargeIgneousProvince(PlateSimulation sim, int cell) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int h = sim.grid.height();
        int ci = cell % w;
        int cj = cell / w;
        int r = Math.max(1, (int) Math.round(sim.cfg.plumeRadiusKm() / sim.grid.cellKm()));
        for (int dj = -r; dj <= r; dj++) {
            int j = cj + dj;
            if (j < 0 || j >= h) {
                continue;
            }
            for (int di = -r; di <= r; di++) {
                double d = Math.sqrt(di * di + dj * dj) / r;
                if (d > 1.0) {
                    continue;
                }
                int idx = j * w + Math.floorMod(ci + di, w);
                double f = 1.0 - d;
                if (L.type[idx] == Lithosphere.OCEANIC) {
                    L.thick[idx] = (float) Math.min(30.0, L.thick[idx] + 8.0 * f);
                } else {
                    L.thick[idx] = (float) Math.min(sim.cfg.maxCrustKm(), L.thick[idx] + 2.0 * f);
                }
                L.marks[idx] |= Lithosphere.MARK_PLUME;
            }
        }
    }

    /**
     * Splits {@code plate} along a noisy line through (ci, cj) at the given angle. The two halves drift
     * apart at {@code separation} km/Myr. Returns the new plate, or null if either half would be too small.
     */
    static Plate split(PlateSimulation sim, Plate plate, double ci, double cj, double angle, double separation,
                       long noiseStream) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int n = sim.grid.size();
        double nx = Math.cos(angle);
        double ny = Math.sin(angle);
        int periodX = Math.max(4, (int) Math.round(sim.grid.width() * sim.grid.cellKm() / 600.0));
        double lattice = (double) periodX / w;
        PeriodicNoise wiggle = new PeriodicNoise(sim.seed, noiseStream, periodX, 4, 0.5);
        double amplitude = 900.0 / sim.grid.cellKm();
        boolean[] side = new boolean[n];
        int id = plate.id;
        // each cell's side from its own position only
        sim.parallel.cells(n, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                if (L.owner[idx] != id) {
                    continue;
                }
                int i = idx % w;
                int j = idx / w;
                double dx = sim.wrapDelta(i - ci);
                double dy = j - cj;
                double s = dx * nx + dy * ny + amplitude * wiggle.sample(i * lattice, j * lattice);
                side[idx] = s > 0;
            }
        });
        int countA = 0;
        int countB = 0;
        for (int idx = 0; idx < n; idx++) {
            if (L.owner[idx] == id) {
                if (side[idx]) {
                    countB++;
                } else {
                    countA++;
                }
            }
        }
        int minCells = minPlateCells(sim);
        if (countA < minCells || countB < minCells) {
            return null;
        }
        Plate created = sim.newPlate(plate.vx + separation * nx, plate.vy + separation * ny);
        plate.vx -= separation * nx;
        plate.vy -= separation * ny;
        for (int idx = 0; idx < n; idx++) {
            if (side[idx]) {
                L.owner[idx] = created.id;
            }
        }
        plate.lastRiftTime = sim.time;
        created.lastRiftTime = sim.time;
        plate.area = countA;
        created.area = countB;
        inheritSpin(sim, plate, created);
        return created;
    }

    // ------------------------------------------------------------------ subduction initiation

    /**
     * Old ocean floor attached to a continent becomes too dense to stay: it breaks away along the margin
     * and starts sinking beneath the continent (the closing half of a Wilson cycle).
     */
    private static void marginSplits(PlateSimulation sim, double dt) {
        double chance = 1.0 - Math.exp(-sim.cfg.marginSplitRatePerMyr() * dt);
        int existing = sim.plates.size();
        double oceanChance = 1.0 - Math.exp(-sim.cfg.oceanSubductionRatePerMyr() * dt);
        for (int id = 0; id < existing; id++) {
            Plate p = sim.plates.get(id);
            double cf = p.continentalFraction();
            if (p.alive && cf < 0.1 && p.maxOceanAge >= sim.cfg.oceanSubductionAgeMyr()
                    && sim.alivePlateCount() < sim.cfg.maxPlates()
                    && Rng.unit(sim.seed, Streams.MARGIN_SPLIT + 1, sim.stepIndex, id) < oceanChance) {
                Plate created = breakOldOcean(sim, p);
                if (created != null) {
                    sim.events.add(GeoEvent.Type.SUBDUCTION_START, sim.time, -1, p.id, created.id, created.area);
                }
                continue;
            }
            if (!p.alive || cf < 0.1 || cf > 0.9 || p.maxOceanAge < sim.cfg.marginSplitAgeMyr()
                    || sim.alivePlateCount() >= sim.cfg.maxPlates()
                    || Rng.unit(sim.seed, Streams.MARGIN_SPLIT, sim.stepIndex, id) >= chance) {
                continue;
            }
            Plate created = splitOffOcean(sim, p);
            if (created != null) {
                sim.events.add(GeoEvent.Type.SUBDUCTION_START, sim.time, -1, p.id, created.id, created.area);
            }
        }
    }

    /**
     * Intra-oceanic subduction initiation (Izu-Bonin style): an ocean plate whose oldest floor has become
     * too dense breaks along a fracture through that floor, and the two halves converge so a new trench
     * forms there.
     */
    private static Plate breakOldOcean(PlateSimulation sim, Plate p) {
        Lithosphere L = sim.lith;
        int oldest = -1;
        for (int idx = 0; idx < L.n; idx++) {
            if (L.owner[idx] == p.id && L.type[idx] == Lithosphere.OCEANIC
                    && (oldest < 0 || L.oceanAge[idx] > L.oceanAge[oldest])) {
                oldest = idx;
            }
        }
        if (oldest < 0) {
            return null;
        }
        int w = sim.grid.width();
        double angle = Rng.unit(sim.seed, Streams.MARGIN_SPLIT + 2, sim.stepIndex, p.id) * Math.PI;
        return split(sim, p, oldest % w, oldest / w, angle, -sim.cfg.riftSeparationKmPerMyr(),
                (Streams.MARGIN_SPLIT + 2) * 1000L + sim.stepIndex);
    }

    private static Plate splitOffOcean(PlateSimulation sim, Plate p) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int n = sim.grid.size();
        double[] oc = new double[3];
        double[] cc = new double[3];
        int oceanCells = 0;
        for (int idx = 0; idx < n; idx++) {
            if (L.owner[idx] != p.id) {
                continue;
            }
            double a = 2.0 * Math.PI * (idx % w) / w;
            double[] acc = L.type[idx] == Lithosphere.OCEANIC ? oc : cc;
            acc[0] += Math.cos(a);
            acc[1] += Math.sin(a);
            acc[2] += idx / w;
            if (L.type[idx] == Lithosphere.OCEANIC) {
                oceanCells++;
            }
        }
        int contCells = p.area - oceanCells;
        int minCells = minPlateCells(sim);
        if (oceanCells < minCells || contCells < minCells) {
            return null;
        }
        double oi = Math.atan2(oc[1], oc[0]) * w / (2.0 * Math.PI);
        double ci = Math.atan2(cc[1], cc[0]) * w / (2.0 * Math.PI);
        double dx = sim.wrapDelta(ci - oi);
        double dy = cc[2] / contCells - oc[2] / oceanCells;
        double len = Math.hypot(dx, dy);
        if (len < 1e-9) {
            return null;
        }
        double push = 30.0;
        Plate created = sim.newPlate(p.vx + push * dx / len, p.vy + push * dy / len);
        for (int idx = 0; idx < n; idx++) {
            if (L.owner[idx] == p.id && L.type[idx] == Lithosphere.OCEANIC) {
                L.owner[idx] = created.id;
            }
        }
        created.area = oceanCells;
        p.area = contCells;
        inheritSpin(sim, p, created);
        return created;
    }

    /** A plate split off {@code parent} keeps its spin and turn history, in a fresh frame of its own (D-42). */
    private static void inheritSpin(PlateSimulation sim, Plate parent, Plate created) {
        created.omega = parent.omega;
        created.spinDrift = parent.spinDrift;
        created.turned = parent.turned;
        created.turnedMark = parent.turnedMark;
        sim.reframe(created);
    }

    // ------------------------------------------------------------------ housekeeping

    static int minPlateCells(PlateSimulation sim) {
        return Math.max(4, (int) (sim.cfg.minPlateFraction() * sim.grid.size()));
    }

    private static void absorbTinyPlates(PlateSimulation sim) {
        int minCells = minPlateCells(sim);
        int existing = sim.plates.size();
        for (int id = 0; id < existing; id++) {
            Plate p = sim.plates.get(id);
            if (!p.alive || p.area >= minCells) {
                continue;
            }
            Plate into = bestNeighbour(sim, p);
            if (into != null) {
                absorb(sim, p, into);
                sim.events.add(GeoEvent.Type.ABSORB, sim.time, -1, into.id, p.id, p.area);
            }
        }
    }

    private static Plate bestNeighbour(PlateSimulation sim, Plate p) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int h = sim.grid.height();
        TreeMap<Integer, Integer> contacts = new TreeMap<>();
        for (int idx = 0; idx < L.n; idx++) {
            if (L.owner[idx] != p.id) {
                continue;
            }
            int i = idx % w;
            int j = idx / w;
            countContact(L, contacts, p.id, j * w + (i + 1 == w ? 0 : i + 1));
            countContact(L, contacts, p.id, j * w + (i == 0 ? w - 1 : i - 1));
            if (j > 0) {
                countContact(L, contacts, p.id, idx - w);
            }
            if (j < h - 1) {
                countContact(L, contacts, p.id, idx + w);
            }
        }
        int best = -1;
        int bestCount = 0;
        for (Map.Entry<Integer, Integer> e : contacts.entrySet()) {
            if (e.getValue() > bestCount) {
                bestCount = e.getValue();
                best = e.getKey();
            }
        }
        return best < 0 ? null : sim.plates.get(best);
    }

    private static void countContact(Lithosphere L, TreeMap<Integer, Integer> contacts, int self, int idx) {
        int o = L.owner[idx];
        if (o != self) {
            contacts.merge(o, 1, Integer::sum);
        }
    }

    private static void keepMinimumPlates(PlateSimulation sim) {
        int guard = 0;
        while (sim.alivePlateCount() < sim.cfg.minPlates() && guard++ < 8) {
            Plate largest = null;
            for (Plate p : sim.plates) {
                if (p.alive && (largest == null || p.area > largest.area)) {
                    largest = p;
                }
            }
            if (largest == null) {
                return;
            }
            double angle = Rng.unit(sim.seed, Streams.BREAKUP, sim.stepIndex, guard) * Math.PI;
            Plate created = split(sim, largest, largest.centroidI, largest.centroidJ, angle,
                    sim.cfg.riftSeparationKmPerMyr(), Streams.BREAKUP * 1000 + sim.stepIndex * 8L + guard);
            if (created == null) {
                return;
            }
            sim.events.add(GeoEvent.Type.BREAKUP, sim.time, -1, largest.id, created.id, created.area);
        }
    }

    /** Inverse-CDF Poisson draw from one uniform number (fine for small means). */
    static int poisson(double mean, double u) {
        double p = Math.exp(-mean);
        double cdf = p;
        int k = 0;
        while (u > cdf && k < 50) {
            k++;
            p *= mean / k;
            cdf += p;
        }
        return k;
    }
}
