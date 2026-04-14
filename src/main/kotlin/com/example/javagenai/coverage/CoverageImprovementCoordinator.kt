package com.example.javagenai.coverage

import com.example.javagenai.analysis.PatternAnalyzerService
import com.example.javagenai.generation.GeneratedPatchApplier
import com.example.javagenai.generation.LlmGenerationCoordinator
import com.example.javagenai.generation.TestTemplateStrategy
import com.example.javagenai.prompt.PromptRequest
import com.example.javagenai.settings.PluginSettingsState
import com.example.javagenai.ui.model.IterationStatus
import com.example.javagenai.ui.model.ToolWindowStateService
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class CoverageImprovementCoordinator(private val project: Project) {
    private val coverageService = project.getService(CoverageService::class.java)
    private val uncoveredLocator = UncoveredCodeLocator()
    private val analyzer = project.getService(PatternAnalyzerService::class.java)
    private val llm = project.getService(LlmGenerationCoordinator::class.java)
    private val uiState = project.getService(ToolWindowStateService::class.java)
    private val settings = PluginSettingsState.getInstance()

    fun improveCoverageFor(sourceFile: VirtualFile): String {
        uiState.reset()
        val before = coverageService.runAndCollectCoverage()
            ?: return "Coverage report not found. Configure JaCoCo (Gradle/Maven) and rerun."

        val filter = sourceFile.nameWithoutExtension
        val reportFile = coverageService.findCoverageReport() ?: return "Coverage report path missing."
        val uncovered = uncoveredLocator.findUncovered(reportFile, filter).take(30)

        if (uncovered.isEmpty()) {
            return "No uncovered lines found for ${sourceFile.name}; coverage may already be high or report lacks class mapping."
        }

        val profile = analyzer.analyzeProjectPatterns()
        val sourceText = VfsUtilCore.loadText(sourceFile)
        val template = TestTemplateStrategy()
        val testPath = template.targetTestPath(project.basePath.orEmpty(), sourceFile, profile)
        val patcher = GeneratedPatchApplier(project)

        uiState.update {
            it.copy(
                generatedFiles = listOf(testPath),
                iterations = listOf(IterationStatus(0, "Coverage baseline captured")),
                summary = "Coverage baseline captured",
                coverageSummary = "Before: lines ${"%.2f".format(before.lineCoveragePct)}%, branches ${"%.2f".format(before.branchCoveragePct)}%"
            )
        }

        val prompt = PromptRequest(
            systemPrompt = """
                Improve meaningful test coverage for Java code.
                Respect repository style and dependencies.
                JUnit=${profile.junitVersion}; mocking=${profile.mockingLibraries.joinToString()}; assertions=${profile.assertionLibraries.joinToString()}.
                Prioritize branch coverage, edge cases, exceptions, and behavior assertions.
                Avoid trivial tests and avoid introducing new dependencies.
                Explain uncertainty with TODO comments only when unavoidable.
                Output only Java source for the updated test class.
            """.trimIndent(),
            userPrompt = """
                Target source file: ${sourceFile.path}
                Source:
                $sourceText

                Uncovered locations:
                ${uncovered.joinToString("\n") { "${it.className}:${it.lineNumber} (mi=${it.instructionMissed}, mb=${it.branchMissed})" }}

                Generate minimal, behavior-validating additions for coverage improvement.
            """.trimIndent(),
            temperature = settings.state.temperature,
            maxTokens = settings.state.maxTokens
        )

        val output = llm.generate(prompt)
        val java = extractJava(output)
        val plan = patcher.buildPlan(testPath, java)
        uiState.update { it.copy(diffPreview = plan.diffPreview) }

        if (!settings.state.autoApplyGeneratedTests) {
            return "Coverage improvement diff prepared. Enable auto-apply to run before/after workflow."
        }

        patcher.apply(plan)
        val after = coverageService.runAndCollectCoverage()
            ?: return "Generated tests were applied, but post-run coverage report could not be collected."

        val delta = CoverageDelta(
            before = before,
            after = after,
            lineDeltaPct = after.lineCoveragePct - before.lineCoveragePct,
            branchDeltaPct = after.branchCoveragePct - before.branchCoveragePct,
            uncovered = uncovered
        )

        uiState.update {
            it.copy(
                iterations = it.iterations + IterationStatus(1, "Coverage rerun complete"),
                summary = "Coverage improvement run complete",
                coverageSummary = "After: lines ${"%.2f".format(after.lineCoveragePct)}%, branches ${"%.2f".format(after.branchCoveragePct)}% | Δ line=${"%.2f".format(delta.lineDeltaPct)}%, branch=${"%.2f".format(delta.branchDeltaPct)}%",
                errors = if (delta.lineDeltaPct < 0 || delta.branchDeltaPct < 0) {
                    listOf("Coverage decreased in one dimension; inspect generated tests and report details.")
                } else emptyList()
            )
        }

        return buildString {
            append("Before: line ${"%.2f".format(before.lineCoveragePct)}%, branch ${"%.2f".format(before.branchCoveragePct)}%. ")
            append("After: line ${"%.2f".format(after.lineCoveragePct)}%, branch ${"%.2f".format(after.branchCoveragePct)}%. ")
            append("Delta: line ${"%.2f".format(delta.lineDeltaPct)}%, branch ${"%.2f".format(delta.branchDeltaPct)}%. ")
            append("Uncovered paths may remain for environment-coupled code, defensive guards, or hard-to-trigger branches.")
        }
    }

    private fun extractJava(raw: String): String {
        val start = raw.indexOf("```")
        val end = raw.lastIndexOf("```")
        if (start < 0 || end <= start) return raw.trim()
        return raw.substring(start + 3, end).removePrefix("java").trim()
    }
}
