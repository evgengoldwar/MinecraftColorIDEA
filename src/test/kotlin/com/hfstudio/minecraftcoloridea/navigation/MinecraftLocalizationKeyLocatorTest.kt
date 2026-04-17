package com.hfstudio.minecraftcoloridea.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MinecraftLocalizationKeyLocatorTest {
    @Test
    fun resolvesKeyUnderCaretInsideStringLiteral() {
        val source = """list.add("tooltip.backpack")"""

        val resolved = MinecraftLocalizationKeyLocator().locate(
            source = source,
            caretOffset = source.indexOf("backpack")
        )

        assertEquals("tooltip.backpack", resolved?.key)
    }

    @Test
    fun resolvesSupportedLocalizationCallUnderCaret() {
        val source = """list.add(LangHelpers.localize("tooltip.backpack"))"""

        val resolved = MinecraftLocalizationKeyLocator().locate(
            source = source,
            caretOffset = source.indexOf("backpack")
        )

        val located = assertNotNull(resolved)
        assertEquals("tooltip.backpack", located.key)
        assertEquals("tooltip.backpack", source.substring(located.range.first, located.range.last + 1))
    }

    @Test
    fun resolvesExplicitStringLiteralUnderCaretWithoutDottedHeuristic() {
        val source = """list.add("Backpack title")"""

        val resolved = MinecraftLocalizationKeyLocator().locate(
            source = source,
            caretOffset = source.indexOf("title")
        )

        val located = assertNotNull(resolved)
        assertEquals("Backpack title", located.key)
        assertEquals("Backpack title", source.substring(located.range.first, located.range.last + 1))
    }

    @Test
    fun fallsBackToSingleResolvableKeyOnLineWhenCaretIsElsewhere() {
        val source = """val label = LangHelpers.localize("tooltip.backpack") + suffix"""

        val resolved = MinecraftLocalizationKeyLocator().locate(
            source = source,
            caretOffset = source.indexOf("suffix")
        )

        assertEquals("tooltip.backpack", resolved?.key)
    }

    @Test
    fun strictModeDoesNotFallbackFromOtherSymbolOnSameLine() {
        val source = """val label = LangHelpers.localize("tooltip.backpack") + suffix"""

        val resolved = MinecraftLocalizationKeyLocator().locateStrictly(
            source = source,
            caretOffset = source.indexOf("suffix")
        )

        assertNull(resolved)
    }

    @Test
    fun fallsBackToSingleResolvableCallKeyWhenCaretIsInNonKeyArgument() {
        val source = """I18n.format("tooltip.slot", "A")"""

        val resolved = MinecraftLocalizationKeyLocator().locate(
            source = source,
            caretOffset = source.indexOf("A")
        )

        assertEquals("tooltip.slot", resolved?.key)
    }

    @Test
    fun strictModeDoesNotFallbackFromOtherArgumentOnSameLine() {
        val source = """I18n.format("tooltip.slot", "A")"""

        val resolved = MinecraftLocalizationKeyLocator().locateStrictly(
            source = source,
            caretOffset = source.indexOf("A")
        )

        assertNull(resolved)
    }

    @Test
    fun declarationModeFallsBackInsideSupportedCallRange() {
        val source = """desc.add(StatCollector.translateToLocal("Tooltip_NinefoldInputHatch_00"));"""

        val resolved = MinecraftLocalizationKeyLocator().locateForDeclaration(
            source = source,
            caretOffset = source.indexOf("translateToLocal")
        )

        assertEquals("Tooltip_NinefoldInputHatch_00", resolved?.key)
    }

    @Test
    fun declarationModeDoesNotFallbackFromUnrelatedSymbolOnSameLine() {
        val source = """val label = LangHelpers.localize("tooltip.backpack") + suffix"""

        val resolved = MinecraftLocalizationKeyLocator().locateForDeclaration(
            source = source,
            caretOffset = source.indexOf("suffix")
        )

        assertNull(resolved)
    }

    @Test
    fun resolvesEnclosingCallKeyWhenMultipleSupportedCallsShareLine() {
        val source = """I18n.format("a", foo) + I18n.format("b", bar)"""
        val locator = MinecraftLocalizationKeyLocator()

        val left = locator.locate(source, source.indexOf("foo"))
        val right = locator.locate(source, source.indexOf("bar"))

        assertEquals("a", left?.key)
        assertEquals("b", right?.key)
    }

    @Test
    fun doesNotFallbackToPlainLiteralWhenCaretIsElsewhere() {
        val source = """val label = "Backpack title" + suffix"""

        val resolved = MinecraftLocalizationKeyLocator().locate(
            source = source,
            caretOffset = source.indexOf("suffix")
        )

        assertNull(resolved)
    }

    @Test
    fun ignoresObviousCommentOnlyLineContainingSupportedCallText() {
        val source = """// I18n.format("tooltip.backpack")"""

        val resolved = MinecraftLocalizationKeyLocator().locate(
            source = source,
            caretOffset = source.indexOf("backpack")
        )

        assertNull(resolved)
    }

    @Test
    fun ignoresSupportedCallTextInsideTrailingInlineComment() {
        val source = """val x = foo // I18n.format("tooltip.backpack")"""

        val resolved = MinecraftLocalizationKeyLocator().locate(
            source = source,
            caretOffset = source.indexOf("backpack")
        )

        assertNull(resolved)
    }

    @Test
    fun resolvesRealCallAfterClosedBlockCommentPrefix() {
        val source = """/* note */ I18n.format("tooltip.backpack")"""

        val resolved = MinecraftLocalizationKeyLocator().locate(
            source = source,
            caretOffset = source.indexOf("backpack")
        )

        assertEquals("tooltip.backpack", resolved?.key)
    }

    @Test
    fun doesNotGuessWhenMultipleUnrelatedKeysExistOnOneLine() {
        val source = """"tooltip.first" + "tooltip.second""""

        val resolved = MinecraftLocalizationKeyLocator().locate(
            source = source,
            caretOffset = source.indexOf("+")
        )

        assertNull(resolved)
    }
}
