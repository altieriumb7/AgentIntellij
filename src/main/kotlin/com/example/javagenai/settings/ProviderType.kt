package com.example.javagenai.settings

enum class ProviderType {
    OPENAI,
    ANTHROPIC;

    companion object {
        fun fromExternal(value: String?): ProviderType =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OPENAI
    }
}
