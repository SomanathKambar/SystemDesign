plugins {
    kotlin("jvm")
    kotlin("plugin.spring") version "1.8.20"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":strategies:fixed-window"))
    implementation(project(":strategies:sliding-window"))
    implementation(project(":strategies:token-bucket"))
    implementation(project(":persistence:redis"))

    compileOnly("org.springframework.boot:spring-boot-autoconfigure:3.1.0")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.1.0")

    implementation(kotlin("stdlib"))
}
