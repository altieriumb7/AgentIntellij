package com.example.javagenai.context

import com.example.javagenai.analysis.RelatedFileRanker
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope

@Service(Service.Level.PROJECT)
class JavaContextCollector(private val project: Project) {
    private val ranker = RelatedFileRanker(project)

    fun collectRelatedJavaFiles(anchorFile: VirtualFile): List<VirtualFile> {
        val allJavaFiles = FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.projectScope(project)).toList()
        return ranker.rank(anchorFile, allJavaFiles)
    }
}
