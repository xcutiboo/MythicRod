import io.papermc.hangarpublishplugin.model.Platforms

plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
    alias(libs.plugins.hangar.publish)
}

dependencies {
    implementation(project(":mythicrod-api"))
    implementation(project(":mythicrod-common"))
    implementation(libs.bstats.bukkit)

    compileOnly(libs.paper.api)
    compileOnly(libs.lombok)

    annotationProcessor(libs.lombok)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.paper.api)
    testImplementation(libs.adventure.text.minimessage)
    testImplementation(libs.adventure.text.serializer.legacy)
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

        // Paper provides Adventure; bStats is the only bundled runtime library.
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

val rawAssetBase = "https://raw.githubusercontent.com/xcutiboo/MythicRod/master"

fun externalizeReadme(text: String): String {
    // Hangar/Modrinth render the page in isolation and cannot resolve repository-relative
    // paths. Promote the embedded SVG/PNG/JPG references and the docs/ inline links to
    // absolute URLs so the assets actually load when the page is viewed off-GitHub.
    val assetPath = Regex("""(!\[[^]]*]\(|src=")(?:\./)?(assets/[^)"\s]+)""")
    val docsPath = Regex("""\[([^]]+)]\((?:\./)?docs/([^)\s]+)\)""")
    val licenseLink = Regex("""\[([^]]+)]\(LICENSE\)""")
    val changelogLink = Regex("""\[([^]]+)]\(CHANGELOG\.md\)""")
    return text
        .replace(assetPath) { mr -> mr.groupValues[1] + rawAssetBase + "/" + mr.groupValues[2] }
        .replace(docsPath) { mr ->
            val label = mr.groupValues[1]
            val rel = mr.groupValues[2]
            "[" + label + "](https://github.com/xcutiboo/MythicRod/blob/master/docs/" + rel + ")"
        }
        .replace(licenseLink) { mr -> "[" + mr.groupValues[1] + "](https://github.com/xcutiboo/MythicRod/blob/master/LICENSE)" }
        .replace(changelogLink) { mr -> "[" + mr.groupValues[1] + "](https://github.com/xcutiboo/MythicRod/blob/master/CHANGELOG.md)" }
}

hangarPublish {
    publications.register("plugin") {
        version.set(project.version.toString())
        channel.set(if (project.version.toString().contains('-')) "Snapshot" else "Release")
        id.set(providers.gradleProperty("hangarProjectId").orElse("MythicRod"))
        apiKey.set(providers.environmentVariable("HANGAR_API_TOKEN").orElse(""))
        changelog.set(providers.provider {
            rootProject.file("CHANGELOG.md").takeIf { it.isFile }?.readText() ?: "MythicRod ${project.version}"
        })
        pages.resourcePage(externalizeReadme(rootProject.file("README.md").readText()))

        platforms {
            register(Platforms.PAPER) {
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                platformVersions.set(providers.gradleProperty("paperVersion")
                    .map { value -> value.split(",").map(String::trim).filter(String::isNotEmpty) })
            }
        }
    }
}
