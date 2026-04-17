package com.hfstudio.minecraftcoloridea.lang

import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftLangFileParserTest {
    @Test
    fun parsesSimpleJsonKeyValueEntries() {
        val content = """
            {
              "tooltip.backpack": "Backpack",
              "tooltip.slot": "Slot"
            }
        """.trimIndent()

        assertEquals(
            mapOf(
                "tooltip.backpack" to "Backpack",
                "tooltip.slot" to "Slot"
            ),
            MinecraftLangFileParser.parseJson(content)
        )
    }

    @Test
    fun ignoresHugeUnterminatedEscapedJsonStringWithoutStackOverflow() {
        val content = "{\"tooltip.backpack\":\"" + "\\".repeat(200_000) + "x"

        assertEquals(emptyMap(), MinecraftLangFileParser.parseJson(content))
        assertEquals(
            emptyList(),
            MinecraftLangFileParser.parseJsonSources(
                content = content,
                locale = "en_us",
                filePath = "assets/example/lang/en_us.json"
            )
        )
    }
}
