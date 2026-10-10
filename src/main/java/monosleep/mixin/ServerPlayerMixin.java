package monosleep.mixin;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;

import monosleep.MonosleepNotifications;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sends the "somebody went to bed" notice to everyone in the level whenever a player successfully
 * goes to sleep. Where it lands is decided by {@link monosleep.config.NotificationTarget}, which
 * {@link monosleep.config.MonosleepConfig} reads from {@code config/monosleep.json}.
 */
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
	@Inject(
		method = "startSleepInBed(Lnet/minecraft/world/level/block/AbstractBedBlock;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/attribute/BedRule;Lnet/minecraft/core/BlockPos;)Lcom/mojang/datafixers/util/Either;",
		at = @At("RETURN")
	)
	private void monosleep$announceSleep(AbstractBedBlock bedBlock, BlockState bedBlockState, BedRule rule, BlockPos pos, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
		if (cir.getReturnValue().right().isEmpty()) {
			return;
		}

		ServerPlayer player = (ServerPlayer) (Object) this;

		MonosleepNotifications.playerSlept(player, player.level().players());
	}
}