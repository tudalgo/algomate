package org.tudalgo.algomate.extension

import org.gradle.api.provider.Property

/**
 * A Gradle extension for configuring exercise-specific details.
 *
 * This extension allows defining metadata for an exercise.
 */
@Deprecated("The exercise assignment ID will be automatically derived from the project name.")
abstract class ExerciseExtension {

    /**
     * The unique identifier (exercise number) for the assignment.
     */
    abstract val assignmentId: Property<String>
}
