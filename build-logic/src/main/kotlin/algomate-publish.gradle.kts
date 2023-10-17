import java.net.URI

plugins {
    java
    `maven-publish`
}

extensions.configure<JavaPluginExtension> {
    withJavadocJar()
    withSourcesJar()
}

extensions.configure<PublishingExtension> {
    repositories {
        maven {
            credentials {
                username = project.findProperty("sonatypeUsername") as? String
                password = project.findProperty("sonatypePassword") as? String
            }
            // only publish snapshots
            url = URI("https://s01.oss.sonatype.org/content/repositories/snapshots")
        }
    }
    publications.register<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name.set("algomate")
            description.set("Gradle plugin for TU Darmstadt Algorithmik assignments")
            url.set("https://wiki.tudalgo.org")
        }
    }
}
