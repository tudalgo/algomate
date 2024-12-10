package org.tudalgo.algomate.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

/**
 * A Gradle extension for configuring submission-specific details. This extension allows setting metadata about the
 * student submitting the project and defining requirements that must be met before submission.
 *
 * Example usage:
 * ```
 * submission {
 *     studentId = "jd12abcd"
 *     firstName = "John"
 *     lastName = "Doe"
 *     requireTests = true
 *     requireGraderPublic = true
 * }
 * ```
 *
 * @param objectFactory A factory for creating Gradle property objects.
 */
abstract class SubmissionExtension @Inject constructor(
    objectFactory: ObjectFactory
) {

    /**
     * Property to store the student's matriculation number.
     */
    internal val studentIdProperty = objectFactory.property<String>()

    /**
     * Property to store the student's first name.
     */
    internal val firstNameProperty = objectFactory.property<String>()

    /**
     * Property to store the student's last name.
     */
    internal val lastNameProperty = objectFactory.property<String>()

    /**
     * The matriculation number of the student.
     */
    var studentId: String?
        get() = studentIdProperty.orNull
        set(value) {
            value?.also(studentIdProperty::set)
        }

    /**
     * The first name of the student.
     */
    var firstName: String?
        get() = firstNameProperty.orNull
        set(value) {
            value?.also(firstNameProperty::set)
        }

    /**
     * The last name of the student.
     */
    var lastName: String?
        get() = lastNameProperty.orNull
        set(value) {
            value?.also(lastNameProperty::set)
        }

    /**
     * Flag indicating whether running tests is mandatory before submission.
     *
     * If `true`, the build process ensures all tests are executed and pass prior to allowing submission. This helps
     * maintain project quality standards.
     * Defaults to `false`.
     */
    var requireTests: Boolean = false

    /**
     * Flag indicating whether the `graderPublic` task must be executed before submission.
     *
     * If `true`, the build process enforces execution of the `graderPublic` task to validate the project against
     * public grading criteria. Defaults to `false`.
     */
    var requireGraderPublic: Boolean = false
}
