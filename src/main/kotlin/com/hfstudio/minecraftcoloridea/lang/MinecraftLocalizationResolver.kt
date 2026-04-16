package com.hfstudio.minecraftcoloridea.lang

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.core.ExtendedColorParser
import com.hfstudio.minecraftcoloridea.core.MinecraftFormatInterpolator
import com.hfstudio.minecraftcoloridea.core.SpecialFormatting
import com.hfstudio.minecraftcoloridea.core.MinecraftVersion
import com.hfstudio.minecraftcoloridea.core.MinecraftVersionRegistry

class MinecraftLocalizationResolver(
    private val index: MinecraftLangIndex,
    extraMethodNames: Set<String> = emptySet()
) {
    private data class StyledPreviewText(
        val visibleText: String,
        val baseColorHex: String?
    )

    private data class ParsedArgument(
        val text: String,
        val range: IntRange
    )

    private data class SupportedCall(
        val methodName: String,
        val key: String,
        val rawArgs: List<String>,
        val range: IntRange,
        val keyRange: IntRange
    )

    private val supportedCalls = linkedSetOf(
        "StatCollector.translateToLocalFormatted",
        "StatCollector.translateToLocal",
        "I18n.format",
        "I18n.get",
        "LangHelpers.localize"
    ).apply {
        extraMethodNames
            .map(String::trim)
            .filter(String::isNotEmpty)
            .forEach(::add)
    }
    private val literalPattern = Regex(""""((?:\\.|[^"])*)"""")

    fun resolveExpression(source: String, localeOrder: List<String>): MinecraftResolvedPreview? {
        val baseColorHex = resolveInheritedColor(source)
        val callMatch = findSupportedCall(source)

        if (callMatch != null) {
            val resolvedCall = resolveCall(
                methodName = callMatch.methodName,
                key = callMatch.key,
                rawArgs = callMatch.rawArgs,
                localeOrder = localeOrder,
                baseColorHex = baseColorHex
            ) ?: return null

            val rewritten = source.replaceRange(
                callMatch.range,
                "\"${escapeForLiteral(resolvedCall.previewText)}\""
            )
            return resolveLiteralConcatenation(
                source = rewritten,
                localeOrder = localeOrder,
                baseColorHex = baseColorHex,
                referencedKeys = setOf(callMatch.key),
                excludedSourceRanges = listOf(callMatch.keyRange)
            ) ?: resolvedCall.copy(
                anchorOffset = source.length,
                excludedSourceRanges = listOf(callMatch.keyRange)
            )
        }

        return resolveLiteralConcatenation(
            source = source,
            localeOrder = localeOrder,
            baseColorHex = baseColorHex
        )
    }

    fun resolveCall(
        methodName: String,
        key: String,
        rawArgs: List<String>,
        localeOrder: List<String>,
        baseColorHex: String? = null
    ): MinecraftResolvedPreview? {
        val template = index.lookup(key, localeOrder) ?: return null
        val args = rawArgs.map { arg ->
            arg.trim().removePrefix("\"").removeSuffix("\"")
        }
        val styledPreview = normalizePreviewText(
            text = MinecraftFormatInterpolator.interpolate(template, args),
            inheritedBaseColorHex = baseColorHex
        )
        return MinecraftResolvedPreview(
            anchorOffset = 0,
            previewText = styledPreview.visibleText,
            excludedSourceRanges = emptyList(),
            referencedKeys = setOf(key),
            baseColorHex = styledPreview.baseColorHex
        )
    }

    private fun resolveLiteralConcatenation(
        source: String,
        localeOrder: List<String>,
        baseColorHex: String?,
        referencedKeys: Set<String> = emptySet(),
        excludedSourceRanges: List<IntRange> = emptyList()
    ): MinecraftResolvedPreview? {
        val resolvedKeys = referencedKeys.toMutableSet()
        val pieces = literalPattern.findAll(source)
            .map { match ->
                val decoded = decodeLiteral(match.groupValues[1])
                val localized = index.lookup(decoded, localeOrder)
                if (localized != null) {
                    resolvedKeys += decoded
                    localized
                } else {
                    decoded
                }
            }
            .toList()

        if (pieces.isEmpty() || resolvedKeys.isEmpty()) {
            return null
        }

        val styledPreview = normalizePreviewText(
            text = pieces.joinToString(""),
            inheritedBaseColorHex = baseColorHex
        )

        return MinecraftResolvedPreview(
            anchorOffset = source.length,
            previewText = styledPreview.visibleText,
            excludedSourceRanges = excludedSourceRanges,
            referencedKeys = resolvedKeys,
            baseColorHex = styledPreview.baseColorHex
        )
    }

    private fun findSupportedCall(source: String): SupportedCall? {
        return supportedCalls.asSequence()
            .flatMap { methodName -> findCallCandidates(source, methodName).asSequence() }
            .minByOrNull { it.range.first }
    }

    private fun findCallCandidates(source: String, methodName: String): List<SupportedCall> {
        val matches = mutableListOf<SupportedCall>()
        var searchFrom = 0

        while (searchFrom < source.length) {
            val start = source.indexOf("$methodName(", searchFrom)
            if (start < 0) {
                break
            }
            searchFrom = start + methodName.length

            if (!hasIdentifierBoundary(source, start)) {
                continue
            }

            val openParenIndex = start + methodName.length
            val parsed = parseArguments(source, openParenIndex) ?: continue
            val arguments = parsed.first
            if (arguments.isEmpty()) {
                continue
            }

            val keyLiteral = arguments.first().text
            if (keyLiteral.length < 2 || !keyLiteral.startsWith("\"") || !keyLiteral.endsWith("\"")) {
                continue
            }

            val key = decodeLiteral(keyLiteral.substring(1, keyLiteral.length - 1))
            matches += SupportedCall(
                methodName = methodName,
                key = key,
                rawArgs = arguments.drop(1).map(ParsedArgument::text),
                range = start..parsed.second,
                keyRange = arguments.first().range
            )
        }

        return matches
    }

    private fun parseArguments(source: String, openParenIndex: Int): Pair<List<ParsedArgument>, Int>? {
        if (openParenIndex >= source.length || source[openParenIndex] != '(') {
            return null
        }

        val arguments = mutableListOf<ParsedArgument>()
        var depth = 0
        var inString = false
        var escaped = false
        var argumentStart = openParenIndex + 1

        for (index in openParenIndex + 1 until source.length) {
            val char = source[index]

            if (escaped) {
                escaped = false
                continue
            }

            if (char == '\\') {
                if (inString) {
                    escaped = true
                }
                continue
            }

            if (char == '"') {
                inString = !inString
                continue
            }

            if (!inString) {
                when (char) {
                    '(' -> {
                        depth += 1
                        continue
                    }

                    ')' -> {
                        if (depth == 0) {
                            trimRange(source, argumentStart, index)?.let { range ->
                                arguments += ParsedArgument(
                                    text = source.substring(range.first, range.last + 1),
                                    range = range
                                )
                            }
                            return arguments to index
                        }

                        depth -= 1
                        continue
                    }

                    ',' -> {
                        if (depth == 0) {
                            trimRange(source, argumentStart, index)?.let { range ->
                                arguments += ParsedArgument(
                                    text = source.substring(range.first, range.last + 1),
                                    range = range
                                )
                            }
                            argumentStart = index + 1
                            continue
                        }
                    }
                }
            }
        }

        return null
    }

    private fun resolveInheritedColor(source: String): String? {
        val formatCodes = MinecraftVersionRegistry.profile(MinecraftColorConfig(version = MinecraftVersion.JAVA))
        val match = Regex("""(?:\\u00[aA]7|\u00a7)([0-9A-Fa-f])""")
            .findAll(source)
            .lastOrNull()
            ?: return null
        return formatCodes.colors[match.groupValues[1].lowercase().first()]?.lowercase()
    }

    private fun hasIdentifierBoundary(source: String, start: Int): Boolean {
        if (start == 0) {
            return true
        }

        val previous = source[start - 1]
        return !previous.isLetterOrDigit() && previous != '_' && previous != '.'
    }

    private fun trimRange(source: String, start: Int, endExclusive: Int): IntRange? {
        var left = start
        var right = endExclusive - 1

        while (left <= right && source[left].isWhitespace()) {
            left += 1
        }
        while (right >= left && source[right].isWhitespace()) {
            right -= 1
        }

        return if (left <= right) {
            left..right
        } else {
            null
        }
    }

    private fun decodeLiteral(value: String): String {
        return value
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun escapeForLiteral(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
    }

    private fun normalizePreviewText(
        text: String,
        inheritedBaseColorHex: String?
    ): StyledPreviewText {
        var index = 0
        var resolvedBaseColor = inheritedBaseColorHex
        val formatCodes = MinecraftVersionRegistry.profile(MinecraftColorConfig(version = MinecraftVersion.JAVA))

        while (index < text.length) {
            val hexColor = ExtendedColorParser.matchAt(text, index)
            if (hexColor != null) {
                resolvedBaseColor = ExtendedColorParser.toColorHex(hexColor).lowercase()
                index += hexColor.length
                continue
            }

            val markerLength = when {
                text.startsWith("\\u00a7", index) || text.startsWith("\\u00A7", index) -> 6
                text[index] == '\u00a7' -> 1
                else -> 0
            }
            if (markerLength == 0 || index + markerLength >= text.length) {
                break
            }

            val code = text[index + markerLength].lowercaseChar()
            when {
                formatCodes.colors.containsKey(code) -> {
                    resolvedBaseColor = formatCodes.colors.getValue(code).lowercase()
                    index += markerLength + 1
                }

                formatCodes.special[code] == SpecialFormatting.RESET -> {
                    resolvedBaseColor = null
                    index += markerLength + 1
                }

                formatCodes.special.containsKey(code) -> {
                    index += markerLength + 1
                }

                else -> break
            }
        }

        return StyledPreviewText(
            visibleText = text.substring(index),
            baseColorHex = resolvedBaseColor
        )
    }
}
