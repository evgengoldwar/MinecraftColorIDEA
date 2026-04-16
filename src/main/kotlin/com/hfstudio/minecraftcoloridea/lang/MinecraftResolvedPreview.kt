package com.hfstudio.minecraftcoloridea.lang

data class MinecraftResolvedPreview(
    val anchorOffset: Int,
    val previewText: String,
    val excludedSourceRanges: List<IntRange>,
    val referencedKeys: Set<String>,
    val baseColorHex: String? = null
)
