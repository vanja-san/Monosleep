package monosleep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MonosleepNoticeCooldownTest {
	private static final UUID STEVE = UUID.randomUUID();
	private static final UUID ALEX = UUID.randomUUID();

	private MonosleepNoticeCooldown cooldown;

	@BeforeEach
	void setUp() {
		this.cooldown = new MonosleepNoticeCooldown();
	}

	/**
	 * The regression this class exists for: the first notice used to be silenced for good, because
	 * the "nothing sent yet" timestamp was seeded with a value that made the subtraction overflow
	 * into a number smaller than the cooldown. {@code System.nanoTime()} may also start negative,
	 * so no fixed seed is safe.
	 */
	@Test
	@DisplayName("the first notice is due whatever the clock says")
	void firstNoticeIsAlwaysDue() {
		assertTrue(this.cooldown.isDue(STEVE, 1_000L, 5, 5));
		assertTrue(this.cooldown.isDue(STEVE, 0L, 5, 5));
		assertTrue(this.cooldown.isDue(STEVE, -1_750_000_000_000L, 5, 5));
		assertTrue(this.cooldown.isDue(STEVE, Long.MIN_VALUE + 1L, 5, 5));
	}

	@Test
	@DisplayName("the shared window drops a notice that lands inside it")
	void sharedWindowDropsInside() {
		this.cooldown.record(STEVE, 1_000L);

		assertFalse(this.cooldown.isDue(ALEX, 2_000L, 5, 5));
		assertFalse(this.cooldown.isDue(ALEX, 5_999L, 5, 5));
	}

	@Test
	@DisplayName("the shared window lets a notice through once it has passed")
	void sharedWindowAllowsAfter() {
		this.cooldown.record(STEVE, 1_000L);

		assertTrue(this.cooldown.isDue(ALEX, 6_000L, 5, 5));
	}

	@Test
	@DisplayName("the shared window counts from the last notice that got through")
	void sharedWindowMeasuresFromTheLastSentNotice() {
		this.cooldown.record(STEVE, 1_000L);

		// Dropped notices must not push the window forward, or a steady stream of players would
		// hold it open for good.
		assertFalse(this.cooldown.isDue(ALEX, 3_000L, 5, 0));
		assertFalse(this.cooldown.isDue(ALEX, 4_000L, 5, 0));

		this.cooldown.record(ALEX, 4_000L);

		assertFalse(this.cooldown.isDue(STEVE, 8_000L, 5, 0));
		assertTrue(this.cooldown.isDue(STEVE, 9_000L, 5, 0));
	}

	@Test
	@DisplayName("a player's own window drops the same player while it has not passed")
	void perPlayerWindowDropsSamePlayer() {
		this.cooldown.record(STEVE, 1_000L);

		assertFalse(this.cooldown.isDue(STEVE, 4_000L, 0, 5));
	}

	@Test
	@DisplayName("a player's own window does not hold anybody else back")
	void perPlayerWindowDoesNotHoldBackOtherPlayers() {
		this.cooldown.record(STEVE, 1_000L);

		// The shared window has passed, so it is Steve's own window that is still in the way, and
		// it only applies to him.
		assertTrue(this.cooldown.isDue(ALEX, 4_000L, 2, 10));
		assertFalse(this.cooldown.isDue(STEVE, 4_000L, 2, 10));
	}

	@Test
	@DisplayName("a window of zero turns the matching limit off")
	void zeroDisablesTheLimit() {
		this.cooldown.record(STEVE, 1_000L);

		assertTrue(this.cooldown.isDue(STEVE, 1_000L, 0, 0));
		assertTrue(this.cooldown.isDue(ALEX, 1_000L, 0, 0));
		assertTrue(this.cooldown.isDue(STEVE, 1_001L, 0, 5));
	}

	@Test
	@DisplayName("a new server starts with nothing remembered")
	void resetClearsEverything() {
		this.cooldown.record(STEVE, 1_000L);
		this.cooldown.reset();

		assertEquals(0, this.cooldown.trackedPlayers());
		assertTrue(this.cooldown.isDue(STEVE, 2_000L, 5, 5));
	}

	@Test
	@DisplayName("players are forgotten once their own window has passed")
	void prunesExpiredPlayers() {
		UUID idle = UUID.randomUUID();

		this.cooldown.record(idle, 0L);
		this.cooldown.record(STEVE, 1_000L);

		this.cooldown.isDue(ALEX, 2_000L, 5, 5);
		assertEquals(2, this.cooldown.trackedPlayers());

		// Both entries are older than the window now and can never block anything again, so they
		// are dropped rather than kept for the rest of the session.
		this.cooldown.isDue(ALEX, 8_000L, 5, 5);
		assertEquals(0, this.cooldown.trackedPlayers());
	}
}