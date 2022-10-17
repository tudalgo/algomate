package org.sourcegrade.jagr.script

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import java.net.URI

class AlgoMatePublishPlugin : Plugin<Project> {
    override fun apply(target: Project) = target.afterEvaluate { configure() }
    private fun Project.configure() {
        apply<JavaBasePlugin>()
        apply<MavenPublishPlugin>()
        apply<SigningPlugin>()
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
            publications {
                val mainPublication = create<MavenPublication>("maven") {
                    from(components["java"])
                    pom {
                        name.set("algomate")
                        description.set("Gradle plugin for TU Darmstadt Algorithmik assignments")
                        url.set("https://wiki.tudalgo.org")
                    }
                }
                extensions.configure<SigningExtension> {
                    sign(mainPublication)
                }
            }
        }
    }
}
