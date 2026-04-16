package com.hfstudio.minecraftcoloridea.lang

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MinecraftLangIndexServiceTest {
    @Test
    fun resolvesPreferredThenSecondaryThenEnglishLocales() {
        val index = MinecraftLangIndex(
            projectLocales = mapOf(
                "zh_cn" to mapOf("tooltip.backpack" to "\\u00a7eBackpackZh"),
                "ja_jp" to mapOf("tooltip.backpack" to "\\u00a7eBackpackJa"),
                "en_us" to mapOf("tooltip.backpack" to "\\u00a7eBackpack")
            ),
            dependencyLocales = emptyMap()
        )

        assertEquals("\\u00a7eBackpackZh", index.lookup("tooltip.backpack", listOf("zh_cn", "ja_jp", "en_us")))
        assertEquals("\\u00a7eBackpackJa", index.lookup("tooltip.backpack", listOf("ru_ru", "ja_jp", "en_us")))
        assertEquals("\\u00a7eBackpack", index.lookup("tooltip.backpack", listOf("ru_ru", "ko_kr", "en_us")))
        assertNull(index.lookup("tooltip.missing", listOf("zh_cn", "en_us")))
    }

    @Test
    fun prefersProjectResourcesBeforeDependencyResources() {
        val index = MinecraftLangIndex(
            projectLocales = mapOf("en_us" to mapOf("tooltip.backpack" to "ProjectValue")),
            dependencyLocales = mapOf("en_us" to mapOf("tooltip.backpack" to "DependencyValue"))
        )

        assertEquals("ProjectValue", index.lookup("tooltip.backpack", listOf("en_us")))
    }
}
