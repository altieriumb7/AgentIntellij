package com.example.javagenai.providers

import com.example.javagenai.prompt.PromptRequest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OpenAiProvider(
    private val apiKey: String,
    private val model: String
) : AiProvider {
    override val id: String = "openai"

    override fun complete(request: PromptRequest): ProviderResponse {
        val payload = buildJsonObject {
            put("model", model)
            put("temperature", request.temperature)
            put("max_tokens", request.maxTokens)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", request.systemPrompt)
                })
                add(buildJsonObject {
                    put("role", "user")
                    put("content", request.userPrompt)
                })
            })
        }.toString()

        val httpRequest = HttpRequest.newBuilder(URI.create("https://api.openai.com/v1/chat/completions"))
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()

        val response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            return ProviderResponse("OpenAI error (${response.statusCode()}).", response.body(), id)
        }

        val body = response.body()
        val parsed = Json.parseToJsonElement(body).jsonObject
        val text = parsed["choices"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("message")
            ?.jsonObject
            ?.get("content")
            ?.jsonPrimitive
            ?.content
            .orEmpty()

        return ProviderResponse(text, body, id)
    }
}
