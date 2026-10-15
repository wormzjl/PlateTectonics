package com.platetectonics.geosim.climate;

/**
 * Whittaker-style climate classes from annual mean temperature, precipitation and aridity
 * (docs/design/04-surface-processes.md#climate). Phase 4 maps
 * them to TFC biomes (D-17).
 */
public enum Biome {
    DEEP_OCEAN(0x0B2447),
    SHELF_SEA(0x2F6FA8),
    SEA_ICE(0xD8E8F0),
    ICE_SHEET(0xF4F8FC),
    POLAR_DESERT(0xB9B4A8),
    TUNDRA(0x9CA88C),
    BOREAL_FOREST(0x3F6B4E),
    COLD_STEPPE(0xB7B07A),
    COLD_DESERT(0xC9B98F),
    TEMPERATE_GRASSLAND(0xA9C26B),
    TEMPERATE_FOREST(0x4E8F3A),
    TEMPERATE_RAINFOREST(0x2E6E4C),
    HOT_DESERT(0xE3C98D),
    SAVANNA(0xC2B254),
    TROPICAL_SEASONAL_FOREST(0x6E9B2E),
    TROPICAL_RAINFOREST(0x1F6B2A);

    private final int rgb;

    Biome(int rgb) {
        this.rgb = rgb;
    }

    public int rgb() {
        return rgb;
    }

    /**
     * @param elevationM above sea level (negative = sea floor)
     * @param tempC      annual mean temperature
     * @param precipMm   annual precipitation
     * @param aridity    precipitation / potential evaporation
     * @param iceM       ice thickness
     */
    public static Biome classify(double elevationM, double tempC, double precipMm, double aridity, double iceM) {
        if (elevationM < 0.0) {
            if (tempC < -2.0) {
                return SEA_ICE;
            }
            return elevationM > -200.0 ? SHELF_SEA : DEEP_OCEAN;
        }
        if (iceM > 0.0) {
            return ICE_SHEET;
        }
        if (tempC < -5.0) {
            return precipMm < 150.0 ? POLAR_DESERT : TUNDRA;
        }
        if (tempC < 3.0) {
            if (precipMm < 250.0) {
                return aridity < 0.4 ? COLD_STEPPE : TUNDRA;
            }
            return BOREAL_FOREST;
        }
        if (tempC < 18.0) {
            if (aridity < 0.2) {
                return COLD_DESERT;
            }
            if (aridity < 0.5) {
                return COLD_STEPPE;
            }
            if (precipMm < 600.0) {
                return TEMPERATE_GRASSLAND;
            }
            return precipMm < 1_600.0 ? TEMPERATE_FOREST : TEMPERATE_RAINFOREST;
        }
        if (aridity < 0.2) {
            return HOT_DESERT;
        }
        if (aridity < 0.6) {
            return SAVANNA;
        }
        return precipMm < 1_800.0 ? TROPICAL_SEASONAL_FOREST : TROPICAL_RAINFOREST;
    }
}
