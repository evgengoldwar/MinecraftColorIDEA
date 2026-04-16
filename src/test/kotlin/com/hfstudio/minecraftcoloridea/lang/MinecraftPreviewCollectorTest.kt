package com.hfstudio.minecraftcoloridea.lang

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinecraftPreviewCollectorTest {
    private val index = MinecraftLangIndex(
        projectLocales = mapOf(
            "en_us" to mapOf(
                "tooltip.backpack" to "Backpack",
                "tooltip.colored" to "\\u00a7cColored value"
            )
        ),
        dependencyLocales = emptyMap()
    )

    @Test
    fun collectsLocalizedPreviewFromSingleLineExpression() {
        val text = "list.add(\"\\u00a7e\" + LangHelpers.localize(\"tooltip.backpack\"));"

        val previews = MinecraftPreviewCollector(index).collect(
            text = text,
            localeOrder = listOf("en_us")
        )

        assertEquals(1, previews.size)
        assertEquals("Backpack", previews.single().preview.previewText)
        assertEquals("#ffff55", previews.single().preview.baseColorHex)
        assertEquals(text.length - 1, previews.single().anchorOffset)
    }

    @Test
    fun ignoresCommentOnlyLines() {
        val text = "// LangHelpers.localize(\"tooltip.backpack\")"

        val previews = MinecraftPreviewCollector(index).collect(
            text = text,
            localeOrder = listOf("en_us")
        )

        assertTrue(previews.isEmpty())
    }
}
