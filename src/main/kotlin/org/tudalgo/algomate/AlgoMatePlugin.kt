package org.tudalgo.algomate

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.*
import org.sourcegrade.jagr.gradle.extension.GraderConfiguration
import org.sourcegrade.jagr.gradle.extension.JagrExtension
import org.sourcegrade.jagr.gradle.task.submission.SubmissionWriteInfoTask
import org.tudalgo.algomate.configuration.Dependency
import org.tudalgo.algomate.configuration.addPlugin
import org.tudalgo.algomate.configuration.dependencies
import org.tudalgo.algomate.configuration.implementation
import org.tudalgo.algomate.extension.ExerciseExtension
import org.tudalgo.algomate.extension.SubmissionExtension
import org.tudalgo.algomate.task.StudentRepositoryConverterTask

/**
 * The Java version used for the project.
 */
const val JAVA_VERSION = 21

/**
 * The encoding used for the project.
 */
const val ENCODING = "UTF-8"

/**
 * The course name.
 */
const val COURSE_NAME = "FOP"

/**
 * The course year.
 */
const val COURSE_YEAR = "2425"

/**
 * The task number from which public tests are available.
 * This is used to determine whether the public tests should be included in the grader configuration.
 * Public tests are only available for tasks with a number greater than or equal to this value.
 */
const val PUBLIC_TEST_AVAILABLE_FROM_TASK = 10

/**
 * The `AlgoMatePlugin` is a Gradle plugin designed to simplify and streamline the setup process for student
 * assignments and exercise submissions.
 *
 * This plugin configures essential project settings, integrates with Jagr for automated grading, and enforce
 * submission requirements like testing and grader tasks.
 *
 * ### Features:
 * - Applies the `java`, `application`, and `org.sourcegrade.jagr-gradle` plugins.
 * - Provides extensions for configuring exercise and submission metadata:
 *   - `ExerciseExtension`: Configures assignment-specific details.
 *   - `SubmissionExtension`: Configures submission-specific details such as student metadata.
 * - Automatically configures dependencies required for student and grading functionalities.
 * - Validates student ID format during submission tasks.
 * - Sets up common tasks (e.g., `test`, `graderPublicRun`) with dependencies and constraints.
 *
 * ### Example Usage:
 * In the `build.gradle.kts`:
 * ```kotlin
 * // Deprecated: Assignment ID is now set automatically based on the project name.
 * exercise {
 *     assignmentId.set("myAssignment")
 * }
 * submission {
 *     studentId = "jd12abcd"
 *     firstName = "John"
 *     lastName = "Doe"
 *     requireTests = true
 *     requireGraderPublic = true
 * }
 * // No longer needed. Only needed if you want to override the default Jagr configuration.
 * jagr {
 *     graders {
 *         val graderPublic by getting {
 *             ...
 *         }
 *         ...
 *     }
 * }
 * ```
 *
 * @see ExerciseExtension
 * @see SubmissionExtension
 * @see JagrExtension
 */
@Suppress("unused")
class AlgoMatePlugin : Plugin<Project> {


    override fun apply(target: Project) {
        // apply plugins
        org.tudalgo.algomate.configuration.Plugin.entries.forEach { target.addPlugin(it) }

        // Create and configure extensions
        val exerciseExtension = target.extensions.create<ExerciseExtension>("exercise")
        val submissionExtension = target.extensions.create<SubmissionExtension>("submission")

        // Whether running tests and graderPublic tasks is mandatory for submission
        // If true, the submission task will depend on the test and graderPublic tasks and enforce their
        // execution before submission.
        target.afterEvaluate {
            if (submissionExtension.requireTests) {
                target.tasks["mainBuildSubmission"].dependsOn("test")
            }
            if (submissionExtension.requireGraderPublic) {
                target.tasks["mainBuildSubmission"].dependsOn("graderPublicRun")
                target.tasks["graderPublicRun"].shouldRunAfter("test")
            }
        }

        // Format: HXX-Root/HXX-Student/HXX-Review
        // Task name: HXX
        val taskName = target.name.substring(0, 3)
        // Task type = Root/Student/Review
        val taskType = target.name.substring(4)
        val assignmentId = taskName.lowercase()
        val taskNumber = taskName.substring(1).toInt()

        // Main class is always set to <assignmentId>.Main class
        target.extensions.getByType<JavaApplication>().apply {
            mainClass.set("$assignmentId.Main")
        }

        // Jagr configuration
        target.extensions.getByType<JagrExtension>().apply {
            // Submission metadata
            this.assignmentId.set(assignmentId)
            submissions {
                create("main") { main ->
                    main.studentId.set(submissionExtension.studentIdProperty)
                    main.firstName.set(submissionExtension.firstNameProperty)
                    main.lastName.set(submissionExtension.lastNameProperty)
                }
            }

            // Creating graders
            graders {
                fun createGrader(isPrivate: Boolean, parent: GraderConfiguration? = null): GraderConfiguration {
                    val graderName = "grader" + if (isPrivate) "Private" else "Public"
                    val type = if (isPrivate) "Private" else "Public"
                    return create(graderName) { grader ->
                        grader.graderName.set("$COURSE_NAME-$COURSE_YEAR-$taskName-$type")
                        grader.rubricProviderName.set("$assignmentId.${taskName}_RubricProvider$type")
                        if (parent != null) {
                            grader.parent(parent)
                        } else {
                            grader.configureDependencies {
                                dependencies(
                                    Dependency.JUNIT_PIONEER,
                                    Dependency.ALGOUTILS_TUTOR
                                )
                            }
                        }
                    }
                }
                // Public tests only available for task number >= PUBLIC_TEST_AVAILABLE_FROM_TASK
                if (taskNumber >= PUBLIC_TEST_AVAILABLE_FROM_TASK) {
                    val graderPublic = createGrader(false)
                    if (taskType == "Root") {
                        createGrader(true, graderPublic)
                    }
                } else if (taskType == "Root") {
                    createGrader(true)
                }
            }
        }

        // Student available dependencies
        target.dependencies {
            implementation(Dependency.JUNIT_JUPITER)
            implementation(Dependency.ALGOUTILS_STUDENT)
            implementation(Dependency.JETBRAINS_ANNOTATIONS)
        }

        // Tasks
        target.tasks.register<StudentRepositoryConverterTask>("toStudentRepository")

        TaskContainerScope.of(target.tasks).apply {
            val runDir = target.file("build/run")
            withType<JavaExec> {
                doFirst {
                    runDir.mkdirs()
                }
                workingDir = runDir
            }
            withType<Test> {
                doFirst {
                    runDir.mkdirs()
                }
                workingDir = runDir
                useJUnitPlatform()
            }

            // Supported a Java version for the project
            withType<JavaCompile> {
                options.encoding = ENCODING
                sourceCompatibility = JAVA_VERSION.toString()
                targetCompatibility = JAVA_VERSION.toString()
            }

            // Validation of student ID format
            withType<SubmissionWriteInfoTask> {
                doFirst {
                    if (!studentId.get().matches(".*[a-zA-Z].*".toRegex())) {
                        throw GradleException(
                            """
                            The student ID does not contain a letter.
                            Are you sure that you haven't entered your matriculation number instead of your student ID?
                            """.trimIndent()
                        )
                    } else if (!studentId.get().all { !it.isUpperCase() }) {
                        throw GradleException(
                            """
                            The student ID should not contain any uppercase letters.
                            """.trimIndent()
                        )
                    }
                }
            }
        }
    }
}
