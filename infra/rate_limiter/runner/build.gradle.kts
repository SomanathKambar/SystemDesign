plugins {
    application
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":strategies:fixed-window"))
    implementation(project(":strategies:sliding-window"))
    implementation(project(":strategies:token-bucket"))
    implementation(project(":strategies:leaky-bucket"))
    
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("com.github.ajalt.clikt:clikt:4.2.1")
}

application {
    mainClass.set("com.systemdesign.infra.ratelimiter.runner.MainKt")
}
