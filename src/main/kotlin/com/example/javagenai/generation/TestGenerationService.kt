package com.example.javagenai.generation

import com.example.javagenai.analysis.PatternAnalyzerService
import com.example.javagenai.context.JavaContextCollector
import com.example.javagenai.execution.TestExecutionService
import com.example.javagenai.prompt.PromptRequest
import com.example.javagenai.settings.PluginSettingsState
import com.example.javagenai.ui.model.IterationStatus
import com.example.javagenai.ui.model.ToolWindowStateService
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore

data class GenerationReport(
    val success: Boolean,
    val summary: String,
    val targetTestPath: String,
    val applied: Boolean
)

@Service(Service.Level.PROJECT)
class TestGenerationService(private val project: Project) {
    private val analyzer = project.getService(PatternAnalyzerService::class.java)
    private val collector = project.getService(JavaContextCollector::class.java)
    private val llm = project.getService(LlmGenerationCoordinator::class.java)
    private val executor = project.getService(TestExecutionService::class.java)
    private val uiState = project.getService(ToolWindowStateService::class.java)
    private val templateStrategy = TestTemplateStrategy()
    private val settings = PluginSettingsState.getInstance()

    fun generateAndRepairFor(sourceFile: VirtualFile): GenerationReport {
        uiState.reset()
        val profile = analyzer.analyzeProjectPatterns()
        val sourceText = VfsUtilCore.loadText(sourceFile)
        val related = collector.collectRelatedJavaFiles(sourceFile)
        val relatedTests = related.filter { it.name.endsWith("Test.java") || it.name.endsWith("IT.java") }.take(3)
        val basePath = project.basePath ?: return GenerationReport(false, "No project base path", "", false)
        val testPath = templateStrategy.targetTestPath(basePath, sourceFile, profile)

        val initialPrompt = PromptRequest(
            systemPrompt = buildSystemPrompt(profile),
            userPrompt = buildUserPrompt(sourceFile.path, sourceText, relatedTests),
            temperature = settings.state.temperature,
            maxTokens = settings.state.maxTokens
        )

        val initialOutput = llm.generate(initialPrompt)
        val testCode = extractJavaCode(initialOutput)
        val applier = GeneratedPatchApplier(project)
        var plan = applier.buildPlan(testPath, testCode)

        uiState.update {
            it.copy(
                generatedFiles = listOf(testPath),
                iterations = listOf(IterationStatus(0, "Generated")),
                diffPreview = plan.diffPreview,
                summary = "Generated candidate test file"
            )
        }

        if (!settings.state.autoApplyGeneratedTests) {
            return GenerationReport(false, "Preview ready. Enable auto-apply to run repair loop.", testPath, false)
        }

        applier.apply(plan)
        val testClass = fqcnFromPath(testPath)
        val retryBudget = settings.state.repairRetryBudget

        for (iteration in 1..retryBudget) {
            val execution = executor.runTestClass(testClass)
            if (execution.passed) {
                uiState.update {
                    it.copy(
                        iterations = it.iterations + IterationStatus(iteration, "Passed"),
                        summary = "Tests passed after $iteration iteration(s)",
                        errors = emptyList()
                    )
                }
                return GenerationReport(true, "Generated and validated tests successfully.", testPath, true)
            }

            uiState.update {
                it.copy(
                    iterations = it.iterations + IterationStatus(iteration, "Failed"),
                    errors = execution.errors,
                    summary = "Repairing failures from iteration $iteration"
                )
            }

            val fixPrompt = PromptRequest(
                systemPrompt = buildSystemPrompt(profile),
                userPrompt = buildRepairPrompt(sourceText, plan.newText, execution.errors),
                temperature = settings.state.temperature,
                maxTokens = settings.state.maxTokens
            )
            val repaired = extractJavaCode(llm.generate(fixPrompt))
            plan = applier.buildPlan(testPath, repaired)
            applier.apply(plan)
            uiState.update { it.copy(diffPreview = plan.diffPreview) }
        }

        return GenerationReport(false, "Retry budget exhausted. Check tool window for failure details.", testPath, true)
    }

    private fun buildSystemPrompt(profile: com.example.javagenai.analysis.ProjectPatternProfile): String = """
        You generate high-value Java unit tests only.
        Match repository conventions exactly.
        JUnit=${profile.junitVersion}
        Mocking libraries=${profile.mockingLibraries.joinToString()}
        Assertion libraries=${profile.assertionLibraries.joinToString()}
        Naming=${profile.testNamePatterns.joinToString()}
        Do not introduce dependencies not already used.
        Keep tests deterministic and meaningful.
    """.trimIndent()

    private fun buildUserPrompt(sourcePath: String, sourceText: String, representativeTests: List<VirtualFile>): String {
        val sampleTests = representativeTests.joinToString("\n\n") { file ->
            "File: ${file.path}\n${VfsUtilCore.loadText(file)}"
        }
        return """
            Target class path: $sourcePath
            Target class source:
            $sourceText

            Representative existing tests:
            $sampleTests

            Requirements:
            - mimic existing naming/import/annotation/assertion/mocking style
            - avoid introducing new dependencies
            - respect legacy Java style if source indicates older syntax
            - prioritize behavior assertions and edge cases over superficial line hits
            - propose minimal changes only
            - if uncertain about behavior, encode assumptions in test names/assertions

            Return only Java source for one test class.
        """.trimIndent()
    }

    private fun buildRepairPrompt(sourceText: String, currentTestText: String, errors: List<String>): String = """
        Repair the test with minimal changes and preserve existing assertions unless incorrect.
        Source under test:
        $sourceText

        Current generated test:
        $currentTestText

        Failures:
        ${errors.joinToString("\n")}

        Return only corrected Java source.
    """.trimIndent()

    private fun extractJavaCode(modelOutput: String): String {
        val marker = "```"
        if (!modelOutput.contains(marker)) return modelOutput.trim()
        val start = modelOutput.indexOf(marker)
        val end = modelOutput.lastIndexOf(marker)
        if (end <= start) return modelOutput.trim()
        val block = modelOutput.substring(start + 3, end)
        return block.removePrefix("java").trim()
    }

    private fun fqcnFromPath(path: String): String {
        val normalized = path.substringAfter("/src/test/java/").removeSuffix(".java")
        return normalized.replace('/', '.')
    }
}
