package com.hfstudio.minecraftcoloridea.editor

data class MinecraftDocumentChange(
    val offset: Int,
    val oldLength: Int,
    val newLength: Int
)

data class MinecraftDocumentRegion(
    val start: Int,
    val endExclusive: Int
) {
    fun substring(text: String): String {
        if (start >= endExclusive || text.isEmpty()) {
            return ""
        }
        return text.substring(start, endExclusive.coerceAtMost(text.length))
    }

    fun overlaps(startOffset: Int, endOffset: Int): Boolean {
        return if (isEmpty()) {
            start in startOffset..endOffset
        } else {
            start < endOffset && endExclusive > startOffset
        }
    }

    fun isEmpty(): Boolean = start >= endExclusive
}

object MinecraftDocumentChangeRegion {
    fun fromTextChange(
        text: String,
        offset: Int,
        oldLength: Int,
        newLength: Int
    ): MinecraftDocumentRegion {
        if (text.isEmpty()) {
            return MinecraftDocumentRegion(0, 0)
        }

        val normalizedOffset = offset.coerceIn(0, text.length)
        val lastIndex = text.lastIndex
        val anchorStart = normalizedOffset.coerceAtMost(lastIndex)
        val candidateEnd = if (newLength > 0) {
            normalizedOffset + newLength - 1
        } else if (normalizedOffset >= text.length) {
            lastIndex
        } else {
            normalizedOffset
        }
        val anchorEnd = candidateEnd.coerceIn(anchorStart, lastIndex)

        val lineStart = text.lastIndexOf('\n', anchorStart - 1).let { newline ->
            if (newline < 0) 0 else newline + 1
        }
        val lineEndExclusive = text.indexOf('\n', anchorEnd).let { newline ->
            if (newline < 0) text.length else newline
        }

        return MinecraftDocumentRegion(lineStart, lineEndExclusive)
    }
}
