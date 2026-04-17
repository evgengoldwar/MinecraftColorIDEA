package com.hfstudio.minecraftcoloridea.lang

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MinecraftLocalizationCallParserTest {
    @Test
    fun extractsSupportedLocalizationCallKeyAndRange() {
        val source = """LangHelpers.localize("tooltip.backpack")"""

        val match = MinecraftLocalizationCallParser.findFirst(source)

        val resolved = assertNotNull(match)
        assertEquals("LangHelpers.localize", resolved.methodName)
        assertEquals("tooltip.backpack", resolved.key)
        assertEquals("tooltip.backpack", source.substring(resolved.keyRange))
    }

    @Test
    fun parsesFormattedCallArgumentsWithoutLosingKeyRange() {
        val source = """I18n.format("tooltip.slot", "A", computeValue(2, "B"))"""

        val match = MinecraftLocalizationCallParser.findFirst(source)

        val resolved = assertNotNull(match)
        assertEquals("tooltip.slot", resolved.key)
        assertEquals(listOf("\"A\"", "computeValue(2, \"B\")"), resolved.rawArgs)
        assertEquals("tooltip.slot", source.substring(resolved.keyRange))
    }

    @Test
    fun returnsNullWhenFirstArgumentIsNotAStringLiteral() {
        assertNull(MinecraftLocalizationCallParser.findFirst("""I18n.format(keyName)"""))
    }

    private fun String.substring(range: IntRange): String {
        assertTrue(range.first >= 0)
        assertTrue(range.last < length)
        return substring(range.first, range.last + 1)
    }
}
