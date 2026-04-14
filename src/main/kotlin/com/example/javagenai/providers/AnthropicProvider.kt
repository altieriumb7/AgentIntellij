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

class AnthropicProvider(
    private val apiKey: String,
    private val model: String
) : AiProvider {
    override val id: String = "anthropic"

    override fun complete(request: PromptRequest): ProviderResponse {
        val payload = buildJsonObject {
            put("model", model)
            put("max_tokens", request.maxTokens)
            put("temperature", request.temperature)
            put("system", request.systemPrompt)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", request.userPrompt)
                })
            })
        }.toString()

        val httpRequest = HttpRequest.newBuilder(URI.create("https://api.anthropic.com/v1/messages"))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()

        val response = HttpClient.newHttpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            return ProviderResponse("Anthropic error (${response.statusCode()}).", response.body(), id)
        }

        val body = response.body()
        val parsed = Json.parseToJsonElement(body).jsonObject
        val text = parsed["content"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("text")
            ?.jsonPrimitive
            ?.content
            .orEmpty()

        return ProviderResponse(text, body, id)
    }
}
