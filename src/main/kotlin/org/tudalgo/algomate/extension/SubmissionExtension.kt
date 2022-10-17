package org.tudalgo.algomate.extension

import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.property
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.setValue
import javax.inject.Inject

abstract class SubmissionExtension @Inject constructor(
    objectFactory: ObjectFactory,
) {
    internal val assignmentIdProperty = objectFactory.property<String>()
    internal val studentIdProperty = objectFactory.property<String>()
    internal val firstNameProperty = objectFactory.property<String>()
    internal val lastNameProperty = objectFactory.property<String>()

    var assignmentId: String by assignmentIdProperty
    var studentId: String by studentIdProperty
    var firstName: String by firstNameProperty
    var lastName: String by lastNameProperty
}
