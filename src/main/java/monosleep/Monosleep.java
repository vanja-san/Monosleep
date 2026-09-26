package monosleep;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint. All behaviour lives in {@code monosleep.mixin}; this class exists because Fabric
 * requires a main entrypoint, and because it gives the mod a log source that is easy to attribute.
 */
public class Monosleep implements ModInitializer {
	public static final String MOD_ID = "monosleep";

	/** Named after the mod id, so every line it writes says which mod wrote it. */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("One sleeping player is enough to skip the night");
	}
}
