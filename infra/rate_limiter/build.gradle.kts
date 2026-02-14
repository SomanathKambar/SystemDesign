plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
}

allprojects {
    group = "com.systemdesign.infra.ratelimiter"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")

    dependencies {
        implementation(kotlin("stdlib"))
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
        testImplementation("io.mockk:mockk:1.13.9")
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])
            }
        }
        repositories {
            maven {
                name = "JFrog"
                url = uri("https://your-jfrog-instance.jfrog.io/artifactory/libs-release-local")
                credentials {
                    username = System.getenv("JFROG_USER")
                    password = System.getenv("JFROG_PASSWORD")
                }
            }
        }
    }

    tasks.test {
        useJUnitPlatform()
    }
}
