package org.tudalgo.algomate.task

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.tudalgo.algoutils.student.annotation.SolutionOnly
import org.tudalgo.algoutils.student.annotation.StudentCreationRequired
import org.tudalgo.algoutils.student.annotation.StudentImplementationRequired
import spoon.Launcher
import spoon.reflect.code.CtBodyHolder
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtType
import spoon.support.sniper.SniperJavaPrettyPrinter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString

abstract class AnnotationProcessorTask : DefaultTask() {

    /**
     * The root package of the assignment, usually the same as the assignment ID.
     */
    @get:Input
    abstract val assignmentPackage: Property<String>

    /**
     * The targeted Java version. Gets passed to Spoon as the compliance level.
     */
    @get:Input
    abstract val javaVersion: Property<Int>

    private val sourceCodeDirectory = project.projectDir.toPath()
        .resolve("src")
        .resolve("main")
        .resolve("java")

    init {
        group = "distribution"
        description = "Processes AlgoUtils annotations to create the review / student template."
        outputs.upToDateWhen { false }
    }

    @TaskAction
    fun run() {
        // Set up temp dir for output
        val tmpDir = Files.createTempDirectory("spoon_output")
        // Set up Spoon model
        val spoon = Launcher()
        spoon.environment.isAutoImports = true
        spoon.environment.complianceLevel = javaVersion.get()
        spoon.environment.noClasspath = true
        spoon.environment.setCommentEnabled(true)
        spoon.environment.ignoreSyntaxErrors = true
        spoon.environment.setPrettyPrinterCreator { SniperJavaPrettyPrinter(spoon.environment) }
        spoon.environment.sourceOutputDirectory = tmpDir.toFile()
        spoon.addInputResource(sourceCodeDirectory.resolve(assignmentPackage.get()).pathString)
        val model = spoon.buildModel()

        // Only copy modified files
        val modifiedFiles = mutableSetOf<Path>()
        val removedFiles = mutableSetOf<Path>()
        val removalAnnotations = arrayOf(StudentCreationRequired::class.java, SolutionOnly::class.java)
        val modificationAnnotations = arrayOf(StudentImplementationRequired::class.java, *removalAnnotations)
        // Scan all types in main module for annotations
        for (type in model.allTypes) {
            // Skip processing as it will be deleted anyway
            if (removalAnnotations.any { type.hasAnnotation(it) }) {
                removedFiles.add(type.getSourceFile())
                println("Removed ${type.qualifiedName}")
                continue
            }

            // Scan type for each annotation and process it
            for (annotationType in modificationAnnotations) {
                type.getAnnotatedChildren<CtElement>(annotationType)
                    .apply {
                        if (isNotEmpty() && modifiedFiles.add(type.getSourceFile()))
                            println("Modified ${type.qualifiedName}")
                    }
                    .forEach { processAnnotation(annotationType, it) }
            }
        }

        // Write modified source to tmp dir
        spoon.prettyprint()
        Files.walk(tmpDir)
            .sorted(Comparator.reverseOrder()) // Reverse so we can clean up at the same time
            .forEach { path ->
                val sourceFile = sourceCodeDirectory.resolve(tmpDir.relativize(path))
                if (path.isDirectory() || sourceFile !in modifiedFiles) {
                    // Delete if path is a directory (already exists in source root) or
                    // if source has not been modified (marked for deletion or no annotations present)
                    Files.deleteIfExists(path)
                } else {
                    // Otherwise, delete the source file and replace it
                    Files.deleteIfExists(sourceFile)
                    Files.move(path, sourceFile)
                }
            }
        // Delete source files that are marked for deletion, if they exist
        removedFiles.forEach { Files.deleteIfExists(sourceCodeDirectory.resolve(it)) }

        println("\nProcessing complete. Check the modified files and remove leftover imports.")
    }

    /**
     * Process the given [element], depending on the [annotation class][annotationClass].
     */
    private fun processAnnotation(annotationClass: Class<out Annotation>, element: CtElement) {
        when (annotationClass) {
            StudentImplementationRequired::class.java if element is CtMethod<*> -> {
                val exercise = element.getAnnotation(annotationClass).value
                val snippet = """
                    // TODO: $exercise
                    ${if (element.type.simpleName != "void") "return " else ""}org.tudalgo.algoutils.student.Student.crash("${if (exercise.isNotEmpty()) "$exercise - " else ""}Remove if implemented")
                """.trimIndent()
                element.setBody<CtBodyHolder>(element.factory.Code().createCodeSnippetStatement(snippet))
            }
            StudentCreationRequired::class.java, SolutionOnly::class.java -> element.delete()
        }
    }

    /**
     * Returns the absolute path to this type's source file.
     */
    private fun CtType<*>.getSourceFile(): Path {
        return this.qualifiedName.split('.')
            .toMutableList()
            .apply { set(lastIndex, last() + ".java") }
            .fold(sourceCodeDirectory) { acc, name -> acc.resolve(name) }
    }
}
