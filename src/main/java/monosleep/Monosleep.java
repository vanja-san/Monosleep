package monosleep;

import monosleep.config.MonosleepConfig;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mod entrypoint. All behaviour lives in {@code monosleep.mixin}; this class exists because Fabric
 * requires a main entrypoint, and because it gives the mod a log source that is easy to attribute.
 * It also owns the notice cooldown, which is state that has to outlive a single notice.
 */
public class Monosleep implements ModInitializer {
	public static final String MOD_ID = "monosleep";

	/** Named after the mod id, so every line it writes says which mod wrote it. */
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static final MonosleepNoticeCooldown NOTICE_COOLDOWN = new MonosleepNoticeCooldown();

	public static MonosleepNoticeCooldown noticeCooldown() {
		return NOTICE_COOLDOWN;
	}

	@Override
	public void onInitialize() {
		LOGGER.info("One sleeping player is enough to skip the night");
		LOGGER.info("Sleep notice target: {}", MonosleepConfig.get().notificationTarget());

		// An integrated server keeps the game running after the world is closed, so without this the
		// window of the last world would still count against the first notices in the next one.
		ServerLifecycleEvents.SERVER_STARTED.register(server -> NOTICE_COOLDOWN.reset());
	}
}