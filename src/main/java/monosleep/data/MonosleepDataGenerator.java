package monosleep.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator.Pack;

/**
 * Generates the advancement and the language files into src/main/generated/resources with
 * {@code .\gradlew.bat runDatagen}.
 */
public class MonosleepDataGenerator implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator generator) {
		Pack pack = generator.createPack();

		pack.addProvider(MonosleepAdvancementProvider::new);

		for (MonosleepLang language : MonosleepLang.values()) {
			pack.addProvider((output, registries) -> new MonosleepLangProvider(output, registries, language));
		}
	}
}
