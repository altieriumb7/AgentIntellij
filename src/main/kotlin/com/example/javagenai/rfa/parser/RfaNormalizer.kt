package com.example.javagenai.rfa.parser

import com.example.javagenai.rfa.model.RfaSpec

class RfaNormalizer {
    fun normalize(spec: RfaSpec): RfaSpec {
        val warnings = spec.parseWarnings.toMutableList()
        val endpoint = spec.endpoint
        if (spec.featureName.isNullOrBlank()) warnings += "Feature name is missing."
        if (endpoint?.method.isNullOrBlank()) warnings += "HTTP method is missing."
        if (endpoint?.path.isNullOrBlank()) warnings += "Endpoint path is missing."
        if (spec.requestDto?.name.isNullOrBlank()) warnings += "Request DTO name is missing."
        if (spec.responseDto?.name.isNullOrBlank()) warnings += "Response DTO name is missing."

        return spec.copy(
            endpoint = endpoint?.copy(
                method = endpoint.method?.uppercase(),
                path = endpoint.path?.trim()
            ),
            module = spec.module?.trim(),
            returnType = spec.returnType?.trim(),
            affectedLayers = spec.affectedLayers.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet(),
            parseWarnings = warnings.distinct()
        )
    }
}
