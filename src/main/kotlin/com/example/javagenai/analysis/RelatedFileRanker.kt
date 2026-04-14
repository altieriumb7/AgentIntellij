package com.example.javagenai.analysis

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class RelatedFileRanker(private val project: Project) {
    fun rank(anchorFile: VirtualFile, candidates: List<VirtualFile>): List<VirtualFile> {
        val anchorSegments = anchorFile.path.split('/')
        return candidates
            .asSequence()
            .filter { it != anchorFile }
            .sortedByDescending { commonPrefixSize(anchorSegments, it.path.split('/')) }
            .take(20)
            .toList()
    }

    private fun commonPrefixSize(a: List<String>, b: List<String>): Int =
        a.zip(b).takeWhile { it.first == it.second }.count()
}
