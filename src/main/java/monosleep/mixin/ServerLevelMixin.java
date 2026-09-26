package monosleep.mixin;

import java.util.List;

import monosleep.MonosleepAdvancements;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla only skips the night once the amount of sleeping players reaches
 * {@code max(1, ceil(activePlayers * playersSleepingPercentage / 100))}, so a single
 * sleeping player cannot skip the night while other non-spectator players are online.
 * This mixin lowers the requirement to a single sleeping player.
 */
@Mixin(ServerLevel.class)
public class ServerLevelMixin {
	@Redirect(
		method = "tick(Ljava/util/function/BooleanSupplier;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughSleeping(I)Z")
	)
	private boolean monosleep$anyPlayerSleeping(SleepStatus sleepStatus, int percentage) {
		return sleepStatus.areEnoughSleeping(percentage) || monosleep$hasSleepingPlayer();
	}

	@Redirect(
		method = "tick(Ljava/util/function/BooleanSupplier;)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/SleepStatus;areEnoughDeepSleeping(ILjava/util/List;)Z")
	)
	private boolean monosleep$anyPlayerSleepingLongEnough(SleepStatus sleepStatus, int percentage, List<ServerPlayer> players) {
		return sleepStatus.areEnoughDeepSleeping(percentage, players) || monosleep$hasDeepSleepingPlayer();
	}

	/**
	 * Vanilla wakes everyone up as soon as the night is skipped, and the sleeping players are still
	 * marked as sleeping at that point, so this is the exact moment to check whether the mod was the
	 * reason the night was skipped.
	 */
	@Inject(method = "wakeUpAllPlayers()V", at = @At("HEAD"))
	private void monosleep$awardSoloNightSkip(CallbackInfo ci) {
		MonosleepAdvancements.awardSoloNightSkip((ServerLevel) (Object) this);
	}

	/**
	 * The vanilla action bar line ("3 players sleeping out of 5") counts towards the
	 * {@code playersSleepingPercentage} rule that this mixin bypasses, so it no longer describes what
	 * is actually going on. It is cancelled here and the same spot is used by the sleep notification
	 * of {@link ServerPlayerMixin} instead.
	 */
	@Inject(method = "announceSleepStatus", at = @At("HEAD"), cancellable = true)
	private void monosleep$cancelSleepStatus(CallbackInfo ci) {
		ci.cancel();
	}

	private boolean monosleep$hasSleepingPlayer() {
		for (ServerPlayer player : ((ServerLevel) (Object) this).players()) {
			if (!player.isSpectator() && player.isSleeping()) {
				return true;
			}
		}

		return false;
	}

	private boolean monosleep$hasDeepSleepingPlayer() {
		for (ServerPlayer player : ((ServerLevel) (Object) this).players()) {
			if (!player.isSpectator() && player.isSleepingLongEnough()) {
				return true;
			}
		}

		return false;
	}
}
