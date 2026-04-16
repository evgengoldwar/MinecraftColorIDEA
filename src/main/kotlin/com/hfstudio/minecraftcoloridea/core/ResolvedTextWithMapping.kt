package com.hfstudio.minecraftcoloridea.core

data class ResolvedTextWithMapping(
    val text: String,
    val originalOffsets: IntArray
) {
    fun toOriginalOffset(normalizedOffset: Int): Int {
        return originalOffsets[normalizedOffset.coerceIn(0, originalOffsets.lastIndex)]
    }

    fun toOriginalExclusiveOffset(normalizedExclusiveOffset: Int, originalLength: Int): Int {
        return if (normalizedExclusiveOffset >= originalOffsets.size) {
            originalLength
        } else {
            originalOffsets[normalizedExclusiveOffset]
        }
    }
}
