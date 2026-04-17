package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.ExtendedColorParser
import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.core.MinecraftGrammar
import com.hfstudio.minecraftcoloridea.core.MinecraftGrammars
import com.hfstudio.minecraftcoloridea.core.MinecraftToken
import com.hfstudio.minecraftcoloridea.core.MinecraftTokenizer
import com.hfstudio.minecraftcoloridea.core.MinecraftVersionRegistry
import com.hfstudio.minecraftcoloridea.core.fallbackTokenizer

class MinecraftSourceMarkerCollector {
    private data class IntRangeSpan(val start: Int, val end: Int)

    fun collect(
        text: String,
        languageId: String?,
        config: MinecraftColorConfig
    ): List<MinecraftSourceMarker> {
        if (!config.enable || text.isEmpty()) {
            return emptyList()
        }

        val grammar = MinecraftGrammars.find(languageId)
        val tokens = when {
            grammar != null -> MinecraftTokenizer(grammar).tokenize(text)
            config.fallback -> fallbackTokenizer(text, config.compiledFallbackRegex())
            else -> emptyList()
        }
        val allowedRanges = when {
            grammar != null -> grammarTokensToAllowedRanges(text.length, tokens)
            tokens.isNotEmpty() -> tokensToAllowedRanges(tokens)
            else -> emptyList()
        }
        if (allowedRanges.isEmpty()) {
            return emptyList()
        }

        val formatCodes = MinecraftVersionRegistry.profile(config)
        val markers = mutableListOf<MinecraftSourceMarker>()

        allowedRanges.forEach { range ->
            var index = range.start
            while (index < range.end) {
                val hexToken = ExtendedColorParser.matchAt(text, index)
                    ?.takeIf { index + it.length <= range.end }
                if (hexToken != null) {
                    markers += MinecraftSourceMarker(
                        start = index,
                        end = index + hexToken.length,
                        rawText = hexToken,
                        kind = MinecraftSourceMarkerKind.HEX_COLOR,
                        colorHex = ExtendedColorParser.toColorHex(hexToken).lowercase(),
                        hexFormat = if (hexToken.startsWith("#")) {
                            MinecraftSourceHexFormat.HASH
                        } else {
                            MinecraftSourceHexFormat.HEX_PREFIX
                        },
                        hasAlpha = hexToken.length == 9 || hexToken.length == 10
                    )
                    index += hexToken.length
                    continue
                }

                val markerLength = when {
                    text.startsWith("\\u00a7", index) || text.startsWith("\\u00A7", index) -> 6
                    text[index] == '\u00a7' -> 1
                    else -> 0
                }
                if (markerLength == 0 || index + markerLength >= range.end) {
                    index += 1
                    continue
                }

                val code = text[index + markerLength].lowercaseChar()
                val color = formatCodes.colors[code]
                val formatting = formatCodes.special[code]
                if (color == null && formatting == null) {
                    index += 1
                    continue
                }

                markers += MinecraftSourceMarker(
                    start = index,
                    end = index + markerLength + 1,
                    rawText = text.substring(index, index + markerLength + 1),
                    kind = if (color != null) {
                        MinecraftSourceMarkerKind.MINECRAFT_COLOR
                    } else {
                        MinecraftSourceMarkerKind.MINECRAFT_FORMAT
                    },
                    colorHex = color?.lowercase(),
                    formatting = formatting,
                    code = code,
                    codeIndex = index + markerLength
                )
                index += markerLength + 1
            }
        }

        return markers
    }

    fun collectInRegion(
        text: String,
        languageId: String?,
        config: MinecraftColorConfig,
        region: MinecraftDocumentRegion
    ): List<MinecraftSourceMarker> {
        if (region.isEmpty()) {
            return emptyList()
        }

        val scopedText = region.substring(text)
        if (scopedText.isEmpty()) {
            return emptyList()
        }

        return collect(scopedText, languageId, config)
            .map { it.shifted(region.start) }
    }

    private fun tokensToAllowedRanges(tokens: List<MinecraftToken>): List<IntRangeSpan> {
        val flattened = mutableListOf<IntRangeSpan>()

        for (token in tokens) {
            if (token.type.startsWith(MinecraftGrammar.scope())) {
                flattened += tokensToAllowedRanges(token.tokens)
                continue
            }

            if (token.tokens.isEmpty()) {
                flattened += IntRangeSpan(token.innerStart, token.innerEnd)
                continue
            }

            var current = token.innerStart
            token.tokens.forEach { child ->
                if (current < child.start) {
                    flattened += IntRangeSpan(current, child.start)
                }
                flattened += tokensToAllowedRanges(listOf(child))
                current = child.end
            }
            if (current < token.innerEnd) {
                flattened += IntRangeSpan(current, token.innerEnd)
            }
        }

        return flattened.filter { it.start < it.end }
    }

    private fun grammarTokensToAllowedRanges(textLength: Int, tokens: List<MinecraftToken>): List<IntRangeSpan> {
        if (textLength <= 0) {
            return emptyList()
        }

        if (tokens.isEmpty()) {
            return listOf(IntRangeSpan(0, textLength))
        }

        val flattened = mutableListOf<IntRangeSpan>()
        var current = 0

        tokens.forEach { token ->
            if (current < token.start) {
                flattened += IntRangeSpan(current, token.start)
            }
            flattened += tokenToAllowedRanges(token)
            current = token.end
        }

        if (current < textLength) {
            flattened += IntRangeSpan(current, textLength)
        }

        return flattened.filter { it.start < it.end }
    }

    private fun tokenToAllowedRanges(token: MinecraftToken): List<IntRangeSpan> {
        if (token.type.startsWith(MinecraftGrammar.scope())) {
            return token.tokens.flatMap(::tokenToAllowedRanges)
        }

        if (token.tokens.isEmpty()) {
            return listOf(IntRangeSpan(token.innerStart, token.innerEnd))
        }

        val flattened = mutableListOf<IntRangeSpan>()
        var current = token.innerStart

        token.tokens.forEach { child ->
            if (current < child.start) {
                flattened += IntRangeSpan(current, child.start)
            }
            flattened += tokenToAllowedRanges(child)
            current = child.end
        }

        if (current < token.innerEnd) {
            flattened += IntRangeSpan(current, token.innerEnd)
        }

        return flattened.filter { it.start < it.end }
    }
}
