dependencies {
    api(project(":mythicrod-api"))

    // Code generation
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    // Adventure API for text handling
    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.adventure.text.serializer.legacy)

    // Caching
    implementation(libs.caffeine)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.adventure.text.minimessage)
    testImplementation(libs.adventure.text.serializer.legacy)
}
