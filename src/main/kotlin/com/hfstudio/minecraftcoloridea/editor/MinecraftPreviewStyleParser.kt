package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.ExtendedColorParser
import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.core.FormattingState
import com.hfstudio.minecraftcoloridea.core.MinecraftVersion
import com.hfstudio.minecraftcoloridea.core.MinecraftVersionRegistry
import com.hfstudio.minecraftcoloridea.core.SpecialFormatting

data class MinecraftPreviewStyleSegment(
    val text: String,
    val colorHex: String?,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val obfuscated: Boolean = false
)

class MinecraftPreviewStyleParser {
    fun parse(
        previewText: String,
        baseColorHex: String?,
        baseFormatting: FormattingState = FormattingState()
    ): List<MinecraftPreviewStyleSegment> {
        if (previewText.isEmpty()) {
            return emptyList()
        }

        val formatCodes = MinecraftVersionRegistry.profile(MinecraftColorConfig(version = MinecraftVersion.JAVA))
        val segments = mutableListOf<MinecraftPreviewStyleSegment>()
        val buffer = StringBuilder()

        var colorHex = baseColorHex?.lowercase()
        var bold = baseFormatting.bold
        var italic = baseFormatting.italic
        var underline = baseFormatting.underline
        var strikethrough = baseFormatting.strikethrough
        var obfuscated = baseFormatting.obfuscated
        var index = 0

        fun flush() {
            if (buffer.isEmpty()) {
                return
            }

            segments += MinecraftPreviewStyleSegment(
                text = buffer.toString(),
                colorHex = colorHex,
                bold = bold,
                italic = italic,
                underline = underline,
                strikethrough = strikethrough,
                obfuscated = obfuscated
            )
            buffer.setLength(0)
        }

        while (index < previewText.length) {
            val hexColor = ExtendedColorParser.matchAt(previewText, index)
            if (hexColor != null) {
                flush()
                colorHex = ExtendedColorParser.toColorHex(hexColor).lowercase()
                bold = false
                italic = false
                underline = false
                strikethrough = false
                obfuscated = false
                index += hexColor.length
                continue
            }

            val markerLength = when {
                previewText.startsWith("\\u00a7", index) || previewText.startsWith("\\u00A7", index) -> 6
                previewText[index] == '\u00a7' -> 1
                else -> 0
            }
            if (markerLength > 0 && index + markerLength < previewText.length) {
                val code = previewText[index + markerLength].lowercaseChar()
                val nextColor = formatCodes.colors[code]
                val nextFormatting = formatCodes.special[code]

                if (nextColor != null || nextFormatting != null) {
                    flush()
                    if (nextColor != null) {
                        colorHex = nextColor.lowercase()
                        bold = false
                        italic = false
                        underline = false
                        strikethrough = false
                        obfuscated = false
                    } else {
                        when (nextFormatting) {
                            SpecialFormatting.BOLD -> bold = true
                            SpecialFormatting.ITALIC -> italic = true
                            SpecialFormatting.UNDERLINE -> underline = true
                            SpecialFormatting.STRIKETHROUGH -> strikethrough = true
                            SpecialFormatting.UNDERLINE_STRIKETHROUGH -> {
                                underline = true
                                strikethrough = true
                            }

                            SpecialFormatting.OBFUSCATED -> obfuscated = true
                            SpecialFormatting.RESET -> {
                                colorHex = baseColorHex?.lowercase()
                                bold = false
                                italic = false
                                underline = false
                                strikethrough = false
                                obfuscated = false
                            }

                            null -> Unit
                        }
                    }

                    index += markerLength + 1
                    continue
                }
            }

            buffer.append(previewText[index])
            index += 1
        }

        flush()
        return segments
    }
}
