dependencies {
    // Core dependencies needed by the common module
    implementation(libs.guice)
    
    // Adventure API for text handling
    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.adventure.text.serializer.legacy)
}
