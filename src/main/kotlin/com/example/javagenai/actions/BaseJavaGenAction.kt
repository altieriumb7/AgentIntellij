package com.example.javagenai.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

abstract class BaseJavaGenAction(private val actionTitle: String) : AnAction(actionTitle) {
    final override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val file = event.getData(CommonDataKeys.VIRTUAL_FILE)
        val message = invoke(project, file)
        Messages.showInfoMessage(project, message, actionTitle)
    }

    abstract fun invoke(project: Project, file: com.intellij.openapi.vfs.VirtualFile?): String
}
