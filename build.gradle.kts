plugins {
    java
}

group = "dev.wolfstudios"
version = "1.2.0"

val mcApiVersion = project.findProperty("mc") as? String ?: "1.21.4-R0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$mcApiVersion")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    archiveBaseName.set("Vanish+")
    archiveVersion.set(project.version.toString())
}
