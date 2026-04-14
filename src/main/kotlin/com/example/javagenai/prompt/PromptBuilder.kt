package com.example.javagenai.prompt

import com.example.javagenai.analysis.ProjectPatternProfile

class PromptBuilder {
    fun buildJavaGenerationPrompt(goal: String, profile: ProjectPatternProfile, context: PromptContext): PromptRequest {
        val system = """
            You are an expert Java engineer. Follow project conventions and testing styles strictly.
            JUnit: ${profile.junitVersion}
            Naming patterns: ${profile.classNamePatterns.joinToString()}
            Fixture hints: ${profile.fixtureStyleHints.joinToString()}
        """.trimIndent()

        val user = """
            Goal: $goal
            Anchor file: ${context.anchorPath}
            Project context:
            ${context.summary}
            Related files:
            ${context.relatedPaths.joinToString("\n")}
        """.trimIndent()

        return PromptRequest(
            systemPrompt = system,
            userPrompt = user,
            temperature = 0.2,
            maxTokens = 1200
        )
    }
}
