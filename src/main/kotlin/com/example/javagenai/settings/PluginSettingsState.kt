package com.example.javagenai.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service

@State(name = "JavaGenAiPluginSettings", storages = [Storage("java-gen-ai.xml")])
class PluginSettingsState : PersistentStateComponent<PluginSettingsState.State> {
    data class State(
        var provider: String = ProviderType.OPENAI.name,
        var model: String = "",
        var temperature: Double = 0.2,
        var maxTokens: Int = 1024,
        var repairRetryBudget: Int = 3,
        var autoApplyGeneratedTests: Boolean = false
    )

    private var currentState = State()

    override fun getState(): State = currentState

    override fun loadState(state: State) {
        currentState = state
    }

    companion object {
        fun getInstance(): PluginSettingsState = service()
    }
}
