package com.example.javagenai.execution

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File

data class TestExecutionResult(
    val passed: Boolean,
    val command: String,
    val output: String,
    val errors: List<String>
)

@Service(Service.Level.PROJECT)
class TestExecutionService(private val project: Project) {

    fun runTestClass(fullyQualifiedClassName: String): TestExecutionResult {
        val base = project.basePath?.let { File(it) } ?: return TestExecutionResult(false, "", "", listOf("No project path"))
        val command = detectTestCommand(base, fullyQualifiedClassName)
            ?: return TestExecutionResult(false, "", "", listOf("No Maven/Gradle test runner detected"))
        return runCommand(base, command)
    }

    fun runCoverageTask(): TestExecutionResult {
        val base = project.basePath?.let { File(it) } ?: return TestExecutionResult(false, "", "", listOf("No project path"))
        val command = detectCoverageCommand(base)
            ?: return TestExecutionResult(false, "", "", listOf("No coverage command detected. Configure JaCoCo task or report plugin."))
        return runCommand(base, command)
    }

    private fun runCommand(base: File, command: List<String>): TestExecutionResult {
        val process = ProcessBuilder(command)
            .directory(base)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        val errors = parseErrors(output)
        return TestExecutionResult(
            passed = exit == 0 && errors.isEmpty(),
            command = command.joinToString(" "),
            output = output,
            errors = errors
        )
    }

    private fun detectTestCommand(base: File, testClass: String): List<String>? = when {
        File(base, "gradlew").exists() -> listOf("./gradlew", "test", "--tests", testClass)
        File(base, "mvnw").exists() -> listOf("./mvnw", "-Dtest=$testClass", "test")
        File(base, "build.gradle").exists() || File(base, "build.gradle.kts").exists() -> listOf("gradle", "test", "--tests", testClass)
        File(base, "pom.xml").exists() -> listOf("mvn", "-Dtest=$testClass", "test")
        else -> null
    }

    private fun detectCoverageCommand(base: File): List<String>? = when {
        File(base, "gradlew").exists() -> listOf("./gradlew", "test", "jacocoTestReport")
        File(base, "mvnw").exists() -> listOf("./mvnw", "test", "jacoco:report")
        File(base, "build.gradle").exists() || File(base, "build.gradle.kts").exists() -> listOf("gradle", "test", "jacocoTestReport")
        File(base, "pom.xml").exists() -> listOf("mvn", "test", "jacoco:report")
        else -> null
    }

    private fun parseErrors(output: String): List<String> {
        val lines = output.lines()
        return lines.filter {
            it.contains("error:") ||
                it.contains("AssertionError") ||
                it.contains("cannot find symbol") ||
                it.contains("NoClassDefFoundError") ||
                it.contains("package does not exist") ||
                it.contains("BUILD FAILED") ||
                it.contains("There are test failures") ||
                it.contains("Tests run:") && it.contains("Failures")
        }.take(30)
    }
}
