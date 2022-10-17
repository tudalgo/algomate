package org.tudalgo.algomate

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
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.sourcegrade.jagr.gradle.extension.JagrExtension
import org.tudalgo.algomate.extension.SubmissionExtension

@Suppress("unused")
class AlgoMatePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // apply plugins
        target.apply(plugin = "java")
        target.apply(plugin = "application")
        target.apply(plugin = "org.sourcegrade.jagr-gradle")

        // create and configure extensions
        val submissionExtension = target.extensions.create<SubmissionExtension>("submission")

        target.extensions.getByType<JavaApplication>().apply {
            mainClass.set(submissionExtension.assignmentIdProperty.map { "$it.Main" })
        }

        target.extensions.getByType<JagrExtension>().apply {
            assignmentId.set(submissionExtension.assignmentIdProperty)
            submissions {
                create("main") { main ->
                    main.studentId.set(submissionExtension.studentIdProperty)
                    main.firstName.set(submissionExtension.firstNameProperty)
                    main.lastName.set(submissionExtension.lastNameProperty)
                }
            }
            graders {
                create("graderPublic") { graderPublic ->
                    val id = submissionExtension.assignmentIdProperty
                    val idU = id.map { it.uppercase() }
                    graderPublic.graderName.set(idU.map { "FOP-2223-$it-Public" })
                    graderPublic.rubricProviderName.set(id.zip(idU) { a, b -> "$a.${b}_RubricProvider" })
                    graderPublic.configureDependencies {
                        implementation("org.tudalgo:algoutils-tutor:0.3.1")
                    }
                }
            }
        }

        target.dependencies {
            "implementation"("org.tudalgo:algoutils-student:0.3.1")
            "implementation"("org.jetbrains:annotations:23.0.0")
            "testImplementation"("org.junit.jupiter:junit-jupiter:5.9.1")
        }

        TaskContainerScope.of(target.tasks).apply {
            val runDir = target.file("build/run")
            named<JavaExec>("run") {
                doFirst {
                    runDir.mkdirs()
                }
                workingDir = runDir
            }
            named<Test>("test") {
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
        }
    }
}
