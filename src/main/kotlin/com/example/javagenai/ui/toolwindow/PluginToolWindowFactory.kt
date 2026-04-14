package com.example.javagenai.ui.toolwindow

import com.example.javagenai.ui.model.ToolWindowStateService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JTextArea
import javax.swing.JSplitPane

class PluginToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val generatedFilesModel = DefaultListModel<String>()
        val generatedFilesList = JList(generatedFilesModel)
        val iterationArea = JTextArea().apply { isEditable = false }
        val errorsArea = JTextArea().apply { isEditable = false }
        val diffArea = JTextArea().apply { isEditable = false }
        val summaryLabel = JBLabel("Idle")
        val coverageLabel = JBLabel("Coverage: n/a")

        val rfaPathLabel = JBLabel("RFA: none")
        val rfaSummaryArea = JTextArea().apply { isEditable = false }
        val rfaWarningsArea = JTextArea().apply { isEditable = false }
        val rfaPlanArea = JTextArea().apply { isEditable = false }

        val generatedFilesPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Generated / Updated Files")
            add(JBScrollPane(generatedFilesList), BorderLayout.CENTER)
        }

        val statusPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Iterations, Repairs, and Summary")
            val text = JTextArea().apply { isEditable = false }
            text.document = iterationArea.document
            val top = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(summaryLabel, BorderLayout.NORTH)
                add(coverageLabel, BorderLayout.SOUTH)
            }
            add(top, BorderLayout.NORTH)
            add(JBScrollPane(text), BorderLayout.CENTER)
        }

        val errorsPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Errors / Remaining Gaps")
            add(JBScrollPane(errorsArea), BorderLayout.CENTER)
        }

        val diffPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Diff Preview (review before apply)")
            add(JBScrollPane(diffArea), BorderLayout.CENTER)
        }

        val rfaPanel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("RFA Import & Plan")
            val text = JTextArea().apply { isEditable = false }
            val rfaTop = JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(rfaPathLabel, BorderLayout.NORTH)
                add(JBScrollPane(rfaSummaryArea), BorderLayout.CENTER)
            }
            text.document = rfaPlanArea.document
            val lower = JSplitPane(JSplitPane.VERTICAL_SPLIT, JBScrollPane(rfaWarningsArea), JBScrollPane(text)).apply {
                resizeWeight = 0.35
            }
            add(rfaTop, BorderLayout.NORTH)
            add(lower, BorderLayout.CENTER)
        }

        val leftUpper = JSplitPane(JSplitPane.VERTICAL_SPLIT, generatedFilesPanel, statusPanel).apply { resizeWeight = 0.4 }
        val leftSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, leftUpper, rfaPanel).apply { resizeWeight = 0.65 }

        val rightSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, errorsPanel, diffPanel).apply { resizeWeight = 0.3 }
        val mainSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightSplit).apply { resizeWeight = 0.45 }

        project.getService(ToolWindowStateService::class.java).subscribe { state ->
            generatedFilesModel.clear()
            state.generatedFiles.forEach { generatedFilesModel.addElement(it) }
            iterationArea.text = state.iterations.joinToString("\n") { "#${it.iteration}: ${it.status}" }
            errorsArea.text = state.errors.joinToString("\n")
            diffArea.text = state.diffPreview
            summaryLabel.text = state.summary
            coverageLabel.text = state.coverageSummary

            rfaPathLabel.text = "RFA: ${state.importedRfaPath ?: "none"}"
            rfaSummaryArea.text = state.rfaSummary
            rfaWarningsArea.text = if (state.rfaWarnings.isEmpty()) "Warnings: none" else "Warnings:\n${state.rfaWarnings.joinToString("\n")}" +
                if (state.rfaAmbiguities.isNotEmpty()) "\n\nAmbiguities:\n${state.rfaAmbiguities.joinToString("\n")}" else ""

            val planText = state.rfaPlan?.let { plan ->
                buildString {
                    appendLine(plan.title)
                    appendLine(plan.summary)
                    appendLine("--- Planned changes ---")
                    plan.changes.forEachIndexed { idx, ch ->
                        appendLine("${idx + 1}. [${ch.layer}] ${ch.action} -> ${ch.targetPath}")
                        appendLine("   rationale: ${ch.rationale}")
                        if (ch.patternMatches.isNotEmpty()) {
                            appendLine("   similar: ${ch.patternMatches.joinToString { it.filePath }}")
                        }
                    }
                }
            } ?: "Plan: not built"
            rfaPlanArea.text = planText
        }

        val root = JBPanel<JBPanel<*>>(BorderLayout()).apply { add(mainSplit, BorderLayout.CENTER) }
        val content = ContentFactory.getInstance().createContent(root, "Assistant", false)
        toolWindow.contentManager.addContent(content)
    }
}
