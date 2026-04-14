package com.example.javagenai.actions

import com.example.javagenai.services.GenerationOrchestrator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class ImproveCoverageAction : BaseJavaGenAction("Improve test coverage for this file/package") {
    override fun invoke(project: Project, file: VirtualFile?): String {
        if (file == null) return "No file selected."
        return project.getService(GenerationOrchestrator::class.java).improveCoverage(file)
    }
}
