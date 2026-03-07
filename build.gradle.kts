// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.50" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0" apply false
}

// Configure Java toolchain to use JDK 17 for KAPT compatibility
allprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
