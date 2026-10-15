package com.platetectonics.geosim.tectonics;

import com.platetectonics.geosim.rng.Rng;
import com.platetectonics.geosim.rng.Streams;

/**
 * Plate velocities from a force balance (docs/design/03-tectonics.md#plate-kinematics).
 * <p>
 * Every cell edge between two plates is classified by the plates' relative motion:
 * <ul>
 *   <li>converging, with ocean on one side: slab pull drags the sinking plate toward the trench,
 *       stronger for older (denser) ocean floor;</li>
 *   <li>converging continent against continent: collision resistance pushes both apart;</li>
 *   <li>diverging at young ocean floor: ridge push pushes both apart.</li>
 * </ul>
 * Basal drag (larger under continents) turns force into a target velocity. A global "mantle vigor"
 * sets the area-weighted RMS speed, so forces decide directions and relative speeds while the planet
 * keeps Earth-like plate speeds (faster when young). Each plate also feels slow random mantle drift.
 */
final class PlateDynamics {
    private static final double EPS = 0.5; // km/Myr: slower relative motion counts as a transform edge

    private PlateDynamics() {
    }

    static void updateVelocities(PlateSimulation sim, double dt) {
        Lithosphere L = sim.lith;
        TectonicsConfig cfg = sim.cfg;
        int w = sim.grid.width();
        int h = sim.grid.height();
        for (Plate p : sim.plates) {
            p.forceX = 0;
            p.forceY = 0;
        }

        for (int idx = 0; idx < sim.grid.size(); idx++) {
            int i = idx % w;
            int j = idx / w;
            int east = j * w + (i + 1 == w ? 0 : i + 1);
            if (L.owner[east] != L.owner[idx]) {
                boundary(sim, idx, east, 1.0, 0.0);
            }
            if (j < h - 1 && L.owner[idx + w] != L.owner[idx]) {
                boundary(sim, idx, idx + w, 0.0, 1.0);
            }
        }

        // the pole edges of the cylinder act as walls for continents: pushing into them meets the same
        // resistance as a continental collision (ocean floor is simply consumed there)
        for (int i = 0; i < w; i++) {
            poleWall(sim, i, -1.0);
            poleWall(sim, (h - 1) * w + i, 1.0);
        }

        // target velocities from force / drag
        double areaSum = 0.0;
        double speedSq = 0.0;
        double[] tx = new double[sim.plates.size()];
        double[] ty = new double[sim.plates.size()];
        for (Plate p : sim.plates) {
            if (!p.alive) {
                continue;
            }
            double drag = p.area * (1.0 + cfg.continentalDrag() * p.continentalFraction());
            tx[p.id] = p.forceX / drag;
            ty[p.id] = p.forceY / drag;
            areaSum += p.area;
            speedSq += p.area * (tx[p.id] * tx[p.id] + ty[p.id] * ty[p.id]);
        }
        double vigor = cfg.speedPresentKmPerMyr()
                * (1.0 + cfg.speedEarlyBoost() * Math.exp(-sim.time / cfg.speedBoostTimescaleMyr()));
        double rms = Math.sqrt(speedSq / Math.max(1.0, areaSum));
        double scale = rms > 1e-12 ? vigor / rms : 0.0;

        double relax = 1.0 - Math.exp(-dt / cfg.velocityRelaxMyr());
        double driftKeep = Math.exp(-dt / cfg.driftTimescaleMyr());
        double driftKick = cfg.driftKmPerMyr() * Math.sqrt(1.0 - driftKeep * driftKeep);
        double meanX = 0.0;
        double meanY = 0.0;
        for (Plate p : sim.plates) {
            if (!p.alive) {
                continue;
            }
            p.driftX = p.driftX * driftKeep + driftKick * gaussian(sim, p.id, 0);
            p.driftY = p.driftY * driftKeep + driftKick * gaussian(sim, p.id, 1);
            double targetX = tx[p.id] * scale + p.driftX;
            double targetY = ty[p.id] * scale + p.driftY;

            // keep plates from crowding the pole edges of the cylinder
            double lat = 90.0 - 180.0 * (p.centroidJ + 0.5) / h;
            if (Math.abs(lat) > 55.0) {
                targetY += Math.signum(lat) * 25.0 * (Math.abs(lat) - 55.0) / 35.0;
            }

            p.vx += (targetX - p.vx) * relax;
            p.vy += (targetY - p.vy) * relax;
            meanX += p.area * p.vx;
            meanY += p.area * p.vy;
        }
        // no-net-translation frame: the plate system as a whole does not drift toward a pole
        meanX /= Math.max(1.0, areaSum);
        meanY /= Math.max(1.0, areaSum);
        for (Plate p : sim.plates) {
            if (!p.alive) {
                continue;
            }
            p.vx -= meanX;
            p.vy -= meanY;
            double s = Math.hypot(p.vx, p.vy);
            if (s > cfg.maxSpeedKmPerMyr()) {
                p.vx *= cfg.maxSpeedKmPerMyr() / s;
                p.vy *= cfg.maxSpeedKmPerMyr() / s;
            }
        }
    }

    /** Edge between cell {@code a} (plate p) and cell {@code b} (plate q); (nx, ny) points from a to b. */
    private static void boundary(PlateSimulation sim, int a, int b, double nx, double ny) {
        Lithosphere L = sim.lith;
        TectonicsConfig cfg = sim.cfg;
        Plate p = sim.plates.get(L.owner[a]);
        Plate q = sim.plates.get(L.owner[b]);
        double closing = (p.vx - q.vx) * nx + (p.vy - q.vy) * ny;
        boolean contA = L.isBuoyant(a, cfg.buoyantCrustKm());
        boolean contB = L.isBuoyant(b, cfg.buoyantCrustKm());
        if (closing > EPS) {
            if (contA && contB) {
                double f = cfg.collisionResistance();
                p.forceX -= nx * f;
                p.forceY -= ny * f;
                q.forceX += nx * f;
                q.forceY += ny * f;
                return;
            }
            boolean pSinks;
            if (contA || contB) {
                pSinks = contB;
            } else {
                int known = sim.polarity.known(p.id, q.id);
                pSinks = known >= 0 ? known == p.id : L.oceanAge[a] >= L.oceanAge[b];
            }
            float age = pSinks ? L.oceanAge[a] : L.oceanAge[b];
            double f = cfg.slabPull() * slabFactor(age);
            if (pSinks) {
                p.forceX += nx * f;
                p.forceY += ny * f;
            } else {
                q.forceX -= nx * f;
                q.forceY -= ny * f;
            }
        } else if (closing < -EPS) {
            boolean young = (!contA && L.oceanAge[a] < 20f) || (!contB && L.oceanAge[b] < 20f);
            if (young) {
                double f = cfg.ridgePush();
                p.forceX -= nx * f;
                p.forceY -= ny * f;
                q.forceX += nx * f;
                q.forceY += ny * f;
            }
        }
    }

    /** {@code outward} is the direction of the wall in j: -1 for the north edge, +1 for the south edge. */
    private static void poleWall(PlateSimulation sim, int idx, double outward) {
        if (sim.lith.type[idx] != Lithosphere.CONTINENTAL) {
            return;
        }
        Plate p = sim.plates.get(sim.lith.owner[idx]);
        if (p.vy * outward > EPS) {
            p.forceY -= outward * sim.cfg.collisionResistance();
        }
    }

    /** Older ocean floor is colder and denser, so its slab pulls harder. */
    static double slabFactor(float oceanAgeMyr) {
        return Math.min(1.5, 0.3 + oceanAgeMyr / 80.0);
    }

    private static double gaussian(PlateSimulation sim, int plate, int axis) {
        double u1 = Math.max(1e-12, Rng.unit(sim.seed, Streams.DRIFT, sim.stepIndex, plate * 2L + axis));
        double u2 = Rng.unit(sim.seed, Streams.DRIFT + 1, sim.stepIndex, plate * 2L + axis);
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }
}
