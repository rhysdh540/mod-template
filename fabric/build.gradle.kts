architectury.fabric()

operator fun String.invoke(): String {
    return rootProject.ext[this] as? String
            ?: throw IllegalStateException("Property $this is not defined")
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${"fabric_version"()}")
    modApi("net.fabricmc.fabric-api:fabric-api:${"fabric_api_version"()}+${"minecraft_version"()}")

    modLocalRuntime("net.fabricmc.fabric-api:fabric-api-deprecated:${"fabric_api_version"()}+${"minecraft_version"()}")

    modImplementation("com.terraformersmc:modmenu:${"mod_menu_version"()}")
}