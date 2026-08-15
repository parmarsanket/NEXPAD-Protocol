plugins {
    kotlin("multiplatform") version "1.9.22"
    `maven-publish`
}

group = "com.sanket.tools.nexpad"
version = "1.0.0"

repositories {
    mavenCentral()
}

kotlin {
    jvm()

    sourceSets {
        val commonMain = getByName("commonMain") {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
    }
}

// Ensure the JVM target compiles to Java 17
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}
