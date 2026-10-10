package monosleep;

import java.util.List;

import monosleep.config.MonosleepConfig;
import monosleep.config.NotificationTarget;
import monosleep.data.MonosleepLang;
import monosleep.data.MonosleepLangKeys;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundTabListPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;

/**
 * Shows the "somebody went to bed" notice to everyone in the level, in the place the server owner
 * picked in {@code config/monosleep.json}.
 * <p>
 * The notice is a translatable component with a Russian fallback, because the mod is server-side
 * only: clients that do have the mod installed translate it, clients that do not fall back to
 * readable text instead of showing a raw key.
 */
public final class MonosleepNotifications {
	private MonosleepNotifications() {
	}

	public static void playerSlept(ServerPlayer sleeper, List<ServerPlayer> viewers) {
		MonosleepConfig config = MonosleepConfig.get();
		NotificationTarget target = config.notificationTarget();

		if (target == NotificationTarget.OFF) {
			return;
		}

		long now = Util.getMillis();
		MonosleepNoticeCooldown cooldown = Monosleep.noticeCooldown();

		if (!cooldown.isDue(
			sleeper.getUUID(),
			now,
			config.notificationCooldownSeconds(),
			config.notificationCooldownPerPlayerSeconds()
		)) {
			return;
		}

		cooldown.record(sleeper.getUUID(), now);

		Component message = Component.translatableWithFallback(
			MonosleepLangKeys.PLAYER_SLEPT,
			MonosleepLang.RU_RU.playerSlept(),
			sleeper.getDisplayName()
		);

		switch (target) {
			case CHAT -> {
				for (ServerPlayer viewer : viewers) {
					viewer.sendSystemMessage(message);
				}
			}
			case ACTION_BAR -> {
				for (ServerPlayer viewer : viewers) {
					viewer.sendOverlayMessage(message);
				}
			}
			case TITLE -> sendTitle(viewers, message, config);
			case TAB_LIST -> sendTabList(viewers, message);
			// Unreachable, OFF returned above, but a switch over an enum still has to name it.
			case OFF -> { }
		}
	}

	/**
	 * The sentence goes into the subtitle slot, which the client draws in the middle of the screen
	 * at {@code scale(2.0F)} instead of the {@code scale(4.0F)} the title uses, and that is what
	 * makes the whole line fit: a title is one centred line with no wrapping and no shrink to fit,
	 * so on the narrowest common screen it has room for roughly twenty characters, while the
	 * subtitle has twice that. The title packet still has to be sent, because the client skips the
	 * whole block unless a title is set; an empty one draws nothing at all, as HUD text has no
	 * background to fill in behind it.
	 */
	private static void sendTitle(List<ServerPlayer> viewers, Component message, MonosleepConfig config) {
		for (ServerPlayer viewer : viewers) {
			viewer.connection.send(new ClientboundSetTitlesAnimationPacket(
				config.titleFadeInTicks(),
				config.titleStayTicks(),
				config.titleFadeOutTicks()
			));
			viewer.connection.send(new ClientboundSetTitleTextPacket(Component.empty()));
			viewer.connection.send(new ClientboundSetSubtitleTextPacket(message));
		}
	}

	private static void sendTabList(List<ServerPlayer> viewers, Component header) {
		for (ServerPlayer viewer : viewers) {
			viewer.connection.send(new ClientboundTabListPacket(header, Component.empty()));
		}
	}
}