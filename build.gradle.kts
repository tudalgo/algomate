import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.gradle.publish)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.style)
    id("algomate-publish")
}

group = "org.tudalgo"
version = file("version").readLines().first()

dependencies {
    implementation(gradleKotlinDsl())
    implementation("org.sourcegrade:jagr-launcher-gradle-plugin:${libs.versions.jagr.get()}")
    implementation(libs.spoon)
    implementation(libs.algoutils.student)
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "21"
        targetCompatibility = "21"
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
            tags = listOf("jagr", "assignment", "submission", "grading")
        }
    }
    website = "https://wiki.tudalgo.org"
    vcsUrl = "https://github.com/tudalgo/algomate"
}
