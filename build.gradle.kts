// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.24" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" apply false
    id("pl.allegro.tech.build.axion-release") version "1.17.2"
}

// Configure semantic versioning based on git commits
scmVersion {
    // Use simple version creator (just the tag)
    versionCreator("simple")

    // Conventional commits patterns for version bumps
    nextVersion {
        suffix = "SNAPSHOT"
        separator = "-"
    }

    // Tag prefix (e.g., v1.0.0)
    tag {
        prefix.set("v")
        versionSeparator.set("")
    }

    // Checks for uncommitted changes
    checks {
        uncommittedChanges.set(false)  // Allow building with uncommitted changes
    }

    // Don't append branch name
    branchVersionCreator.putAll(mapOf(
        "main" to "simple",
        "master" to "simple"
    ))
}

// Expose version to subprojects
val releaseVersion = scmVersion.version
project.version = releaseVersion

// Calculate versionCode from semantic version
// Format: Major * 10000 + Minor * 100 + Patch
// Example: 1.2.3 -> 10203
fun semanticVersionToCode(version: String): Int {
    val versionPattern = Regex("""(\d+)\.(\d+)\.(\d+)""")
    val match = versionPattern.find(version.split("-")[0]) // Remove -SNAPSHOT suffix
    return if (match != null) {
        val (major, minor, patch) = match.destructured
        major.toInt() * 10000 + minor.toInt() * 100 + patch.toInt()
    } else {
        1 // Fallback
    }
}

// Make version info available to app module
ext {
    set("appVersionName", releaseVersion)
    set("appVersionCode", semanticVersionToCode(releaseVersion))
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
