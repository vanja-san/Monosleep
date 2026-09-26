plugins {
	alias(libs.plugins.fabric.loom)
	`maven-publish`
}

// Minecraft 26.3 ships Java 25 class files, so 25 is both the compile target and
// the minimum JDK required to run the game. Declared once and reused below.
val javaLanguageVersion = 25

// Release artifact naming: <mod id>-v<version>-mc<minecraft>-fabric.jar
val modId = "monosleep"
val minecraftVersion = libs.versions.minecraft.get()

loom {
	mods {
		register(modId) {
			sourceSet(sourceSets.main.get())
		}
	}

	runs {
		// Data generation for translations and advancements, run with:
		//   .\gradlew.bat runDatagen
		// Output goes to src/main/generated/resources and is committed, so a normal build uses it.
		create("datagen") {
			client()
			displayName = "Data Generation"

			val outputDir = layout.projectDirectory.dir("src/main/generated/resources").asFile
			jvmArguments.add("-Dfabric-api.datagen")
			jvmArguments.add("-Dfabric-api.datagen.output-dir=" + outputDir.absolutePath)
			jvmArguments.add("-Dfabric-api.datagen.modid=monosleep")
		}
	}
}

sourceSets {
	main {
		resources.srcDir("src/main/generated/resources")
	}
}

dependencies {
	// Versions live in gradle/libs.versions.toml.
	minecraft(libs.minecraft)
	implementation(libs.fabric.loader)
	implementation(libs.fabric.api)
}

java {
	// Pin the JDK so the produced bytecode does not depend on the local JAVA_HOME.
	toolchain {
		languageVersion = JavaLanguageVersion.of(javaLanguageVersion)
	}

	// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
	// if it is present.
	// If you remove this line, sources will not be generated.
	withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
	options.release = javaLanguageVersion
}

tasks.processResources {
	// Local on purpose: the filesMatching action below is stored in the task and serialized for the
	// configuration cache, and it must not capture the script instance.
	val modVersion = project.version.toString()

	inputs.property("version", modVersion)

	filesMatching("fabric.mod.json") {
		expand("version" to modVersion)
	}

	// Datagen keeps its incremental cache inside the output directory, it must not ship in the jar.
	exclude(".cache/**")
}

tasks.jar {
	// The release artifact carries the mod id, version, Minecraft version and loader, so several
	// jars can sit in one folder without guessing which one belongs where. Example:
	//   monosleep-v1.0-mc26.3-fabric.jar
	archiveFileName = "${modId}-v${project.version}-mc${minecraftVersion}-fabric.jar"
	inputs.property("archiveFileName", archiveFileName)

	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// withSourcesJar() creates this task at configuration time, so there is no generated accessor for it.
tasks.named<Jar>("sourcesJar") {
	archiveFileName = "${modId}-v${project.version}-mc${minecraftVersion}-fabric-sources.jar"
	inputs.property("archiveFileName", archiveFileName)
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// Repositories to publish to are intentionally left empty; see
	// https://docs.gradle.org/current/userguide/publishing_maven.html
}
