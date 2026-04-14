package com.example.javagenai.analysis

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.PsiImportStatement
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiAnnotation
import com.intellij.ide.highlighter.JavaFileType

@Service(Service.Level.PROJECT)
class PatternAnalyzerService(private val project: Project) {

    fun analyzeProjectPatterns(): ProjectPatternProfile {
        val javaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project))
            .mapNotNull { PsiManager.getInstance(project).findFile(it) as? PsiJavaFile }

        val packageStyle = javaFiles.map { it.packageName }.groupingBy { it }.eachCount()
            .maxByOrNull { it.value }?.key.orEmpty()

        val classNamePatterns = PsiShortNamesCache.getInstance(project).allClassNames
            .asSequence()
            .mapNotNull { detectRolePattern(it) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(8)
            .map { it.key }

        val testFiles = javaFiles.filter { it.name.contains("Test") || it.name.contains("IT") }
        val testNamePatterns = testFiles.map { it.name.substringBefore('.') }
            .mapNotNull { detectRolePattern(it) }
            .distinct()

        val allImports = javaFiles.flatMap { file ->
            PsiTreeUtil.findChildrenOfType(file, PsiImportStatement::class.java).mapNotNull { it.qualifiedName }
        }

        val annotations = javaFiles.flatMap { file ->
            PsiTreeUtil.findChildrenOfType(file, PsiAnnotation::class.java).mapNotNull { it.qualifiedName }
        }.toSet()

        return ProjectPatternProfile(
            packageStyle = packageStyle,
            classNamePatterns = classNamePatterns,
            testNamePatterns = testNamePatterns,
            junitVersion = detectJunitVersion(allImports),
            mockingLibraries = detectMockingLibraries(allImports),
            assertionLibraries = detectAssertionLibraries(allImports),
            fixtureStyleHints = detectFixtureStyleHints(testFiles.map { it.text }),
            commonAnnotations = annotations,
            commonImports = allImports.toSet(),
            javaVersionSignals = detectJavaVersionSignals(allImports, annotations),
            architecturalRoles = classNamePatterns.toSet()
        )
    }

    private fun detectRolePattern(name: String): String? = when {
        name.endsWith("Service") -> "service"
        name.endsWith("Repository") -> "repository"
        name.endsWith("Controller") -> "controller"
        name.endsWith("Helper") -> "helper"
        name.endsWith("Test") -> "test"
        else -> null
    }

    private fun detectJunitVersion(imports: List<String>): String = when {
        imports.any { it.startsWith("org.junit.jupiter") } -> "junit5"
        imports.any { it.startsWith("org.junit.") } -> "junit4"
        else -> "unknown"
    }

    private fun detectMockingLibraries(imports: List<String>): Set<String> = buildSet {
        if (imports.any { it.startsWith("org.mockito") }) add("mockito")
        if (imports.any { it.startsWith("io.mockk") }) add("mockk")
        if (imports.any { it.startsWith("org.easymock") }) add("easymock")
    }

    private fun detectAssertionLibraries(imports: List<String>): Set<String> = buildSet {
        if (imports.any { it.startsWith("org.assertj") }) add("assertj")
        if (imports.any { it.startsWith("org.hamcrest") }) add("hamcrest")
        if (imports.any { it.startsWith("org.junit") }) add("junit")
    }

    private fun detectFixtureStyleHints(testTexts: List<String>): List<String> = buildList {
        if (testTexts.any { "@BeforeEach" in it || "@Before" in it }) add("setup-method")
        if (testTexts.any { "@Nested" in it }) add("nested-tests")
        if (testTexts.any { "@TestInstance" in it }) add("test-instance-lifecycle")
        if (testTexts.any { "given" in it && "when" in it && "then" in it }) add("given-when-then")
    }

    private fun detectJavaVersionSignals(imports: List<String>, annotations: Set<String>): Set<String> = buildSet {
        if (annotations.any { it.contains("jakarta") }) add("jakarta-era")
        if (imports.any { it.contains("java.util.Optional") }) add("optional-usage")
        if (imports.any { it.contains("java.time") }) add("java-time")
    }
}
