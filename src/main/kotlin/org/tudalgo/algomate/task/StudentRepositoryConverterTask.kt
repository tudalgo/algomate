package org.tudalgo.algomate.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * A Gradle task that resets the repository to the original 'student perspective' state for the student to implement the
 * solution.
 *
 * @author Nhan Huynh
 */
abstract class StudentRepositoryConverterTask : DefaultTask() {

    init {
        group = "formatting"
        description =
            "Resets the repository to the original 'student perspective' state for the student to implement the solution."
    }

    /**
     * Converts the repository to the original 'student perspective' state.
     */
    @TaskAction
    fun convert() {
        StudenRepositoryConverter(project.projectDir.toPath()).convert()
    }
}
