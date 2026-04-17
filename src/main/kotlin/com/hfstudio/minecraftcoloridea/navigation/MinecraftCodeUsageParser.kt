package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.lang.MinecraftLocalizationCallMatch
import com.hfstudio.minecraftcoloridea.lang.MinecraftLocalizationCallParser

class MinecraftCodeUsageParser(
    private val callParser: MinecraftLocalizationCallParser = MinecraftLocalizationCallParser
) {
    private data class RawLocalizationCall(
        val expression: String,
        val expressionRange: IntRange,
        val range: IntRange
    )

    private data class RawLiteralKey(
        val key: String,
        val range: IntRange
    )

    private val supportedMethodNames = listOf(
        "StatCollector.translateToLocalFormatted",
        "StatCollector.translateToLocal",
        "I18n.format",
        "I18n.get",
        "LangHelpers.localize"
    )
    private val literalPattern = Regex(""""((?:\\.|[^"])*)"""")
    private val keyPattern = Regex("""[A-Za-z0-9_.-]+\.[A-Za-z0-9_.-]+""")

    fun parseFile(
        filePath: String,
        text: String,
        maxEnumeratedKeys: Int
    ): List<MinecraftCodeUsageEntry> {
        if (text.isEmpty() || maxEnumeratedKeys < 1) {
            return emptyList()
        }

        val visibleText = maskCommentSpans(text)
        val exactEntries = callParser.findAll(visibleText).flatMap { call ->
            createExpandedEntries(
                keys = listOf(call.key),
                filePath = filePath,
                text = text,
                matchRange = call.keyRange,
                lineAnchorRange = call.range,
                baseConfidence = MinecraftCodeUsageConfidence.EXACT
            )
        }
        val composedEntries = findComposedCalls(visibleText).flatMap { call ->
            MinecraftKeyCompositionResolver.resolveCandidates(
                source = visibleText,
                expression = call.expression,
                maxEnumeratedKeys = maxEnumeratedKeys
            ).flatMap { key ->
                createExpandedEntries(
                    keys = listOf(key),
                    filePath = filePath,
                    text = text,
                    matchRange = call.expressionRange,
                    lineAnchorRange = call.range,
                    baseConfidence = MinecraftCodeUsageConfidence.ENUMERATED
                )
            }
        }
        val explicitLiteralEntries = findExplicitLiteralKeys(visibleText).flatMap { literal ->
            createExpandedEntries(
                keys = listOf(literal.key),
                filePath = filePath,
                text = text,
                matchRange = literal.range,
                lineAnchorRange = literal.range,
                baseConfidence = MinecraftCodeUsageConfidence.EXACT
            )
        }

        return (exactEntries + composedEntries + explicitLiteralEntries)
            .distinctBy { listOf(it.key, it.filePath, it.matchStartOffset, it.confidence) }
    }

    private fun findExplicitLiteralKeys(source: String): List<RawLiteralKey> {
        return literalPattern.findAll(source)
            .mapNotNull { match ->
                val group = match.groups[1] ?: return@mapNotNull null
                val key = MinecraftLocalizationCallParser.decodeLiteral(group.value)
                if (!keyPattern.matches(key)) {
                    return@mapNotNull null
                }

                RawLiteralKey(
                    key = key,
                    range = group.range
                )
            }
            .toList()
    }

    private fun createExpandedEntries(
        keys: List<String>,
        filePath: String,
        text: String,
        matchRange: IntRange,
        lineAnchorRange: IntRange,
        baseConfidence: MinecraftCodeUsageConfidence
    ): List<MinecraftCodeUsageEntry> {
        return keys.flatMap { key ->
            MinecraftTranslationKeyFamilyExpander.expand(key).map { (expandedKey, derivedConfidence) ->
                createEntry(
                    key = expandedKey,
                    filePath = filePath,
                    text = text,
                    lineAnchorRange = lineAnchorRange,
                    matchRange = matchRange,
                    confidence = when {
                        baseConfidence == MinecraftCodeUsageConfidence.ENUMERATED -> {
                            if (derivedConfidence == MinecraftCodeUsageConfidence.FAMILY_DERIVED) {
                                MinecraftCodeUsageConfidence.FAMILY_DERIVED
                            } else {
                                MinecraftCodeUsageConfidence.ENUMERATED
                            }
                        }

                        else -> derivedConfidence
                    }
                )
            }
        }
    }

    private fun createEntry(
        key: String,
        filePath: String,
        text: String,
        lineAnchorRange: IntRange,
        matchRange: IntRange,
        confidence: MinecraftCodeUsageConfidence
    ): MinecraftCodeUsageEntry {
        val lineStart = text.lastIndexOf('\n', lineAnchorRange.first).let { if (it < 0) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', lineAnchorRange.first).let { if (it < 0) text.length else it }
        return MinecraftCodeUsageEntry(
            key = key,
            filePath = filePath.replace('\\', '/'),
            lineNumber = text.substring(0, lineAnchorRange.first).count { it == '\n' } + 1,
            lineStartOffset = lineStart,
            matchStartOffset = matchRange.first,
            matchEndOffset = matchRange.last + 1,
            snippet = text.substring(lineStart, lineEnd).trim(),
            confidence = confidence
        )
    }

    private fun findComposedCalls(source: String): List<RawLocalizationCall> {
        val matches = mutableListOf<RawLocalizationCall>()
        supportedMethodNames.forEach { methodName ->
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

                val firstArgument = arguments.first()
                val firstArgumentText = source.substring(firstArgument.first, firstArgument.last + 1).trim()
                if (firstArgumentText.startsWith('"') && firstArgumentText.endsWith('"')) {
                    continue
                }

                matches += RawLocalizationCall(
                    expression = firstArgumentText,
                    expressionRange = firstArgument,
                    range = start..parsed.second
                )
            }
        }
        return matches
    }

    private fun parseArguments(source: String, openParenIndex: Int): Pair<List<IntRange>, Int>? {
        if (openParenIndex >= source.length || source[openParenIndex] != '(') {
            return null
        }

        val arguments = mutableListOf<IntRange>()
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
                    '(' -> depth += 1
                    ')' -> {
                        if (depth == 0) {
                            trimRange(source, argumentStart, index)?.let(arguments::add)
                            return arguments to index
                        }
                        depth -= 1
                    }
                    ',' -> if (depth == 0) {
                        trimRange(source, argumentStart, index)?.let(arguments::add)
                        argumentStart = index + 1
                    }
                }
            }
        }
        return null
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
        return if (left <= right) left..right else null
    }

    private fun hasIdentifierBoundary(source: String, start: Int): Boolean {
        if (start == 0) {
            return true
        }
        val previous = source[start - 1]
        return !previous.isLetterOrDigit() && previous != '_' && previous != '.'
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
