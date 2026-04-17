package com.hfstudio.minecraftcoloridea.core

data class MinecraftQuotedStringToken(
    val rawContent: String,
    val contentStart: Int,
    val contentEndExclusive: Int,
    val fullStart: Int,
    val fullEndExclusive: Int
) {
    val contentRange: IntRange?
        get() = if (contentStart < contentEndExclusive) {
            contentStart until contentEndExclusive
        } else {
            null
        }
}

object MinecraftQuotedStringScanner {
    fun findAll(source: String): List<MinecraftQuotedStringToken> {
        if (source.isEmpty()) {
            return emptyList()
        }

        val tokens = mutableListOf<MinecraftQuotedStringToken>()
        var index = 0

        while (index < source.length) {
            if (source[index] != '"') {
                index += 1
                continue
            }

            val fullStart = index
            index += 1
            val contentStart = index
            var escaped = false
            var closed = false

            while (index < source.length) {
                val char = source[index]
                if (escaped) {
                    escaped = false
                    index += 1
                    continue
                }

                when (char) {
                    '\\' -> {
                        escaped = true
                        index += 1
                    }

                    '"' -> {
                        tokens += MinecraftQuotedStringToken(
                            rawContent = source.substring(contentStart, index),
                            contentStart = contentStart,
                            contentEndExclusive = index,
                            fullStart = fullStart,
                            fullEndExclusive = index + 1
                        )
                        index += 1
                        closed = true
                    }

                    else -> index += 1
                }

                if (closed) {
                    break
                }
            }

            if (!closed) {
                break
            }
        }

        return tokens
    }
}
