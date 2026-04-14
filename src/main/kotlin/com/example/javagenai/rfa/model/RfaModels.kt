package com.example.javagenai.rfa.model

data class RfaSpec(
    val featureName: String?,
    val module: String?,
    val endpoint: EndpointSpec?,
    val requestDto: DtoSpec?,
    val responseDto: DtoSpec?,
    val returnType: String?,
    val validations: List<ValidationSpec>,
    val businessNotes: List<String>,
    val affectedLayers: Set<String>,
    val freeTextNotes: List<String>,
    val sourceFilePath: String,
    val parseWarnings: List<String>
)

data class EndpointSpec(
    val method: String?,
    val path: String?
)

data class DtoSpec(
    val name: String?,
    val fields: List<FieldSpec>
)

data class FieldSpec(
    val name: String,
    val type: String?,
    val required: Boolean? = null,
    val notes: String? = null
)

data class ValidationSpec(
    val target: String,
    val rule: String,
    val message: String? = null
)

data class ServiceChangeSpec(
    val interfaceName: String?,
    val implementationName: String?,
    val operation: String?,
    val notes: String? = null
)

data class RepositoryChangeSpec(
    val repositoryName: String?,
    val methodName: String?,
    val queryHint: String? = null,
    val notes: String? = null
)

data class SimilarPatternMatch(
    val layer: String,
    val filePath: String,
    val score: Int,
    val reason: String
)

data class PlannedFileChange(
    val layer: String,
    val action: String,
    val packageName: String,
    val className: String,
    val targetPath: String,
    val rationale: String,
    val patternMatches: List<SimilarPatternMatch> = emptyList()
)

data class ImplementationPlan(
    val title: String,
    val summary: String,
    val changes: List<PlannedFileChange>,
    val ambiguities: List<String>,
    val warnings: List<String>
)

data class RfaPromptContext(
    val normalizedRfa: RfaSpec,
    val packageSuggestions: Map<String, String>,
    val similarFiles: List<String>,
    val planSummary: String,
    val ambiguities: List<String>,
    val warnings: List<String>
)
