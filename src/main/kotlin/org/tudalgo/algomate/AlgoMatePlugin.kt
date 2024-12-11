package org.tudalgo.algomate

import org.gradle.api.GradleException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Property
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.TaskContainerScope
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.sourcegrade.jagr.gradle.extension.GraderConfiguration
import org.sourcegrade.jagr.gradle.extension.JagrExtension
import org.sourcegrade.jagr.gradle.task.submission.SubmissionWriteInfoTask
import org.tudalgo.algomate.configuration.Dependency
import org.tudalgo.algomate.configuration.GitRepository
import org.tudalgo.algomate.configuration.addDependency
import org.tudalgo.algomate.configuration.addPlugin
import org.tudalgo.algomate.configuration.implementation
import org.tudalgo.algomate.extension.ExerciseExtension
import org.tudalgo.algomate.extension.SubmissionExtension

/**
 * The Java version used for the project.
 */
const val JAVA_VERSION = 21

/**
 * The course name for the project.
 */
const val COURSE_NAME = "FOP-2425"

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
        // Apply plugins
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

        // Main class is always set to <assignmentId>.Main class
        target.extensions.getByType<JavaApplication>().apply {
            mainClass.set(exerciseExtension.assignmentId.map { "$it.Main" })
        }

        // Jagr configuration
        target.extensions.getByType<JagrExtension>().apply {
            // Submission metadata
            assignmentId.set(exerciseExtension.assignmentId)
            submissions {
                create("main") { main ->
                    main.studentId.set(submissionExtension.studentIdProperty)
                    main.firstName.set(submissionExtension.firstNameProperty)
                    main.lastName.set(submissionExtension.lastNameProperty)
                }
            }

            // Student available dependencies
            target.dependencies {
                implementation(Dependency.JUNIT_JUPITER)
                implementation(Dependency.ALGOUTILS_STUDENT)
                implementation(Dependency.JETBRAINS_ANNOTATIONS)
            }

            // Grader configurations
            graders {
                val projectName = target.name
                val repository = GitRepository.entries.first { projectName.endsWith(it.type) }
                val assignmentNr = projectName.filter { it.isDigit() }.toInt()
                val graderPublic: GraderConfiguration? = if (repository.containsPublicTests(assignmentNr)) {
                    createGrader(false, exerciseExtension.assignmentId)
                } else {
                    null
                }
                if (repository.privateTests) {
                    createGrader(true, exerciseExtension.assignmentId, graderPublic)
                }
            }
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

            // Supported a Java version for the project
            withType<JavaCompile> {
                options.encoding = "UTF-8"
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

    /**
     * Creates a grader configuration for the given path and type.
     * If the grader source set does not exist, the configuration is not created which will be indicated by a `null`
     * return value.
     *
     * @param privateTest whether the grader is for private tests
     * @param id the exercise id (assignment number)
     * @param parent the parent grader configuration
     */
    fun NamedDomainObjectContainer<GraderConfiguration>.createGrader(
        privateTest: Boolean,
        id: Property<String>,
        parent: GraderConfiguration? = null
    ): GraderConfiguration? {
        val type = if (privateTest) {
            "Private"
        } else {
            "Public"
        }
        return create("grader$type") { grader ->
            if (parent == null) {
                grader.configureDependencies {
                    addDependency(Dependency.JUNIT_PIONEER)
                    addDependency(Dependency.ALGOUTILS_TUTOR)
                }
            } else {
                grader.parent(parent)
            }

            val idU = id.map { it.uppercase() }
            val name = type.replaceFirstChar { it.uppercase() }
            grader.graderName.set(idU.map { "$COURSE_NAME-$it-$name" })
            grader.rubricProviderName.set(id.zip(idU) { a, b -> "$a.${b}_RubricProvider$name" })
            grader.configureDependencies {
                addDependency(Dependency.JUNIT_PIONEER)
                addDependency(Dependency.ALGOUTILS_TUTOR)
            }
        }
    }
}
