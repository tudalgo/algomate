import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.sourcegrade.jagr.script.AlgoMatePublishPlugin

@Suppress("DSL_SCOPE_VIOLATION") // https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    `java-gradle-plugin`
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.style)
}

apply<AlgoMatePublishPlugin>()

group = "org.tudalgo"
version = file("version").readLines().first()

dependencies {
    implementation(gradleKotlinDsl())
    implementation("org.sourcegrade:jagr-launcher-gradle-plugin:${libs.versions.jagr.get()}")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<PublishToMavenRepository> {
        onlyIf { project.version.toString().endsWith("-SNAPSHOT") }
    }
}

gradlePlugin {
    plugins {
        create("algomate") {
            id = "org.tudalgo.algomate"
            displayName = "AlgoMate"
            description = "Gradle plugin for TU Darmstadt Algorithmik assignments"
            implementationClass = "org.tudalgo.algomate.AlgoMatePlugin"
        }
    }
}

pluginBundle {
    website = "https://wiki.tudalgo.org"
    vcsUrl = "https://github.com/tudalgo/algomate"
    tags = listOf("jagr", "assignment", "submission", "grading")
}
