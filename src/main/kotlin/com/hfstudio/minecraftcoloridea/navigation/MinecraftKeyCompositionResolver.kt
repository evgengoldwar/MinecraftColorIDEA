package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.lang.MinecraftLocalizationCallParser

object MinecraftKeyCompositionResolver {
    fun resolveCandidates(
        source: String,
        expression: String,
        maxEnumeratedKeys: Int
    ): List<String> {
        if (maxEnumeratedKeys < 1) {
            return emptyList()
        }

        val sourcePrefix = source.substring(
            0,
            source.indexOf(expression).takeIf { it >= 0 } ?: source.length
        )

        return resolveExpression(
            sourcePrefix = sourcePrefix,
            expression = expression.trim(),
            maxEnumeratedKeys = maxEnumeratedKeys,
            visitedVariables = linkedSetOf()
        )
            .filter(::looksLikeLocalizationKey)
            .distinct()
            .take(maxEnumeratedKeys)
    }

    private fun resolveExpression(
        sourcePrefix: String,
        expression: String,
        maxEnumeratedKeys: Int,
        visitedVariables: Set<String>
    ): List<String> {
        if (expression.isEmpty()) {
            return emptyList()
        }

        unwrapEnclosingParentheses(expression)?.let { unwrapped ->
            return resolveExpression(sourcePrefix, unwrapped, maxEnumeratedKeys, visitedVariables)
        }

        decodeLiteral(expression)?.let { return listOf(it) }

        parseJavaTernary(expression)?.let { (whenTrue, whenFalse) ->
            return (resolveExpression(sourcePrefix, whenTrue, maxEnumeratedKeys, visitedVariables) +
                resolveExpression(sourcePrefix, whenFalse, maxEnumeratedKeys, visitedVariables))
                .distinct()
                .take(maxEnumeratedKeys)
        }

        parseKotlinIf(expression)?.let { (whenTrue, whenFalse) ->
            return (resolveExpression(sourcePrefix, whenTrue, maxEnumeratedKeys, visitedVariables) +
                resolveExpression(sourcePrefix, whenFalse, maxEnumeratedKeys, visitedVariables))
                .distinct()
                .take(maxEnumeratedKeys)
        }

        splitTopLevelConcatenation(expression)?.let { parts ->
            var accumulated = listOf("")
            parts.forEach { part ->
                val resolvedPart = resolveExpression(sourcePrefix, part, maxEnumeratedKeys, visitedVariables)
                if (resolvedPart.isEmpty()) {
                    return emptyList()
                }
                accumulated = crossJoin(accumulated, resolvedPart, maxEnumeratedKeys)
                if (accumulated.isEmpty()) {
                    return emptyList()
                }
            }
            return accumulated
        }

        if (expression.matches(IDENTIFIER_PATTERN) && expression !in visitedVariables) {
            findLatestAssignment(sourcePrefix, expression)?.let { assignedExpression ->
                return resolveExpression(
                    sourcePrefix = sourcePrefix,
                    expression = assignedExpression,
                    maxEnumeratedKeys = maxEnumeratedKeys,
                    visitedVariables = visitedVariables + expression
                )
            }
        }

        return emptyList()
    }

    private fun decodeLiteral(expression: String): String? {
        if (expression.length < 2 || !expression.startsWith('"') || !expression.endsWith('"')) {
            return null
        }
        return MinecraftLocalizationCallParser.decodeLiteral(expression.substring(1, expression.length - 1))
    }

    private fun unwrapEnclosingParentheses(expression: String): String? {
        if (!expression.startsWith('(') || !expression.endsWith(')')) {
            return null
        }

        var depth = 0
        var inString = false
        var escaped = false
        for (index in expression.indices) {
            val char = expression[index]
            if (escaped) {
                escaped = false
                continue
            }
            if (char == '\\' && inString) {
                escaped = true
                continue
            }
            if (char == '"') {
                inString = !inString
                continue
            }
            if (inString) {
                continue
            }
            if (char == '(') {
                depth += 1
            } else if (char == ')') {
                depth -= 1
                if (depth == 0 && index != expression.lastIndex) {
                    return null
                }
            }
        }
        return expression.substring(1, expression.length - 1).trim()
    }

    private fun parseJavaTernary(expression: String): Pair<String, String>? {
        var questionIndex = -1
        var colonIndex = -1
        var depth = 0
        var ternaryDepth = 0
        var inString = false
        var escaped = false

        expression.forEachIndexed { index, char ->
            if (escaped) {
                escaped = false
                return@forEachIndexed
            }
            if (char == '\\' && inString) {
                escaped = true
                return@forEachIndexed
            }
            if (char == '"') {
                inString = !inString
                return@forEachIndexed
            }
            if (inString) {
                return@forEachIndexed
            }
            when (char) {
                '(' -> depth += 1
                ')' -> depth -= 1
                '?' -> if (depth == 0) {
                    ternaryDepth += 1
                    if (questionIndex < 0) {
                        questionIndex = index
                    }
                }
                ':' -> if (depth == 0 && ternaryDepth > 0) {
                    ternaryDepth -= 1
                    if (ternaryDepth == 0 && questionIndex >= 0) {
                        colonIndex = index
                        return@forEachIndexed
                    }
                }
            }
        }

        if (questionIndex < 0 || colonIndex < 0) {
            return null
        }
        return expression.substring(questionIndex + 1, colonIndex).trim() to
            expression.substring(colonIndex + 1).trim()
    }

    private fun parseKotlinIf(expression: String): Pair<String, String>? {
        if (!expression.startsWith("if")) {
            return null
        }
        val match = Regex("""if\s*\([^)]*\)\s*(.+?)\s+else\s+(.+)""").matchEntire(expression) ?: return null
        return match.groupValues[1].trim() to match.groupValues[2].trim()
    }

    private fun splitTopLevelConcatenation(expression: String): List<String>? {
        val parts = mutableListOf<String>()
        var start = 0
        var depth = 0
        var inString = false
        var escaped = false
        var foundTopLevelPlus = false

        expression.forEachIndexed { index, char ->
            if (escaped) {
                escaped = false
                return@forEachIndexed
            }
            if (char == '\\' && inString) {
                escaped = true
                return@forEachIndexed
            }
            if (char == '"') {
                inString = !inString
                return@forEachIndexed
            }
            if (inString) {
                return@forEachIndexed
            }
            when (char) {
                '(' -> depth += 1
                ')' -> depth -= 1
                '+' -> if (depth == 0) {
                    val part = expression.substring(start, index).trim()
                    if (part.isNotEmpty()) {
                        parts += part
                    }
                    start = index + 1
                    foundTopLevelPlus = true
                }
            }
        }

        if (!foundTopLevelPlus) {
            return null
        }

        val trailing = expression.substring(start).trim()
        if (trailing.isNotEmpty()) {
            parts += trailing
        }
        return parts.takeIf { it.isNotEmpty() }
    }

    private fun findLatestAssignment(sourcePrefix: String, variableName: String): String? {
        val escapedName = Regex.escape(variableName)
        val patterns = listOf(
            Regex("""(?:val|var)\s+$escapedName\s*=\s*(.+)$"""),
            Regex("""$escapedName\s*=\s*(.+)$""")
        )

        return sourcePrefix.lineSequence()
            .toList()
            .asReversed()
            .firstNotNullOfOrNull { line ->
                patterns.firstNotNullOfOrNull { pattern ->
                    pattern.find(line.trim())?.groupValues?.get(1)?.trim()
                }
            }
    }

    private fun crossJoin(
        left: List<String>,
        right: List<String>,
        maxEnumeratedKeys: Int
    ): List<String> {
        val result = linkedSetOf<String>()
        left.forEach { leftValue ->
            right.forEach { rightValue ->
                result += leftValue + rightValue
                if (result.size >= maxEnumeratedKeys) {
                    return result.toList()
                }
            }
        }
        return result.toList()
    }

    private fun looksLikeLocalizationKey(value: String): Boolean {
        return value.contains('.') && value.any { it.isLetterOrDigit() }
    }

    private val IDENTIFIER_PATTERN = Regex("""[A-Za-z_][A-Za-z0-9_\.]*""")
}
