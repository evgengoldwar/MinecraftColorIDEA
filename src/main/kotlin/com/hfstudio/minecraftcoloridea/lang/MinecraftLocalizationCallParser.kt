package com.hfstudio.minecraftcoloridea.lang

data class MinecraftLocalizationCallMatch(
    val methodName: String,
    val key: String,
    val rawArgs: List<String>,
    val range: IntRange,
    val keyRange: IntRange
)

object MinecraftLocalizationCallParser {
    private data class ParsedArgument(
        val text: String,
        val range: IntRange
    )

    private val supportedCalls = linkedSetOf(
        "StatCollector.translateToLocalFormatted",
        "StatCollector.translateToLocal",
        "I18n.format",
        "I18n.get",
        "LangHelpers.localize"
    )

    fun findFirst(
        source: String,
        extraMethodNames: Set<String> = emptySet()
    ): MinecraftLocalizationCallMatch? {
        return findAll(source, extraMethodNames).minByOrNull { it.range.first }
    }

    fun findAll(
        source: String,
        extraMethodNames: Set<String> = emptySet()
    ): List<MinecraftLocalizationCallMatch> {
        return supportedMethodNames(extraMethodNames).asSequence()
            .flatMap { methodName -> findCallCandidates(source, methodName).asSequence() }
            .sortedBy { it.range.first }
            .toList()
    }

    fun decodeLiteral(value: String): String {
        if ('\\' !in value) {
            return value
        }

        val result = StringBuilder(value.length)
        var index = 0

        while (index < value.length) {
            val char = value[index]
            if (char != '\\' || index + 1 >= value.length) {
                result.append(char)
                index += 1
                continue
            }

            when (val escaped = value[index + 1]) {
                '\\' -> {
                    result.append('\\')
                    index += 2
                }

                '"' -> {
                    result.append('"')
                    index += 2
                }

                '\'' -> {
                    result.append('\'')
                    index += 2
                }

                'n' -> {
                    result.append('\n')
                    index += 2
                }

                'r' -> {
                    result.append('\r')
                    index += 2
                }

                't' -> {
                    result.append('\t')
                    index += 2
                }

                'b' -> {
                    result.append('\b')
                    index += 2
                }

                'f' -> {
                    result.append('\u000C')
                    index += 2
                }

                'u' -> {
                    if (index + 6 <= value.length) {
                        val hex = value.substring(index + 2, index + 6)
                        val decoded = hex.toIntOrNull(16)?.toChar()
                        if (decoded != null) {
                            result.append(decoded)
                            index += 6
                            continue
                        }
                    }

                    result.append('\\')
                    result.append(escaped)
                    index += 2
                }

                else -> {
                    result.append(escaped)
                    index += 2
                }
            }
        }

        return result.toString()
    }

    private fun supportedMethodNames(extraMethodNames: Set<String>): Set<String> {
        return linkedSetOf<String>().apply {
            addAll(supportedCalls)
            extraMethodNames
                .map(String::trim)
                .filter(String::isNotEmpty)
                .forEach(::add)
        }
    }

    private fun findCallCandidates(source: String, methodName: String): List<MinecraftLocalizationCallMatch> {
        val matches = mutableListOf<MinecraftLocalizationCallMatch>()
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
            if (keyLiteral.length < 2 || !keyLiteral.startsWith('"') || !keyLiteral.endsWith('"')) {
                continue
            }

            val keyArgument = arguments.first().range
            val key = decodeLiteral(keyLiteral.substring(1, keyLiteral.length - 1))
            matches += MinecraftLocalizationCallMatch(
                methodName = methodName,
                key = key,
                rawArgs = arguments.drop(1).map(ParsedArgument::text),
                range = start..parsed.second,
                keyRange = (keyArgument.first + 1)..(keyArgument.last - 1)
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
}
