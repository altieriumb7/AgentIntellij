package com.example.javagenai.coverage

data class CoverageSnapshot(
    val lineCoveragePct: Double,
    val branchCoveragePct: Double,
    val coveredLines: Int,
    val totalLines: Int,
    val coveredBranches: Int,
    val totalBranches: Int,
    val reportPath: String? = null
)

data class UncoveredLocation(
    val className: String,
    val lineNumber: Int,
    val instructionMissed: Int,
    val branchMissed: Int
)

data class CoverageDelta(
    val before: CoverageSnapshot,
    val after: CoverageSnapshot,
    val lineDeltaPct: Double,
    val branchDeltaPct: Double,
    val uncovered: List<UncoveredLocation>
)
