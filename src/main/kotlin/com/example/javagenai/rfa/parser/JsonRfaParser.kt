package com.example.javagenai.rfa.parser

import com.example.javagenai.rfa.model.DtoSpec
import com.example.javagenai.rfa.model.EndpointSpec
import com.example.javagenai.rfa.model.FieldSpec
import com.example.javagenai.rfa.model.RfaSpec
import com.example.javagenai.rfa.model.ValidationSpec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JsonRfaParser : RfaParser {
    override fun supports(extension: String): Boolean = extension.equals("json", ignoreCase = true)

    override fun parse(content: String, sourceFilePath: String): RfaSpec {
        val warnings = mutableListOf<String>()
        val root = runCatching { Json.parseToJsonElement(content).jsonObject }.getOrElse {
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
                parseWarnings = listOf("Invalid JSON format: ${it.message}")
            )
        }

        val endpointObj = root.obj("endpoint")
        val requestObj = root.obj("requestDto")
        val responseObj = root.obj("responseDto")

        val endpoint = if (endpointObj != null) {
            EndpointSpec(endpointObj.str("method"), endpointObj.str("path"))
        } else null

        if (endpoint?.method == null || endpoint.path == null) warnings += "Endpoint method/path missing or incomplete."

        val validations = root.arr("validations").mapNotNull { element ->
            val obj = (element as? JsonObject) ?: return@mapNotNull null
            ValidationSpec(
                target = obj.str("target") ?: "unknown",
                rule = obj.str("rule") ?: "unspecified",
                message = obj.str("message")
            )
        }

        val requestDto = dtoFromObj(requestObj)
        val responseDto = dtoFromObj(responseObj)
        if (requestDto?.name == null) warnings += "Request DTO name missing."
        if (responseDto?.name == null) warnings += "Response DTO name missing."

        return RfaSpec(
            featureName = root.str("featureName"),
            module = root.str("module"),
            endpoint = endpoint,
            requestDto = requestDto,
            responseDto = responseDto,
            returnType = root.str("returnType"),
            validations = validations,
            businessNotes = root.strList("businessNotes"),
            affectedLayers = root.strList("affectedLayers").toSet(),
            freeTextNotes = root.strList("notes"),
            sourceFilePath = sourceFilePath,
            parseWarnings = warnings
        )
    }

    private fun dtoFromObj(obj: JsonObject?): DtoSpec? {
        if (obj == null) return null
        val fields = obj.arr("fields").mapNotNull { fieldEl ->
            val f = fieldEl as? JsonObject ?: return@mapNotNull null
            val name = f.str("name") ?: return@mapNotNull null
            FieldSpec(
                name = name,
                type = f.str("type"),
                required = f.bool("required"),
                notes = f.str("notes")
            )
        }
        return DtoSpec(name = obj.str("name"), fields = fields)
    }

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.ifBlank { null }
    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
    private fun JsonObject.obj(key: String): JsonObject? = (this[key] as? JsonObject)
    private fun JsonObject.arr(key: String): JsonArray = (this[key]?.jsonArray ?: JsonArray(emptyList()))

    private fun JsonObject.strList(key: String): List<String> = arr(key).mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
}
