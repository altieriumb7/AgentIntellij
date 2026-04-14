package com.example.javagenai.actions

import com.example.javagenai.services.GenerationOrchestrator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class GenerateCodeWithPatternsAction : BaseJavaGenAction("Generate code following project patterns") {
    override fun invoke(project: Project, file: VirtualFile?): String {
        if (file == null) return "No file selected."
        return project.getService(GenerationOrchestrator::class.java).generateCodeFollowingPatterns(file)
    }
}
