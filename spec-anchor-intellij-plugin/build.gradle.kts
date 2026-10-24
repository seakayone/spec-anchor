import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.intellij.platform)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // Lowest Kotlin API available in IntelliJ IDEA 2025.2 (sinceBuild 252).
        apiVersion = KotlinVersion.KOTLIN_2_1
        freeCompilerArgs.add("-Xsuppress-version-warnings")
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity(libs.versions.intellijIdea)
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.intellij.plugins.markdown")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    testImplementation(libs.junit4)
    testImplementation(libs.opentest4j)
    testImplementation(project(":spec-anchor-annotations"))
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
        }
    }
    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, libs.versions.intellijIdea.get())
        }
    }
}
