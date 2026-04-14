package com.example.javagenai.rfa.service

import com.example.javagenai.rfa.model.RfaSpec
import com.example.javagenai.rfa.parser.JsonRfaParser
import com.example.javagenai.rfa.parser.MarkdownRfaParser
import com.example.javagenai.rfa.parser.RfaNormalizer
import com.example.javagenai.rfa.parser.RfaParser
import com.example.javagenai.rfa.parser.YamlRfaParser
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore

@Service(Service.Level.PROJECT)
class RfaImportService(private val project: Project) {
    private val parsers: List<RfaParser> = listOf(JsonRfaParser(), YamlRfaParser(), MarkdownRfaParser())
    private val normalizer = RfaNormalizer()

    fun import(file: VirtualFile): RfaSpec {
        val extension = file.extension.orEmpty().lowercase()
        val parser = parsers.firstOrNull { it.supports(extension) }
            ?: return RfaSpec(
                featureName = null,
                module = null,
                endpoint = null,
                requestDto = null,
                responseDto = null,
                returnType = null,
                validations = emptyList(),
                businessNotes = emptyList(),
                affectedLayers = emptySet(),
                freeTextNotes = emptyList(),
                sourceFilePath = file.path,
                parseWarnings = listOf("Unsupported RFA format: .$extension. Supported: .md, .yaml, .yml, .json")
            )

        val raw = parser.parse(VfsUtilCore.loadText(file), file.path)
        return normalizer.normalize(raw)
    }
}
