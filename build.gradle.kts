plugins {
    `java`
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.ccorp2002"
version = "0.1.7"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "glaremasters repo"
        url = uri("https://repo.glaremasters.me/repository/towny/")
    }
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "bkcommon"
        url = uri("https://ci.mg-dev.eu/plugin/repository/everything")
    }
    maven { url = uri("https://repo.codemc.io/repository/maven-releases/") }

    maven { url = uri("https://repo.codemc.io/repository/maven-snapshots/") }
    maven { url = uri("https://maven.enginehub.org/repo/") }
    flatDir {
        dirs("./.libs")
        //I just have a local folder where I dump all the other compile dependencies for ease of use, probably not good practice, but whatever.
    }
}

dependencies {
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.17")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0")
    compileOnly("net.kyori:adventure-api:5.0.0")
    compileOnly("net.dmulloy2:ProtocolLib:5.4.0")
    implementation("com.jeff-media:MorePersistentDataTypes:2.4.0")
    implementation("com.jeff-media:custom-block-data:2.2.8")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(fileTree(mapOf("dir" to "./.libs", "include" to listOf("*.jar"))))
    //See above comment
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveFileName.set("SmoothSailing.jar")
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
    }

    shadowJar {
        archiveFileName.set("SmoothSailing.jar")
        archiveClassifier.set("")
        manifest {
            attributes["paperweight-mappings-namespace"] = "mojang"
        }
        relocate("com.jeff_media.customblockdata", "com.ccorp2002.smoothsailing.libs.customblockdata")
        relocate("com.jeff_media.morepersistentdatatypes", "com.ccorp2002.smoothsailing.libs.morepdct")
        mergeServiceFiles()
    }
}