// Top-level Gradle build file
// This version works with AGP 8.8.1 and Kotlin 1.9.x

plugins {
    id("com.android.application") version "8.8.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    id("com.google.dagger.hilt.android") version "2.52" apply false
}

// No need for "org.jetbrains.kotlin.plugin.compose" — Compose is built into Kotlin Android plugin.
