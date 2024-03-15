architectury.neoForge()

operator fun String.invoke(): String {
    return rootProject.ext[this] as? String
            ?: throw IllegalStateException("Property $this is not defined")
}

dependencies {
    val neoMinecraftVersion = "minecraft_version"().substring(2)
    neoForge("net.neoforged:neoforge:${neoMinecraftVersion}.${"neoforge_version"()}")
}