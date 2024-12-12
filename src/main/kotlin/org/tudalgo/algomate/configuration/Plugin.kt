package org.tudalgo.algomate.configuration

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * Represents a Gradle plugin.
 *
 * @property id the plugin identifier
 */
enum class Plugin(
    val id: String
) {

    /**
     * The Java plugin.
     */
    JAVA("java"),

    /**
     * The Java application plugin.
     */
    APPLICATION("application"),

    /**
     * The Jagr plugin.
     */
    JAGR("org.sourcegrade.jagr-gradle")
}

/**
 * Applies a plugin to the project.
 */
fun Project.addPlugin(plugin: Plugin) {
    apply(plugin = plugin.id)
}
