package com.hfstudio.minecraftcoloridea.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftKeyCompositionResolverTest {
    @Test
    fun resolvesJavaTernaryConcatenation() {
        val source = """I18n.format("lang." + (flag ? "1" : "2"))"""

        assertEquals(
            listOf("lang.1", "lang.2"),
            MinecraftKeyCompositionResolver.resolveCandidates(
                source = source,
                expression = "\"lang.\" + (flag ? \"1\" : \"2\")",
                maxEnumeratedKeys = 8
            ).sorted()
        )
    }

    @Test
    fun resolvesSimpleVariableAssignmentsNearUsage() {
        val source = """
            val prefix = "tooltip."
            val suffix = if (flag) "alpha" else "beta"
            I18n.format(prefix + suffix)
        """.trimIndent()

        assertEquals(
            listOf("tooltip.alpha", "tooltip.beta"),
            MinecraftKeyCompositionResolver.resolveCandidates(
                source = source,
                expression = "prefix + suffix",
                maxEnumeratedKeys = 8
            ).sorted()
        )
    }
}
