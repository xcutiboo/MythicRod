plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":mythicrod-common"))
    
    compileOnly(libs.paper.api)
    
    // Code generation
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    
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
        
        // DO NOT relocate Adventure - Paper provides it
        // Only relocate libraries not provided by Paper
        relocate("org.spongepowered.configurate", "io.xcutiboo.mythicrod.shaded.configurate")
        relocate("com.zaxxer.hikari", "io.xcutiboo.mythicrod.shaded.hikari")
        relocate("io.lettuce", "io.xcutiboo.mythicrod.shaded.lettuce")
        relocate("org.bstats", "io.xcutiboo.mythicrod.shaded.bstats")
        
        // Include common module and all dependencies
        configurations = listOf(project.configurations.runtimeClasspath.get())
        
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
    
    assemble {
        dependsOn(shadowJar)
    }
    
    jar {
        archiveBaseName.set("MythicRod-Paper")
        archiveClassifier.set("dev")
    }
}
