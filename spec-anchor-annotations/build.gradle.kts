plugins {
    `java-library`
    `maven-publish`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Only the published classes target Java 8; tests use the toolchain's Java 21 (JUnit 6 needs 17+).
tasks.compileJava {
    options.release = 8
    options.compilerArgs.add("-Xlint:-options")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name = "spec-anchor-annotations"
                description = "Annotation that links Java tests to OpenSpec scenarios"
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
    }

    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}
