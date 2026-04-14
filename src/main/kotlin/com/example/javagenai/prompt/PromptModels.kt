package com.example.javagenai.prompt

data class PromptContext(
    val anchorPath: String,
    val summary: String,
    val relatedPaths: List<String>
)

data class PromptRequest(
    val systemPrompt: String,
    val userPrompt: String,
    val temperature: Double,
    val maxTokens: Int
)
