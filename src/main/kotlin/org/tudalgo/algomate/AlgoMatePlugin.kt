package org.tudalgo.algomate

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.TaskContainerScope
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.sourcegrade.jagr.gradle.extension.JagrExtension
import org.sourcegrade.jagr.gradle.task.submission.SubmissionWriteInfoTask
import org.tudalgo.algomate.extension.ExerciseExtension
import org.tudalgo.algomate.extension.SubmissionExtension

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
        target.apply(plugin = "java")
        target.apply(plugin = "application")
        target.apply(plugin = "org.sourcegrade.jagr-gradle")

        // create and configure extensions
        val exerciseExtension = target.extensions.create<ExerciseExtension>("exercise")
        val submissionExtension = target.extensions.create<SubmissionExtension>("submission")

        target.afterEvaluate {
            if (submissionExtension.requireTests) {
                target.tasks["mainBuildSubmission"].dependsOn("test")
            }
            if (submissionExtension.requireGraderPublic) {
                target.tasks["mainBuildSubmission"].dependsOn("graderPublicRun")
                target.tasks["graderPublicRun"].shouldRunAfter("test")
            }
        }

        target.extensions.getByType<JavaApplication>().apply {
            mainClass.set(exerciseExtension.assignmentId.map { "$it.Main" })
        }

        target.extensions.getByType<JagrExtension>().apply {
            assignmentId.set(exerciseExtension.assignmentId)
            submissions {
                create("main") { main ->
                    main.studentId.set(submissionExtension.studentIdProperty)
                    main.firstName.set(submissionExtension.firstNameProperty)
                    main.lastName.set(submissionExtension.lastNameProperty)
                }
            }
            graders {
                create("graderPublic") { graderPublic ->
                    val id = exerciseExtension.assignmentId
                    val idU = id.map { it.uppercase() }
                    graderPublic.graderName.set(idU.map { "FOP-2425-$it-Public" })
                    graderPublic.rubricProviderName.set(id.zip(idU) { a, b -> "$a.${b}_RubricProvider" })
                    graderPublic.configureDependencies {
                        implementation("org.tudalgo:algoutils-tutor:0.9.0")
                        implementation("org.junit-pioneer:junit-pioneer:2.3.0")
                    }
                }
            }
        }

        target.dependencies {
            "implementation"("org.tudalgo:algoutils-student:0.9.0")
            "implementation"("org.jetbrains:annotations:26.0.0")
            "testImplementation"("org.junit.jupiter:junit-jupiter:5.11.2")
        }

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
            withType<JavaCompile> {
                options.encoding = "UTF-8"
                sourceCompatibility = "21"
                targetCompatibility = "21"
            }
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
