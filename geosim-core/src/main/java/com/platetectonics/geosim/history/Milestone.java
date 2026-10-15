package com.platetectonics.geosim.history;

/**
 * Biological innovations (docs/design/07-life-atmosphere-sun.md#life-milestones-stochastic). Each waits a
 * random time once its prerequisites are met. Some prerequisites are oxygen levels, which the oxygen box
 * model in {@link WorldHistory} produces from life itself: the Great Oxidation Event and the Neoproterozoic
 * Oxygenation Event are outcomes, not milestones.
 * <p>
 * Two waits per milestone: the median of the random draw, and Earth's own wait for the Earth-like preset.
 * Earth is a somewhat slow world (forests only ~390 Myr before the present), so the medians are shorter:
 * about three quarters of random worlds grow forests, and coal, by 4.57 Gyr.
 */
public enum Milestone {
    /** First life, with methanogens (a methane greenhouse); needs t > 100 Myr. Earth: ~0.5-0.8 Gyr. */
    LIFE(null, null, 300.0, 400.0),
    /** Oxygenic photosynthesis (cyanobacteria): the first oxygen source. Earth: ~1.6-1.9 Gyr. */
    PHOTOSYNTHESIS(LIFE, null, 900.0, 1_100.0),
    /** Sulfate-reducing microbes; needs sulfate, i.e. an oxygenated atmosphere. MVT, SEDEX, sediment-hosted Cu. */
    SULFATE_REDUCERS(LIFE, GlobalState.OxygenState.INTERMEDIATE, 50.0, 50.0),
    /** Complex cells; need some oxygen. Earth: ~2.7-2.9 Gyr. */
    EUKARYOTES(LIFE, GlobalState.OxygenState.INTERMEDIATE, 400.0, 500.0),
    /** Eukaryotic algae take over marine productivity, raising organic burial. Earth: ~3.7-3.8 Gyr. */
    ALGAE(EUKARYOTES, null, 800.0, 1_100.0),
    /** Animals with shells and reefs; need modern-like oxygen. Earth: Cambrian, ~4.03 Gyr. */
    ANIMALS(ALGAE, GlobalState.OxygenState.POST_NOE, 150.0, 250.0),
    /** Land plants (ozone shield, soils): slower erosion, faster weathering, burial on land. Earth: ~4.12 Gyr. */
    LAND_PLANTS(ANIMALS, GlobalState.OxygenState.POST_NOE, 80.0, 100.0),
    /** Forests and peat swamps: coal. Earth: ~4.18 Gyr. */
    FORESTS(LAND_PLANTS, null, 50.0, 60.0),
    /** Calcareous plankton: chalk. Earth: Jurassic, ~4.37 Gyr. */
    CALCAREOUS_PLANKTON(ANIMALS, null, 300.0, 350.0),
    /** Diatoms: diatomite. Earth: Cretaceous, ~4.47 Gyr. */
    DIATOMS(CALCAREOUS_PLANKTON, null, 100.0, 100.0);

    private final Milestone prerequisite;
    private final GlobalState.OxygenState oxygenNeeded;
    private final double medianWaitMyr;
    private final double earthWaitMyr;

    Milestone(Milestone prerequisite, GlobalState.OxygenState oxygenNeeded, double medianWaitMyr, double earthWaitMyr) {
        this.prerequisite = prerequisite;
        this.oxygenNeeded = oxygenNeeded;
        this.medianWaitMyr = medianWaitMyr;
        this.earthWaitMyr = earthWaitMyr;
    }

    public Milestone prerequisite() {
        return prerequisite;
    }

    /** Oxygen state that must have been reached first, or null. */
    public GlobalState.OxygenState oxygenNeeded() {
        return oxygenNeeded;
    }

    /** Median of the random wait once the prerequisites are met. */
    public double medianWaitMyr() {
        return medianWaitMyr;
    }

    /** Earth's wait, used by the Earth-like preset. */
    public double earthWaitMyr() {
        return earthWaitMyr;
    }
}
