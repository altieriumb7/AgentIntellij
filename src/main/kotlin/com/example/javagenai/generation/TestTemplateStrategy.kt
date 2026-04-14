package com.example.javagenai.generation

import com.example.javagenai.analysis.ProjectPatternProfile
import com.intellij.openapi.vfs.VirtualFile

class TestTemplateStrategy {
    fun targetTestPath(projectBasePath: String, sourceFile: VirtualFile, profile: ProjectPatternProfile): String {
        val sourcePath = sourceFile.path
        val testPath = sourcePath
            .replace("/src/main/java/", "/src/test/java/")
            .removeSuffix(".java") + "Test.java"

        if (testPath != sourcePath) return testPath

        val inferredPackage = profile.packageStyle.takeIf { it.isNotBlank() }
            ?.replace('.', '/')
            ?: ""
        return "$projectBasePath/src/test/java/$inferredPackage/${sourceFile.nameWithoutExtension}Test.java"
    }
}
