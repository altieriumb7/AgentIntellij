package com.example.javagenai.generation

import com.example.javagenai.prompt.PromptRequest
import com.example.javagenai.providers.ProviderRegistry
import com.example.javagenai.settings.PluginSettingsState
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class LlmGenerationCoordinator(private val project: Project) {
    private val providerRegistry = ProviderRegistry()
    private val settingsState = PluginSettingsState.getInstance()

    fun generate(prompt: PromptRequest): String {
        val settings = settingsState.state
        val resolved = prompt.copy(
            temperature = settings.temperature.coerceIn(0.0, 1.0),
            maxTokens = settings.maxTokens.coerceAtLeast(256)
        )
        return try {
            providerRegistry.resolveActiveProvider().complete(resolved).text
        } catch (e: Exception) {
            "Generation failed: ${e.message ?: "unknown provider error"}"
        }
    }
}
