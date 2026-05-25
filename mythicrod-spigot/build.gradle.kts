plugins {
    alias(libs.plugins.shadow)
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/groups/public/") {
        name = "spigotmc"
    }
}

dependencies {
    implementation(project(":mythicrod-api"))
    implementation(project(":mythicrod-common"))

    compileOnly(libs.spigot.api)
}

tasks {
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveBaseName.set("MythicRod-Spigot")
        archiveClassifier.set("")
        configurations = listOf(project.configurations.runtimeClasspath.get())
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    assemble {
        dependsOn(shadowJar)
    }

    jar {
        archiveBaseName.set("MythicRod-Spigot")
        archiveClassifier.set("dev")
    }
}
