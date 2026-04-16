package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.SpecialFormatting

enum class MinecraftSourceMarkerKind {
    MINECRAFT_COLOR,
    MINECRAFT_FORMAT,
    HEX_COLOR
}

enum class MinecraftSourceHexFormat {
    HASH,
    HEX_PREFIX
}

data class MinecraftSourceMarker(
    val start: Int,
    val end: Int,
    val rawText: String,
    val kind: MinecraftSourceMarkerKind,
    val colorHex: String? = null,
    val formatting: SpecialFormatting? = null,
    val code: Char? = null,
    val codeIndex: Int? = null,
    val hexFormat: MinecraftSourceHexFormat? = null,
    val hasAlpha: Boolean = false
) {
    fun shifted(offset: Int): MinecraftSourceMarker = copy(
        start = start + offset,
        end = end + offset,
        codeIndex = codeIndex?.plus(offset)
    )
}
