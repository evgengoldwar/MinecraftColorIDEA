package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.core.MinecraftQuotedStringScanner
import com.hfstudio.minecraftcoloridea.lang.MinecraftLocalizationCallParser

data class MinecraftLocatedKey(
    val key: String,
    val range: IntRange
)

class MinecraftLocalizationKeyLocator(
    private val extraMethodNames: Set<String> = emptySet()
) {
    private enum class FallbackMode {
        NONE,
        DECLARATION,
        ACTION
    }

    private data class ParsedLine(
        val supportedCalls: List<com.hfstudio.minecraftcoloridea.lang.MinecraftLocalizationCallMatch>,
        val explicitLiterals: List<ExplicitLiteralCandidate>,
        val keyCandidates: List<MinecraftLocatedKey>
    )

    private data class ExplicitLiteralCandidate(
        val locatedKey: MinecraftLocatedKey,
        val fullRange: IntRange
    )

    private val keyPattern = Regex("""[A-Za-z0-9_.-]+\.[A-Za-z0-9_.-]+""")

    fun locate(source: String, caretOffset: Int): MinecraftLocatedKey? {
        return locate(source, caretOffset, fallbackMode = FallbackMode.ACTION)
    }

    fun locateStrictly(source: String, caretOffset: Int): MinecraftLocatedKey? {
        return locate(source, caretOffset, fallbackMode = FallbackMode.NONE)
    }

    fun locateForDeclaration(source: String, caretOffset: Int): MinecraftLocatedKey? {
        return locate(source, caretOffset, fallbackMode = FallbackMode.DECLARATION)
    }

    private fun locate(source: String, caretOffset: Int, fallbackMode: FallbackMode): MinecraftLocatedKey? {
        val parsed = parseLine(source)
        parsed.keyCandidates
            .firstOrNull { caretOffset in it.range }
            ?.let { return it }

        if (fallbackMode != FallbackMode.NONE) {
            parsed.supportedCalls
                .firstOrNull { caretOffset in it.range && caretOffset !in it.keyRange }
                ?.let { return MinecraftLocatedKey(it.key, it.keyRange) }
        }

        if (fallbackMode != FallbackMode.NONE) {
            parsed.explicitLiterals
                .firstOrNull { caretOffset in explicitLiteralRange(it, fallbackMode) }
                ?.let { return it.locatedKey }
        }

        return if (fallbackMode == FallbackMode.ACTION) {
            parsed.keyCandidates.distinctBy(MinecraftLocatedKey::key).singleOrNull()
        } else {
            null
        }
    }

    private fun parseLine(source: String): ParsedLine {
        val visibleSource = maskCommentSpans(source)
        val supportedCalls = MinecraftLocalizationCallParser.findAll(visibleSource, extraMethodNames)
        val explicitLiterals = MinecraftQuotedStringScanner.findAll(visibleSource)
            .mapNotNull { token ->
                val contentRange = token.contentRange ?: return@mapNotNull null
                MinecraftLocatedKey(
                    key = MinecraftLocalizationCallParser.decodeLiteral(token.rawContent),
                    range = contentRange
                ).let { locatedKey ->
                    ExplicitLiteralCandidate(
                        locatedKey = locatedKey,
                        fullRange = token.fullStart until token.fullEndExclusive
                    )
                }
            }
            .toList()
        val candidates = mutableListOf<MinecraftLocatedKey>()

        supportedCalls.forEach { match ->
            candidates += MinecraftLocatedKey(match.key, match.keyRange)
        }

        explicitLiterals.forEach { literal ->
            if (keyPattern.matches(literal.locatedKey.key)) {
                candidates += literal.locatedKey
            }
        }

        return ParsedLine(
            supportedCalls = supportedCalls,
            explicitLiterals = explicitLiterals,
            keyCandidates = candidates
                .distinctBy { "${it.range.first}:${it.range.last}:${it.key}" }
                .sortedBy { it.range.first }
        )
    }

    private fun explicitLiteralRange(
        candidate: ExplicitLiteralCandidate,
        fallbackMode: FallbackMode
    ): IntRange {
        return if (fallbackMode == FallbackMode.DECLARATION) {
            candidate.fullRange
        } else {
            candidate.locatedKey.range
        }
    }

    private fun maskCommentSpans(source: String): String {
        if (source.isEmpty()) {
            return source
        }

        val chars = source.toCharArray()
        var index = 0
        var inString = false
        var escaped = false

        while (index < chars.size) {
            val char = chars[index]

            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (char == '\\') {
                    escaped = true
                } else if (char == '"') {
                    inString = false
                }
                index += 1
                continue
            }

            when {
                char == '"' -> {
                    inString = true
                    index += 1
                }

                char == '/' && index + 1 < chars.size && chars[index + 1] == '/' -> {
                    for (maskIndex in index until chars.size) {
                        chars[maskIndex] = ' '
                    }
                    break
                }

                char == '/' && index + 1 < chars.size && chars[index + 1] == '*' -> {
                    val endIndex = source.indexOf("*/", startIndex = index + 2)
                    val maskEnd = if (endIndex >= 0) endIndex + 1 else chars.lastIndex
                    for (maskIndex in index..maskEnd) {
                        chars[maskIndex] = ' '
                    }
                    index = maskEnd + 1
                }

                else -> index += 1
            }
        }

        return String(chars)
    }
}
