package com.example.javagenai.ui.model

import com.example.javagenai.rfa.model.ImplementationPlan
import com.example.javagenai.rfa.model.RfaSpec
import com.example.javagenai.rfa.model.SimilarPatternMatch
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

data class IterationStatus(val iteration: Int, val status: String)

data class UiGenerationState(
    val generatedFiles: List<String> = emptyList(),
    val iterations: List<IterationStatus> = emptyList(),
    val errors: List<String> = emptyList(),
    val summary: String = "Idle",
    val coverageSummary: String = "Coverage: n/a",
    val diffPreview: String = "",
    val importedRfaPath: String? = null,
    val rfaSummary: String = "",
    val rfaWarnings: List<String> = emptyList(),
    val rfaPatternMatches: List<SimilarPatternMatch> = emptyList(),
    val rfaPlan: ImplementationPlan? = null,
    val rfaAmbiguities: List<String> = emptyList()
)

@Service(Service.Level.PROJECT)
class ToolWindowStateService(private val project: Project) {
    private val listeners = mutableListOf<(UiGenerationState) -> Unit>()
    private var state = UiGenerationState()

    fun subscribe(listener: (UiGenerationState) -> Unit) {
        listeners += listener
        listener(state)
    }

    fun update(transform: (UiGenerationState) -> UiGenerationState) {
        state = transform(state)
        listeners.forEach { it(state) }
    }

    fun reset() {
        state = UiGenerationState()
        listeners.forEach { it(state) }
    }

    fun setImportedRfa(spec: RfaSpec) {
        update {
            it.copy(
                importedRfaPath = spec.sourceFilePath,
                rfaSummary = buildRfaSummary(spec),
                rfaWarnings = spec.parseWarnings,
                rfaPatternMatches = emptyList(),
                rfaPlan = null,
                rfaAmbiguities = emptyList()
            )
        }
    }

    fun setRfaPlan(plan: ImplementationPlan) {
        val matches = plan.changes.flatMap { it.patternMatches }
        update {
            it.copy(
                rfaPlan = plan,
                rfaPatternMatches = matches,
                rfaAmbiguities = plan.ambiguities
            )
        }
    }

    private fun buildRfaSummary(spec: RfaSpec): String {
        return buildString {
            append("Feature: ${spec.featureName ?: "<missing>"}; ")
            append("Module: ${spec.module ?: "<missing>"}; ")
            append("Endpoint: ${spec.endpoint?.method ?: "<missing>"} ${spec.endpoint?.path ?: "<missing>"}; ")
            append("Request DTO: ${spec.requestDto?.name ?: "<missing>"}; ")
            append("Response DTO: ${spec.responseDto?.name ?: "<missing>"}")
        }
    }
}
