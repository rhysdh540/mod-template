import dev.architectury.plugin.ArchitectPluginExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI

plugins {
    java
    id("architectury-plugin") apply(false)
    id("dev.architectury.loom") apply(false)
    id("com.github.johnrengelman.shadow") apply(false)

    id("io.github.pacifistmc.forgix")
}

operator fun String.invoke(): String {
    return rootProject.ext[this] as? String
            ?: throw IllegalStateException("Property $this is not defined")
}

setup()

allprojects {
    apply(plugin = "java")
    apply(plugin = "architectury-plugin")

    base.archivesName.set("archives_base_name"())
    version = "modVersion"()
    group = "maven_group"()

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(17)
    }

    java.withSourcesJar()

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.architectury.dev")
        maven("https://maven.quiltmc.org/repository/release")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://maven.parchmentmc.org")
        maven("https://jitpack.io")
        maven("https://maven.terraformersmc.com/releases")
        maven("https://maven.blamejared.com")
        maven("https://maven.tterrag.com")
        maven("https://api.modrinth.com/maven") {
            content {
                includeGroup("maven.modrinth")
            }
        }
        maven("https://cursemaven.com") {
            content {
                includeGroup("curse.maven")
            }
        }
    }
}

extensions.getByType<ArchitectPluginExtension>().apply {
    minecraft = "minecraft_version"()
}

tasks.clean.configure {
    delete(".architectury-transformer")
}

tasks.jar.configure {
    enabled = false
}

subprojects {
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "dev.architectury.loom")

    configurations.register("shade")

    val loom = extensions.getByType<LoomGradleExtensionAPI>()
    loom.silentMojangMappingsLicense()

    dependencies {
        "minecraft"("com.mojang:minecraft:${"minecraft_version"()}")
        @Suppress("UnstableApiUsage")
        "mappings"(loom.layered {
            mappings("org.quiltmc:quilt-mappings:${"minecraft_version"()}+build.${"quilt_mappings_version"()}:intermediary-v2")
            parchment("org.parchmentmc.data:parchment-${"minecraft_version"()}:${"parchment_version"()}@zip")
            officialMojangMappings { nameSyntheticMembers = false }
        })
    }

    tasks.processResources {
        val props = mapOf(
                "version" to version,
                "minecraft" to "minecraft_version"(),
                "fabric" to "fabric_version"(),
                "fabric_api" to "fabric_api_version"(),
                "forge" to "forge_version"().split("\\.")[0],
        )

        inputs.properties(props)

        filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml")) {
            expand(props)
        }
    }

    tasks.getByName("remapJar").finalizedBy("mergeJars")
}

tasks.assemble.configure {
    finalizedBy("mergeJars")
}

fun setup() {
    println("Template Mod v${"mod_version"()}")
    val buildNumber = System.getenv("GITHUB_RUN_NUMBER")
    if(buildNumber != null) {
        println("Build #$buildNumber")
        ext["build_number"] = buildNumber
    } else ext["build_number"] = null
    println()

    println("Plugin versions:")
    apply(plugin = "architectury-plugin")

    ext["modVersion"] = "mod_version"() + (if(buildNumber != null) "-build.$buildNumber" else "")

    tasks.register("nukeGradleCaches") {
        dependsOn("clean")
        group = "build"
        doLast {
            allprojects.forEach {
                it.file(".gradle").deleteRecursively()
            }
        }
    }
}