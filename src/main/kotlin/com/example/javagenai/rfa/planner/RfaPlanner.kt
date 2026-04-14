package com.example.javagenai.rfa.planner

import com.example.javagenai.analysis.PatternAnalyzerService
import com.example.javagenai.rfa.model.ImplementationPlan
import com.example.javagenai.rfa.model.RfaSpec
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class RfaPlanner(private val project: Project) {
    private val patternAnalyzer = project.getService(PatternAnalyzerService::class.java)
    private val matcher = RepositoryPatternMatcher(project)
    private val builder = ImplementationPlanBuilder()

    fun buildPlan(spec: RfaSpec): ImplementationPlan {
        val profile = patternAnalyzer.analyzeProjectPatterns()
        val matches = matcher.findMatches(spec)
        return builder.build(spec, profile, matches)
    }
}
