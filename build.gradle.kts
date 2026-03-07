plugins {
    id("java")
    id("maven-publish")
}

group = "dev.rarehyperion.hzp"
version = "0.1.1-DEV"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.ow2.asm:asm:9.7")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    from("LICENSE") {
        into("META-INF")
    }
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}