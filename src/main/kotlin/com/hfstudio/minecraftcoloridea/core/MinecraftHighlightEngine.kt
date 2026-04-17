package com.hfstudio.minecraftcoloridea.core

data class ColorMarkerInfo(
    val marker: MinecraftMarker,
    val colorHex: String,
    val contrastHex: String? = null
)

data class FormattingState(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strikethrough: Boolean = false,
    val obfuscated: Boolean = false
) {
    fun hasFormatting(): Boolean = bold || italic || underline || strikethrough || obfuscated
}

data class ResolvedHighlightSpan(
    val start: Int,
    val end: Int,
    val colorMarker: ColorMarkerInfo? = null,
    val formatting: FormattingState = FormattingState()
)

class MinecraftHighlightEngine {
    private sealed interface ExtractionValue
    private data class ColorValue(val hex: String) : ExtractionValue
    private data class SpecialValue(val formatting: SpecialFormatting) : ExtractionValue

    private data class Extraction(
        val start: Int,
        var end: Int,
        var value: ExtractionValue
    )

    private data class IntRangeSpan(val start: Int, val end: Int)

    private data class ScopeRange(
        val start: Int,
        val end: Int,
        val ranges: MutableList<IntRangeSpan> = mutableListOf()
    )

    fun highlight(text: String, languageId: String?, config: MinecraftColorConfig): List<ResolvedHighlightSpan> {
        if (!config.enable || text.isEmpty()) {
            return emptyList()
        }

        val grammar = MinecraftGrammars.find(languageId)
        val tokens = when {
            grammar != null -> MinecraftTokenizer(grammar).tokenize(text)
            config.fallback -> fallbackTokenizer(
                text,
                fallbackDelimiterRegexes(languageId, config.compiledFallbackRegex())
            )
            else -> emptyList()
        }

        if (tokens.isEmpty()) {
            return emptyList()
        }

        val scopes = tokensToScopeRanges(tokens)
        val extractions = processScopes(text, scopes, config)
        return resolveSpans(extractions, config.marker)
    }

    private fun processScopes(
        text: String,
        scopes: List<ScopeRange>,
        config: MinecraftColorConfig
    ): List<Extraction> {
        val result = mutableListOf<Extraction>()

        for (scope in scopes) {
            val sliced = text.substring(scope.start, scope.end)
            val normalized = EscapeNormalizer.normalize(sliced)
            val processed = mergeTypes(
                extendColors(
                    extendFormatting(
                        extract(normalized.text, 0, config),
                        config.version
                    )
                ),
                to = SpecialFormatting.UNDERLINE_STRIKETHROUGH,
                types = listOf(SpecialFormatting.UNDERLINE, SpecialFormatting.STRIKETHROUGH)
            ).map { extraction ->
                Extraction(
                    start = scope.start + normalized.toOriginalOffset(extraction.start),
                    end = scope.start + normalized.toOriginalExclusiveOffset(extraction.end, sliced.length),
                    value = extraction.value
                )
            }

            for (extraction in processed) {
                val allowedRanges = if (markerRespectsScope(config.marker)) {
                    trimExtractionToAllowedRanges(extraction, scope.ranges)
                } else {
                    listOf(IntRangeSpan(extraction.start, extraction.end))
                }

                for (allowedRange in allowedRanges) {
                    result += Extraction(
                        start = allowedRange.start,
                        end = allowedRange.end,
                        value = extraction.value
                    )
                }
            }
        }

        return result.filter { it.start < it.end }
    }

    private fun extract(text: String, start: Int, config: MinecraftColorConfig): List<Extraction> {
        val final = mutableListOf<Extraction>()
        if (text.isEmpty()) {
            return final
        }

        val prefixes = config.prefixes
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
        val formatCodes = MinecraftVersionRegistry.profile(config)

        var index = 0
        while (index < text.length) {
            val extendedColor = ExtendedColorParser.matchAt(text, index)
            if (extendedColor != null) {
                final += Extraction(
                    start = start + index,
                    end = start + findNextBoundary(text, index + extendedColor.length, prefixes),
                    value = ColorValue(ExtendedColorParser.toColorHex(extendedColor))
                )
                index += extendedColor.length
                continue
            }

            if (text[index].toString() !in prefixes || index + 1 >= text.length) {
                index += 1
                continue
            }

            val codeChar = text[index + 1].lowercaseChar()
            val value = formatCodes.colors[codeChar]?.let(::ColorValue)
                ?: formatCodes.special[codeChar]?.let(::SpecialValue)

            if (value != null) {
                final += Extraction(
                    start = start + index,
                    end = start + findNextBoundary(text, index + 2, prefixes),
                    value = value
                )
                index += 2
                continue
            }

            index += 1
        }

        return final
    }

    private fun trimExtractionToAllowedRanges(
        extraction: Extraction,
        allowedRanges: List<IntRangeSpan>
    ): List<IntRangeSpan> {
        return allowedRanges.mapNotNull { range ->
            val overlapStart = maxOf(extraction.start, range.start)
            val overlapEnd = minOf(extraction.end, range.end)
            if (overlapStart < overlapEnd) IntRangeSpan(overlapStart, overlapEnd) else null
        }
    }

    private fun mergeTypes(
        extraction: List<Extraction>,
        to: SpecialFormatting,
        types: List<SpecialFormatting>
    ): List<Extraction> {
        val groups = extraction.groupBy { it.end }.values
        for (group in groups) {
            if (group.size < types.size) {
                continue
            }

            val samples = types.mapNotNull { type ->
                group.find { (it.value as? SpecialValue)?.formatting == type }
            }
            if (samples.size < types.size) {
                continue
            }

            samples.maxBy { it.start }.value = SpecialValue(to)
        }

        return extraction
    }

    private fun extendColors(extraction: List<Extraction>): List<Extraction> {
        var index = extraction.lastOrNull()?.end ?: 0

        for (item in extraction.asReversed()) {
            when (val value = item.value) {
                is ColorValue -> {
                    item.end = index
                    index = item.start
                }

                is SpecialValue -> {
                    if (isResetCode(value.formatting)) {
                        index = item.start
                    }
                }
            }
        }

        return extraction
    }

    private fun extendFormatting(
        extraction: List<Extraction>,
        version: MinecraftVersion
    ): List<Extraction> {
        var index = extraction.lastOrNull()?.end ?: 0

        for (item in extraction.asReversed()) {
            when (val value = item.value) {
                is SpecialValue -> {
                    if (isResetCode(value.formatting)) {
                        index = item.start
                    } else {
                        item.end = index
                    }
                }

                is ColorValue -> {
                    if (version == MinecraftVersion.JAVA) {
                        index = item.start
                    }
                }
            }
        }

        return extraction
    }

    private fun findNextBoundary(text: String, index: Int, delimiters: Set<String>): Int {
        for (i in index until text.length) {
            if (text[i].toString() in delimiters || ExtendedColorParser.matchAt(text, i) != null) {
                return i
            }
        }
        return text.length
    }

    private fun tokensToScopeRanges(tokens: List<MinecraftToken>): List<ScopeRange> {
        val flattened = mutableListOf<ScopeRange>()

        for (token in tokens) {
            if (token.type.startsWith(MinecraftGrammar.scope())) {
                flattened += tokensToScopeRanges(token.tokens)
                continue
            }

            val scopedRange = ScopeRange(
                start = token.innerStart,
                end = token.innerEnd
            )

            if (token.tokens.isEmpty()) {
                scopedRange.ranges += IntRangeSpan(token.innerStart, token.innerEnd)
            } else {
                var currentPos = token.innerStart
                for (child in token.tokens) {
                    if (currentPos < child.start) {
                        scopedRange.ranges += IntRangeSpan(currentPos, child.start)
                    }

                    val childFlattened = tokensToScopeRanges(listOf(child))
                    for (childScope in childFlattened) {
                        scopedRange.ranges += childScope.ranges
                    }

                    currentPos = child.end
                }

                if (currentPos < token.innerEnd) {
                    scopedRange.ranges += IntRangeSpan(currentPos, token.innerEnd)
                }
            }

            flattened += scopedRange
        }

        return flattened
    }

    private fun resolveSpans(
        extractions: List<Extraction>,
        marker: MinecraftMarker
    ): List<ResolvedHighlightSpan> {
        if (extractions.isEmpty()) {
            return emptyList()
        }

        val boundaries = extractions
            .flatMap { listOf(it.start, it.end) }
            .distinct()
            .sorted()

        val resolved = mutableListOf<ResolvedHighlightSpan>()
        for (index in 0 until boundaries.lastIndex) {
            val start = boundaries[index]
            val end = boundaries[index + 1]
            if (start >= end) {
                continue
            }

            val active = extractions.filter { it.start < end && it.end > start }
            val span = createResolvedSpan(start, end, active, marker) ?: continue
            if (resolved.isNotEmpty() && canMerge(resolved.last(), span)) {
                val previous = resolved.removeAt(resolved.lastIndex)
                resolved += previous.copy(end = span.end)
            } else {
                resolved += span
            }
        }

        return resolved
    }

    private fun createResolvedSpan(
        start: Int,
        end: Int,
        active: List<Extraction>,
        marker: MinecraftMarker
    ): ResolvedHighlightSpan? {
        val activeColor = active.mapNotNull { it.value as? ColorValue }.lastOrNull()
        val formattingValues = active.mapNotNull { (it.value as? SpecialValue)?.formatting }.toSet()

        val formatting = FormattingState(
            bold = SpecialFormatting.BOLD in formattingValues,
            italic = SpecialFormatting.ITALIC in formattingValues,
            underline = SpecialFormatting.UNDERLINE in formattingValues ||
                SpecialFormatting.UNDERLINE_STRIKETHROUGH in formattingValues,
            strikethrough = SpecialFormatting.STRIKETHROUGH in formattingValues ||
                SpecialFormatting.UNDERLINE_STRIKETHROUGH in formattingValues,
            obfuscated = SpecialFormatting.OBFUSCATED in formattingValues
        )

        val colorMarker = activeColor?.let {
            ColorMarkerInfo(
                marker = marker,
                colorHex = it.hex,
                contrastHex = if (marker == MinecraftMarker.BACKGROUND) getColorContrast(it.hex) else null
            )
        }

        if (colorMarker == null && !formatting.hasFormatting()) {
            return null
        }

        return ResolvedHighlightSpan(
            start = start,
            end = end,
            colorMarker = colorMarker,
            formatting = formatting
        )
    }

    private fun canMerge(left: ResolvedHighlightSpan, right: ResolvedHighlightSpan): Boolean {
        return left.end == right.start &&
            left.colorMarker == right.colorMarker &&
            left.formatting == right.formatting
    }
}
