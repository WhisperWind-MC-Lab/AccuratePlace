package dev.accurateplace;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(AccuratePlace.MOD_ID)
public final class AccuratePlace {
    public static final String MOD_ID = "accurateplace";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AccuratePlace() {
        LOGGER.info("Accurate Place v3 protocol enabled");
    }
}
