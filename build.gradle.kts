// File: /build.gradle.kts (File root project Anda)
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // DIUBAH: Alias yang benar adalah "androidApplication" (tanpa titik)
    alias(libs.plugins.androidApplication) apply false

    // DIUBAH: Gunakan alias dari TOML, jangan gunakan id()
    alias(libs.plugins.googleGmsServices) apply false
}