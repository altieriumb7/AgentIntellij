package com.example.javagenai.context

import com.example.javagenai.analysis.ProjectPatternProfile
import com.example.javagenai.prompt.PromptContext
import com.intellij.openapi.components.Service
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class PromptContextAssembler {
    fun assemble(
        profile: ProjectPatternProfile,
        anchorFile: VirtualFile,
        relatedFiles: List<VirtualFile>
    ): PromptContext {
        return PromptContext(
            anchorPath = anchorFile.path,
            summary = buildString {
                appendLine("Package style: ${profile.packageStyle}")
                appendLine("JUnit: ${profile.junitVersion}")
                appendLine("Roles: ${profile.architecturalRoles.joinToString()}")
                appendLine("Mocking: ${profile.mockingLibraries.joinToString()}")
                appendLine("Assertions: ${profile.assertionLibraries.joinToString()}")
            }.trim(),
            relatedPaths = relatedFiles.map { it.path }
        )
    }
}
