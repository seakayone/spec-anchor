plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.intellij.platform) apply false
}

allprojects {
    group = "org.kleinb.openspec"
    version = rootProject.file("version.txt").readText().trim()
}
