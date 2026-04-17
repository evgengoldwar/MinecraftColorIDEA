package com.hfstudio.minecraftcoloridea.editor

import java.awt.Color
import com.intellij.ui.ColorUtil

object MinecraftSourceMarkerEditSupport {
    internal data class NormalizedRange(
        val start: Int,
        val end: Int
    )

    internal data class ReplacementResult(
        val text: String,
        val end: Int
    )

    fun parseColor(marker: MinecraftSourceMarker): Color? {
        val hex = marker.colorHex ?: return null
        return parseColorHex(hex)
    }

    fun parseColorHex(hex: String): Color? {
        val normalized = hex.removePrefix("#")
        return when (normalized.length) {
            6 -> Color(
                normalized.substring(0, 2).toInt(16),
                normalized.substring(2, 4).toInt(16),
                normalized.substring(4, 6).toInt(16)
            )
            8 -> Color(
                normalized.substring(2, 4).toInt(16),
                normalized.substring(4, 6).toInt(16),
                normalized.substring(6, 8).toInt(16),
                normalized.substring(0, 2).toInt(16)
            )

            else -> null
        }
    }

    fun parseRenderColorHex(hex: String): Color {
        return parseColorHex(hex) ?: ColorUtil.fromHex(hex)
    }

    fun formatHexReplacement(marker: MinecraftSourceMarker, color: Color): String {
        val includeAlpha = marker.hasAlpha || color.alpha < 255
        val digits = if (includeAlpha) {
            "%02X%02X%02X%02X".format(color.alpha, color.red, color.green, color.blue)
        } else {
            "%02X%02X%02X".format(color.red, color.green, color.blue)
        }

        return when (marker.hexFormat) {
            MinecraftSourceHexFormat.HASH -> "#$digits"
            MinecraftSourceHexFormat.HEX_PREFIX -> "0x$digits"
            null -> marker.rawText
        }
    }

    fun replaceCodeChar(
        source: String,
        marker: MinecraftSourceMarker,
        code: Char
    ): String {
        val codeIndex = marker.codeIndex ?: return source
        if (codeIndex !in source.indices) {
            return source
        }
        return source.replaceRange(codeIndex, codeIndex + 1, code.toString())
    }

    internal fun normalizeRange(
        textLength: Int,
        start: Int,
        end: Int
    ): NormalizedRange {
        val safeStart = start.coerceIn(0, textLength)
        val safeEnd = end.coerceIn(safeStart, textLength)
        return NormalizedRange(start = safeStart, end = safeEnd)
    }

    internal fun replaceTextRange(
        source: String,
        start: Int,
        end: Int,
        replacement: String
    ): ReplacementResult {
        val range = normalizeRange(source.length, start, end)
        return ReplacementResult(
            text = source.replaceRange(range.start, range.end, replacement),
            end = range.start + replacement.length
        )
    }
}
