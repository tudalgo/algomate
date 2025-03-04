package org.tudalgo.algomate.configuration

import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.sourcegrade.jagr.gradle.extension.DependencyConfiguration

/**
 * Represents a dependency for a Gradle project.
 *
 * @property group the group identifier of the dependency
 * @property artifactName the artifact name of the dependency
 * @property version the version of the dependency
 */
enum class Dependency(
    val group: String,
    val artifactName: String,
    val version: String,
    val configuration: String = "implementation"
) {

    /**
     * The JetBrains annotations library.
     */
    JETBRAINS_ANNOTATIONS("org.jetbrains", "annotations", "26.0.0"),

    /**
     * The JUnit Jupiter testing framework.
     */
    JUNIT_JUPITER("org.junit.jupiter", "junit-jupiter", "5.11.2"),

    /**
     * The JUnit Pioneer extension library.
     */
    JUNIT_PIONEER("org.junit-pioneer", "junit-pioneer", "2.3.0"),

    /**
     * The AlgoUtils library for student use.
     */
    ALGOUTILS_STUDENT("org.tudalgo", "algoutils-student", "0.9.0"),

    /**
     * The AlgoUtils library for tutors or graders.
     */
    ALGOUTILS_TUTOR("org.tudalgo", "algoutils-tutor", "0.9.0");

    /**
     * Provides the dependency notation in the format `group:artifactName:version`.
     *
     * This notation can be directly used in Gradle dependency declarations.
     *
     * @return the dependency notation string.
     */
    val notation: String
        get() = "$group:$artifactName:$version"
}

/**
 * Adds the dependencies to `implementation` configuration in the [DependencyConfiguration].
 *
 * @receiver the [DependencyConfiguration] instance to which the dependency is added
 * @param dependencies the dependencies to add
 */
fun DependencyConfiguration.dependencies(vararg dependencies: Dependency) = dependencies.forEach {
    implementation(it.notation)
}

/**
 * Adds the dependency to `implementation` configuration in the [DependencyHandlerScope].
 * @receiver the [DependencyHandlerScope] to which the dependencies are added
 * @param dependency the dependency to add
 */
fun DependencyHandlerScope.implementation(dependency: Dependency) = "implementation"(dependency.notation)

/**
 * Adds a dependency to the `testImplementation` configuration in the [DependencyHandlerScope].
 *
 * @receiver the [DependencyHandlerScope] to which the dependency is added
 * @param dependency the [Dependency] to add
 */
fun DependencyHandlerScope.testImplementation(dependency: Dependency) {
    "testImplementation"(dependency.notation)
}
