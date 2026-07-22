// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.1.0" apply false
    id("com.android.library") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("com.google.devtools.ksp") version "2.3.6" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.2" apply false
    id("pl.allegro.tech.build.axion-release") version "1.21.1"
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

// Calculate versionCode from git commit count (auto-increments with every commit)
// Offset ensures it's higher than any previously uploaded version code (10080 on Play Console)
fun gitCommitCount(): Int {
    val process = ProcessBuilder("git", "rev-list", "--count", "HEAD")
        .directory(projectDir)
        .redirectErrorStream(true)
        .start()
    val count = process.inputStream.bufferedReader().readText().trim().toIntOrNull() ?: 0
    process.waitFor()
    return count + 10011
}

// Make version info available to app module
ext {
    set("appVersionName", releaseVersion)
    set("appVersionCode", gitCommitCount())
}

// Configure Kotlin JVM target for all subprojects
allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
