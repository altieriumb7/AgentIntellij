package com.example.javagenai.actions

import com.example.javagenai.services.GenerationOrchestrator
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class BuildRfaImplementationPlanAction : AnAction("Build Implementation Plan from RFA") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val message = project.getService(GenerationOrchestrator::class.java).buildImplementationPlanFromImportedRfa()
        Messages.showInfoMessage(project, message, "RFA Implementation Plan")
    }
}
