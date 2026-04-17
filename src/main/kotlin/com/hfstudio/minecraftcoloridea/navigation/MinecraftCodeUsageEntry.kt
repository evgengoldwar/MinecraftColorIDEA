package com.hfstudio.minecraftcoloridea.navigation

enum class MinecraftCodeUsageConfidence {
    EXACT,
    ENUMERATED,
    FAMILY_DERIVED
}

data class MinecraftCodeUsageEntry(
    val key: String,
    val filePath: String,
    val lineNumber: Int,
    val lineStartOffset: Int,
    val matchStartOffset: Int,
    val matchEndOffset: Int,
    val snippet: String,
    val confidence: MinecraftCodeUsageConfidence
)
