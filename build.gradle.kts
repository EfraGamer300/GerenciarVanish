plugins {
    java
    id("com.gradleup.shadow") version "9.3.1"
}

group = "dev.wolfstudios"
version = "1.3.0"

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
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("com.mysql:mysql-connector-j:8.3.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveBaseName.set("VanishPlus")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    configurations = project.configurations.runtimeClasspath.map { setOf(it) }
    dependencies {
        exclude { dep ->
            val group = dep.moduleGroup
            group != "org.bstats" && group != "org.xerial" && group != "com.mysql"
        }
    }
    relocate("org.bstats", project.group.toString())
    relocate("org.sqlite", project.group.toString() + ".sqlite")
    relocate("com.mysql", project.group.toString() + ".mysql")
}

tasks.jar {
    dependsOn(tasks.shadowJar)
    enabled = false
}
