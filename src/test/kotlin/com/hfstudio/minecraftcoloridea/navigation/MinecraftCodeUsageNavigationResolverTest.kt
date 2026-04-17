package com.hfstudio.minecraftcoloridea.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftCodeUsageNavigationResolverTest {
    @Test
    fun returnsIndexNotReadyWhenUsageIndexIsCold() {
        val result = MinecraftCodeUsageNavigationResolver.resolve(
            key = "tooltip.backpack",
            usageIndexStamp = 0L
        ) { emptyList() }

        assertEquals(MinecraftCodeUsageNavigationResolver.NavigationRequestResult.IndexNotReady, result)
    }

    @Test
    fun sortsExactEntriesAheadOfDerivedAndEnumeratedOnes() {
        val result = MinecraftCodeUsageNavigationResolver.resolve(
            key = "tooltip.backpack",
            usageIndexStamp = 1L
        ) {
            listOf(
                testEntry("b.kt", 4, MinecraftCodeUsageConfidence.ENUMERATED),
                testEntry("a.kt", 3, MinecraftCodeUsageConfidence.EXACT),
                testEntry("c.kt", 5, MinecraftCodeUsageConfidence.FAMILY_DERIVED)
            )
        }

        val target = result as MinecraftCodeUsageNavigationResolver.NavigationRequestResult.Target
        assertEquals(
            listOf("a.kt", "c.kt", "b.kt"),
            target.target.entries.map(MinecraftCodeUsageEntry::filePath)
        )
    }

    private fun testEntry(
        filePath: String,
        lineNumber: Int,
        confidence: MinecraftCodeUsageConfidence
    ) = MinecraftCodeUsageEntry(
        key = "tooltip.backpack",
        filePath = filePath,
        lineNumber = lineNumber,
        lineStartOffset = 0,
        matchStartOffset = 0,
        matchEndOffset = 16,
        snippet = """I18n.format("tooltip.backpack")""",
        confidence = confidence
    )
}
