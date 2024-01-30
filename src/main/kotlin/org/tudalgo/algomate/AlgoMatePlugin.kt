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
                    graderPublic.graderName.set(idU.map { "FOP-2324-$it-Public" })
                    graderPublic.rubricProviderName.set(id.zip(idU) { a, b -> "$a.${b}_RubricProvider" })
                    graderPublic.configureDependencies {
                        implementation("org.tudalgo:algoutils-tutor:0.7.0")
                        implementation("org.junit-pioneer:junit-pioneer:2.1.0")
                    }
                }
            }
        }

        target.dependencies {
            "implementation"("org.tudalgo:algoutils-student:0.7.0")
            "implementation"("org.jetbrains:annotations:24.0.1")
            "testImplementation"("org.junit.jupiter:junit-jupiter:5.10.0")
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
                sourceCompatibility = "17"
                targetCompatibility = "17"
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
                    }
                }
            }
        }
    }
}
