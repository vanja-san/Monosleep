package monosleep.data;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;

import monosleep.MonosleepAdvancements;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;

public class MonosleepAdvancementProvider extends FabricAdvancementProvider {
	public MonosleepAdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
		super(output, registries);
	}

	@Override
	public void generateAdvancement(HolderLookup.Provider registries, Consumer<AdvancementHolder> consumer) {
		AdvancementHolder advancement = Advancement.Builder.advancement()
			.rootDisplay(
				Items.DAYLIGHT_DETECTOR,
				Component.translatableWithFallback(MonosleepLangKeys.SLEPT_ALONE_TITLE, MonosleepLang.RU_RU.advancementTitle()),
				Component.translatableWithFallback(MonosleepLangKeys.SLEPT_ALONE_DESCRIPTION, MonosleepLang.RU_RU.advancementDescription()),
				// Visible advancement roots are required to have a background, and a vanilla one keeps
				// the mod from shipping a texture of its own.
				Identifier.withDefaultNamespace("gui/advancements/backgrounds/stone"),
				AdvancementType.CHALLENGE,
				true,
				true,
				false
			)
			.addCriterion(
				MonosleepAdvancements.SLEPT_ALONE_CRITERION,
				new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance())
			)
			.rewards(AdvancementRewards.Builder.experience(50))
			.build(MonosleepAdvancements.SLEPT_ALONE);

		consumer.accept(advancement);
	}
}
