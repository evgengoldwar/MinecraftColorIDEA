package com.hfstudio.minecraftcoloridea.editor

import java.awt.Color

object MinecraftSourceMarkerEditSupport {
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
}
