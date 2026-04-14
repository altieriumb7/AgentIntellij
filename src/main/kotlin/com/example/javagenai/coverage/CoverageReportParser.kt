package com.example.javagenai.coverage

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class CoverageReportParser {
    fun parseJacocoXml(reportFile: File): CoverageSnapshot {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(reportFile)
        val counters = doc.getElementsByTagName("counter")

        var lineCovered = 0
        var lineMissed = 0
        var branchCovered = 0
        var branchMissed = 0

        for (i in 0 until counters.length) {
            val element = counters.item(i) as? Element ?: continue
            when (element.getAttribute("type")) {
                "LINE" -> {
                    lineMissed += element.getAttribute("missed").toIntOrNull() ?: 0
                    lineCovered += element.getAttribute("covered").toIntOrNull() ?: 0
                }
                "BRANCH" -> {
                    branchMissed += element.getAttribute("missed").toIntOrNull() ?: 0
                    branchCovered += element.getAttribute("covered").toIntOrNull() ?: 0
                }
            }
        }

        val totalLines = lineCovered + lineMissed
        val totalBranches = branchCovered + branchMissed

        return CoverageSnapshot(
            lineCoveragePct = percent(lineCovered, totalLines),
            branchCoveragePct = percent(branchCovered, totalBranches),
            coveredLines = lineCovered,
            totalLines = totalLines,
            coveredBranches = branchCovered,
            totalBranches = totalBranches,
            reportPath = reportFile.absolutePath
        )
    }

    private fun percent(numerator: Int, denominator: Int): Double {
        if (denominator == 0) return 0.0
        return (numerator.toDouble() / denominator.toDouble()) * 100.0
    }
}
