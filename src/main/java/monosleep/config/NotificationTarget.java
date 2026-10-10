package monosleep.config;

/**
 * Where the "somebody went to bed" notice is shown. Picked once at server start in
 * {@code config/monosleep.json}, see {@link MonosleepConfig}.
 */
public enum NotificationTarget {
	/** A line in the regular chat, next to whatever else the server says. */
	CHAT,

	/** Above the hotbar, the spot vanilla uses for its own sleep notice. */
	ACTION_BAR,

	/**
	 * In the middle of the screen, at half the size of a vanilla title. See
	 * {@link monosleep.MonosleepNotifications} for why the sentence cannot go in the title itself.
	 */
	TITLE,

	/**
	 * The header above the player list. The header stays until something else writes its own, so
	 * this target collides with any other mod or plugin that puts a header there.
	 */
	TAB_LIST,

	/** No notice at all. */
	OFF
}