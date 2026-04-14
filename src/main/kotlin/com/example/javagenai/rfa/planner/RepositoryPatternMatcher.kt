package com.example.javagenai.rfa.planner

import com.example.javagenai.analysis.PatternAnalyzerService
import com.example.javagenai.rfa.model.RfaSpec
import com.example.javagenai.rfa.model.SimilarPatternMatch
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

class RepositoryPatternMatcher(private val project: Project) {
    private val patternAnalyzer = project.getService(PatternAnalyzerService::class.java)

    fun findMatches(spec: RfaSpec): List<SimilarPatternMatch> {
        val profile = patternAnalyzer.analyzeProjectPatterns()
        val files = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project))

        val endpointPathHint = spec.endpoint?.path?.substringAfterLast('/')?.replace('{', ' ')?.replace('}', ' ')?.trim()
        return files.asSequence()
            .map { vf ->
                val score = score(vf.path, spec, profile, endpointPathHint)
                SimilarPatternMatch(
                    layer = inferLayer(vf.path),
                    filePath = vf.path,
                    score = score,
                    reason = "name/package similarity"
                )
            }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(30)
            .toList()
    }

    private fun score(path: String, spec: RfaSpec, profile: com.example.javagenai.analysis.ProjectPatternProfile, endpointHint: String?): Int {
        var score = 0
        if (spec.module != null && path.contains(spec.module!!, ignoreCase = true)) score += 3
        if (endpointHint != null && path.contains(endpointHint, ignoreCase = true)) score += 4
        if (profile.packageStyle.isNotBlank() && path.contains(profile.packageStyle.replace('.', '/'))) score += 1
        if (spec.requestDto?.name != null && path.contains(spec.requestDto.name!!, ignoreCase = true)) score += 3
        if (spec.responseDto?.name != null && path.contains(spec.responseDto.name!!, ignoreCase = true)) score += 3
        return score
    }

    private fun inferLayer(path: String): String = when {
        path.contains("controller", true) -> "controller"
        path.contains("service", true) -> "service"
        path.contains("repository", true) -> "repository"
        path.contains("mapper", true) -> "mapper"
        path.contains("test", true) -> "tests"
        else -> "unknown"
    }
}
