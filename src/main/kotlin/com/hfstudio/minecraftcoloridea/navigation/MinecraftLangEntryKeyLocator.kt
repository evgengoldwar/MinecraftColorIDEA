package com.hfstudio.minecraftcoloridea.navigation

data class MinecraftLocatedLangKey(
    val key: String,
    val range: IntRange
)

object MinecraftLangEntryKeyLocator {
    private val jsonKeyPattern = Regex(""""((?:\\.|[^"])*)"\s*:""")

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
        val match = jsonKeyPattern.find(line) ?: return null
        val group = match.groups[1] ?: return null
        return if (caretOffset in group.range) {
            MinecraftLocatedLangKey(group.value, group.range)
        } else {
            null
        }
    }
}
