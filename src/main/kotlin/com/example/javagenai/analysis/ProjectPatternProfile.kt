package com.example.javagenai.analysis

data class ProjectPatternProfile(
    val packageStyle: String,
    val classNamePatterns: List<String>,
    val testNamePatterns: List<String>,
    val junitVersion: String,
    val mockingLibraries: Set<String>,
    val assertionLibraries: Set<String>,
    val fixtureStyleHints: List<String>,
    val commonAnnotations: Set<String>,
    val commonImports: Set<String>,
    val javaVersionSignals: Set<String>,
    val architecturalRoles: Set<String>
)
