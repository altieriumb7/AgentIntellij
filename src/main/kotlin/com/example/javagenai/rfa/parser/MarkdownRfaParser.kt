package com.example.javagenai.rfa.parser

import com.example.javagenai.rfa.model.DtoSpec
import com.example.javagenai.rfa.model.EndpointSpec
import com.example.javagenai.rfa.model.FieldSpec
import com.example.javagenai.rfa.model.RfaSpec
import com.example.javagenai.rfa.model.ValidationSpec

class MarkdownRfaParser : RfaParser {
    override fun supports(extension: String): Boolean = extension.equals("md", ignoreCase = true)

    override fun parse(content: String, sourceFilePath: String): RfaSpec {
        val warnings = mutableListOf<String>()
        val lines = content.lines()

        fun findValue(vararg keys: String): String? {
            val lowerKeys = keys.map { it.lowercase() }
            return lines.firstOrNull { line ->
                lowerKeys.any { k -> line.lowercase().startsWith("$k:") || line.lowercase().contains("**$k**") }
            }?.substringAfter(':', "")?.trim()?.ifBlank { null }
        }

        val method = findValue("method", "http method")
        val path = findValue("path", "endpoint", "url")
        if (method == null || path == null) warnings += "Could not confidently parse endpoint method/path from Markdown."

        val requestName = findValue("request dto", "request")
        val responseName = findValue("response dto", "response")
        if (requestName == null) warnings += "Request DTO not clearly specified in Markdown."
        if (responseName == null) warnings += "Response DTO not clearly specified in Markdown."

        val validations = lines
            .filter { it.trim().startsWith("-") && it.lowercase().contains("valid") }
            .map { ValidationSpec(target = "request", rule = it.removePrefix("-").trim()) }

        return RfaSpec(
            featureName = findValue("feature", "feature name", "title") ?: lines.firstOrNull { it.startsWith("#") }?.removePrefix("#")?.trim(),
            module = findValue("module", "domain"),
            endpoint = EndpointSpec(method = method, path = path),
            requestDto = DtoSpec(name = requestName, fields = extractFieldHints(lines, "request")),
            responseDto = DtoSpec(name = responseName, fields = extractFieldHints(lines, "response")),
            returnType = findValue("return type"),
            validations = validations,
            businessNotes = lines.filter { it.lowercase().contains("business") || it.lowercase().contains("rule") }.map { it.trim() }.take(20),
            affectedLayers = inferLayers(lines),
            freeTextNotes = listOf(content.take(4000)),
            sourceFilePath = sourceFilePath,
            parseWarnings = warnings
        )
    }

    private fun extractFieldHints(lines: List<String>, sectionKeyword: String): List<FieldSpec> {
        val relevant = lines.filter { it.lowercase().contains(sectionKeyword) && it.contains(':') }
        return relevant.mapNotNull { line ->
            val maybe = line.substringAfter(':').trim()
            val fieldName = maybe.split(' ', '-', ',').firstOrNull()?.trim()?.takeIf { it.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")) }
            fieldName?.let { FieldSpec(name = it, type = null, required = null, notes = "parsed from markdown hint") }
        }.distinctBy { it.name }.take(30)
    }

    private fun inferLayers(lines: List<String>): Set<String> = buildSet {
        val text = lines.joinToString("\n").lowercase()
        if ("controller" in text || "endpoint" in text) add("controller")
        if ("service" in text) add("service")
        if ("repository" in text || "dao" in text) add("repository")
        if ("mapper" in text) add("mapper")
        if ("test" in text || "junit" in text) add("tests")
    }
}
