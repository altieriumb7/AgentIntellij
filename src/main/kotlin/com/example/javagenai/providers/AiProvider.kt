package com.example.javagenai.providers

import com.example.javagenai.prompt.PromptRequest

interface AiProvider {
    val id: String
    fun complete(request: PromptRequest): ProviderResponse
}
