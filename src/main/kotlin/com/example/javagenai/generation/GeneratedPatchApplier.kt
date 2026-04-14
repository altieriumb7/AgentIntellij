package com.example.javagenai.generation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

data class PatchPlan(
    val targetPath: String,
    val oldText: String,
    val newText: String,
    val diffPreview: String
)

class GeneratedPatchApplier(private val project: Project) {

    fun buildPlan(targetPath: String, newText: String): PatchPlan {
        val file = File(targetPath)
        val oldText = if (file.exists()) file.readText() else ""
        return PatchPlan(targetPath, oldText, newText, createSimpleDiff(oldText, newText, targetPath))
    }

    fun apply(plan: PatchPlan): VirtualFile? {
        var written: VirtualFile? = null
        WriteCommandAction.runWriteCommandAction(project) {
            val ioFile = File(plan.targetPath)
            ioFile.parentFile?.mkdirs()
            ioFile.writeText(plan.newText)
            written = ApplicationManager.getApplication().runReadAction<VirtualFile?> {
                LocalFileSystem.getInstance().refreshAndFindFileByIoFile(ioFile)
            }
            written?.let { VfsUtil.markDirtyAndRefresh(false, true, true, it) }
        }
        return written
    }

    private fun createSimpleDiff(oldText: String, newText: String, path: String): String {
        val oldLines = oldText.lines()
        val newLines = newText.lines()
        val sb = StringBuilder()
        sb.appendLine("--- a/$path")
        sb.appendLine("+++ b/$path")
        val max = maxOf(oldLines.size, newLines.size)
        for (i in 0 until max) {
            val old = oldLines.getOrNull(i)
            val new = newLines.getOrNull(i)
            when {
                old == new && old != null -> sb.appendLine(" $old")
                old != null -> sb.appendLine("-$old")
            }
            if (old != new && new != null) {
                sb.appendLine("+$new")
            }
        }
        return sb.toString()
    }
}
