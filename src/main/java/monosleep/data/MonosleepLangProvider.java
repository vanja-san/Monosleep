package monosleep.data;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;

import net.minecraft.core.HolderLookup;

public class MonosleepLangProvider extends FabricLanguageProvider {
	private final MonosleepLang language;

	public MonosleepLangProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries, MonosleepLang language) {
		super(output, language.code(), registries);
		this.language = language;
	}

	@Override
	public void generateTranslations(HolderLookup.Provider registryLookup, TranslationBuilder builder) {
		builder.add(MonosleepLangKeys.SLEPT_ALONE_TITLE, this.language.advancementTitle());
		builder.add(MonosleepLangKeys.SLEPT_ALONE_DESCRIPTION, this.language.advancementDescription());
		builder.add(MonosleepLangKeys.PLAYER_SLEPT, this.language.playerSlept());
	}
}
