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

        val leftSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, generatedFilesPanel, statusPanel).apply { resizeWeight = 0.4 }
        val rightSplit = JSplitPane(JSplitPane.VERTICAL_SPLIT, errorsPanel, diffPanel).apply { resizeWeight = 0.3 }
        val mainSplit = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftSplit, rightSplit).apply { resizeWeight = 0.35 }

        project.getService(ToolWindowStateService::class.java).subscribe { state ->
            generatedFilesModel.clear()
            state.generatedFiles.forEach { generatedFilesModel.addElement(it) }
            iterationArea.text = state.iterations.joinToString("\n") { "#${it.iteration}: ${it.status}" }
            errorsArea.text = state.errors.joinToString("\n")
            diffArea.text = state.diffPreview
            summaryLabel.text = state.summary
            coverageLabel.text = state.coverageSummary
        }

        val root = JBPanel<JBPanel<*>>(BorderLayout()).apply { add(mainSplit, BorderLayout.CENTER) }
        val content = ContentFactory.getInstance().createContent(root, "Assistant", false)
        toolWindow.contentManager.addContent(content)
    }
}
