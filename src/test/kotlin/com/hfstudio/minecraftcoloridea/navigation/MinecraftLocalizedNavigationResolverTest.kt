package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceEntry
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MinecraftLocalizedNavigationResolverTest {
    @Test
    fun ordersAllLocaleMatchesByPreferredLocaleBeforeEnglishFallback() {
        val store = MinecraftLangSourceStore().apply {
            replaceFile(
                path = "zh_cn.lang",
                locale = "zh_cn",
                entries = listOf(MinecraftLangSourceEntry("zh_cn", "tooltip.backpack", "zh_cn.lang", 8, 0))
            )
            replaceFile(
                path = "en_us.lang",
                locale = "en_us",
                entries = listOf(MinecraftLangSourceEntry("en_us", "tooltip.backpack", "en_us.lang", 2, 0))
            )
        }

        val resolved = MinecraftLocalizedNavigationResolver.resolve(
            key = "tooltip.backpack",
            localeOrder = listOf("zh_cn", "en_us"),
            sourceStore = store
        )

        val target = assertNotNull(resolved)
        assertEquals("zh_cn", target.locale)
        assertEquals(listOf("zh_cn.lang", "en_us.lang"), target.entries.map(MinecraftLangSourceEntry::filePath))
    }

    @Test
    fun skipsMissingPreferredLocaleBeforeFallingBackToEnglish() {
        val store = MinecraftLangSourceStore().apply {
            replaceFile(
                path = "en_us.lang",
                locale = "en_us",
                entries = listOf(MinecraftLangSourceEntry("en_us", "tooltip.backpack", "en_us.lang", 2, 0))
            )
        }

        val resolved = MinecraftLocalizedNavigationResolver.resolve(
            key = "tooltip.backpack",
            localeOrder = listOf("zh_cn", "en_us"),
            sourceStore = store
        )

        val target = assertNotNull(resolved)
        assertEquals("en_us", target.locale)
        assertEquals(listOf("en_us.lang"), target.entries.map(MinecraftLangSourceEntry::filePath))
    }

    @Test
    fun keepsAllSamePriorityMatchesAndAppendsOtherLocalesAfterThem() {
        val store = MinecraftLangSourceStore().apply {
            replaceFile(
                path = "first.json",
                locale = "zh_cn",
                entries = listOf(MinecraftLangSourceEntry("zh_cn", "tooltip.backpack", "first.json", 4, 10))
            )
            replaceFile(
                path = "second.json",
                locale = "zh_cn",
                entries = listOf(MinecraftLangSourceEntry("zh_cn", "tooltip.backpack", "second.json", 9, 0))
            )
            replaceFile(
                path = "en_us.json",
                locale = "en_us",
                entries = listOf(MinecraftLangSourceEntry("en_us", "tooltip.backpack", "en_us.json", 1, 0))
            )
            replaceFile(
                path = "ja_jp.json",
                locale = "ja_jp",
                entries = listOf(MinecraftLangSourceEntry("ja_jp", "tooltip.backpack", "ja_jp.json", 7, 0))
            )
        }

        val resolved = MinecraftLocalizedNavigationResolver.resolve(
            key = "tooltip.backpack",
            localeOrder = listOf("zh_cn", "en_us"),
            sourceStore = store
        )

        val target = assertNotNull(resolved)
        assertEquals("zh_cn", target.locale)
        assertEquals(
            listOf("first.json", "second.json", "en_us.json", "ja_jp.json"),
            target.entries.map(MinecraftLangSourceEntry::filePath)
        )
    }

    @Test
    fun returnsNullWhenNoLocalizedEntryExists() {
        val resolved = MinecraftLocalizedNavigationResolver.resolve(
            key = "tooltip.missing",
            localeOrder = listOf("zh_cn", "en_us"),
            sourceStore = MinecraftLangSourceStore()
        )

        assertNull(resolved)
    }
}
