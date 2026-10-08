package org.tudalgo.algomate.extension

import org.gradle.api.provider.Property

/**
 * A Gradle extension for configuring exercise-specific details.
 *
 * This extension allows defining metadata for an exercise.
 */
abstract class ExerciseExtension {

    internal abstract val courseNameProperty: Property<String>

    internal abstract val courseYearProperty: Property<String>

    /**
     * Name of the course to use in the grader name
     */
    var courseName: String
        get() = courseNameProperty.get()
        set(value) = courseNameProperty.set(value)

    /**
     * Concatenated, shorthand year to use in the grader name
     */
    var courseYear: String
        get() = courseYearProperty.get()
        set(value) = courseYearProperty.set(value)

    init {
        courseNameProperty.convention("FOP")
        courseYearProperty.convention("2627")
    }
}
