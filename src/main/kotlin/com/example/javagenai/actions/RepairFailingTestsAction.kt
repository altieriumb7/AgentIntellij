package com.example.javagenai.actions

import com.example.javagenai.services.GenerationOrchestrator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class RepairFailingTestsAction : BaseJavaGenAction("Repair failing JUnit tests") {
    override fun invoke(project: Project, file: VirtualFile?): String {
        if (file == null) return "No file selected."
        return project.getService(GenerationOrchestrator::class.java).repairFailingTests(file)
    }
}
