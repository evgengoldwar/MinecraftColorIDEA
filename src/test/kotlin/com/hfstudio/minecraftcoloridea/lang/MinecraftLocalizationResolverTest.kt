package com.hfstudio.minecraftcoloridea.lang

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MinecraftLocalizationResolverTest {
    @Test
    fun resolvesLocalizedPreviewWithoutColorizingKeyLiteral() {
        val source = "\"\\u00a7e\" + LangHelpers.localize(\"tooltip.backpack\")"
        val index = MinecraftLangIndex(
            projectLocales = mapOf("en_us" to mapOf("tooltip.backpack" to "Backpack")),
            dependencyLocales = emptyMap()
        )

        val resolved = MinecraftLocalizationResolver(index).resolveExpression(
            source = source,
            localeOrder = listOf("en_us")
        )
        val preview = assertNotNull(resolved)

        assertEquals("Backpack", preview.previewText)
        assertEquals("#ffff55", preview.baseColorHex)
        assertTrue(preview.excludedSourceRanges.any { range ->
            source.substring(range.first, range.last + 1).contains("tooltip.backpack")
        })
    }

    @Test
    fun resolvesDirectLangKeyLiteralInSimpleConcatenation() {
        val source = "\"prefix: \" + \"tooltip.backpack\""
        val index = MinecraftLangIndex(
            projectLocales = mapOf("en_us" to mapOf("tooltip.backpack" to "Backpack")),
            dependencyLocales = emptyMap()
        )

        val resolved = MinecraftLocalizationResolver(index).resolveExpression(
            source = source,
            localeOrder = listOf("en_us")
        )

        assertEquals("prefix: Backpack", resolved?.previewText)
    }

    @Test
    fun expandsCommonMinecraftFormatPlaceholders() {
        val index = MinecraftLangIndex(
            projectLocales = mapOf("en_us" to mapOf("tooltip.slot" to "Slot %1\$s / %2\$d")),
            dependencyLocales = emptyMap()
        )

        val resolved = MinecraftLocalizationResolver(index).resolveCall(
            methodName = "I18n.format",
            key = "tooltip.slot",
            rawArgs = listOf("\"A\"", "3"),
            localeOrder = listOf("en_us")
        )

        assertEquals("Slot A / 3", resolved?.previewText)
    }

    @Test
    fun resolvesCustomLocalizationMethodNames() {
        val index = MinecraftLangIndex(
            projectLocales = mapOf("en_us" to mapOf("tooltip.custom" to "Custom value")),
            dependencyLocales = emptyMap()
        )

        val resolved = MinecraftLocalizationResolver(
            index = index,
            extraMethodNames = setOf("CustomLang.translate")
        ).resolveExpression(
            source = "CustomLang.translate(\"tooltip.custom\")",
            localeOrder = listOf("en_us")
        )

        assertEquals("Custom value", resolved?.previewText)
    }

    @Test
    fun leadingFormattingInLocalizedValueSetsPreviewBaseColor() {
        val index = MinecraftLangIndex(
            projectLocales = mapOf("en_us" to mapOf("tooltip.colored" to "\\u00a7cColored value")),
            dependencyLocales = emptyMap()
        )

        val resolved = MinecraftLocalizationResolver(index).resolveExpression(
            source = "\"tooltip.colored\"",
            localeOrder = listOf("en_us")
        )

        assertEquals("Colored value", resolved?.previewText)
        assertEquals("#ff5555", resolved?.baseColorHex)
    }

    @Test
    fun leadingFormattingInLocalizedValueSetsPreviewBaseFormatting() {
        val index = MinecraftLangIndex(
            projectLocales = mapOf(
                "en_us" to mapOf("tooltip.styled" to "\\u00a7f\\u00a7l\\u00a7oStyled value")
            ),
            dependencyLocales = emptyMap()
        )

        val resolved = MinecraftLocalizationResolver(index).resolveExpression(
            source = "\"tooltip.styled\"",
            localeOrder = listOf("en_us")
        )

        assertEquals("Styled value", resolved?.previewText)
        assertEquals("#ffffff", resolved?.baseColorHex)
        assertTrue(resolved?.baseFormatting?.bold == true)
        assertTrue(resolved?.baseFormatting?.italic == true)
    }

    @Test
    fun decodesUnicodeEscapesInsideFormattedArguments() {
        val index = MinecraftLangIndex(
            projectLocales = mapOf(
                "en_us" to mapOf(
                    "tooltip.backpack.contents.stack_multiplier" to "Stack multiplier: %s"
                )
            ),
            dependencyLocales = emptyMap()
        )

        val resolved = MinecraftLocalizationResolver(index).resolveExpression(
            source = "\"\\u00a7a\" + LangHelpers.localize(\"tooltip.backpack.contents.stack_multiplier\", \"\\u221E\")",
            localeOrder = listOf("en_us")
        )

        assertEquals("Stack multiplier: \u221E", resolved?.previewText)
        assertEquals("#55ff55", resolved?.baseColorHex)
    }
}
