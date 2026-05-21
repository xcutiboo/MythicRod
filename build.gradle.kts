plugins {
    `java-library`
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.sonarqube)
}

sonar {
    properties {
        property("sonar.projectKey", "xcutiboo_MythicRod")
        property("sonar.organization", "xcutiboo")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.projectName", "MythicRod")
        property("sonar.exclusions", "**/build/**,**/.gradle/**,**/generated/**,graphify-out/**,.playwright-mcp/**")
    }
}

group = "io.xcutiboo"
version = providers.gradleProperty("version").get()

subprojects {
    apply(plugin = "java-library")

    dependencyLocking {
        lockAllConfigurations()
    }

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    dependencies {
        val libsCatalog = rootProject.extensions
            .getByType<org.gradle.api.artifacts.VersionCatalogsExtension>()
            .named("libs")
        "testRuntimeOnly"(libsCatalog.findLibrary("junit-platform-launcher").get())
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(25)

        // Strict compilation: treat all warnings as errors
        options.compilerArgs.addAll(
            listOf(
                "-Werror",                    // Treat warnings as errors
                "-Xlint:all",                 // Enable all warnings
                "-Xlint:-deprecation",        // Disable deprecation warnings (Paper API has deprecated methods we must use)
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

    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        useJUnitPlatform()
    }
}
