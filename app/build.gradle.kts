import groovy.xml.dom.DOMCategory.attributes

/*
 * This file was generated by the Gradle 'init' task.
 *
 * This generated file contains a sample Java application project to get you started.
 * For more details on building Java & JVM projects, please refer to https://docs.gradle.org/8.6/userguide/building_java_projects.html in the Gradle documentation.
 */

plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lib"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "com.github.arobie1992.clarinet.sampleapp.App"
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.github.arobie1992.clarinet.sampleapp.App"
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
