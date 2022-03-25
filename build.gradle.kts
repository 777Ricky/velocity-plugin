import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

repositories {
    mavenLocal()
    maven("https://nexus.velocitypowered.com/repository/maven-public/")
    maven("https://repo.maven.apache.org/maven2/")
    mavenCentral()
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.1.0")
    implementation("redis.clients:jedis:4.2.0")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.0")
}

group = "net.analyse"
version = "1.0.0"
description = "AnalyseVelocity"
java.sourceCompatibility = JavaVersion.VERSION_1_8

val shadowJar: ShadowJar by tasks

task<com.github.jengelman.gradle.plugins.shadow.tasks.ConfigureShadowRelocation>("relocateShadowJar") {
    target = tasks.shadowJar.get()
    prefix = "net.analyse.plugin.libs"
}

tasks.shadowJar.get().dependsOn(tasks.getByName("relocateShadowJar"))

tasks.withType<ProcessResources> {
    filesMatching("**/config.yml") {
        expand("version" to project.version)
    }
}

tasks.register<Copy>("copyJarToServerPlugins") {
    from(tasks.getByPath("shadowJar"))
    into(layout.projectDirectory.dir("server/plugins"))
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}