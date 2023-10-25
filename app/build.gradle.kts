plugins {
    // Apply the groovy plugin to also add support for Groovy (needed for Spock)
    groovy
    // Apply the application plugin to add support for building a CLI application in Java.
    application
    id("java")
    id("io.ktor.plugin") version "2.3.5"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Use the latest Groovy version for Spock testing
    testImplementation("org.codehaus.groovy:groovy:3.0.13")

    // Use the awesome Spock testing and specification framework even with Java
    testImplementation("org.spockframework:spock-core:2.2-groovy-3.0")
    testImplementation("junit:junit:4.13.2")

    // The dependencies are used by the application.
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.jfree:jfreechart:1.5.4")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("net.sourceforge.argparse4j:argparse4j:0.9.0")
    implementation("commons-io:commons-io:2.6")
}

application {
    // Define the main class for the application.
    mainClass.set("org.cap.Main")
}

ktor {
    fatJar {
        archiveFileName.set("run.jar")
    }
}

tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()
}
