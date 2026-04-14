package com.example.javagenai.actions

import com.example.javagenai.services.GenerationOrchestrator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.Messages

class ImportRfaAction : AnAction("Import RFA") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("md", "yaml", "yml", "json")
        descriptor.title = "Import RFA File"
        descriptor.description = "Choose an RFA file (.md, .yaml, .yml, .json)"

        val file = FileChooser.chooseFile(descriptor, project, null) ?: return
        val message = project.getService(GenerationOrchestrator::class.java).importRfa(file)
        Messages.showInfoMessage(project, message, "Import RFA")
    }
}
