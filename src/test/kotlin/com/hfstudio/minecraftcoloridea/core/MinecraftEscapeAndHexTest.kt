package com.hfstudio.minecraftcoloridea.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinecraftEscapeAndHexTest {
    private val engine = MinecraftHighlightEngine()

    @Test
    fun highlightsUnicodeEscapedSectionSignLikeLiteralSectionSign() {
        val lower = engine.highlight(
            text = "\"\\u00a7eHello\"",
            languageId = "java",
            config = MinecraftColorConfig()
        )
        val upper = engine.highlight(
            text = "\"\\u00A7eHello\"",
            languageId = "java",
            config = MinecraftColorConfig()
        )

        assertEquals("#ffff55", lower.single().colorMarker?.colorHex)
        assertEquals("#ffff55", upper.single().colorMarker?.colorHex)
    }

    @Test
    fun supportsHashArgbAndOxRgbColorsInSourceText() {
        val hashSpans = engine.highlight(
            text = "\"#80FFAA00Hello\"",
            languageId = "java",
            config = MinecraftColorConfig()
        )
        val oxSpans = engine.highlight(
            text = "\"0xFFAA00Hello\"",
            languageId = "java",
            config = MinecraftColorConfig()
        )

        assertTrue(hashSpans.isNotEmpty())
        assertTrue(oxSpans.isNotEmpty())
        assertEquals("#80FFAA00", hashSpans.single().colorMarker?.colorHex)
        assertEquals("#FFAA00", oxSpans.single().colorMarker?.colorHex)
    }
}
