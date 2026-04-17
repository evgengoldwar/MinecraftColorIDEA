package com.hfstudio.minecraftcoloridea.lang

import com.hfstudio.minecraftcoloridea.core.MinecraftColorConfig
import com.hfstudio.minecraftcoloridea.core.ExtendedColorParser
import com.hfstudio.minecraftcoloridea.core.FormattingState
import com.hfstudio.minecraftcoloridea.core.MinecraftFormatInterpolator
import com.hfstudio.minecraftcoloridea.core.SpecialFormatting
import com.hfstudio.minecraftcoloridea.core.MinecraftVersion
import com.hfstudio.minecraftcoloridea.core.MinecraftVersionRegistry

class MinecraftLocalizationResolver(
    private val index: MinecraftLangIndex,
    private val extraMethodNames: Set<String> = emptySet()
) {
    private data class StyledPreviewText(
        val visibleText: String,
        val baseColorHex: String?,
        val baseFormatting: FormattingState
    )

    private val literalPattern = Regex(""""((?:\\.|[^"])*)"""")

    fun resolveExpression(source: String, localeOrder: List<String>): MinecraftResolvedPreview? {
        val baseColorHex = resolveInheritedColor(source)
        val callMatch = MinecraftLocalizationCallParser.findFirst(source, extraMethodNames)

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
        val args = rawArgs.map(::decodeArgument)
        val styledPreview = normalizePreviewText(
            text = MinecraftFormatInterpolator.interpolate(template, args),
            inheritedBaseColorHex = baseColorHex
        )
        return MinecraftResolvedPreview(
            anchorOffset = 0,
            previewText = styledPreview.visibleText,
            excludedSourceRanges = emptyList(),
            referencedKeys = setOf(key),
            baseColorHex = styledPreview.baseColorHex,
            baseFormatting = styledPreview.baseFormatting
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
            baseColorHex = styledPreview.baseColorHex,
            baseFormatting = styledPreview.baseFormatting
        )
    }

    private fun decodeLiteral(value: String): String {
        return MinecraftLocalizationCallParser.decodeLiteral(value)
    }

    private fun resolveInheritedColor(source: String): String? {
        val formatCodes = MinecraftVersionRegistry.profile(MinecraftColorConfig(version = MinecraftVersion.JAVA))
        val match = Regex("""(?:\\u00[aA]7|\u00a7)([0-9A-Fa-f])""")
            .findAll(source)
            .lastOrNull()
            ?: return null
        return formatCodes.colors[match.groupValues[1].lowercase().first()]?.lowercase()
    }

    private fun decodeArgument(rawArg: String): String {
        val trimmed = rawArg.trim()
        return if (trimmed.length >= 2 && trimmed.startsWith('"') && trimmed.endsWith('"')) {
            decodeLiteral(trimmed.substring(1, trimmed.length - 1))
        } else {
            trimmed
        }
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
        var baseFormatting = FormattingState()
        val formatCodes = MinecraftVersionRegistry.profile(MinecraftColorConfig(version = MinecraftVersion.JAVA))

        while (index < text.length) {
            val hexColor = ExtendedColorParser.matchAt(text, index)
            if (hexColor != null) {
                resolvedBaseColor = ExtendedColorParser.toColorHex(hexColor).lowercase()
                baseFormatting = FormattingState()
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
                    baseFormatting = FormattingState()
                    index += markerLength + 1
                }

                formatCodes.special[code] == SpecialFormatting.RESET -> {
                    resolvedBaseColor = inheritedBaseColorHex
                    baseFormatting = FormattingState()
                    index += markerLength + 1
                }

                formatCodes.special[code] == SpecialFormatting.BOLD -> {
                    baseFormatting = baseFormatting.copy(bold = true)
                    index += markerLength + 1
                }

                formatCodes.special[code] == SpecialFormatting.ITALIC -> {
                    baseFormatting = baseFormatting.copy(italic = true)
                    index += markerLength + 1
                }

                formatCodes.special[code] == SpecialFormatting.UNDERLINE -> {
                    baseFormatting = baseFormatting.copy(underline = true)
                    index += markerLength + 1
                }

                formatCodes.special[code] == SpecialFormatting.STRIKETHROUGH -> {
                    baseFormatting = baseFormatting.copy(strikethrough = true)
                    index += markerLength + 1
                }

                formatCodes.special[code] == SpecialFormatting.UNDERLINE_STRIKETHROUGH -> {
                    baseFormatting = baseFormatting.copy(
                        underline = true,
                        strikethrough = true
                    )
                    index += markerLength + 1
                }

                formatCodes.special[code] == SpecialFormatting.OBFUSCATED -> {
                    baseFormatting = baseFormatting.copy(obfuscated = true)
                    index += markerLength + 1
                }

                else -> break
            }
        }

        return StyledPreviewText(
            visibleText = text.substring(index),
            baseColorHex = resolvedBaseColor,
            baseFormatting = baseFormatting
        )
    }
}
