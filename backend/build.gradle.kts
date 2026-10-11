plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // json
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // async
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // gson
    implementation("com.google.code.gson:gson:2.10.1")

    // test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
