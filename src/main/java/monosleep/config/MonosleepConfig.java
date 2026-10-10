package monosleep.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loader.api.FabricLoader;

/**
 * The mod settings, read once from {@code config/monosleep.json} when the server starts.
 * <p>
 * The file is JSON that also accepts {@code //} comments, because it is read with Gson's lenient
 * reader and written from {@link #TEMPLATE}, so an admin can annotate their own setup. A missing
 * file is created with the defaults, and a file that cannot be read or does not parse falls back to
 * the defaults instead of stopping the server. Keys that a file does not carry keep their default,
 * so a file written before a setting existed still loads.
 */
public final class MonosleepConfig {
	/**
	 * Its own logger under the mod's own name, rather than {@code Monosleep.LOGGER}: the entrypoint
	 * reads this class to log the target it loaded, so borrowing its logger would make the two
	 * packages depend on each other.
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger("monosleep");

	private static final String FILE_NAME = "monosleep.json";
	private static final String KEY_NOTIFICATION_TARGET = "notificationTarget";
	private static final String KEY_COOLDOWN_SECONDS = "notificationCooldownSeconds";
	private static final String KEY_COOLDOWN_PER_PLAYER_SECONDS = "notificationCooldownPerPlayerSeconds";
	private static final String KEY_TITLE_FADE_IN_TICKS = "titleFadeInTicks";
	private static final String KEY_TITLE_STAY_TICKS = "titleStayTicks";
	private static final String KEY_TITLE_FADE_OUT_TICKS = "titleFadeOutTicks";

	private static final String TEMPLATE = """
			{
			  // Where the "somebody went to bed" notice is shown. It is read once at server start,
			  // so a change here only takes effect after a restart.
			  //
			  // CHAT                a line in the regular chat
			  // ACTION_BAR          above the hotbar, where vanilla puts its own sleep notice
			  // TITLE               in the middle of the screen, at half the size of a vanilla
			  //                     title so that the whole line fits
			  // TAB_LIST            above the player list; it replaces the header until another mod
			  //                     or plugin writes its own
			  // OFF                 no notice at all
			  "notificationTarget": "%s",

			  // Seconds that have to pass between two notices. While they have not, the next notice
			  // is dropped instead of being sent. It counts for every target, not only for chat,
			  // because the notice is dropped before the target is even looked at.
			  "notificationCooldownSeconds": %d,

			  // The same, but counted for each player separately: a notice about somebody who just
			  // went to bed is dropped for this many seconds, even when the cooldown above has
			  // already passed.
			  "notificationCooldownPerPlayerSeconds": %d,

			  // How long a TITLE notice stays on screen: fade in, hold, fade out. A tick is a
			  // twentieth of a second, so the default holds it for two seconds. These only matter
			  // for the TITLE target, the other places are replaced or timed by the client.
			  "titleFadeInTicks": %d,
			  "titleStayTicks": %d,
			  "titleFadeOutTicks": %d
			}
			""";

	private static final MonosleepConfig INSTANCE = load();

	private NotificationTarget notificationTarget = NotificationTarget.CHAT;
	private int notificationCooldownSeconds = 5;
	private int notificationCooldownPerPlayerSeconds = 5;
	private int titleFadeInTicks = 5;
	private int titleStayTicks = 40;
	private int titleFadeOutTicks = 10;

	private MonosleepConfig() {
	}

	public static MonosleepConfig get() {
		return INSTANCE;
	}

	public NotificationTarget notificationTarget() {
		return this.notificationTarget;
	}

	public int notificationCooldownSeconds() {
		return this.notificationCooldownSeconds;
	}

	public int notificationCooldownPerPlayerSeconds() {
		return this.notificationCooldownPerPlayerSeconds;
	}

	public int titleFadeInTicks() {
		return this.titleFadeInTicks;
	}

	public int titleStayTicks() {
		return this.titleStayTicks;
	}

	public int titleFadeOutTicks() {
		return this.titleFadeOutTicks;
	}

	private static MonosleepConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		MonosleepConfig config = new MonosleepConfig();

		try {
			if (Files.exists(path)) {
				config.read(path);

				return config;
			}

			// The fields above are the only place the defaults are written; the template reads them
			// back so the file a server gets always shows the values the mod actually uses.
			Files.createDirectories(path.getParent());
			Files.writeString(
				path,
				TEMPLATE.formatted(
					config.notificationTarget().name(),
					config.notificationCooldownSeconds(),
					config.notificationCooldownPerPlayerSeconds(),
					config.titleFadeInTicks(),
					config.titleStayTicks(),
					config.titleFadeOutTicks()
				),
				StandardCharsets.UTF_8
			);
			LOGGER.info("Wrote default settings to {}", path);
		} catch (IOException | JsonParseException e) {
			LOGGER.error("Could not use {}, falling back to defaults", FILE_NAME, e);

			return new MonosleepConfig();
		}

		return config;
	}

	private void read(Path path) throws IOException {
		JsonElement root;

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			root = JsonParser.parseReader(reader);
		}

		if (root == null || !root.isJsonObject()) {
			LOGGER.error("{} holds no JSON object, ignoring every setting in it", FILE_NAME);

			return;
		}

		JsonObject object = root.getAsJsonObject();

		this.notificationTarget = readTarget(object, this.notificationTarget);
		this.notificationCooldownSeconds = readNonNegativeInt(object, KEY_COOLDOWN_SECONDS, this.notificationCooldownSeconds);
		this.notificationCooldownPerPlayerSeconds = readNonNegativeInt(
			object,
			KEY_COOLDOWN_PER_PLAYER_SECONDS,
			this.notificationCooldownPerPlayerSeconds
		);
		this.titleFadeInTicks = readNonNegativeInt(object, KEY_TITLE_FADE_IN_TICKS, this.titleFadeInTicks);
		this.titleStayTicks = readNonNegativeInt(object, KEY_TITLE_STAY_TICKS, this.titleStayTicks);
		this.titleFadeOutTicks = readNonNegativeInt(object, KEY_TITLE_FADE_OUT_TICKS, this.titleFadeOutTicks);
	}

	private static NotificationTarget readTarget(JsonObject root, NotificationTarget fallback) {
		JsonElement value = root.get(KEY_NOTIFICATION_TARGET);

		if (value == null || value.isJsonNull()) {
			return fallback;
		}

		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
			LOGGER.error("\"{}\" must be a string, keeping {}", KEY_NOTIFICATION_TARGET, fallback);

			return fallback;
		}

		String raw = value.getAsString().trim();

		try {
			return NotificationTarget.valueOf(raw.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			LOGGER.error("\"{}\" has no target \"{}\", keeping {}", KEY_NOTIFICATION_TARGET, raw, fallback);

			return fallback;
		}
	}

	private static int readNonNegativeInt(JsonObject root, String key, int fallback) {
		JsonElement value = root.get(key);

		if (value == null || value.isJsonNull()) {
			return fallback;
		}

		if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
			LOGGER.error("\"{}\" must be a number, keeping {}", key, fallback);

			return fallback;
		}

		int number;

		try {
			number = value.getAsInt();
		} catch (NumberFormatException e) {
			LOGGER.error("\"{}\" is not a whole number, keeping {}", key, fallback);

			return fallback;
		}

		if (number < 0) {
			LOGGER.error("\"{}\" cannot be negative, keeping {}", key, fallback);

			return fallback;
		}

		return number;
	}
}