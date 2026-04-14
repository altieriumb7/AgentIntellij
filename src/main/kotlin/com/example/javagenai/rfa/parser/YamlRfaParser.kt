package com.example.javagenai.rfa.parser

import com.example.javagenai.rfa.model.DtoSpec
import com.example.javagenai.rfa.model.EndpointSpec
import com.example.javagenai.rfa.model.FieldSpec
import com.example.javagenai.rfa.model.RfaSpec
import com.example.javagenai.rfa.model.ValidationSpec
import org.yaml.snakeyaml.Yaml

class YamlRfaParser : RfaParser {
    override fun supports(extension: String): Boolean = extension.equals("yaml", true) || extension.equals("yml", true)

    @Suppress("UNCHECKED_CAST")
    override fun parse(content: String, sourceFilePath: String): RfaSpec {
        val warnings = mutableListOf<String>()
        val root = runCatching { Yaml().load(content) as? Map<String, Any?> }.getOrElse {
            return RfaSpec(
                featureName = null,
                module = null,
                endpoint = null,
                requestDto = null,
                responseDto = null,
                returnType = null,
                validations = emptyList(),
                businessNotes = emptyList(),
                affectedLayers = emptySet(),
                freeTextNotes = listOf(content.take(2000)),
                sourceFilePath = sourceFilePath,
                parseWarnings = listOf("Invalid YAML format: ${it.message}")
            )
        } ?: emptyMap()

        val endpointMap = root["endpoint"] as? Map<String, Any?>
        val endpoint = endpointMap?.let { EndpointSpec(it["method"]?.toString(), it["path"]?.toString()) }
        if (endpoint?.method == null || endpoint.path == null) warnings += "Endpoint method/path missing or incomplete."

        val validations = (root["validations"] as? List<Map<String, Any?>>).orEmpty().map {
            ValidationSpec(
                target = it["target"]?.toString() ?: "unknown",
                rule = it["rule"]?.toString() ?: "unspecified",
                message = it["message"]?.toString()
            )
        }

        val requestDto = dto(root["requestDto"] as? Map<String, Any?>)
        val responseDto = dto(root["responseDto"] as? Map<String, Any?>)

        return RfaSpec(
            featureName = root["featureName"]?.toString(),
            module = root["module"]?.toString(),
            endpoint = endpoint,
            requestDto = requestDto,
            responseDto = responseDto,
            returnType = root["returnType"]?.toString(),
            validations = validations,
            businessNotes = listOfStrings(root["businessNotes"]),
            affectedLayers = listOfStrings(root["affectedLayers"]).toSet(),
            freeTextNotes = listOfStrings(root["notes"]),
            sourceFilePath = sourceFilePath,
            parseWarnings = warnings
        )
    }

    private fun dto(map: Map<String, Any?>?): DtoSpec? {
        if (map == null) return null
        val fields = (map["fields"] as? List<Map<String, Any?>>).orEmpty().mapNotNull {
            val name = it["name"]?.toString() ?: return@mapNotNull null
            FieldSpec(
                name = name,
                type = it["type"]?.toString(),
                required = it["required"] as? Boolean,
                notes = it["notes"]?.toString()
            )
        }
        return DtoSpec(name = map["name"]?.toString(), fields = fields)
    }

    private fun listOfStrings(value: Any?): List<String> = (value as? List<*>)?.mapNotNull { it?.toString()?.trim() } ?: emptyList()
}
