package com.platetectonics.mod;

import com.mojang.logging.LogUtils;
import com.platetectonics.geosim.GeoSim;
import com.platetectonics.geosim.world.PlanetSpec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(PlateTectonicsMod.MOD_ID)
public final class PlateTectonicsMod {
    public static final String MOD_ID = "platetectonics";
    private static final Logger LOGGER = LogUtils.getLogger();

    public PlateTectonicsMod(IEventBus modBus, ModContainer container) {
        PlanetSpec earth = PlanetSpec.earth(PlanetSpec.DEFAULT_SCALE);
        LOGGER.info("PlateTectonics loaded (geosim {}): Earth-scale planet at {} m/block, {} blocks around",
                GeoSim.SIM_VERSION, earth.metersPerBlock(), Math.round(earth.blocksAround()));
    }
}
