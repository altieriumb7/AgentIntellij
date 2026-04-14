package com.example.javagenai.rfa.planner

import com.example.javagenai.analysis.ProjectPatternProfile
import com.example.javagenai.rfa.model.ImplementationPlan
import com.example.javagenai.rfa.model.PlannedFileChange
import com.example.javagenai.rfa.model.RfaSpec
import com.example.javagenai.rfa.model.SimilarPatternMatch

class ImplementationPlanBuilder {
    fun build(spec: RfaSpec, profile: ProjectPatternProfile, matches: List<SimilarPatternMatch>): ImplementationPlan {
        val changes = mutableListOf<PlannedFileChange>()
        val pkgBase = inferBasePackage(profile)
        val feature = spec.featureName?.replace(" ", "") ?: "Feature"

        changes += PlannedFileChange(
            layer = "controller",
            action = "add endpoint",
            packageName = "$pkgBase.controller",
            className = "${feature}Controller",
            targetPath = "src/main/java/${pkgBase.replace('.', '/')}/controller/${feature}Controller.java",
            rationale = "Expose ${spec.endpoint?.method ?: "UNKNOWN"} ${spec.endpoint?.path ?: "<missing-path>"}",
            patternMatches = matches.filter { it.layer == "controller" }.take(3)
        )

        changes += PlannedFileChange(
            layer = "dto",
            action = "add request dto",
            packageName = "$pkgBase.dto",
            className = spec.requestDto?.name ?: "${feature}Request",
            targetPath = "src/main/java/${pkgBase.replace('.', '/')}/dto/${spec.requestDto?.name ?: "${feature}Request"}.java",
            rationale = "Represent incoming payload fields from RFA",
            patternMatches = matches.filter { it.layer == "unknown" || it.filePath.contains("dto", true) }.take(3)
        )

        changes += PlannedFileChange(
            layer = "dto",
            action = "add response dto",
            packageName = "$pkgBase.dto",
            className = spec.responseDto?.name ?: "${feature}Response",
            targetPath = "src/main/java/${pkgBase.replace('.', '/')}/dto/${spec.responseDto?.name ?: "${feature}Response"}.java",
            rationale = "Represent response contract from RFA",
            patternMatches = matches.filter { it.filePath.contains("dto", true) }.take(3)
        )

        changes += PlannedFileChange(
            layer = "service",
            action = "update service interface/impl",
            packageName = "$pkgBase.service",
            className = "${feature}Service",
            targetPath = "src/main/java/${pkgBase.replace('.', '/')}/service/${feature}Service.java",
            rationale = "Implement business rules and validations",
            patternMatches = matches.filter { it.layer == "service" }.take(3)
        )

        changes += PlannedFileChange(
            layer = "repository",
            action = "update/add repository method",
            packageName = "$pkgBase.repository",
            className = "${feature}Repository",
            targetPath = "src/main/java/${pkgBase.replace('.', '/')}/repository/${feature}Repository.java",
            rationale = "Persist/query data required by service rules",
            patternMatches = matches.filter { it.layer == "repository" }.take(3)
        )

        changes += PlannedFileChange(
            layer = "tests",
            action = "add unit tests",
            packageName = "$pkgBase",
            className = "${feature}ServiceTest",
            targetPath = "src/test/java/${pkgBase.replace('.', '/')}/${feature}ServiceTest.java",
            rationale = "Validate behavior, validation rules, and edge cases",
            patternMatches = matches.filter { it.layer == "tests" }.take(3)
        )

        val ambiguities = mutableListOf<String>()
        if (spec.endpoint?.path.isNullOrBlank()) ambiguities += "Endpoint path missing in RFA."
        if (spec.endpoint?.method.isNullOrBlank()) ambiguities += "HTTP method missing in RFA."
        if (spec.requestDto?.fields.isNullOrEmpty()) ambiguities += "Request fields are missing/unclear."
        if (spec.responseDto?.fields.isNullOrEmpty()) ambiguities += "Response fields are missing/unclear."

        return ImplementationPlan(
            title = "Implementation plan for ${spec.featureName ?: "unnamed feature"}",
            summary = "Plan based on repository patterns (${matches.size} similar files matched).",
            changes = changes,
            ambiguities = ambiguities,
            warnings = spec.parseWarnings
        )
    }

    private fun inferBasePackage(profile: ProjectPatternProfile): String {
        if (profile.packageStyle.isNotBlank()) return profile.packageStyle
        return "com.example.app"
    }
}
