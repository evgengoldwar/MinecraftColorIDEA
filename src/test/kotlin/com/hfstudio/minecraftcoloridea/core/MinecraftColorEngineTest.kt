package com.hfstudio.minecraftcoloridea.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MinecraftColorEngineTest {
    private val engine = MinecraftHighlightEngine()

    @Test
    fun highlightsJavaStringsWithoutIncludingClosingQuote() {
        val text = "\"&aHello\""

        val spans = engine.highlight(
            text = text,
            languageId = "java",
            config = MinecraftColorConfig()
        )

        val green = spans.single()
        assertEquals("&aHello", text.substring(green.start, green.end))
        assertEquals("#55FF55", green.colorMarker?.colorHex)
    }

    @Test
    fun javaVersionStopsFormattingAtNextColor() {
        val text = "\"&lBold &aGreen\""

        val spans = engine.highlight(
            text = text,
            languageId = "java",
            config = MinecraftColorConfig(version = MinecraftVersion.JAVA)
        )

        val green = spans.first { text.substring(it.start, it.end).contains("Green") }
        assertEquals("#55FF55", green.colorMarker?.colorHex)
        assertFalse(green.formatting.bold)
    }

    @Test
    fun bedrockVersionKeepsFormattingAcrossColorChanges() {
        val text = "\"&lBold &aGreen\""

        val spans = engine.highlight(
            text = text,
            languageId = "java",
            config = MinecraftColorConfig(version = MinecraftVersion.BEDROCK)
        )

        val green = spans.first { text.substring(it.start, it.end).contains("Green") }
        assertEquals("#55FF55", green.colorMarker?.colorHex)
        assertTrue(green.formatting.bold)
    }

    @Test
    fun foregroundMarkerRespectsTemplateScopes() {
        val text = "`&afoo \${bar} baz`"

        val spans = engine.highlight(
            text = text,
            languageId = "javascript",
            config = MinecraftColorConfig(marker = MinecraftMarker.FOREGROUND)
        )

        val covered = spans
            .filter { it.colorMarker?.colorHex == "#55FF55" }
            .map { text.substring(it.start, it.end) }

        assertTrue("&afoo " in covered)
        assertTrue(" baz" in covered)
        assertTrue(covered.none { "bar" in it })
    }

    @Test
    fun underlineMarkerMatchesUpstreamScopeBleedBehavior() {
        val text = "`&afoo \${bar} baz`"

        val spans = engine.highlight(
            text = text,
            languageId = "javascript",
            config = MinecraftColorConfig(marker = MinecraftMarker.UNDERLINE)
        )

        val span = spans.first { it.colorMarker?.marker == MinecraftMarker.UNDERLINE }
        assertTrue(text.substring(span.start, span.end).contains("\${bar}"))
    }

    @Test
    fun fallbackTokenizerHandlesUnknownLanguages() {
        val text = "\"&aHello\""

        val spans = engine.highlight(
            text = text,
            languageId = "totally-unknown-language",
            config = MinecraftColorConfig(fallback = true)
        )

        val green = spans.single()
        assertEquals("&aHello", text.substring(green.start, green.end))
        assertEquals("#55FF55", green.colorMarker?.colorHex)
    }

    @Test
    fun newerBedrockProfileAddsVColorCode() {
        val text = "\"&vCopper\""

        val latest = engine.highlight(
            text = text,
            languageId = "java",
            config = MinecraftColorConfig(version = MinecraftVersion.BEDROCK)
        )
        val older = engine.highlight(
            text = text,
            languageId = "java",
            config = MinecraftColorConfig(version = MinecraftVersion.BEDROCK_PRE_1_21_50)
        )

        val latestSpan = latest.single()
        assertEquals("#eb7214", latestSpan.colorMarker?.colorHex)
        assertTrue(older.isEmpty())
    }

    @Test
    fun backgroundMarkerComputesContrastColor() {
        val text = "\"&0Dark\""

        val spans = engine.highlight(
            text = text,
            languageId = "java",
            config = MinecraftColorConfig(marker = MinecraftMarker.BACKGROUND)
        )

        val span = spans.single()
        val marker = assertNotNull(span.colorMarker)
        assertEquals(MinecraftMarker.BACKGROUND, marker.marker)
        assertEquals("#000000", marker.colorHex)
        assertEquals("#FFFFFF", marker.contrastHex)
    }
}
