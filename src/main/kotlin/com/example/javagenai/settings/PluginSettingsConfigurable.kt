package com.example.javagenai.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JPasswordField
import javax.swing.JTextField

class PluginSettingsConfigurable : Configurable {
    private val state = PluginSettingsState.getInstance()
    private val secureStore = SecureApiKeyStore()

    private val providerCombo = JComboBox(ProviderType.entries.toTypedArray())
    private val apiKeyField = JPasswordField()
    private val modelField = JTextField()
    private val temperatureField = JTextField()
    private val maxTokensField = JTextField()
    private val retryBudgetField = JTextField()
    private val autoApplyBox = JCheckBox("Auto-apply generated test changes")

    override fun getDisplayName(): String = "Java Gen AI"

    override fun createComponent(): JComponent = JPanel(GridBagLayout()).apply {
        val c = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            insets = java.awt.Insets(4, 4, 4, 4)
        }

        addRow(this, c, 0, "Provider", providerCombo)
        addRow(this, c, 1, "API key", apiKeyField)
        addRow(this, c, 2, "Model", modelField)
        addRow(this, c, 3, "Temperature (0.0-1.0)", temperatureField)
        addRow(this, c, 4, "Max tokens", maxTokensField)
        addRow(this, c, 5, "Repair retries", retryBudgetField)
        addRow(this, c, 6, "Apply mode", autoApplyBox)

        reset()
    }

    override fun isModified(): Boolean {
        val selectedProvider = providerCombo.selectedItem as ProviderType
        val persisted = state.state
        val currentApiKey = String(apiKeyField.password)
        return persisted.provider != selectedProvider.name ||
            persisted.model != modelField.text ||
            persisted.temperature.toString() != temperatureField.text ||
            persisted.maxTokens.toString() != maxTokensField.text ||
            persisted.repairRetryBudget.toString() != retryBudgetField.text ||
            persisted.autoApplyGeneratedTests != autoApplyBox.isSelected ||
            (currentApiKey.isNotBlank() && currentApiKey != secureStore.getApiKey(selectedProvider))
    }

    override fun apply() {
        val selectedProvider = providerCombo.selectedItem as ProviderType
        val temperature = temperatureField.text.toDoubleOrNull()
            ?: throw ConfigurationException("Temperature must be a decimal value between 0.0 and 1.0.")
        if (temperature !in 0.0..1.0) {
            throw ConfigurationException("Temperature must be between 0.0 and 1.0.")
        }

        val maxTokens = maxTokensField.text.toIntOrNull()
            ?: throw ConfigurationException("Max tokens must be a positive integer.")
        if (maxTokens < 128) {
            throw ConfigurationException("Max tokens should be at least 128.")
        }

        val retries = retryBudgetField.text.toIntOrNull()
            ?: throw ConfigurationException("Repair retries must be a positive integer.")
        if (retries !in 1..10) {
            throw ConfigurationException("Repair retries must be between 1 and 10.")
        }

        state.loadState(
            PluginSettingsState.State(
                provider = selectedProvider.name,
                model = modelField.text.trim(),
                temperature = temperature,
                maxTokens = maxTokens,
                repairRetryBudget = retries,
                autoApplyGeneratedTests = autoApplyBox.isSelected
            )
        )

        val trimmedKey = String(apiKeyField.password).trim()
        if (trimmedKey.isNotBlank()) {
            secureStore.setApiKey(selectedProvider, trimmedKey)
        }
    }

    override fun reset() {
        val persisted = state.state
        val provider = ProviderType.fromExternal(persisted.provider)
        providerCombo.selectedItem = provider
        modelField.text = persisted.model
        temperatureField.text = persisted.temperature.toString()
        maxTokensField.text = persisted.maxTokens.toString()
        retryBudgetField.text = persisted.repairRetryBudget.toString()
        autoApplyBox.isSelected = persisted.autoApplyGeneratedTests
        apiKeyField.text = secureStore.getApiKey(provider).orEmpty()
    }

    private fun addRow(panel: JPanel, c: GridBagConstraints, row: Int, label: String, field: JComponent) {
        c.gridx = 0
        c.gridy = row
        c.weightx = 0.2
        panel.add(JLabel(label), c)
        c.gridx = 1
        c.weightx = 0.8
        panel.add(field, c)
    }
}
