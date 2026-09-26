package monosleep.mixin;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Unit;

import monosleep.data.MonosleepLang;
import monosleep.data.MonosleepLangKeys;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * Shows a notification in the action bar, the same spot vanilla uses for the "3 players sleeping out
 * of 5" line, to everyone in the level whenever a player successfully goes to sleep. The text is a
 * translatable component with a Russian fallback, because the mod is server-side only: clients that do
 * have the mod installed translate it, clients that do not fall back to readable text instead of
 * showing a raw key.
 * <p>
 * The action bar is drawn with the regular font at the client's own GUI scale, unlike a screen title,
 * which the client renders at 4x the font size.
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
		Component message = Component.translatableWithFallback(
			MonosleepLangKeys.PLAYER_SLEPT,
			MonosleepLang.RU_RU.playerSlept(),
			player.getDisplayName()
		);

		for (ServerPlayer viewer : player.level().players()) {
			viewer.sendOverlayMessage(message);
		}
	}
}
