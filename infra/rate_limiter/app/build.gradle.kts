plugins {
    application
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation(project(":strategies:fixed-window"))
    implementation(project(":strategies:sliding-window"))
    implementation(project(":strategies:token-bucket"))
    implementation(project(":core"))
    implementation(project(":engine"))
    
    val ktorVersion = "2.3.7"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion") // For HTML DSL if needed, but static files are likely easier
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("com.systemdesign.infra.ratelimiter.app.AppKt")
}
