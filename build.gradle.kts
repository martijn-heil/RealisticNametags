import com.github.jengelman.gradle.plugins.shadow.ShadowExtension
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.JavaVersion.VERSION_1_8
import java.net.URI
import org.apache.tools.ant.filters.*
import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
    `java-gradle-plugin`
    kotlin("jvm") version "1.2.40"
    id("com.github.johnrengelman.shadow") version "2.0.3"
    idea
}

group = "com.github.martijn_heil.realisticnametags"
version = "1.0-SNAPSHOT"
description = "RealisticNametags"

apply {
    plugin("java")
    plugin("kotlin")
    plugin("com.github.johnrengelman.shadow")
    plugin("idea")
}

java {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
}

tasks {
    withType<ProcessResources> {
        filter(mapOf(Pair("tokens", mapOf(Pair("version", version)))), ReplaceTokens::class.java)
    }
    withType<ShadowJar> {
        this.classifier = null
        this.configurations = listOf(project.configurations.shadow)
        relocate("com.comphenix.packetwrapper", "shadow.com.comphenix.packetwrapper")
        exclude("com.comphenix.protocol")
    }
}

defaultTasks = listOf("shadowJar")

repositories {
    maven { url = URI("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }

    mavenCentral()
    mavenLocal()
}

idea {
    project {
        languageLevel = IdeaLanguageLevel("1.8")
    }
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

dependencies {
    compileOnly("org.bukkit:bukkit:1.12.2-R0.1-SNAPSHOT") { isChanging = true }
    compileOnly("org.spigotmc:spigot:1.12.2-R0.1-SNAPSHOT") { isChanging = true }
    compileOnly(fileTree("lib") { include("*.jar") })
    shadow(kotlin("stdlib"))
    shadow(fileTree("lib") { include("PacketWrapper.jar") })
}