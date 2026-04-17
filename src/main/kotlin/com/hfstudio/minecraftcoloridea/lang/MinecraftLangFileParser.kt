package com.hfstudio.minecraftcoloridea.lang

import com.hfstudio.minecraftcoloridea.core.MinecraftQuotedStringScanner

object MinecraftLangFileParser {
    fun parseLang(content: String): Map<String, String> {
        return parseLangLines(content).associate { line ->
            line.key to line.value
        }
    }

    fun parseJson(content: String): Map<String, String> {
        return parseJsonEntries(content).associate { entry ->
            entry.key to entry.value
        }
    }

    fun parseLangSources(content: String, locale: String, filePath: String): List<MinecraftLangSourceEntry> {
        return parseLangLines(content).map { line ->
            MinecraftLangSourceEntry(
                locale = locale.lowercase(),
                key = line.key,
                filePath = filePath,
                lineNumber = line.lineNumber,
                lineStartOffset = line.lineStartOffset
            )
        }
    }

    fun parseJsonSources(content: String, locale: String, filePath: String): List<MinecraftLangSourceEntry> {
        return parseJsonEntries(content).map { entry ->
            MinecraftLangSourceEntry(
                locale = locale.lowercase(),
                key = entry.key,
                filePath = filePath,
                lineNumber = entry.lineNumber,
                lineStartOffset = entry.lineStartOffset
            )
        }
    }

    private data class ParsedLangLine(
        val key: String,
        val value: String,
        val lineNumber: Int,
        val lineStartOffset: Int
    )

    private fun parseLangLines(content: String): List<ParsedLangLine> {
        val lines = mutableListOf<ParsedLangLine>()
        var lineNumber = 1
        var lineStart = 0

        while (lineStart <= content.length) {
            val lineEnd = findLineEnd(content, lineStart)
            val rawLine = content.substring(lineStart, lineEnd)
            val trimmedLine = rawLine.trim()
            if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#") && trimmedLine.contains("=")) {
                val separatorIndex = trimmedLine.indexOf('=')
                lines += ParsedLangLine(
                    key = trimmedLine.substring(0, separatorIndex),
                    value = trimmedLine.substring(separatorIndex + 1),
                    lineNumber = lineNumber,
                    lineStartOffset = lineStart
                )
            }

            if (lineEnd >= content.length) {
                break
            }

            lineStart = advanceToNextLine(content, lineEnd)
            lineNumber += 1
        }

        return lines
    }

    private fun parseJsonEntries(content: String): List<ParsedLangLine> {
        val entries = mutableListOf<ParsedLangLine>()
        val tokens = MinecraftQuotedStringScanner.findAll(content)
        var index = 0

        while (index < tokens.size) {
            val keyToken = tokens[index]
            val colonIndex = content.indexOfFirstNonWhitespace(keyToken.fullEndExclusive)
            if (colonIndex !in content.indices || content[colonIndex] != ':') {
                index += 1
                continue
            }

            val valueStart = content.indexOfFirstNonWhitespace(colonIndex + 1)
            val valueToken = tokens.getOrNull(index + 1)
                ?.takeIf { it.fullStart == valueStart }
                ?: break

            entries += ParsedLangLine(
                key = decode(keyToken.rawContent),
                value = decode(valueToken.rawContent),
                lineNumber = lineNumberAt(content, keyToken.fullStart),
                lineStartOffset = lineStartOffsetAt(content, keyToken.fullStart)
            )
            index += 2
        }

        return entries
    }

    private fun findLineEnd(content: String, start: Int): Int {
        var index = start
        while (index < content.length && content[index] != '\n' && content[index] != '\r') {
            index += 1
        }
        return index
    }

    private fun advanceToNextLine(content: String, lineEnd: Int): Int {
        var index = lineEnd
        if (index < content.length && content[index] == '\r') {
            index += 1
        }
        if (index < content.length && content[index] == '\n') {
            index += 1
        }
        return index
    }

    private fun lineNumberAt(content: String, offset: Int): Int {
        var lineNumber = 1
        var index = 0
        while (index < offset) {
            if (content[index] == '\n') {
                lineNumber += 1
            }
            index += 1
        }
        return lineNumber
    }

    private fun lineStartOffsetAt(content: String, offset: Int): Int {
        var index = offset.coerceAtMost(content.length)
        while (index > 0 && content[index - 1] != '\n' && content[index - 1] != '\r') {
            index -= 1
        }
        return index
    }

    private fun decode(value: String): String {
        return value
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun String.indexOfFirstNonWhitespace(startIndex: Int): Int {
        var index = startIndex
        while (index < length && this[index].isWhitespace()) {
            index += 1
        }
        return index
    }
}
