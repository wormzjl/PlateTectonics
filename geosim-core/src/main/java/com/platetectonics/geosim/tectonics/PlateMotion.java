package com.platetectonics.geosim.tectonics;

/**
 * One motion substep: every cell moves to where its plate's pose puts its place in the plate's raster (D-42;
 * at most a few cells), overlaps are resolved as subduction or collision, gaps are filled with new ocean
 * crust, ocean floor ages, and arcs grow behind the trenches. The pose maps a plate's raster onto the world
 * one-to-one ({@link PlateFrame}), so moving and turning a plate is an exact copy: no data is blurred.
 */
final class PlateMotion {
    private PlateMotion() {
    }

    /**
     * Cells move in index order and the first to reach a target cell takes it; later arrivals are
     * overlaps. To use several threads with the same result, the move runs in four passes: targets
     * (parallel), claims in index order (one thread, integers only), the bulk copy of first claims
     * (parallel; most of the work) and then pole losses and overlaps in index order (one thread), so
     * every shared total, trench and polarity memory is updated in the same order as a plain loop.
     */
    static void substep(PlateSimulation sim, double subDt, double now) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int h = sim.grid.height();
        int n = sim.grid.size();
        int[] target = sim.moveTarget;
        int[] first = sim.moveFirst;
        int[] later = sim.moveLater;
        sim.trenchCount = 0;

        sim.parallel.cells(n, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                first[idx] = -1;
                Plate p = sim.plates.get(L.owner[idx]);
                long at = p.frame.forward(L.u[idx], L.v[idx]);
                int ti = Math.floorMod(p.originI + PlateFrame.x(at), w);
                int tj = p.originJ + PlateFrame.y(at);
                if (tj < 0 || tj >= h) {
                    if (L.type[idx] != Lithosphere.CONTINENTAL) {
                        target[idx] = -1; // ocean floor pushed past a pole is consumed
                        continue;
                    }
                    tj = tj < 0 ? 0 : h - 1; // continents pile up against the pole edge
                }
                target[idx] = tj * w + ti;
            }
        });
        int laterCount = 0;
        for (int idx = 0; idx < n; idx++) {
            int t = target[idx];
            if (t >= 0 && first[t] < 0) {
                first[t] = idx;
            } else {
                later[laterCount++] = idx;
            }
        }
        sim.parallel.cells(n, (from, to) -> {
            for (int t = from; t < to; t++) {
                int src = first[t];
                if (src >= 0) {
                    L.copyToBack(src, t);
                } else {
                    L.owner2[t] = -1;
                }
            }
        });
        for (int k = 0; k < laterCount; k++) {
            int idx = later[k];
            int t = target[idx];
            if (t < 0) {
                sim.poleLostCells++;
                sim.budgetSubducted += Math.max(0.0, L.thick[idx] - sim.cfg.oceanCrustKm());
            } else {
                resolveOverlap(sim, t, idx);
            }
        }
        reframePoleCells(sim);

        fillGaps(sim, now);
        L.swap();

        float age = (float) subDt;
        sim.parallel.cells(n, (from, to) -> {
            for (int idx = from; idx < to; idx++) {
                if (L.type[idx] == Lithosphere.OCEANIC) {
                    L.oceanAge[idx] += age;
                }
            }
        });
        erodeForearcs(sim, subDt);
        growArcs(sim, subDt, now);
    }

    /**
     * Subduction erosion: the sinking slab scrapes crust off the base and front of the overriding plate
     * and carries it down (von Huene &amp; Scholl 1991). On Earth it removes about as much crust as
     * sediment subduction, together roughly what arcs add, which keeps continental volume near steady.
     * The rate is per km of trench, so the volume does not depend on the cell size.
     */
    private static void erodeForearcs(PlateSimulation sim, double subDt) {
        Lithosphere L = sim.lith;
        TectonicsConfig cfg = sim.cfg;
        double cellKm = sim.grid.cellKm();
        float rate = (float) (cfg.subductionErosionKm3PerKmMyr() * cellKm / sim.grid.cellAreaKm2() * subDt);
        float floor = (float) cfg.oceanCrustKm();
        for (int k = 0; k < sim.trenchCount; k++) {
            int t = sim.trench[k];
            float removed = Math.min(rate, L.thick[t] - floor);
            if (removed <= 0f) {
                continue;
            }
            L.thick[t] -= removed;
            L.sediment[t] = Math.min(L.sediment[t], L.thick[t]);
            sim.budgetForearc += removed;
        }
    }

    /**
     * Continent held at a pole edge has moved relative to its plate: it takes the place in the plate's raster
     * that lands on its cell. Any cell of the plate that held that place landed there too and was stacked
     * into it, so places stay one per cell.
     */
    private static void reframePoleCells(PlateSimulation sim) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int h = sim.grid.height();
        for (int k = 0; k < 2 * w; k++) {
            int t = k < w ? k : (h - 1) * w + (k - w);
            int o = L.owner2[t];
            if (o < 0) {
                continue;
            }
            Plate p = sim.plates.get(o);
            long at = p.frame.forward(L.u2[t], L.v2[t]);
            int row = t / w - p.originJ;
            if (PlateFrame.y(at) != row) {
                long uv = p.frame.inverse(PlateFrame.x(at), row);
                L.u2[t] = PlateFrame.x(uv);
                L.v2[t] = PlateFrame.y(uv);
            }
        }
    }

    /** Back cell {@code t} is already occupied; front cell {@code idx} moves onto it. */
    private static void resolveOverlap(PlateSimulation sim, int t, int idx) {
        Lithosphere L = sim.lith;
        TectonicsConfig cfg = sim.cfg;
        int pa = L.owner2[t];
        int pb = L.owner[idx];
        boolean ba = L.isBuoyantBack(t, cfg.buoyantCrustKm());
        boolean bb = L.isBuoyant(idx, cfg.buoyantCrustKm());

        if (pa == pb) {
            // the same plate folding onto itself at a pole edge: stack the crust in place
            stack(sim, t, L.thick[idx], L.keel[idx], L.eclogite[idx], false);
            return;
        }

        if (ba && bb) {
            // collision: neither side can sink (continents, arcs, plateaus). The plate with more continent
            // overrides (Asia over India) and the other's crust underthrusts and stacks beneath it; an
            // arc or plateau arriving at a continent is accreted the same way
            boolean incomingWins = overrides(sim, pb, pa);
            float existingThick = L.thick2[t];
            float existingKeel = L.keel2[t];
            float existingEclogite = L.eclogite2[t];
            float existingFormed = L.formedAt2[t];
            float loserThick = incomingWins ? existingThick : L.thick[idx];
            float loserKeel = incomingWins ? existingKeel : L.keel[idx];
            float loserEclogite = incomingWins ? existingEclogite : L.eclogite[idx];
            if (incomingWins) {
                L.copyToBack(idx, t);
            }
            stack(sim, t, loserThick, loserKeel, loserEclogite, true);
            L.formedAt2[t] = Math.min(existingFormed, L.formedAt[idx]);
            L.marks2[t] |= Lithosphere.MARK_COLLISION;
            sim.collisionCells++;
            sim.stepCollisions.add(pa, pb, 1.0);
            return;
        }

        // at least one side is normal ocean floor: it subducts (older, denser ocean sinks under younger)
        boolean incomingSubducts;
        if (ba) {
            incomingSubducts = true;
        } else if (bb) {
            incomingSubducts = false;
        } else {
            incomingSubducts = sim.polarity.sinking(pa, pb, L.oceanAge2[t], L.oceanAge[idx], sim.stepIndex) == pb;
        }
        double ocean = cfg.oceanCrustKm();
        float sinkingSediment = incomingSubducts ? L.sediment[idx] : L.sediment2[t];
        float sinkingThick = incomingSubducts ? L.thick[idx] : L.thick2[t];
        if (!incomingSubducts) {
            L.copyToBack(idx, t);
        }
        // part of the sediment on the sinking plate is scraped off into an accretionary wedge, which builds
        // up to sea level (a forearc high); what it cannot hold goes down with the slab
        float wedge = (float) Math.min(cfg.accretedSedimentFraction() * sinkingSediment, wedgeRoomKm(sim, t));
        L.thick2[t] += wedge;
        L.sediment2[t] += wedge;
        sim.budgetSubducted += Math.max(0.0, sinkingThick - wedge - ocean);
        L.marks2[t] |= Lithosphere.MARK_TRENCH;
        sim.addTrench(t);
        sim.subductedCells++;
    }

    /** Sediment back cell {@code t} can take before its surface reaches sea level, km. */
    private static double wedgeRoomKm(PlateSimulation sim, int t) {
        Lithosphere L = sim.lith;
        boolean continental = L.type2[t] == Lithosphere.CONTINENTAL;
        double pull = 1000.0 * (sim.cfg.keelDensityFactor() * L.keel2[t] + sim.cfg.eclogiteDensityFactor() * L.eclogite2[t]);
        double e = Isostasy.elevationM(continental, L.thick2[t], L.oceanAge2[t], pull, L.sediment2[t]);
        double f = continental ? Isostasy.SEDIMENT_LAND_FACTOR : Isostasy.SEDIMENT_MARINE_FACTOR;
        return Math.max(0.0, (sim.seaLevelM - e) / (f * 1000.0));
    }

    /**
     * Over-run crust underthrusts the overriding plate at back cell {@code t}, as India under Tibet. Of the
     * crust kept ({@code collisionTransfer}; the rest sinks with the slab), {@code underthrustFrontFraction}
     * stacks at the suture and builds the frontal range; the remainder slides further under the overriding
     * plate and is spread {@code underthrustNearKm..underthrustFarKm} behind the suture after the substeps
     * ({@link #distributeUnderthrust}). The mantle keel thickens with the crust. There is no cap here:
     * {@link CrustalFlow} spreads crust above the plateau ceiling sideways.
     */
    private static void stack(PlateSimulation sim, int t, float loserThick, float loserKeel, float loserEclogite,
                              boolean underthrust) {
        Lithosphere L = sim.lith;
        TectonicsConfig cfg = sim.cfg;
        double kept = cfg.collisionTransfer();
        double front = underthrust ? cfg.underthrustFrontFraction() : kept;
        double behind = kept - front;
        double keelPerKm = cfg.keelPerCollisionCrust();
        L.thick2[t] += (float) (front * loserThick);
        L.eclogite2[t] += (float) (front / kept * loserEclogite);
        L.keel2[t] = (float) Math.min(cfg.keelMaxKm(),
                L.keel2[t] + front / kept * Math.max(0f, loserKeel) + keelPerKm * front * loserThick);
        if (behind > 0.0) {
            int o = L.owner2[t];
            sim.underthrustCrust[o] += behind * loserThick;
            sim.underthrustEclogite[o] += behind / kept * loserEclogite;
            sim.underthrustKeel[o] += behind / kept * Math.max(0f, loserKeel) + keelPerKm * behind * loserThick;
        }
        sim.budgetCollision += (1.0 - kept) * loserThick;
        if (L.type2[t] == Lithosphere.OCEANIC && L.thick2[t] >= cfg.continentThresholdKm()) {
            L.type2[t] = Lithosphere.CONTINENTAL; // accreted arc / plateau crust
            L.oceanAge2[t] = 0f;
        }
    }

    /**
     * Spreads the crust that underthrust each plate this step evenly over its cells
     * {@code underthrustNearKm..underthrustFarKm} behind its sutures (collision-marked cells). A plate too
     * small to have such a band keeps it at the suture.
     */
    static void distributeUnderthrust(PlateSimulation sim) {
        Lithosphere L = sim.lith;
        TectonicsConfig cfg = sim.cfg;
        int plates = sim.plates.size();
        boolean any = false;
        for (int o = 0; o < plates; o++) {
            any |= sim.underthrustCrust[o] > 0.0;
        }
        if (!any) {
            return;
        }
        int w = sim.grid.width();
        int h = sim.grid.height();
        int n = sim.grid.size();
        int near = (int) Math.round(cfg.underthrustNearKm() / sim.grid.cellKm());
        int far = Math.max(near, (int) Math.round(cfg.underthrustFarKm() / sim.grid.cellKm()));
        int[] queue = sim.queue;
        int[] dist = sim.dist;
        int tail = 0;
        int[] band = new int[plates];
        int[] suture = new int[plates];
        for (int idx = 0; idx < n; idx++) {
            if ((L.marks[idx] & Lithosphere.MARK_COLLISION) != 0 && sim.underthrustCrust[L.owner[idx]] > 0.0) {
                dist[idx] = 0;
                queue[tail++] = idx;
                suture[L.owner[idx]]++;
            }
        }
        int head = 0;
        while (head < tail) {
            int idx = queue[head++];
            int d = dist[idx];
            if (d >= near) {
                band[L.owner[idx]]++;
            }
            if (d >= far) {
                continue;
            }
            int owner = L.owner[idx];
            int i = idx % w;
            int j = idx / w;
            tail = visit(L, dist, queue, tail, j * w + (i + 1 == w ? 0 : i + 1), owner, d + 1);
            tail = visit(L, dist, queue, tail, j * w + (i == 0 ? w - 1 : i - 1), owner, d + 1);
            if (j > 0) {
                tail = visit(L, dist, queue, tail, idx - w, owner, d + 1);
            }
            if (j < h - 1) {
                tail = visit(L, dist, queue, tail, idx + w, owner, d + 1);
            }
        }
        float threshold = (float) cfg.continentThresholdKm();
        for (int k = 0; k < tail; k++) {
            int idx = queue[k];
            int o = L.owner[idx];
            int count = band[o] > 0 ? band[o] : suture[o];
            boolean target = band[o] > 0 ? dist[idx] >= near : dist[idx] == 0;
            if (target && count > 0) {
                L.thick[idx] += (float) (sim.underthrustCrust[o] / count);
                L.eclogite[idx] += (float) (sim.underthrustEclogite[o] / count);
                L.keel[idx] = (float) Math.min(cfg.keelMaxKm(), L.keel[idx] + sim.underthrustKeel[o] / count);
                if (L.type[idx] == Lithosphere.OCEANIC && L.thick[idx] >= threshold) {
                    L.type[idx] = Lithosphere.CONTINENTAL;
                    L.oceanAge[idx] = 0f;
                    L.formedAt[idx] = (float) sim.time;
                }
            }
            dist[idx] = -1;
        }
        java.util.Arrays.fill(sim.underthrustCrust, 0.0);
        java.util.Arrays.fill(sim.underthrustKeel, 0.0);
        java.util.Arrays.fill(sim.underthrustEclogite, 0.0);
    }

    /** Whether plate {@code p} overrides plate {@code q} in a continental collision. */
    private static boolean overrides(PlateSimulation sim, int p, int q) {
        long cp = sim.plates.get(p).continentalArea;
        long cq = sim.plates.get(q).continentalArea;
        return cp != cq ? cp > cq : p < q;
    }

    /**
     * Cells no plate moved into are new ocean floor (spreading). Each gap cell joins the plate it borders;
     * wider gaps are filled breadth-first, so the ridge ends up near the middle of the opening.
     * <p>
     * A single cell enclosed on all sides by one turned plate is not an opening but rounding (D-42): the
     * plate's raster, built up at the ridges at many angles, has a gap there, and this angle lands it inside
     * the plate. It takes a copy of a neighbour's crust, ocean floor if any neighbour has it (so rifted margins
     * do not grow continent), and the place in the raster is filled for good. What the copy holds beyond new
     * ocean crust is counted in the crust budget, as for subduction.
     */
    private static void fillGaps(PlateSimulation sim, double now) {
        float crust = (float) sim.cfg.oceanCrustKm();
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int h = sim.grid.height();
        int n = sim.grid.size();
        int[] queue = sim.queue;
        int[] holes = sim.moveLater; // free once the overlaps are resolved
        int holeCount = 0;
        int head = 0;
        int tail = 0;
        // first ring: gaps touching existing crust (decided against the pre-fill state)
        for (int idx = 0; idx < n; idx++) {
            if (L.owner2[idx] >= 0) {
                continue;
            }
            int src = firstOwnedNeighbour(L.owner2, idx, w, h);
            if (src >= 0) {
                if (enclosedByTurnedPlate(sim, idx, L.owner2[src], w, h)) {
                    holes[holeCount++] = idx;
                } else {
                    queue[tail++] = idx;
                }
                sim.dist[idx] = src; // remember the source plate cell temporarily
            }
        }
        for (int k = 0; k < holeCount; k++) {
            int idx = holes[k];
            int src = oceanicNeighbour(L, idx, sim.dist[idx], w, h);
            L.copyWithinBack(src, idx);
            placeNextTo(sim, src, idx);
            sim.budgetRounding += Math.max(0.0, L.thick2[src] - crust);
            sim.dist[idx] = -1;
        }
        sim.roundingHoleCells += holeCount;
        for (int k = 0; k < tail; k++) {
            int idx = queue[k];
            int src = sim.dist[idx];
            newOcean(L, idx, L.owner2[src], now, crust);
            placeNextTo(sim, src, idx);
            sim.dist[idx] = -1;
        }
        // remaining gaps: breadth-first from the first ring
        while (head < tail) {
            int idx = queue[head++];
            int i = idx % w;
            int j = idx / w;
            tail = claim(sim, queue, tail, idx, j * w + (i + 1 == w ? 0 : i + 1), now, crust);
            tail = claim(sim, queue, tail, idx, j * w + (i == 0 ? w - 1 : i - 1), now, crust);
            if (j > 0) {
                tail = claim(sim, queue, tail, idx, idx - w, now, crust);
            }
            if (j < h - 1) {
                tail = claim(sim, queue, tail, idx, idx + w, now, crust);
            }
        }
        sim.newOceanCells += tail;
    }

    private static int claim(PlateSimulation sim, int[] queue, int tail, int from, int idx, double now, float crust) {
        Lithosphere L = sim.lith;
        if (L.owner2[idx] < 0) {
            newOcean(L, idx, L.owner2[from], now, crust);
            placeNextTo(sim, from, idx);
            queue[tail++] = idx;
        }
        return tail;
    }

    /**
     * New crust at back cell {@code idx} takes the place in its plate's raster that lands on it, found from its
     * neighbour {@code from} of the same plate (which fixes the column across the date line). No cell of the
     * plate holds that place, or it would have landed on {@code idx}.
     */
    private static void placeNextTo(PlateSimulation sim, int from, int idx) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        Plate p = sim.plates.get(L.owner2[idx]);
        long at = p.frame.forward(L.u2[from], L.v2[from]);
        int dj = idx / w - from / w;
        int di = dj != 0 ? 0 : (idx % w == (from % w + 1) % w ? 1 : -1);
        long uv = p.frame.inverse(PlateFrame.x(at) + di, PlateFrame.y(at) + dj);
        L.u2[idx] = PlateFrame.x(uv);
        L.v2[idx] = PlateFrame.y(uv);
    }

    /** The first ocean-floor neighbour of back cell {@code idx}, else {@code first}. */
    private static int oceanicNeighbour(Lithosphere L, int idx, int first, int w, int h) {
        int i = idx % w;
        int j = idx / w;
        for (int m = 0; m < 4; m++) {
            int nb = m == 0 ? j * w + (i + 1 == w ? 0 : i + 1)
                    : m == 1 ? j * w + (i == 0 ? w - 1 : i - 1)
                    : m == 2 ? (j > 0 ? idx - w : -1)
                    : (j < h - 1 ? idx + w : -1);
            if (nb >= 0 && L.type2[nb] == Lithosphere.OCEANIC) {
                return nb;
            }
        }
        return first;
    }

    /** Whether every neighbour of gap cell {@code idx} belongs to plate {@code o}, and that plate is turned. */
    private static boolean enclosedByTurnedPlate(PlateSimulation sim, int idx, int o, int w, int h) {
        if (sim.plates.get(o).frame.identity()) {
            return false;
        }
        int[] owner = sim.lith.owner2;
        int i = idx % w;
        int j = idx / w;
        return owner[j * w + (i + 1 == w ? 0 : i + 1)] == o
                && owner[j * w + (i == 0 ? w - 1 : i - 1)] == o
                && (j == 0 || owner[idx - w] == o)
                && (j == h - 1 || owner[idx + w] == o);
    }

    private static int firstOwnedNeighbour(int[] owner, int idx, int w, int h) {
        int i = idx % w;
        int j = idx / w;
        int e = j * w + (i + 1 == w ? 0 : i + 1);
        if (owner[e] >= 0) {
            return e;
        }
        int west = j * w + (i == 0 ? w - 1 : i - 1);
        if (owner[west] >= 0) {
            return west;
        }
        if (j > 0 && owner[idx - w] >= 0) {
            return idx - w;
        }
        if (j < h - 1 && owner[idx + w] >= 0) {
            return idx + w;
        }
        return -1;
    }

    private static void newOcean(Lithosphere L, int idx, int owner, double now, float crust) {
        L.owner2[idx] = owner;
        L.thick2[idx] = crust;
        L.oceanAge2[idx] = 0f;
        L.formedAt2[idx] = (float) now;
        L.type2[idx] = Lithosphere.OCEANIC;
        L.marks2[idx] = Lithosphere.MARK_RIDGE;
        L.keel2[idx] = 0f;
        L.eclogite2[idx] = 0f;
        L.sediment2[idx] = 0f;
        L.regolith2[idx] = 0f;
        L.exhumed2[idx] = 0f;
        L.lastGlaciated2[idx] = -1f;
    }

    /**
     * Arc magmatism: crust thickens in a band {@code arcNearKm..arcFarKm} behind each trench, on the
     * overriding plate. Ocean crust that thickens past the threshold becomes new continental crust.
     */
    private static void growArcs(PlateSimulation sim, double subDt, double now) {
        if (sim.trenchCount == 0) {
            return;
        }
        Lithosphere L = sim.lith;
        TectonicsConfig cfg = sim.cfg;
        int w = sim.grid.width();
        int h = sim.grid.height();
        int near = (int) Math.round(cfg.arcNearKm() / sim.grid.cellKm());
        int far = Math.max(near + 1, (int) Math.round(cfg.arcFarKm() / sim.grid.cellKm()));
        int[] queue = sim.queue;
        int[] dist = sim.dist;
        int tail = 0;
        for (int k = 0; k < sim.trenchCount; k++) {
            int t = sim.trench[k];
            if (dist[t] < 0) {
                dist[t] = 0;
                queue[tail++] = t;
            }
        }
        int head = 0;
        float growth = (float) (cfg.arcGrowthKmPerMyr() * sim.arcMultiplier * subDt);
        while (head < tail) {
            int idx = queue[head++];
            int d = dist[idx];
            if (d >= near) {
                float before = L.thick[idx];
                L.thick[idx] = Math.max(before, (float) Math.min(cfg.maxCrustKm(), before + growth));
                sim.budgetArc += L.thick[idx] - before;
                L.marks[idx] |= Lithosphere.MARK_ARC;
                if (L.type[idx] == Lithosphere.OCEANIC && L.thick[idx] >= cfg.continentThresholdKm()) {
                    L.type[idx] = Lithosphere.CONTINENTAL;
                    L.oceanAge[idx] = 0f;
                    L.formedAt[idx] = (float) now;
                }
            }
            if (d >= far) {
                continue;
            }
            int owner = L.owner[idx];
            int i = idx % w;
            int j = idx / w;
            tail = visit(L, dist, queue, tail, j * w + (i + 1 == w ? 0 : i + 1), owner, d + 1);
            tail = visit(L, dist, queue, tail, j * w + (i == 0 ? w - 1 : i - 1), owner, d + 1);
            if (j > 0) {
                tail = visit(L, dist, queue, tail, idx - w, owner, d + 1);
            }
            if (j < h - 1) {
                tail = visit(L, dist, queue, tail, idx + w, owner, d + 1);
            }
        }
        for (int k = 0; k < tail; k++) {
            dist[queue[k]] = -1;
        }
    }

    private static int visit(Lithosphere L, int[] dist, int[] queue, int tail, int idx, int owner, int d) {
        if (dist[idx] < 0 && L.owner[idx] == owner) {
            dist[idx] = d;
            queue[tail++] = idx;
        }
        return tail;
    }
}
