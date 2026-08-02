import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path

plugins {
    java
    id("net.neoforged.moddev") version "2.0.141"
    id("me.modmuss50.mod-publish-plugin") version "1.1.0"
}

group = property("mod_group_id")!!
version = property("mod_version")!!

base {
    archivesName.set(property("mod_id").toString())
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

val enableMekanism = findProperty("enable_mekanism")?.toString()?.toBoolean() == true

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://maven.latvian.dev/releases") {
        content {
            includeGroup("dev.latvian.mods")
            includeGroup("dev.latvian.apps")
        }
    }
    maven("https://maven.blamejared.com") {
        content {
            includeGroup("mezz.jei")
        }
    }
    maven("https://jitpack.io") {
        content {
            includeGroup("com.github.rtyley")
        }
    }
    maven("https://modmaven.dev/") {
        content {
            includeGroup("mekanism")
        }
    }
    maven("https://maven.ftb.dev/releases") {
        content {
            includeGroup("dev.ftb.mods")
        }
    }
}

neoForge {
    version = property("neo_version").toString()

    mods {
        create(property("mod_id").toString()) {
            sourceSet(sourceSets.main.get())
        }
    }

    runs {
        create("client") {
            client()
            programArguments.addAll("--username", "Alice")
        }
        create("clientBob") {
            client()
            programArguments.addAll("--username", "Bob")
            gameDirectory = file("run-bob")
        }
        create("server") {
            server()
        }
    }
}

sourceSets {
    named("main") {
        java {
            if (!enableMekanism) {
                exclude("**/compat/mekanism/**")
                exclude("**/mixin/compat/mekanism/**")
            }
        }
    }
}

/**
 * Symlink each run directory's `kubejs/` to the canonical `kubejs/` at the project root
 * so KubeJS scripts (server_scripts, startup_scripts, client_scripts, config, assets, data)
 * stay in sync between the dedicated server, Alice's client, and Bob's client.
 */
val linkKubejs by tasks.registering {
    group = "chapters dev"
    description = "Symlink run/kubejs and run-bob/kubejs to the shared project-root kubejs/ folder."

    val rootKubejs = layout.projectDirectory.dir("kubejs").asFile.toPath()
    val targets = listOf(
        layout.projectDirectory.dir("run").asFile.toPath().resolve("kubejs"),
        layout.projectDirectory.dir("run-bob").asFile.toPath().resolve("kubejs"),
    )

    doLast {
        Files.createDirectories(rootKubejs)
        val rootAbs = rootKubejs.toAbsolutePath().normalize()
        for (link in targets) {
            Files.createDirectories(link.parent)
            val expectedRelative: Path = link.parent.relativize(rootKubejs)

            // Windows + WSL-copied reparse points: exists()/isSymbolicLink() can disagree; always clear first if needed.
            val alreadyOk = try {
                Files.isSymbolicLink(link) && Files.readSymbolicLink(link) == expectedRelative
            } catch (_: Exception) {
                false
            }
            if (alreadyOk) {
                continue
            }

            fun removeLinkOrDir(path: Path) {
                if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isSymbolicLink(path)) {
                    return
                }
                if (Files.isDirectory(path) && !Files.isSymbolicLink(path)) {
                    path.toFile().deleteRecursively()
                } else {
                    Files.deleteIfExists(path)
                }
            }

            try {
                removeLinkOrDir(link)
            } catch (e: Exception) {
                logger.warn("Could not clear $link before linking: ${e.message}")
            }

            var linked = false
            try {
                Files.createSymbolicLink(link, expectedRelative)
                logger.lifecycle("Linked $link -> $expectedRelative")
                linked = true
            } catch (e: Exception) {
                // Windows often blocks symlinks without Developer Mode; junctions work without elevation.
                if (System.getProperty("os.name").lowercase().contains("windows")) {
                    try {
                        removeLinkOrDir(link)
                        val code = ProcessBuilder("cmd", "/c", "mklink", "/J", link.toAbsolutePath().toString(), rootAbs.toString())
                            .redirectErrorStream(true)
                            .start()
                            .also { it.inputStream.bufferedReader().use { r -> r.lines().forEach { line -> logger.lifecycle(line) } } }
                            .waitFor()
                        if (code == 0) {
                            logger.lifecycle("Junction $link -> $rootAbs")
                            linked = true
                        }
                    } catch (junctionError: Exception) {
                        logger.warn("Junction fallback failed for $link: ${junctionError.message}")
                    }
                }
                if (!linked) {
                    logger.warn("Could not link $link (${e.message}). Ensure $link points at project kubejs/.")
                }
            }
        }
    }
}

tasks.matching { it.name == "runClient" || it.name == "runClientBob" || it.name == "runServer" }
    .configureEach { dependsOn(linkKubejs) }

dependencies {
    compileOnly("dev.latvian.mods:kubejs-neoforge:${property("kubejs_version")}")
    runtimeOnly("dev.latvian.mods:kubejs-neoforge:${property("kubejs_version")}")
    runtimeOnly("dev.latvian.mods:rhino:${property("rhino_version")}")
    // KubeJS 26.1.2-8.0.4 embeds better-advanced-tooltips build.8 (broken ItemStack mixin on 26.1.2).
    // JarJar build.9+ so NeoForge selects the fixed jar over KubeJS's embedded copy for published + dev runs.
    val batVersion = property("better_advanced_tooltips_version").toString()
    jarJar(implementation("dev.latvian.mods:better-advanced-tooltips") {
        version {
            strictly("[2601.1.0-build.9,)")
            prefer(batVersion)
        }
    })
    val minecraftVersion = property("minecraft_version").toString()
    val jeiVersion = property("jei_version").toString()
    compileOnly("mezz.jei:jei-$minecraftVersion-neoforge:$jeiVersion")
    runtimeOnly("mezz.jei:jei-$minecraftVersion-neoforge:$jeiVersion")

    if (enableMekanism) {
        val mekanismVersion = property("mekanism_version").toString()
        compileOnly("mekanism:Mekanism:$mekanismVersion:api")
    }

    val ftbLibraryVersion = property("ftb_library_version").toString()
    val ftbTeamsVersion = property("ftb_teams_version").toString()
    val ftbQuestsVersion = property("ftb_quests_version").toString()
    compileOnly("dev.ftb.mods:ftb-library-neoforge:$ftbLibraryVersion")
    compileOnly("dev.ftb.mods:ftb-teams-neoforge:$ftbTeamsVersion")
    runtimeOnly("dev.ftb.mods:ftb-library-neoforge:$ftbLibraryVersion")
    runtimeOnly("dev.ftb.mods:ftb-teams-neoforge:$ftbTeamsVersion")
    runtimeOnly("dev.ftb.mods:ftb-quests-neoforge:$ftbQuestsVersion")
}

tasks.withType<ProcessResources>().configureEach {
    val properties = mapOf(
        "minecraft_version" to project.property("minecraft_version"),
        "neo_version" to project.property("neo_version"),
        "mod_id" to project.property("mod_id"),
        "mod_name" to project.property("mod_name"),
        "mod_version" to project.property("mod_version"),
        "mod_authors" to project.property("mod_authors"),
        "mod_description" to project.property("mod_description")
    )

    inputs.properties(properties)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(properties)
    }
}

publishMods {
    file = tasks.jar.flatMap { it.archiveFile }
    type = STABLE
    modLoaders.add("neoforge")
    displayName = "${property("mod_name")} ${property("mod_version")}"

    val changelogFile = rootProject.file("CHANGELOG.md")
    changelog = if (changelogFile.exists()) {
        changelogFile.readText()
    } else {
        "Release ${property("mod_version")}"
    }

    val modrinthProjectId = findProperty("modrinth_project_id")?.toString()?.takeIf { it.isNotBlank() }
    if (modrinthProjectId != null) {
        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            projectId = modrinthProjectId
            minecraftVersions.add(property("minecraft_version").toString())
            optional { slug = "jei" }
            optional { slug = "kubejs" }
            optional { slug = "mekanism" }
            optional { slug = "ftb-library" }
            optional { slug = "ftb-teams" }
            optional { slug = "ftb-quests" }
        }
    }

    val curseforgeProjectId = findProperty("curseforge_project_id")?.toString()?.takeIf { it.isNotBlank() }
    if (curseforgeProjectId != null) {
        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = curseforgeProjectId
            minecraftVersions.add(property("minecraft_version").toString())
            optional { slug = "jei" }
            optional { slug = "kubejs" }
            optional { slug = "mekanism" }
            optional { slug = "ftb-library-forge" }
            optional { slug = "ftb-teams-forge" }
            optional { slug = "ftb-quests-forge" }
        }
    }
}
