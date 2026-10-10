package monosleep;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Decides whether a notice that somebody went to bed is due, so that a crowd getting into bed at
 * the same time does not produce one notice each.
 * <p>
 * The state is held per server rather than per level: chat is shared between levels, so a notice in
 * one dimension counts towards the window of the next notice in another. {@link #reset()} clears
 * it when a server starts, which matters on an integrated server where the game keeps running
 * after the world is closed.
 * <p>
 * A notice the client already shows cannot be taken back, so dropping the next one in time is the
 * only way to slow the notices down.
 */
public final class MonosleepNoticeCooldown {
	private final Map<UUID, Long> lastNoticeByPlayer = new HashMap<>();

	private boolean noticeSent;
	private long lastNoticeAt;

	/**
	 * Both windows are in seconds, the same unit the config file uses. {@code now} is in
	 * milliseconds and comes from {@code Util.getMillis()}, which is {@code System.nanoTime()}
	 * scaled down. That is monotonic rather than a wall clock, may start at a negative value, and
	 * has no natural "very old" value to seed the fields with: doing the arithmetic against one
	 * overflows and then reads as "just now", which silences every notice. The flag says whether
	 * anything was ever sent instead.
	 */
	public boolean isDue(UUID player, long now, int cooldownSeconds, int perPlayerCooldownSeconds) {
		long cooldown = cooldownSeconds * 1000L;
		long perPlayerCooldown = perPlayerCooldownSeconds * 1000L;

		// An entry older than the per-player window can never block anything again, and a window of
		// zero drops every entry. That keeps the map the size of the crowd rather than of the
		// session.
		this.lastNoticeByPlayer.values().removeIf(sentAt -> now - sentAt >= perPlayerCooldown);

		if (cooldown > 0L && this.noticeSent && now - this.lastNoticeAt < cooldown) {
			return false;
		}

		Long sentAt = this.lastNoticeByPlayer.get(player);

		return perPlayerCooldown <= 0L || sentAt == null || now - sentAt >= perPlayerCooldown;
	}

	public void record(UUID player, long now) {
		this.noticeSent = true;
		this.lastNoticeAt = now;
		this.lastNoticeByPlayer.put(player, now);
	}

	public void reset() {
		this.lastNoticeByPlayer.clear();
		this.noticeSent = false;
		this.lastNoticeAt = 0L;
	}

	/** Only the test that pins the bound reads this; the mod never does. */
	int trackedPlayers() {
		return this.lastNoticeByPlayer.size();
	}
}