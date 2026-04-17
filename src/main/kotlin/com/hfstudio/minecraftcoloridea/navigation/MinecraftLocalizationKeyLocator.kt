package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.lang.MinecraftLocalizationCallParser

data class MinecraftLocatedKey(
    val key: String,
    val range: IntRange
)

class MinecraftLocalizationKeyLocator(
    private val extraMethodNames: Set<String> = emptySet()
) {
    private data class ParsedLine(
        val supportedCalls: List<com.hfstudio.minecraftcoloridea.lang.MinecraftLocalizationCallMatch>,
        val explicitLiterals: List<MinecraftLocatedKey>,
        val keyCandidates: List<MinecraftLocatedKey>
    )

    private val literalPattern = Regex(""""((?:\\.|[^"])*)"""")
    private val keyPattern = Regex("""[A-Za-z0-9_.-]+\.[A-Za-z0-9_.-]+""")

    fun locate(source: String, caretOffset: Int): MinecraftLocatedKey? {
        return locate(source, caretOffset, allowLineFallback = true)
    }

    fun locateStrictly(source: String, caretOffset: Int): MinecraftLocatedKey? {
        return locate(source, caretOffset, allowLineFallback = false)
    }

    private fun locate(source: String, caretOffset: Int, allowLineFallback: Boolean): MinecraftLocatedKey? {
        val parsed = parseLine(source)
        parsed.keyCandidates
            .firstOrNull { caretOffset in it.range }
            ?.let { return it }

        if (allowLineFallback) {
            parsed.supportedCalls
                .firstOrNull { caretOffset in it.range && caretOffset !in it.keyRange }
                ?.let { return MinecraftLocatedKey(it.key, it.keyRange) }
        }

        if (allowLineFallback) {
            parsed.explicitLiterals
                .firstOrNull { caretOffset in it.range }
                ?.let { return it }
        }

        return if (allowLineFallback) {
            parsed.keyCandidates.distinctBy(MinecraftLocatedKey::key).singleOrNull()
        } else {
            null
        }
    }

    private fun parseLine(source: String): ParsedLine {
        val visibleSource = maskCommentSpans(source)
        val supportedCalls = MinecraftLocalizationCallParser.findAll(visibleSource, extraMethodNames)
        val explicitLiterals = literalPattern.findAll(visibleSource)
            .mapNotNull { match ->
                val group = match.groups[1] ?: return@mapNotNull null
                MinecraftLocatedKey(
                    key = MinecraftLocalizationCallParser.decodeLiteral(group.value),
                    range = group.range
                )
            }
            .toList()
        val candidates = mutableListOf<MinecraftLocatedKey>()

        supportedCalls.forEach { match ->
            candidates += MinecraftLocatedKey(match.key, match.keyRange)
        }

        explicitLiterals.forEach { literal ->
            if (keyPattern.matches(literal.key)) {
                candidates += literal
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
