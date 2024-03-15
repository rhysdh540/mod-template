import dev.architectury.plugin.ArchitectPluginExtension
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.jvm.tasks.Jar
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.Deflater

plugins {
    java
    id("architectury-plugin") version("3.4.151") apply(false)
    id("dev.architectury.loom") version("1.4.380") apply(false)
    id("com.github.johnrengelman.shadow") version("8.1.1")
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

//    java.withSourcesJar()

    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net")
        maven("https://maven.neoforged.net")
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
    compileOnly()
}

tasks.clean {
    delete(".architectury-transformer")
}

listOf("jar", "sourcesJar").forEach {
    tasks.findByName(it)?.enabled = false
}

subprojects {
    apply(plugin = "com.github.johnrengelman.shadow")
    apply(plugin = "dev.architectury.loom")

    val loom = extensions.getByType<LoomGradleExtensionAPI>()
    loom.silentMojangMappingsLicense()

    @Suppress("UnstableApiUsage")
    dependencies {
        "minecraft"("com.mojang:minecraft:${"minecraft_version"()}")
        "mappings"(loom.layered {
            mappings("org.quiltmc:quilt-mappings:${"minecraft_version"()}+build.${"quilt_mappings_version"()}:intermediary-v2")
            officialMojangMappings { nameSyntheticMembers = false }
            parchment("org.parchmentmc.data:parchment-${"minecraft_version"()}:${"parchment_version"()}@zip")
        })
    }

    tasks.processResources {
        val props = mapOf(
                "version" to version,
                "minecraft" to "minecraft_dependency_version"(),
        )

        inputs.properties(props)

        filesMatching(listOf("fabric.mod.json", "META-INF/mods.toml")) {
            expand(props)
        }
    }

    if (project == project(":common")) {
        return@subprojects
    }

    //region Platform Setup
    sourceSets["main"].resources.srcDirs(project(":common").sourceSets["main"].resources.srcDirs)
    project.extensions.getByType<ArchitectPluginExtension>().platformSetupLoomIde()
    loom.runs {
        remove(findByName("server"))

        maybeCreate("client").apply {
            client()
            name = "Minecraft Client"
            isIdeConfigGenerated = true
            val baseArgs = "-XX:+UnlockExperimentalVMOptions -XX:+UnlockDiagnosticVMOptions -XX:+AlwaysActAsServerClassMachine -XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+UseNUMA -XX:NmethodSweepActivity=1 -XX:ReservedCodeCacheSize=400M -XX:NonNMethodCodeHeapSize=12M -XX:ProfiledCodeHeapSize=194M -XX:NonProfiledCodeHeapSize=194M -XX:-DontCompileHugeMethods -XX:MaxNodeLimit=240000 -XX:NodeLimitFudgeFactor=8000 -XX:+UseVectorCmov -XX:+PerfDisableSharedMem -XX:+UseFastUnorderedTimeStamps -XX:+UseCriticalJavaThreadPriority -XX:ThreadPriorityPolicy=1 -XX:AllocatePrefetchStyle=3"
            val memoryArgs = "-Xmx4G -Xms4G"
            val gcArgs = "-XX:+UseShenandoahGC -XX:ShenandoahGCMode=iu -XX:ShenandoahGuaranteedGCInterval=1000000 -XX:AllocatePrefetchStyle=1"
            vmArgs("$baseArgs $memoryArgs $gcArgs".split(" "))
        }
    }

    val nameLowercase = name.lowercase()
    val nameCapitalized = name.capitalized()

    val common: Configuration by configurations.creating
    val shadowCommon: Configuration by configurations.creating
    configurations {
        compileClasspath.get().extendsFrom(common)
        runtimeClasspath.get().extendsFrom(common)
        val development = register("development$nameCapitalized").get()
        development.extendsFrom(common)
    }

    dependencies {
        (common(project("path" to ":common", "configuration" to "namedElements")) as ModuleDependency).isTransitive = false
        (shadowCommon(project("path" to ":common", "configuration" to "transformProduction$nameCapitalized")) as ModuleDependency).isTransitive = false
    }

    tasks.shadowJar {
        exclude("architectury.common.json")
        exclude("**/PlatformMethods.class")
        configurations = (listOf(shadowCommon))
        archiveClassifier = "shadow-$nameLowercase-dev"
        destinationDirectory = layout.buildDirectory.dir("devlibs")
        relocate("dev.rdh.f3", "dev.rdh.f3.$nameLowercase")
    }

    tasks.named<RemapJarTask>("remapJar") {
        inputFile = tasks.shadowJar.get().archiveFile
        dependsOn(tasks.shadowJar)
        archiveClassifier = nameLowercase
    }

    tasks.named<Jar>("jar") {
        archiveClassifier = "dev-$nameLowercase"
    }

    components.getByName<AdhocComponentWithVariants>("java") {
        withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
            skip()
        }
    }
    //endregion
}

tasks.shadowJar {
    subprojects.forEach {
        if(it != project(":common")) {
            this.from(it.tasks.named("remapJar"))
        }
    }

    archiveBaseName.set("archives_base_name"())
    archiveClassifier.set("")
    archiveVersion.set("modVersion"())

    doLast {
        val jar = archiveFile.get().asFile
        val contents = linkedMapOf<String, ByteArray>()
        JarFile(jar).use {
            it.entries().asIterator().forEach { entry ->
                if(!entry.isDirectory) {
                    contents[entry.name] = it.getInputStream(entry).readAllBytes()
                }
            }
        }

        jar.delete()

        JarOutputStream(jar.outputStream()).use { out ->
            out.setLevel(Deflater.BEST_COMPRESSION)
            contents.forEach { var (name, bytes) = it
                if(name.endsWith(".json") || name.endsWith(".mcmeta")) {
                    bytes = JsonOutput.toJson(JsonSlurper().parse(bytes)).toByteArray()
                }

                if(name.endsWith(".class")) {
                    val node = ClassNode()
                    ClassReader(bytes).accept(node, 0)

                    node.methods.forEach { method ->
                        method.localVariables?.clear()
                        method.parameters?.clear()
                    }

                    val writer = ClassWriter(0)
                    node.accept(writer)
                    bytes = writer.toByteArray()
                }

                out.putNextEntry(JarEntry(name))
                out.write(bytes)
                out.closeEntry()
            }
            out.finish()
            out.close()
        }
    }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

fun setup() {
    println("${project.name} v${"mod_version"()}")
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

operator fun String.invoke(): String {
    return rootProject.ext[this] as? String
            ?: throw IllegalStateException("Property $this is not defined")
}