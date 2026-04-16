package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.FormattingState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MinecraftPreviewStyleParserTest {
    @Test
    fun parsesInternalColorAndItalicFormattingSegments() {
        val segments = MinecraftPreviewStyleParser().parse(
            previewText = "Base \\u00a7cRed \\u00a7oItalic",
            baseColorHex = "#55ff55",
            baseFormatting = FormattingState()
        )

        assertEquals(3, segments.size)
        assertEquals("Base ", segments[0].text)
        assertEquals("#55ff55", segments[0].colorHex)
        assertFalse(segments[0].italic)

        assertEquals("Red ", segments[1].text)
        assertEquals("#ff5555", segments[1].colorHex)
        assertFalse(segments[1].italic)

        assertEquals("Italic", segments[2].text)
        assertEquals("#ff5555", segments[2].colorHex)
        assertTrue(segments[2].italic)
    }

    @Test
    fun resetRestoresDefaultColorAndFormatting() {
        val segments = MinecraftPreviewStyleParser().parse(
            previewText = "\\u00a7cRed\\u00a7rNormal",
            baseColorHex = "#55ff55",
            baseFormatting = FormattingState()
        )

        assertEquals(2, segments.size)
        assertEquals("Red", segments[0].text)
        assertEquals("#ff5555", segments[0].colorHex)
        assertEquals("Normal", segments[1].text)
        assertEquals("#55ff55", segments[1].colorHex)
        assertFalse(segments[1].bold)
        assertFalse(segments[1].italic)
    }

    @Test
    fun keepsBaseFormattingUntilReset() {
        val segments = MinecraftPreviewStyleParser().parse(
            previewText = "Styled\\u00a7rPlain",
            baseColorHex = "#ffffff",
            baseFormatting = FormattingState(
                bold = true,
                italic = true
            )
        )

        assertEquals(2, segments.size)
        assertEquals("Styled", segments[0].text)
        assertTrue(segments[0].bold)
        assertTrue(segments[0].italic)

        assertEquals("Plain", segments[1].text)
        assertFalse(segments[1].bold)
        assertFalse(segments[1].italic)
    }
}
