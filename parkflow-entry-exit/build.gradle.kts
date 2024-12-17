plugins {
    kotlin("jvm")
    application
}

dependencies {
    implementation(project(":parkflow-common"))
    implementation("org.apache.avro:avro:1.11.3")
    implementation("org.apache.kafka:kafka-clients:3.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("io.github.microutils:kotlin-logging-jvm:3.0.5")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("net.datafaker:datafaker:2.0.2")
    
    // Test dependencies
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

application {
    mainClass.set("io.gamov.parkflow.datagen.MainKt")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
