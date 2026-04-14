package com.example.javagenai.rfa.context

import com.example.javagenai.rfa.model.ImplementationPlan
import com.example.javagenai.rfa.model.RfaPromptContext
import com.example.javagenai.rfa.model.RfaSpec

class RfaPromptContextAssembler {
    fun assemble(spec: RfaSpec, plan: ImplementationPlan): RfaPromptContext {
        val packageSuggestions = plan.changes
            .groupBy { it.layer }
            .mapValues { (_, changes) -> changes.first().packageName }

        val similarPaths = plan.changes.flatMap { c -> c.patternMatches.map { it.filePath } }.distinct().take(20)

        return RfaPromptContext(
            normalizedRfa = spec,
            packageSuggestions = packageSuggestions,
            similarFiles = similarPaths,
            planSummary = plan.summary,
            ambiguities = plan.ambiguities,
            warnings = spec.parseWarnings
        )
    }
}
