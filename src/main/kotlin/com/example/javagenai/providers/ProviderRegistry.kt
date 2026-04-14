package com.example.javagenai.providers

import com.example.javagenai.settings.PluginSettingsState
import com.example.javagenai.settings.ProviderType
import com.example.javagenai.settings.SecureApiKeyStore

class ProviderRegistry(
    private val settingsState: PluginSettingsState = PluginSettingsState.getInstance(),
    private val apiKeyStore: SecureApiKeyStore = SecureApiKeyStore()
) {
    fun resolveActiveProvider(): AiProvider {
        val configured = ProviderType.fromExternal(settingsState.state.provider)
        val apiKey = apiKeyStore.getApiKey(configured).orEmpty().trim()
        require(apiKey.isNotBlank()) { "API key is missing for provider ${configured.name}. Configure it in Settings > Java Gen AI." }

        val model = settingsState.state.model.takeIf { it.isNotBlank() } ?: defaultModel(configured)
        return when (configured) {
            ProviderType.OPENAI -> OpenAiProvider(apiKey = apiKey, model = model)
            ProviderType.ANTHROPIC -> AnthropicProvider(apiKey = apiKey, model = model)
        }
    }

    private fun defaultModel(type: ProviderType): String = when (type) {
        ProviderType.OPENAI -> "gpt-4o-mini"
        ProviderType.ANTHROPIC -> "claude-3-5-sonnet-latest"
    }
}
