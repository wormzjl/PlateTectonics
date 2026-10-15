package com.platetectonics.geosim.tectonics;

/**
 * One motion substep: every plate shifts by whole cells (at most a few), overlaps are resolved as
 * subduction or collision, gaps are filled with new ocean crust, ocean floor ages, and arcs grow
 * behind the trenches. Moving a rigid plate by whole cells is an exact copy, so no data is blurred.
 */
final class PlateMotion {
    private PlateMotion() {
    }

    static void substep(PlateSimulation sim, double subDt, double now) {
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int h = sim.grid.height();
        int n = sim.grid.size();
        java.util.Arrays.fill(L.owner2, -1);
        sim.trenchCount = 0;

        for (int idx = 0; idx < n; idx++) {
            Plate p = sim.plates.get(L.owner[idx]);
            int i = idx % w;
            int j = idx / w;
            int ti = Math.floorMod(i + p.shiftX, w);
            int tj = j + p.shiftY;
            if (tj < 0 || tj >= h) {
                if (L.type[idx] == Lithosphere.CONTINENTAL) {
                    tj = tj < 0 ? 0 : h - 1; // continents pile up against the pole edge
                } else {
                    sim.poleLostCells++; // ocean floor pushed past a pole is consumed
                    sim.budgetSubducted += Math.max(0.0, L.thick[idx] - sim.cfg.oceanCrustKm());
                    continue;
                }
            }
            int t = tj * w + ti;
            if (L.owner2[t] < 0) {
                L.copyToBack(idx, t);
            } else {
                resolveOverlap(sim, t, idx);
            }
        }

        fillGaps(sim, now);
        L.swap();

        for (int idx = 0; idx < n; idx++) {
            if (L.type[idx] == Lithosphere.OCEANIC) {
                L.oceanAge[idx] += (float) subDt;
            }
        }
        growArcs(sim, subDt, now);
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
        sim.budgetSubducted += Math.max(0.0, (incomingSubducts ? L.thick[idx] : L.thick2[t]) - ocean);
        if (!incomingSubducts) {
            L.copyToBack(idx, t);
        }
        L.marks2[t] |= Lithosphere.MARK_TRENCH;
        sim.addTrench(t);
        sim.subductedCells++;
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
     */
    private static void fillGaps(PlateSimulation sim, double now) {
        float crust = (float) sim.cfg.oceanCrustKm();
        Lithosphere L = sim.lith;
        int w = sim.grid.width();
        int h = sim.grid.height();
        int n = sim.grid.size();
        int[] queue = sim.queue;
        int head = 0;
        int tail = 0;
        // first ring: gaps touching existing crust (decided against the pre-fill state)
        for (int idx = 0; idx < n; idx++) {
            if (L.owner2[idx] >= 0) {
                continue;
            }
            int src = firstOwnedNeighbour(L.owner2, idx, w, h);
            if (src >= 0) {
                queue[tail++] = idx;
                sim.dist[idx] = src; // remember the source plate cell temporarily
            }
        }
        for (int k = 0; k < tail; k++) {
            int idx = queue[k];
            newOcean(L, idx, L.owner2[sim.dist[idx]], now, crust);
            sim.dist[idx] = -1;
        }
        // remaining gaps: breadth-first from the first ring
        while (head < tail) {
            int idx = queue[head++];
            int i = idx % w;
            int j = idx / w;
            int owner = L.owner2[idx];
            tail = claim(L, queue, tail, j * w + (i + 1 == w ? 0 : i + 1), owner, now, crust);
            tail = claim(L, queue, tail, j * w + (i == 0 ? w - 1 : i - 1), owner, now, crust);
            if (j > 0) {
                tail = claim(L, queue, tail, idx - w, owner, now, crust);
            }
            if (j < h - 1) {
                tail = claim(L, queue, tail, idx + w, owner, now, crust);
            }
        }
        sim.newOceanCells += tail;
    }

    private static int claim(Lithosphere L, int[] queue, int tail, int idx, int owner, double now, float crust) {
        if (L.owner2[idx] < 0) {
            newOcean(L, idx, owner, now, crust);
            queue[tail++] = idx;
        }
        return tail;
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
                L.thick[idx] = (float) Math.min(cfg.maxCrustKm(), before + growth);
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
