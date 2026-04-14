package com.example.javagenai.coverage

import com.example.javagenai.execution.TestExecutionService
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File

@Service(Service.Level.PROJECT)
class CoverageService(private val project: Project) {
    private val parser = CoverageReportParser()
    private val executionService = project.getService(TestExecutionService::class.java)

    fun runAndCollectCoverage(): CoverageSnapshot? {
        executionService.runCoverageTask()
        val report = findCoverageReport() ?: return null
        return parser.parseJacocoXml(report)
    }

    fun findCoverageReport(): File? {
        val base = project.basePath?.let(::File) ?: return null
        val candidates = listOf(
            File(base, "build/reports/jacoco/test/jacocoTestReport.xml"),
            File(base, "build/reports/jacoco/testCodeCoverageReport/testCodeCoverageReport.xml"),
            File(base, "target/site/jacoco/jacoco.xml"),
            File(base, "target/site/jacoco-it/jacoco.xml")
        )

        return candidates.firstOrNull { it.exists() }
    }
}
