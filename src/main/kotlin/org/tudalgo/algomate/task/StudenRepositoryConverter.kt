package org.tudalgo.algomate.task

import org.tudalgo.algoutils.student.annotation.SolutionOnly
import org.tudalgo.algoutils.student.annotation.StudentImplementationRequired
import spoon.Launcher
import spoon.reflect.code.CtComment
import spoon.reflect.code.CtStatement
import spoon.reflect.declaration.CtClass
import spoon.reflect.declaration.CtElement
import spoon.reflect.declaration.CtMethod
import spoon.reflect.declaration.CtType
import spoon.reflect.visitor.ImportScannerImpl
import spoon.support.reflect.code.CtCommentImpl
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.*


/**
 * The regex pattern to find the root package name in the file path.
 */
private val ROOT_PACKAGE_NAME_REGEX = Regex("h\\d{2}")

/**
 * A class that converts the student repository to the original 'student perspective' state.
 */
internal class StudenRepositoryConverter(root: Path) {

    /**
     * The root directory of the repository.
     */
    val src: Path = root.resolve("src/main/java")

    /**
     * A map of all classes in the repository, where the key is the qualified class name and the value is the path
     * to the class file.
     */
    @OptIn(ExperimentalPathApi::class)
    val classes: Map<String, Path> by lazy {
        src.walk().filter { it.isRegularFile() }
            .filter { it.extension == "java" }
            .associateBy { it.qualifiedClassName }
    }

    /**
     * A map of all classes in the repository that can be modified, where the key is the qualified class name and
     * the value is the class object that can be modified.
     */
    private val modifiableClasses: MutableMap<String, CtType<*>> by lazy {
        check(src.exists()) {
            "Source directory does not exist: $src"
        }
        val launcher = Launcher()
        launcher.addInputResource(src.toString())
        val model = launcher.buildModel()
        model.filterChildren<CtType<*>> { it.isClass || it.isInterface || it.isEnum }
            .list<CtType<*>>()
            .associateBy { it.qualifiedName }
            .toMutableMap()
    }

    /**
     * Converts a file path to a qualified class name.
     */
    private val Path.qualifiedClassName: String
        get() {
            val segments = this.toString().split(File.separatorChar).toMutableList()
            val it = segments.iterator()
            while (it.hasNext() && !ROOT_PACKAGE_NAME_REGEX.matches(it.next())) {
                it.remove()
            }
            return segments.joinToString(".").replace(".java", "")
        }


    /**
     * Converts the repository to the original 'student perspective' state.
     */
    fun convert() {
        // Retrieve the methods to be cleaned up and clean them up
        modifiableClasses.values.flatMap {
            it.methods.filter { method -> method.hasAnnotation(StudentImplementationRequired::class.java) }
        }.forEach {
            it.setBody<CtMethod<*>>(it.crashStatement())
        }

        // Remove solution code only constructs
        modifiableClasses.filter { it.value.hasAnnotation(SolutionOnly::class.java) }.forEach {
            modifiableClasses.remove(it.key)
        }

        modifiableClasses.values.forEach { clazz ->
            clazz.fields.filter { field ->
                field.hasAnnotation(SolutionOnly::class.java)
            }.forEach {
                clazz.removeField(it)
            }
        }

        modifiableClasses.values.filter { it.isClass }
            .map { it as CtClass<*> }
            .forEach { clazz ->
                clazz.constructors.filter { constructor ->
                    constructor.hasAnnotation(SolutionOnly::class.java)
                }.forEach {
                    // clazz.removeConstructor(it)
                }
            }

        modifiableClasses.values.forEach { clazz ->
            clazz.methods.filter { method ->
                method.hasAnnotation(SolutionOnly::class.java)
            }.forEach {
                clazz.removeMethod(it)
            }
        }

        // Write the modified classes
        classes.forEach { (className, path) ->
            if (modifiableClasses.containsKey(className)) {
                val clazz = modifiableClasses[className]!!
                val packageName = clazz.`package`
                var code = "package $packageName;\n";
                val scanner = ImportScannerImpl()
                scanner.computeImports(clazz)
                code += scanner.allImports.joinToString("\n")
                code += "\n"
                code += clazz.prettyprint()
                Files.write(path, code.toByteArray())
            } else {
                // Removing @SolutionOnly class
                Files.delete(path)
            }
        }
    }

    /**
     * Creates an inline comment.
     */
    private fun inlineComment(content: String): CtComment = CtCommentImpl().apply {
        setCommentType<CtComment>(CtComment.CommentType.INLINE)
        setContent<CtComment>(content)
    }

    /**
     * Creates a crash statement for the given method.
     */
    private fun CtMethod<*>.crashStatement(): CtStatement {
        val annotation = getAnnotation(StudentImplementationRequired::class.java)
        val taskNumber = annotation.value
        return factory.createCodeSnippetStatement(
            if (getType().simpleName == "void") {
                ""
            } else {
                "return "
            } + "org.tudalgo.algoutils.student.Student.crash(\"$taskNumber -  Remove if implemented\")"
        ).apply {
            addComment<CtElement>(inlineComment("TODO $taskNumber"))
        }
    }
}
