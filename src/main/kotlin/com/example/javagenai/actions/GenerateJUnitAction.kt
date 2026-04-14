package com.example.javagenai.actions

import com.example.javagenai.services.GenerationOrchestrator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class GenerateJUnitAction : BaseJavaGenAction("Generate JUnit for this class") {
    override fun invoke(project: Project, file: VirtualFile?): String {
        if (file == null) return "No file selected."
        return project.getService(GenerationOrchestrator::class.java).generateJUnit(file)
    }
}
