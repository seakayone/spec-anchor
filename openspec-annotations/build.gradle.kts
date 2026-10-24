plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
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
