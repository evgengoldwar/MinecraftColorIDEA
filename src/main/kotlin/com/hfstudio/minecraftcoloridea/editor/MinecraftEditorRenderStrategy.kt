package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.lang.MinecraftCollectedPreview
import com.hfstudio.minecraftcoloridea.lang.MinecraftResolvedPreview
import com.intellij.openapi.editor.Document

data class MinecraftVisibleLineRange(
    val startLine: Int,
    val endLine: Int
)

object MinecraftEditorRenderStrategy {
    internal const val LARGE_FILE_TEXT_LENGTH_THRESHOLD = 1_000_000
    internal const val LARGE_FILE_LINE_COUNT_THRESHOLD = 20_000
    internal const val VISIBLE_LINE_BUFFER = 120

    fun shouldLimitToVisibleRegion(textLength: Int, lineCount: Int): Boolean {
        return textLength >= LARGE_FILE_TEXT_LENGTH_THRESHOLD || lineCount >= LARGE_FILE_LINE_COUNT_THRESHOLD
    }

    fun visibleLineRegion(
        document: Document,
        visibleStartLine: Int,
        visibleEndLine: Int,
        lineBuffer: Int = VISIBLE_LINE_BUFFER
    ): MinecraftDocumentRegion {
        if (document.textLength == 0 || document.lineCount <= 0) {
            return MinecraftDocumentRegion(0, 0)
        }

        val startLine = (visibleStartLine - lineBuffer).coerceAtLeast(0)
        val endLine = (visibleEndLine + lineBuffer).coerceAtMost(document.lineCount - 1)
        return MinecraftDocumentRegion(
            start = document.getLineStartOffset(startLine),
            endExclusive = document.getLineEndOffset(endLine)
        )
    }

    fun limitedRenderRegion(
        document: Document,
        textLength: Int,
        lineCount: Int,
        visibleLines: MinecraftVisibleLineRange?,
        lineBuffer: Int = VISIBLE_LINE_BUFFER
    ): MinecraftDocumentRegion? {
        if (!shouldLimitToVisibleRegion(textLength, lineCount)) {
            return null
        }

        val effectiveVisibleLines = visibleLines ?: MinecraftVisibleLineRange(0, 0)
        return visibleLineRegion(
            document = document,
            visibleStartLine = effectiveVisibleLines.startLine,
            visibleEndLine = effectiveVisibleLines.endLine,
            lineBuffer = lineBuffer
        )
    }

    fun visibleLineRange(
        lineCount: Int,
        visibleStartLine: Int,
        visibleEndLine: Int
    ): MinecraftVisibleLineRange? {
        if (lineCount <= 0) {
            return null
        }

        val lastLine = lineCount - 1
        val startLine = visibleStartLine.coerceIn(0, lastLine)
        val endLine = visibleEndLine.coerceIn(startLine, lastLine)
        return MinecraftVisibleLineRange(startLine, endLine)
    }

    fun shiftPreviews(
        previews: List<MinecraftCollectedPreview>,
        regionStart: Int
    ): List<MinecraftResolvedPreview> {
        if (regionStart == 0) {
            return previews.map(MinecraftCollectedPreview::preview)
        }

        return previews.map { collected ->
            collected.preview.copy(anchorOffset = collected.preview.anchorOffset + regionStart)
        }
    }
}
