plugins {
    kotlin("jvm")
    `java-library`
}

dependencies {
    api(project(":core"))
    api("redis.clients:jedis:5.1.0")
    implementation(kotlin("stdlib"))
}
