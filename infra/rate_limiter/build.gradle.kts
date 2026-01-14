plugins {
    kotlin("jvm") version "1.9.22"
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    dependencies {
        implementation(kotlin("stdlib"))
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
        testImplementation("io.mockk:mockk:1.13.9")
    }

    tasks.test {
        useJUnitPlatform()
    }
}
