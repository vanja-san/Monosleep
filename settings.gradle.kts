pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
		gradlePluginPortal()
	}
}

// Repositories are declared here instead of in build.gradle.kts.
// https://docs.gradle.org/current/userguide/declaring_repositories.html
// Loom still injects the Mojang/Fabric repositories it needs to resolve Minecraft itself.
dependencyResolutionManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		mavenCentral()
	}
}

rootProject.name = "monosleep"
