plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = "dev.wolfstudios"
version = "1.2.0"

val mcApiVersion = project.findProperty("mc") as? String ?: "1.21.4-R0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$mcApiVersion")
    implementation("org.bstats:bstats-bukkit:3.2.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveBaseName.set("Vanish+")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    configurations = project.configurations.runtimeClasspath.map { setOf(it) }
    dependencies {
        exclude { it.moduleGroup != "org.bstats" }
    }
    relocate("org.bstats", project.group.toString())
}

tasks.jar {
    dependsOn(tasks.shadowJar)
    enabled = false
}
