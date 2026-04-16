package com.hfstudio.minecraftcoloridea.core

data class MinecraftToken(
    val type: String,
    val start: Int,
    val end: Int,
    val innerStart: Int,
    val innerEnd: Int,
    val tokens: List<MinecraftToken>
)

class MinecraftTokenizer(private val grammar: MinecraftGrammar) {
    private var index = 0
    private var content = ""

    private data class DefinitionMatch(
        val definition: MinecraftDefinition,
        val directMatch: Boolean
    )

    private val finished: Boolean
        get() = index >= content.length

    private fun peek(amount: Int = 1): String {
        val end = (index + amount).coerceAtMost(content.length)
        return content.substring(index, end)
    }

    private fun consume(amount: Int = 1): String {
        val value = peek(amount)
        index += value.length
        return value
    }

    private fun searchDefinitions(start: String): List<DefinitionMatch> {
        return grammar.definitions
            .filter { it.start.startsWith(start) }
            .map { DefinitionMatch(it, it.start == start) }
    }

    fun tokenize(content: String): List<MinecraftToken> {
        this.content = content
        index = 0
        return tokenizeInternal()
    }

    private fun tokenizeInternal(): List<MinecraftToken> {
        val tokens = mutableListOf<MinecraftToken>()
        while (!finished) {
            tokenizeSingle()?.let(tokens::add)
        }
        return tokens
    }

    private fun tokenizeSingle(): MinecraftToken? {
        val start = index
        val char = consume()
        var matches = searchDefinitions(char)

        if (matches.isEmpty()) {
            return null
        }

        var match: DefinitionMatch? = null
        var consumed = char
        val maxLength = grammar.definitions.maxOfOrNull { it.start.length } ?: 0

        while (matches.size > 1 && !finished) {
            matches.firstOrNull { it.directMatch }?.let { match = it }
            if (consumed.length >= maxLength) {
                break
            }

            consumed += consume()
            matches = searchDefinitions(consumed)
            if (matches.isEmpty()) {
                break
            }
        }

        if (match == null && matches.firstOrNull()?.directMatch == true) {
            match = matches.first()
        }

        return match?.let { createToken(it.definition, start) }
    }

    private fun createToken(match: MinecraftDefinition, start: Int): MinecraftToken {
        val innerStart = start + match.start.length
        val tokens = mutableListOf<MinecraftToken>()

        while (!finished) {
            val nextChar = peek()

            if (match.escape != null && nextChar == match.escape) {
                consume()
                if (!finished) {
                    consume()
                }
                continue
            }

            val scope = match.scope
            if (scope != null && peek(scope.start.length) == scope.start) {
                val scopeStart = index
                consume(scope.start.length)
                val scopeTokens = tokenizeScope(scope)
                val scopeEnd = index
                tokens += MinecraftToken(
                    type = scope.type,
                    start = scopeStart,
                    end = scopeEnd,
                    innerStart = scopeStart + scope.start.length,
                    innerEnd = scopeEnd - scope.end.length,
                    tokens = scopeTokens
                )
                continue
            }

            if (peek(match.end.length) == match.end) {
                consume(match.end.length)
                break
            }

            if (!match.multiline && nextChar == "\n") {
                consume()
                break
            }

            consume()
        }

        val innerEnd = (index - match.end.length).coerceAtLeast(innerStart)
        return MinecraftToken(
            type = match.type,
            start = start,
            end = index,
            innerStart = innerStart,
            innerEnd = innerEnd,
            tokens = tokens
        )
    }

    private fun tokenizeScope(scope: MinecraftDefinition): List<MinecraftToken> {
        val tokens = mutableListOf<MinecraftToken>()

        while (!finished) {
            val char = peek()

            if (scope.escape != null && char == scope.escape) {
                consume()
                if (!finished) {
                    consume()
                }
                continue
            }

            if (peek(scope.end.length) == scope.end) {
                consume(scope.end.length)
                break
            }

            tokenizeSingle()?.let(tokens::add)
        }

        return tokens
    }
}

fun fallbackTokenizer(content: String, regexes: List<Regex>): List<MinecraftToken> {
    val tokens = mutableListOf<MinecraftToken>()
    var currentIndex = 0

    while (currentIndex < content.length) {
        var matchIndex = content.length
        var matchedLength = 0

        for (regex in regexes) {
            val match = regex.find(content, currentIndex)
            if (match != null && match.range.first >= currentIndex && match.range.first < matchIndex) {
                matchIndex = match.range.first
                matchedLength = match.value.length
            }
        }

        if (currentIndex < matchIndex) {
            tokens += MinecraftToken(
                type = "fallback",
                start = currentIndex,
                end = matchIndex,
                innerStart = currentIndex,
                innerEnd = matchIndex,
                tokens = emptyList()
            )
        }

        currentIndex = matchIndex + matchedLength
        if (matchedLength == 0 && currentIndex >= content.length) {
            break
        }
    }

    return tokens
}
