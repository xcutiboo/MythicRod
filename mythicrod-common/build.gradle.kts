dependencies {
    api(project(":mythicrod-api"))

    compileOnly(libs.lombok)
    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.adventure.text.serializer.legacy)

    annotationProcessor(libs.lombok)

    implementation(libs.caffeine)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.adventure.text.minimessage)
    testImplementation(libs.adventure.text.serializer.legacy)
}
