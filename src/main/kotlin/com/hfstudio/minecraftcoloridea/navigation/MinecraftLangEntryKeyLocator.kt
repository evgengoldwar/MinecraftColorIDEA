package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.core.MinecraftQuotedStringScanner

data class MinecraftLocatedLangKey(
    val key: String,
    val range: IntRange
)

object MinecraftLangEntryKeyLocator {
    fun locateLang(line: String, caretOffset: Int): MinecraftLocatedLangKey? {
        val separator = line.indexOf('=').takeIf { it >= 0 } ?: return null
        if (caretOffset >= separator) {
            return null
        }

        val key = line.substring(0, separator).trim()
        if (key.isEmpty()) {
            return null
        }

        val start = line.indexOf(key)
        val end = start + key.length - 1
        return if (caretOffset in start..end) {
            MinecraftLocatedLangKey(key, start..end)
        } else {
            null
        }
    }

    fun locateJson(line: String, caretOffset: Int): MinecraftLocatedLangKey? {
        return MinecraftQuotedStringScanner.findAll(line)
            .firstNotNullOfOrNull { token ->
                val colonIndex = line.indexOfFirstNonWhitespace(token.fullEndExclusive)
                if (colonIndex !in line.indices || line[colonIndex] != ':') {
                    return@firstNotNullOfOrNull null
                }

                val range = token.contentRange ?: return@firstNotNullOfOrNull null
                if (caretOffset !in range) {
                    return@firstNotNullOfOrNull null
                }

                MinecraftLocatedLangKey(token.rawContent, range)
            }
    }

    private fun String.indexOfFirstNonWhitespace(startIndex: Int): Int {
        var index = startIndex
        while (index < length && this[index].isWhitespace()) {
            index += 1
        }
        return index
    }
}
