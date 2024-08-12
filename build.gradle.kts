import org.jetbrains.changelog.closure
import org.jetbrains.changelog.date
plugins {
    id("org.jetbrains.intellij") version "1.13.3"
    kotlin("jvm") version "2.0.10"
    id("org.jetbrains.changelog") version "0.6.2"
}

group = "ws.logv"
version = "1.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version.set("2022.2")
}


changelog {
    version = "${project.version}"
    path = "${project.projectDir}/CHANGELOG.md"
    header = closure { "[${project.version}] - ${date()}" }
    headerParserRegex = """\d+\.\d+""".toRegex()
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security")
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes.set(changelog.getUnreleased().toHTML())
    untilBuild.set("")
}


tasks.getByName<org.jetbrains.intellij.tasks.RunPluginVerifierTask>("runPluginVerifier") {
    ideVersions.set(listOf("MPS-2020.2.3", "MPS-2020.1.7", "MPS-2019.3.7", "IIC-2020.3", "ICC-2020.2.4", "ICC-2020.1.4"))
}

val jb_token: String? by project

tasks.getByName("buildSearchableOptions") {
    enabled = false
}


tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}