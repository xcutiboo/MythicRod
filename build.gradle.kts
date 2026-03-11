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
        
        // Strict compilation: treat all warnings as errors
        options.compilerArgs.addAll(
            listOf(
                "-Werror",                    // Treat warnings as errors
                "-Xlint:all",                 // Enable all warnings
                "-Xlint:deprecation",         // Warn about deprecated APIs
                "-Xlint:unchecked",           // Warn about unchecked operations
                "-Xlint:rawtypes",            // Warn about raw types
                "-Xlint:cast",                // Warn about unnecessary casts
                "-Xlint:divzero",             // Warn about division by zero
                "-Xlint:empty",               // Warn about empty statements
                "-Xlint:fallthrough",         // Warn about fall-through in switch
                "-Xlint:finally",             // Warn about finally blocks
                "-Xlint:overrides",           // Warn about missing @Override
                "-Xlint:path",                // Warn about invalid path elements
                "-Xlint:serial",              // Warn about missing serialVersionUID
                "-Xlint:static",              // Warn about static access issues
                "-Xlint:try",                 // Warn about try-with-resources
                "-Xlint:varargs",             // Warn about varargs issues
                "-Xlint:-processing",         // Disable annotation processing warnings
                "-parameters"                  // Generate parameter metadata for reflection
            )
        )
    }
}
