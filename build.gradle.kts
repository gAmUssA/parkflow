plugins {
    kotlin("jvm") version "1.9.21" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.21" apply false
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1" apply false
}

allprojects {
    group = "io.gamov.parkflow"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven {
            url = uri("https://packages.confluent.io/maven/")
        }
    }
}

// Define versions in the root build file as per guidelines
object Versions {
    const val kafkaStreams = "3.6.1"
    const val flink = "1.19.0"
    const val kotest = "5.8.0"
    const val testcontainers = "1.19.3"
    const val avro = "1.11.3"
    const val confluentKafka = "7.5.1"
}
