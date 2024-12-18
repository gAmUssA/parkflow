plugins {
    kotlin("jvm") version "2.1.0"
    id("io.ktor.plugin") version "3.0.2"
    kotlin("plugin.serialization") version "2.1.0"
    application
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

dependencies {
    implementation(project(":parkflow-common"))
    implementation("org.apache.avro:avro:${Versions.avro}")
    implementation("org.apache.kafka:kafka-clients:${Versions.kafkaStreams}")
    implementation("io.confluent:kafka-avro-serializer:${Versions.confluentKafka}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("net.datafaker:datafaker:2.4.2")
    
    // Configuration
    implementation("com.typesafe:config:1.4.3")
    
    // Ktor server dependencies
    implementation("io.ktor:ktor-server-core:3.0.2")
    implementation("io.ktor:ktor-server-netty:3.0.2")
    implementation("io.ktor:ktor-server-content-negotiation:3.0.2")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.2")
    implementation("io.ktor:ktor-server-config-yaml:3.0.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Test dependencies
    testImplementation("io.kotest:kotest-runner-junit5:${Versions.kotest}")
    testImplementation("io.kotest:kotest-assertions-core:${Versions.kotest}")
    testImplementation("io.kotest:kotest-property:${Versions.kotest}")
    testImplementation("org.testcontainers:testcontainers:${Versions.testcontainers}")
    testImplementation("org.testcontainers:junit-jupiter:${Versions.testcontainers}")
    testImplementation("org.testcontainers:kafka:${Versions.testcontainers}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("io.ktor:ktor-server-test-host:3.0.2")
}

application {
    mainClass.set("io.gamov.parkflow.entry.ApplicationKt")
}

ktor {
    docker {
        jreVersion.set(JavaVersion.VERSION_17)
        localImageName.set("parkflow-entry-exit")
        imageTag.set("0.0.1")
        portMappings.set(listOf(
            io.ktor.plugin.features.DockerPortMapping(
                8085,
                8085,
                io.ktor.plugin.features.DockerPortMappingProtocol.TCP
            )
        ))
        jib {
            container {
                mainClass = "io.gamov.parkflow.entry.ApplicationKt"
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
