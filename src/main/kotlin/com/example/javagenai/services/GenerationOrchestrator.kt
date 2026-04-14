package com.example.javagenai.services

import com.example.javagenai.analysis.PatternAnalyzerService
import com.example.javagenai.coverage.CoverageImprovementCoordinator
import com.example.javagenai.generation.TestGenerationService
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class GenerationOrchestrator(private val project: Project) {
    private val patternAnalyzer = project.getService(PatternAnalyzerService::class.java)
    private val testGenerationService = project.getService(TestGenerationService::class.java)
    private val coverageCoordinator = project.getService(CoverageImprovementCoordinator::class.java)

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
}
