package com.hfstudio.minecraftcoloridea.lang

object MinecraftLangFileParser {
    fun parseLang(content: String): Map<String, String> {
        return content.lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
            .associate { line ->
                val index = line.indexOf('=')
                line.substring(0, index) to line.substring(index + 1)
            }
    }

    fun parseJson(content: String): Map<String, String> {
        val pattern = Regex(""""((?:\\.|[^"])*)"\\s*:\\s*"((?:\\.|[^"])*)"""")
        return pattern.findAll(content).associate { match ->
            decode(match.groupValues[1]) to decode(match.groupValues[2])
        }
    }

    private fun decode(value: String): String {
        return value
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }
}
