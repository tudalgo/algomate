package org.tudalgo.algomate.extension

import org.gradle.api.provider.Property

abstract class ExerciseExtension {
    abstract val assignmentId: Property<String>
}
