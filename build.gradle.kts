plugins {
    id("java")
}

group = "org.cap"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")
    testImplementation("junit:junit:4.12")
}

tasks.test {
    useJUnitPlatform()
}