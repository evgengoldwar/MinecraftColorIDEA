package com.hfstudio.minecraftcoloridea.editor

import java.awt.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftSourceMarkerEditSupportTest {
    @Test
    fun formatsHashColorsAndPromotesAlphaWhenNeeded() {
        val sixDigit = MinecraftSourceMarker(
            start = 0,
            end = 7,
            rawText = "#FFFFFF",
            kind = MinecraftSourceMarkerKind.HEX_COLOR,
            colorHex = "#ffffff",
            hexFormat = MinecraftSourceHexFormat.HASH,
            hasAlpha = false
        )
        val eightDigit = sixDigit.copy(
            rawText = "#FFFFFFFF",
            end = 9,
            hasAlpha = true
        )

        assertEquals("#112233", MinecraftSourceMarkerEditSupport.formatHexReplacement(sixDigit, Color(0x11, 0x22, 0x33)))
        assertEquals("#80112233", MinecraftSourceMarkerEditSupport.formatHexReplacement(sixDigit, Color(0x11, 0x22, 0x33, 0x80)))
        assertEquals("#FF112233", MinecraftSourceMarkerEditSupport.formatHexReplacement(eightDigit, Color(0x11, 0x22, 0x33)))
    }

    @Test
    fun formatsOxColorsAndReplacesMinecraftCodeCharacters() {
        val hexMarker = MinecraftSourceMarker(
            start = 0,
            end = 8,
            rawText = "0xC0C0C0",
            kind = MinecraftSourceMarkerKind.HEX_COLOR,
            colorHex = "#c0c0c0",
            hexFormat = MinecraftSourceHexFormat.HEX_PREFIX,
            hasAlpha = false
        )
        val codeMarker = MinecraftSourceMarker(
            start = 1,
            end = 8,
            rawText = "\\u00a7e",
            kind = MinecraftSourceMarkerKind.MINECRAFT_COLOR,
            colorHex = "#ffff55",
            code = 'e',
            codeIndex = 7
        )

        assertEquals("0x40ABCDEF", MinecraftSourceMarkerEditSupport.formatHexReplacement(hexMarker, Color(0xAB, 0xCD, 0xEF, 0x40)))
        assertEquals("\"\\u00a7cText\"", MinecraftSourceMarkerEditSupport.replaceCodeChar("\"\\u00a7eText\"", codeMarker, 'c'))
    }
}
