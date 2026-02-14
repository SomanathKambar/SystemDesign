plugins {
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":strategies:fixed-window"))
    implementation(project(":strategies:token-bucket"))
    implementation(project(":strategies:leaky-bucket"))
    implementation(project(":strategies:sliding-window"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}
