import org.jetbrains.changelog.closure
import org.jetbrains.changelog.date
plugins {
    id("org.jetbrains.intellij") version "0.6.5"
    kotlin("jvm") version "1.4.21"
    id("org.jetbrains.changelog") version "0.6.2"
}

group = "ws.logv"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.apache.logging.log4j:log4j-core:2.11.0")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2019.3"
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
    changeNotes(closure { changelog.getUnreleased().toHTML() })
    untilBuild(null)
}


tasks.getByName<org.jetbrains.intellij.tasks.RunPluginVerifierTask>("runPluginVerifier") {
    ideVersions(listOf("MPS-2020.2.3", "MPS-2020.1.7", "MPS-2019.3.7", "IIC-2020.3", "ICC-2020.2.4", "ICC-2020.1.4"))
}

val jb_token: String? by project

tasks.getByName<org.jetbrains.intellij.tasks.PublishTask>("publishPlugin") {
    token(jb_token)
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}