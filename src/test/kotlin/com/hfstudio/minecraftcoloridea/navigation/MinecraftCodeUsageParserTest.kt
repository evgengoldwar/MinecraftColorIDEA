package com.hfstudio.minecraftcoloridea.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftCodeUsageParserTest {
    @Test
    fun indexesExactLocalizationCallLiteral() {
        val text = """list.add(LangHelpers.localize("tooltip.backpack"))"""

        val usages = MinecraftCodeUsageParser().parseFile(
            filePath = "src/main/java/example/BackpackItem.java",
            text = text,
            maxEnumeratedKeys = 8
        )

        assertEquals(listOf("tooltip.backpack"), usages.map(MinecraftCodeUsageEntry::key))
        assertEquals(MinecraftCodeUsageConfidence.EXACT, usages.single().confidence)
        assertEquals("src/main/java/example/BackpackItem.java", usages.single().filePath)
    }

    @Test
    fun expandsConditionalCompositionIntoEnumeratedKeys() {
        val text = """I18n.format("lang." + (flag ? "1" : "2"))"""

        val usages = MinecraftCodeUsageParser().parseFile(
            filePath = "src/main/java/example/Example.java",
            text = text,
            maxEnumeratedKeys = 8
        )

        assertEquals(listOf("lang.1", "lang.2"), usages.map(MinecraftCodeUsageEntry::key).sorted())
        assertEquals(setOf(MinecraftCodeUsageConfidence.ENUMERATED), usages.map(MinecraftCodeUsageEntry::confidence).toSet())
    }

    @Test
    fun expandsSimpleLocalVariableComposition() {
        val text = """
            val prefix = "tooltip."
            val suffix = if (flag) "alpha" else "beta"
            I18n.format(prefix + suffix)
        """.trimIndent()

        val usages = MinecraftCodeUsageParser().parseFile(
            filePath = "src/main/kotlin/example/Example.kt",
            text = text,
            maxEnumeratedKeys = 8
        )

        assertEquals(listOf("tooltip.alpha", "tooltip.beta"), usages.map(MinecraftCodeUsageEntry::key).sorted())
        assertEquals(setOf(MinecraftCodeUsageConfidence.ENUMERATED), usages.map(MinecraftCodeUsageEntry::confidence).toSet())
    }

    @Test
    fun indexesExplicitLocalizationKeyLiteralOutsideSupportedCalls() {
        val text = """
            public static UpgradeSlotChangeResult failStorageCapacityLow(int[] upgradeConflictSlots,
                String formattedMultiplier) {
                return fail("gui.backpack.error.remove.stack_low_multiplier", upgradeConflictSlots, formattedMultiplier);
            }
        """.trimIndent()

        val usages = MinecraftCodeUsageParser().parseFile(
            filePath = "src/main/java/example/BackpackErrors.java",
            text = text,
            maxEnumeratedKeys = 8
        )

        val matched = usages.singleOrNull { it.key == "gui.backpack.error.remove.stack_low_multiplier" }
        assertEquals("gui.backpack.error.remove.stack_low_multiplier", matched?.key)
        assertEquals(MinecraftCodeUsageConfidence.EXACT, matched?.confidence)
    }

    @Test
    fun ignoresHugeUnterminatedEscapedStringWithoutStackOverflow() {
        val text = "val broken = \"" + "\\".repeat(200_000) + "x"

        val usages = MinecraftCodeUsageParser().parseFile(
            filePath = "src/main/kotlin/example/Broken.kt",
            text = text,
            maxEnumeratedKeys = 8
        )

        assertEquals(emptyList(), usages)
    }
}
