package com.hfstudio.minecraftcoloridea.lang

import com.hfstudio.minecraftcoloridea.core.FormattingState

data class MinecraftResolvedPreview(
    val anchorOffset: Int,
    val previewText: String,
    val excludedSourceRanges: List<IntRange>,
    val referencedKeys: Set<String>,
    val baseColorHex: String? = null,
    val baseFormatting: FormattingState = FormattingState()
)
