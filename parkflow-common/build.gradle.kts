plugins {
    kotlin("jvm")
    id("com.github.davidmc24.gradle.plugin.avro")
}

dependencies {
    implementation("org.apache.avro:avro:1.11.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}

// Configure Avro plugin
avro {
    isCreateSetters.set(true)
    isCreateOptionalGetters.set(false)
    isGettersReturnOptional.set(false)
    fieldVisibility.set("PRIVATE")
    outputCharacterEncoding.set("UTF-8")
    stringType.set("String")
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

sourceSets {
    main {
        java {
            srcDir("build/generated-main-avro-java")
        }
    }
    test {
        java {
            srcDir("build/generated-test-avro-java")
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateAvroJava")
}

tasks.named("compileTestKotlin") {
    dependsOn("generateTestAvroJava")
}
