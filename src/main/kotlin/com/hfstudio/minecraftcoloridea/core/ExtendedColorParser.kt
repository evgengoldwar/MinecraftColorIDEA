package com.hfstudio.minecraftcoloridea.core

object ExtendedColorParser {
    fun matchAt(text: String, index: Int): String? {
        if (index >= text.length) {
            return null
        }

        return when {
            text[index] == '#' -> matchHash(text, index, 8) ?: matchHash(text, index, 6)
            text[index] == '0' && index + 1 < text.length && (text[index + 1] == 'x' || text[index + 1] == 'X') ->
                matchOx(text, index, 8) ?: matchOx(text, index, 6)
            else -> null
        }
    }

    fun toColorHex(value: String): String {
        return if (value.startsWith("#")) {
            "#${value.removePrefix("#").uppercase()}"
        } else {
            "#${value.removePrefix("0x").removePrefix("0X").uppercase()}"
        }
    }

    private fun matchHash(text: String, index: Int, digits: Int): String? {
        val end = index + 1 + digits
        if (end > text.length) {
            return null
        }
        if (!text.regionMatches(index, "#", 0, 1)) {
            return null
        }
        if (!text.substring(index + 1, end).all(::isHexDigit)) {
            return null
        }
        if (end < text.length && isHexDigit(text[end])) {
            return null
        }
        return text.substring(index, end)
    }

    private fun matchOx(text: String, index: Int, digits: Int): String? {
        val end = index + 2 + digits
        if (end > text.length) {
            return null
        }
        if (text[index] != '0' || (text[index + 1] != 'x' && text[index + 1] != 'X')) {
            return null
        }
        if (!text.substring(index + 2, end).all(::isHexDigit)) {
            return null
        }
        if (end < text.length && isHexDigit(text[end])) {
            return null
        }
        return text.substring(index, end)
    }

    private fun isHexDigit(char: Char): Boolean {
        return char in '0'..'9' || char in 'a'..'f' || char in 'A'..'F'
    }
}
