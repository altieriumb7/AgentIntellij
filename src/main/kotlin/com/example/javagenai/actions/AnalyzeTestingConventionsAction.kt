package com.example.javagenai.actions

import com.example.javagenai.services.GenerationOrchestrator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class AnalyzeTestingConventionsAction : BaseJavaGenAction("Analyze project testing conventions") {
    override fun invoke(project: Project, file: VirtualFile?): String {
        return project.getService(GenerationOrchestrator::class.java).analyzeTestingConventions()
    }
}
