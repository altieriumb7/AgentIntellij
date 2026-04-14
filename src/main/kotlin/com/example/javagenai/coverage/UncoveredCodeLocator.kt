package com.example.javagenai.coverage

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class UncoveredCodeLocator {
    fun findUncovered(reportFile: File, classNameFilter: String? = null): List<UncoveredLocation> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(reportFile)
        val sourceLines = doc.getElementsByTagName("sourcefile")
        val results = mutableListOf<UncoveredLocation>()

        for (i in 0 until sourceLines.length) {
            val sourceFile = sourceLines.item(i) as? Element ?: continue
            val packageElement = sourceFile.parentNode as? Element
            val packageName = packageElement?.getAttribute("name")?.replace('/', '.') ?: ""
            val className = if (packageName.isBlank()) {
                sourceFile.getAttribute("name").removeSuffix(".java")
            } else {
                "$packageName.${sourceFile.getAttribute("name").removeSuffix(".java")}".trim('.')
            }

            if (classNameFilter != null && !className.contains(classNameFilter)) continue

            val lines = sourceFile.getElementsByTagName("line")
            for (j in 0 until lines.length) {
                val line = lines.item(j) as? Element ?: continue
                val missedInstructions = line.getAttribute("mi").toIntOrNull() ?: 0
                val missedBranches = line.getAttribute("mb").toIntOrNull() ?: 0
                if (missedInstructions > 0 || missedBranches > 0) {
                    results += UncoveredLocation(
                        className = className,
                        lineNumber = line.getAttribute("nr").toIntOrNull() ?: -1,
                        instructionMissed = missedInstructions,
                        branchMissed = missedBranches
                    )
                }
            }
        }

        return results.sortedWith(compareByDescending<UncoveredLocation> { it.branchMissed }.thenByDescending { it.instructionMissed })
    }
}
