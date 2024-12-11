package org.tudalgo.algomate.configuration

/**
 * Defines the different type of Git repositories that an exercise can have.
 *
 * @property type the type of the Git repository
 * @property publicTestsThreshold the exercise number from which on public tests is available
 * @property privateTests whether the repository contains private tests
 */
enum class GitRepository(val type: String, val publicTestsThreshold: Int, val privateTests: Boolean) {
    /**
     * A student repository which contains only public tests after the exercise number 9 and do not contain
     * private tests.
     */
    STUDENT("Student", 9, false),

    /**
     * A review repository which is a student repository which only be used for reviewing.
     */
    REVIEW("Review", STUDENT),

    /**
     * The root repository which contains all tests and is used for grading.
     */
    ROOT("Root", 9, true)
    ;

    /**
     * Creates a copy of the given [parent] Git repository with the given [type].
     */
    constructor(type: String, parent: GitRepository) : this(type, parent.publicTestsThreshold, parent.privateTests)

    /**
     * Checks whether the given [exercise] contains public tests.
     */
    fun containsPublicTests(exercise: Int): Boolean = exercise >= publicTestsThreshold
}

fun main() {
    val s = "H09-Student"
    val int = s.filter { it.isDigit() }.toInt()
    println(GitRepository.STUDENT.containsPublicTests(int))
}
