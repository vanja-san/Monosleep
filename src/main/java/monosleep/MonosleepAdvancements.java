package monosleep;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Awards the {@code monosleep:slept_alone} advancement when one single sleeping player was enough
 * for the night to be skipped.
 * <p>
 * Called at the head of {@code ServerLevel.wakeUpAllPlayers()}, which vanilla invokes from
 * {@code ServerLevel.tick()} right after the sleep status was satisfied and before anybody is woken
 * up, so {@link ServerPlayer#isSleeping()} still reports the players that went to bed.
 */
public final class MonosleepAdvancements {
	public static final Identifier SLEPT_ALONE = Identifier.fromNamespaceAndPath("monosleep", "slept_alone");
	public static final String SLEPT_ALONE_CRITERION = "slept_alone";

	private MonosleepAdvancements() {
	}

	public static void awardSoloNightSkip(ServerLevel level) {
		ServerPlayer sleeper = null;

		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || !player.isSleeping()) {
				continue;
			}

			// Two players in bed is not a solo sleep, whoever ended up skipping the night.
			if (sleeper != null) {
				return;
			}

			sleeper = player;
		}

		if (sleeper == null) {
			return;
		}

		AdvancementHolder advancement = level.getServer().getAdvancements().get(SLEPT_ALONE);

		if (advancement == null) {
			return;
		}

		sleeper.getAdvancements().award(advancement, SLEPT_ALONE_CRITERION);
	}
}
