plugins {
    id("fabric-loom") version "1.14-SNAPSHOT"
}

// ---- Versions (edit here if Gradle says one doesn't exist) ----
val minecraftVersion = "1.21.11"
val yarnMappings = "1.21.11+build.3"
val loaderVersion = "0.18.1"
val meteorVersion = "1.21.11-SNAPSHOT"
val jdkVersion = 21
val modVersion = "1.0.0"
// -----------------------------------------------------------------

val archivesBaseName = "floppa-walker"

base {
    archivesName = archivesBaseName
    version = modVersion
    group = "com.example.floppawalker"
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }
}

dependencies {
    // Minecraft 1.21.11 is still obfuscated, so we use Yarn mappings.
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    // Meteor Client
    modImplementation("meteordevelopment:meteor-client:$meteorVersion")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(jdkVersion))
    }
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to modVersion,
            "minecraft_version" to minecraftVersion,
            "jdk_version" to jdkVersion.toString(),
        )

        inputs.properties(propertyMap)

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        inputs.property("archivesName", archivesBaseName)

        from("LICENSE") {
            rename { "${it}_$archivesBaseName" }
        }
    }

    withType<JavaCompile>().configureEach {
        options.release = jdkVersion
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }
}
