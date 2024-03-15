architectury.forge()

operator fun String.invoke(): String {
    return rootProject.ext[this] as? String
            ?: throw IllegalStateException("Property $this is not defined")
}

loom {
    forge {
        mixinConfig("${"archives_base_name"()}.mixins.json")
        mixinConfig("${"archives_base_name"()}-common.mixins.json")
    }
}

dependencies {
    forge("net.minecraftforge:forge:${"minecraft_version"()}-${"forge_version"()}")
}