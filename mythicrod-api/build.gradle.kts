plugins {
    `maven-publish`
}

dependencies {
    compileOnly(libs.jetbrains.annotations)

    testImplementation(libs.junit.jupiter)
    testCompileOnly(libs.jetbrains.annotations)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        register<MavenPublication>("maven") {
            from(components["java"])
            groupId = "io.xcutiboo"
            artifactId = "mythicrod-api"
            version = project.version.toString()

            pom {
                name.set("MythicRod API")
                description.set("Public integration surface for MythicRod (Paper / Folia).")
                url.set("https://github.com/xcutiboo/MythicRod")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://github.com/xcutiboo/MythicRod/blob/master/LICENSE")
                    }
                }
                scm {
                    url.set("https://github.com/xcutiboo/MythicRod")
                    connection.set("scm:git:git://github.com/xcutiboo/MythicRod.git")
                    developerConnection.set("scm:git:ssh://git@github.com:xcutiboo/MythicRod.git")
                }
            }
        }
    }
}
