package com.hfstudio.minecraftcoloridea.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinecraftDocumentChangeRegionTest {
    @Test
    fun expandsSingleLineChangeToWholeCurrentLine() {
        val text = "first\nsecond #FFFFFF\nthird"

        val region = MinecraftDocumentChangeRegion.fromTextChange(
            text = text,
            offset = text.indexOf("#"),
            oldLength = 0,
            newLength = 7
        )

        assertEquals("second #FFFFFF", region.substring(text))
    }

    @Test
    fun keepsMergedLineWhenTextWasDeleted() {
        val text = "first line\nsecond line"

        val region = MinecraftDocumentChangeRegion.fromTextChange(
            text = text,
            offset = text.indexOf("line"),
            oldLength = 4,
            newLength = 0
        )

        assertEquals("first line", region.substring(text))
    }

    @Test
    fun expandsAcrossAllInsertedLines() {
        val text = "before\nalpha\nbeta\nafter"

        val region = MinecraftDocumentChangeRegion.fromTextChange(
            text = text,
            offset = text.indexOf("alpha"),
            oldLength = 0,
            newLength = "alpha\nbeta".length
        )

        assertEquals("alpha\nbeta", region.substring(text))
    }

    @Test
    fun collapsedRangeAtLineEndStillOverlapsEditedLine() {
        val text = "prefix \nnext"

        val region = MinecraftDocumentChangeRegion.fromTextChange(
            text = text,
            offset = "prefix ".length,
            oldLength = "0xFF99FF".length,
            newLength = 0
        )

        assertEquals(true, region.overlaps(region.endExclusive, region.endExclusive))
    }

    @Test
    fun largeDocumentTrailingDeletionStillClearsCollapsedLineEndMarker() {
        val text = buildString {
            repeat(4_000) { index ->
                append("line ")
                append(index)
                append('\n')
            }
            append("tail ")
        }

        val region = MinecraftDocumentChangeRegion.fromTextChange(
            text = text,
            offset = text.length,
            oldLength = "0xFF99FF".length,
            newLength = 0
        )

        assertTrue(region.overlaps(region.endExclusive, region.endExclusive))
        assertEquals("tail ", region.substring(text))
    }
}
