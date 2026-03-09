plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":mythicrod-common"))
    
    compileOnly(libs.paper.api)
    
    implementation(libs.guice)
    implementation(libs.configurate.hocon)
    implementation(libs.hikaricp)
    implementation(libs.lettuce.core)
    implementation(libs.bstats.bukkit)
}

tasks {
    compileJava {
        options.compilerArgs.addAll(
            listOf(
                "-parameters",
                "-Xlint:deprecation",
                "-Xlint:unchecked",
                "-Xlint:-processing"
            )
        )
    }
    
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
    
    shadowJar {
        archiveBaseName.set("MythicRod-Paper")
        archiveClassifier.set("")
        
        relocate("com.google.inject", "io.xcutiboo.mythicrod.shaded.guice")
        relocate("org.spongepowered.configurate", "io.xcutiboo.mythicrod.shaded.configurate")
        relocate("com.zaxxer.hikari", "io.xcutiboo.mythicrod.shaded.hikari")
        relocate("io.lettuce", "io.xcutiboo.mythicrod.shaded.lettuce")
        relocate("org.bstats", "io.xcutiboo.mythicrod.shaded.bstats")
        
        dependencies {
            include(dependency("com.google.inject:guice"))
            include(dependency("org.spongepowered:configurate-hocon"))
            include(dependency("com.zaxxer:HikariCP"))
            include(dependency("io.lettuce:lettuce-core"))
            include(dependency("org.bstats:bstats-bukkit"))
        }
        
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
        
        minimize()
    }
    
    assemble {
        dependsOn(shadowJar)
    }
    
    jar {
        archiveBaseName.set("MythicRod-Paper")
        archiveClassifier.set("dev")
    }
}
