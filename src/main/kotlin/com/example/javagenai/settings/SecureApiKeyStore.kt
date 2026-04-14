package com.example.javagenai.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe

class SecureApiKeyStore {
    private fun attributes(provider: ProviderType): CredentialAttributes {
        val serviceName = "java-gen-ai-plugin-${provider.name.lowercase()}"
        return CredentialAttributes(serviceName, "api-key")
    }

    fun getApiKey(provider: ProviderType): String? =
        PasswordSafe.instance.get(attributes(provider))?.getPasswordAsString()

    fun setApiKey(provider: ProviderType, apiKey: String?) {
        val credentials = apiKey?.takeIf { it.isNotBlank() }?.let { Credentials("api-key", it) }
        PasswordSafe.instance.set(attributes(provider), credentials)
    }
}
