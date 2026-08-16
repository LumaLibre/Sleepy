import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm") version "2.4.0"
    //id("com.gradleup.shadow") version "8.3.5"
    id("de.eldoria.plugin-yml.bukkit") version "0.9.0"
    id("xyz.jpenilla.run-paper") version "3.0.1"
}

group = "dev.lumas.sleepy"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.jsinco.dev/releases")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("org.spongepowered:configurate-yaml:4.2.0")
    compileOnly("dev.lumas.core:LumaCore:dd53fbc")
    compileOnly("dev.lumas.shops:Shops:05cf1ff")
    compileOnly("me.clip:placeholderapi:2.12.2")

    implementation("org.xerial:sqlite-jdbc:3.53.1.0")

    testImplementation(platform("org.junit:junit-bom:5.12.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.spongepowered:configurate-yaml:4.2.0")
    testRuntimeOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(25)
}

tasks {
    runServer {
        minecraftVersion("26.1.2")
    }
    test {
        useJUnitPlatform()
    }
//    build {
//        dependsOn(shadowJar)
//    }
//    shadowJar {
//        archiveClassifier.set("")
//    }
//    jar {
//        enabled = false
//    }
}

bukkit {
    name = "Sleepy"
    main = "dev.lumas.sleepy.Sleepy"
    version = project.version.toString()
    apiVersion = "26.1"
    author = "Jsinco"
    description = "Movement-aware AFK and playtime tracking for Luma."
    foliaSupported = true
    depend = listOf("LumaCore")
    softDepend = listOf("PlaceholderAPI", "Shops")
    permissions {
        register("sleepy.command.playtime") {
            description = "View playtime."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("sleepy.command.playtime.others") {
            description = "View another player's playtime."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("sleepy.command.afktime") {
            description = "View recorded AFK time."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("sleepy.command.afktime.others") {
            description = "View another player's AFK time."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("sleepy.command.afk") {
            description = "Toggle your AFK status."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("sleepy.command.points") {
            description = "Use /sleepy points to view oneira."
            default = BukkitPluginDescription.Permission.Default.TRUE
        }
        register("sleepy.command.points.others") {
            description = "View another player's oneira."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("sleepy.command.points.give") {
            description = "Give oneira."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("sleepy.command.reload") {
            description = "Reload Sleepy."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("sleepy.command.coords") {
            description = "Copy your current coordinates."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("sleepy.exempt") {
            description = "Exempts a player from automatic AFK tracking."
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}
