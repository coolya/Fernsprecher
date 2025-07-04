import org.jetbrains.changelog.Changelog
import java.time.LocalDate
plugins {
    id("org.jetbrains.intellij") version "1.17.4"
    kotlin("jvm") version "2.2.0"
    id("org.jetbrains.changelog") version "2.2.1"
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
    version.set("2023.1")
}


changelog {
    version.set("${project.version}")
    path.set("${project.projectDir}/CHANGELOG.md")
    header.set(provider { "[${project.version}] - ${LocalDate.now()}" })
    headerParserRegex.set("""\d+\.\d+""".toRegex())
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}

tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    changeNotes.set(changelog.renderItem(changelog.getUnreleased(), org.jetbrains.changelog.Changelog.OutputType.HTML))
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