package com.example.javagenai.providers

data class ProviderResponse(
    val text: String,
    val rawPayload: String? = null,
    val providerId: String = ""
)
