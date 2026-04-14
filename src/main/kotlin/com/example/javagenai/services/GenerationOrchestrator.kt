package com.example.javagenai.services

import com.example.javagenai.analysis.PatternAnalyzerService
import com.example.javagenai.coverage.CoverageImprovementCoordinator
import com.example.javagenai.generation.TestGenerationService
import com.example.javagenai.rfa.context.RfaPromptContextAssembler
import com.example.javagenai.rfa.planner.RfaPlanner
import com.example.javagenai.rfa.service.RfaImportService
import com.example.javagenai.rfa.service.RfaSessionService
import com.example.javagenai.ui.model.ToolWindowStateService
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class GenerationOrchestrator(private val project: Project) {
    private val patternAnalyzer = project.getService(PatternAnalyzerService::class.java)
    private val testGenerationService = project.getService(TestGenerationService::class.java)
    private val coverageCoordinator = project.getService(CoverageImprovementCoordinator::class.java)
    private val rfaImportService = project.getService(RfaImportService::class.java)
    private val rfaSession = project.getService(RfaSessionService::class.java)
    private val rfaPlanner = project.getService(RfaPlanner::class.java)
    private val uiState = project.getService(ToolWindowStateService::class.java)
    private val rfaPromptAssembler = RfaPromptContextAssembler()

    fun generateJUnit(anchorFile: VirtualFile): String {
        val report = testGenerationService.generateAndRepairFor(anchorFile)
        return report.summary
    }

    fun repairFailingTests(anchorFile: VirtualFile): String {
        val report = testGenerationService.generateAndRepairFor(anchorFile)
        return report.summary
    }

    fun generateCodeFollowingPatterns(anchorFile: VirtualFile): String {
        val report = testGenerationService.generateAndRepairFor(anchorFile)
        return report.summary
    }

    fun analyzeTestingConventions(): String {
        val profile = patternAnalyzer.analyzeProjectPatterns()
        return "Detected JUnit=${profile.junitVersion}, mocking=${profile.mockingLibraries}, assertions=${profile.assertionLibraries}"
    }

    fun improveCoverage(anchorFile: VirtualFile): String {
        return coverageCoordinator.improveCoverageFor(anchorFile)
    }

    fun importRfa(file: VirtualFile): String {
        val spec = rfaImportService.import(file)
        rfaSession.currentRfa = spec
        rfaSession.currentPlan = null
        uiState.setImportedRfa(spec)
        return if (spec.parseWarnings.isEmpty()) {
            "RFA imported: ${spec.featureName ?: file.name}"
        } else {
            "RFA imported with warnings (${spec.parseWarnings.size}). Check tool window for details."
        }
    }

    fun buildImplementationPlanFromImportedRfa(): String {
        val spec = rfaSession.currentRfa ?: return "No RFA imported. Use 'Import RFA' first."
        val plan = rfaPlanner.buildPlan(spec)
        rfaSession.currentPlan = plan
        uiState.setRfaPlan(plan)

        // Prepared context object for future RFA -> code generation pass.
        rfaPromptAssembler.assemble(spec, plan)

        return "Built implementation plan with ${plan.changes.size} planned change(s) and ${plan.ambiguities.size} ambiguity warning(s)."
    }
}
