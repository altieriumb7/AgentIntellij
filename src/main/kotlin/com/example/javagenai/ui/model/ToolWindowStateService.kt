package com.example.javagenai.ui.model

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

data class IterationStatus(val iteration: Int, val status: String)

data class UiGenerationState(
    val generatedFiles: List<String> = emptyList(),
    val iterations: List<IterationStatus> = emptyList(),
    val errors: List<String> = emptyList(),
    val summary: String = "Idle",
    val coverageSummary: String = "Coverage: n/a",
    val diffPreview: String = ""
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
}
