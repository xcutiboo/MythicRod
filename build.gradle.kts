plugins {
    `java-library`
    alias(libs.plugins.shadow) apply false
}

group = "io.xcutiboo"
version = "1.0.0"

subprojects {
    apply(plugin = "java-library")
    
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }
        maven("https://repo.nexomc.com/snapshots/") {
            name = "nexo"
        }
        maven("https://jitpack.io") {
            name = "jitpack"
        }
    }
    
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
    
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
}
