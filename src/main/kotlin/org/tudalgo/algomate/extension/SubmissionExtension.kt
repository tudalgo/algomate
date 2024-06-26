package org.tudalgo.algomate.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

abstract class SubmissionExtension @Inject constructor(
    objectFactory: ObjectFactory,
) {
    internal val studentIdProperty = objectFactory.property<String>()
    internal val firstNameProperty = objectFactory.property<String>()
    internal val lastNameProperty = objectFactory.property<String>()

    var studentId: String?
        get() = studentIdProperty.orNull
        set(value) {
            value?.also(studentIdProperty::set)
        }
    var firstName: String?
        get() = firstNameProperty.orNull
        set(value) {
            value?.also(firstNameProperty::set)
        }
    var lastName: String?
        get() = lastNameProperty.orNull
        set(value) {
            value?.also(lastNameProperty::set)
        }

    var requireTests: Boolean = false
    var requireGraderPublic: Boolean = false
}
