plugins {
    alias(libs.plugins.shadow)
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigot"
    }
}

dependencies {
    implementation(project(":mythicrod-common"))
    
    compileOnly(libs.spigot.api)
    
    implementation(libs.bundles.adventure)
    implementation(libs.bstats.bukkit)
}

tasks {
    shadowJar {
        archiveBaseName.set("MythicRod-Spigot")
        archiveClassifier.set("")
        
        // Relocate Adventure to prevent conflicts
        relocate("net.kyori", "io.xcutiboo.mythicrod.shaded.kyori")
        relocate("org.bstats", "io.xcutiboo.mythicrod.shaded.bstats")
        
        dependencies {
            include(dependency("net.kyori:.*"))
            include(dependency("org.bstats:bstats-bukkit"))
        }
        
        // Exclude signature files
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        
        minimize()
    }
    
    assemble {
        dependsOn(shadowJar)
    }
    
    jar {
        archiveBaseName.set("MythicRod-Spigot")
        archiveClassifier.set("dev")
    }
}
