operator fun String.invoke(): String {
    return rootProject.ext[this] as? String
            ?: throw IllegalStateException("Property $this is not defined")
}

architectury {
    common {
        for(project in rootProject.subprojects) {
            if(project.path == ":common") continue
            this.add(project.name.lowercase())
        }
    }
}

dependencies {
    "modCompileOnly"("net.fabricmc:fabric-loader:${"fabric_version"()}")
}

tasks.processResources {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${"archives_base_name"()}" }
    }
}