plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.intellij.platform) apply false
}

allprojects {
    group = "org.kleinb.openspec"
    version = "0.1.0-SNAPSHOT"
}
