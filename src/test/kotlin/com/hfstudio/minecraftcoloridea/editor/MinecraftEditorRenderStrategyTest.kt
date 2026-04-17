package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.FormattingState
import com.hfstudio.minecraftcoloridea.lang.MinecraftCollectedPreview
import com.hfstudio.minecraftcoloridea.lang.MinecraftResolvedPreview
import com.intellij.openapi.editor.impl.DocumentImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MinecraftEditorRenderStrategyTest {
    @Test
    fun limitsVeryLargeFilesToVisibleRegion() {
        assertTrue(MinecraftEditorRenderStrategy.shouldLimitToVisibleRegion(5_000_000, 79_542))
        assertTrue(MinecraftEditorRenderStrategy.shouldLimitToVisibleRegion(250_000, 25_000))
        assertFalse(MinecraftEditorRenderStrategy.shouldLimitToVisibleRegion(10_000, 400))
    }

    @Test
    fun visibleRegionExpandsByBufferedLines() {
        val text = (0 until 500).joinToString("\n") { "line-$it" }
        val document = DocumentImpl(text)

        val region = MinecraftEditorRenderStrategy.visibleLineRegion(
            document = document,
            visibleStartLine = 200,
            visibleEndLine = 210,
            lineBuffer = 20
        )

        assertEquals(document.getLineStartOffset(180), region.start)
        assertEquals(document.getLineEndOffset(230), region.endExclusive)
    }

    @Test
    fun limitedRenderRegionUsesCachedVisibleLinesForLargeFiles() {
        val text = (0 until 500).joinToString("\n") { "line-$it" }
        val document = DocumentImpl(text)

        val region = MinecraftEditorRenderStrategy.limitedRenderRegion(
            document = document,
            textLength = 5_000_000,
            lineCount = document.lineCount,
            visibleLines = MinecraftVisibleLineRange(startLine = 200, endLine = 210)
        )

        assertEquals(document.getLineStartOffset(80), region!!.start)
        assertEquals(document.getLineEndOffset(330), region.endExclusive)
    }

    @Test
    fun limitedRenderRegionFallsBackToFirstVisibleLineWhenCacheIsMissing() {
        val text = (0 until 500).joinToString("\n") { "line-$it" }
        val document = DocumentImpl(text)

        val region = MinecraftEditorRenderStrategy.limitedRenderRegion(
            document = document,
            textLength = 5_000_000,
            lineCount = document.lineCount,
            visibleLines = null
        )

        assertEquals(document.getLineStartOffset(0), region!!.start)
        assertEquals(document.getLineEndOffset(120), region.endExclusive)
    }

    @Test
    fun shiftsCollectedPreviewOffsetsIntoDocumentCoordinates() {
        val previews = listOf(
            MinecraftCollectedPreview(
                anchorOffset = 12,
                preview = MinecraftResolvedPreview(
                    anchorOffset = 12,
                    previewText = "Backpack",
                    excludedSourceRanges = emptyList(),
                    referencedKeys = setOf("tooltip.backpack"),
                    baseColorHex = "#ffff55",
                    baseFormatting = FormattingState(bold = true)
                )
            )
        )

        val shifted = MinecraftEditorRenderStrategy.shiftPreviews(
            previews = previews,
            regionStart = 300
        )

        assertEquals(312, shifted.single().anchorOffset)
        assertEquals("Backpack", shifted.single().previewText)
        assertEquals("#ffff55", shifted.single().baseColorHex)
        assertEquals(FormattingState(bold = true), shifted.single().baseFormatting)
    }
}
