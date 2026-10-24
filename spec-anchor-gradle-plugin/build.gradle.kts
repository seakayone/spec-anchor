plugins {
    `java-gradle-plugin`
    `maven-publish`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withJavadocJar()
    withSourcesJar()
}

// Gradle 9 runs on Java 17+, so the plugin must load there.
tasks.compileJava {
    options.release = 17
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:all,-missing", true)
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(gradleTestKit())
    testImplementation(project(":spec-anchor-annotations"))
    testRuntimeOnly(libs.junit.platform.launcher)
}

gradlePlugin {
    plugins {
        create("specAnchor") {
            id = "org.kleinb.openspec"
            implementationClass = "org.kleinb.openspec.gradle.SpecAnchorPlugin"
            displayName = "spec-anchor"
            description = "Checks OpenSpec scenarios against the tests linked to them with @OpenSpec"
        }
    }
}

// java-gradle-plugin creates the pluginMaven publication and the plugin marker; Central needs full POMs for both.
publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name = "spec-anchor-gradle-plugin"
            description = "Gradle plugin that checks OpenSpec scenarios against the tests linked to them with @OpenSpec"
            url = "https://github.com/seakayone/spec-anchor"
            inceptionYear = "2026"
            licenses {
                license {
                    name = "Apache-2.0"
                    url = "https://spdx.org/licenses/Apache-2.0.html"
                }
            }
            developers {
                developer {
                    id = "seakayone"
                    name = "Christian Kleinbölting"
                }
            }
            scm {
                connection = "scm:git:https://github.com/seakayone/spec-anchor"
                developerConnection = "scm:git:ssh://github.com/seakayone/spec-anchor.git"
                url = "https://github.com/seakayone/spec-anchor"
            }
        }
    }

    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

val annotationsJar = project(":spec-anchor-annotations").tasks.named<Jar>("jar")

tasks.test {
    useJUnitPlatform()
    dependsOn(annotationsJar)
    val jar = annotationsJar.flatMap { it.archiveFile }
    val kotlinVersion = libs.versions.kotlin.get()
    jvmArgumentProviders.add(CommandLineArgumentProvider {
        listOf("-DspecAnchor.annotationsJar=${jar.get().asFile.absolutePath}", "-DspecAnchor.kotlinVersion=$kotlinVersion")
    })
}
