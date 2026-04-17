package com.hfstudio.minecraftcoloridea.lang

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class MinecraftLangSourceStoreTest {
    @Test
    fun lookupReusesIndexedEntriesUntilTrackedFilesChange() {
        val store = MinecraftLangSourceStore()
        store.replaceFile(
            path = "src/main/resources/assets/example/lang/zh_cn.lang",
            locale = "zh_cn",
            entries = listOf(
                MinecraftLangSourceEntry("zh_cn", "tooltip.first", "a.lang", 1, 0)
            )
        )

        val firstLookup = store.lookup("tooltip.first", listOf("zh_cn"))
        val secondLookup = store.lookup("tooltip.first", listOf("ZH_CN", "zh_cn"))

        assertSame(firstLookup, secondLookup)

        store.replaceFile(
            path = "src/main/resources/assets/example/lang/zh_cn.lang",
            locale = "zh_cn",
            entries = listOf(
                MinecraftLangSourceEntry("zh_cn", "tooltip.first", "b.lang", 3, 24)
            )
        )

        val updatedLookup = store.lookup("tooltip.first", listOf("zh_cn"))

        assertNotSame(firstLookup, updatedLookup)
        assertEquals(3, updatedLookup!!.single().lineNumber)

        store.removeFile("src/main/resources/assets/example/lang/zh_cn.lang")

        assertNull(store.lookup("tooltip.first", listOf("zh_cn")))
    }

    @Test
    fun lookupAllReturnsEntriesAcrossAllLocales() {
        val store = MinecraftLangSourceStore()
        store.replaceFile(
            path = "src/main/resources/assets/example/lang/zh_cn.lang",
            locale = "zh_cn",
            entries = listOf(
                MinecraftLangSourceEntry("zh_cn", "tooltip.first", "zh_cn.lang", 1, 0)
            )
        )
        store.replaceFile(
            path = "src/main/resources/assets/example/lang/en_us.lang",
            locale = "en_us",
            entries = listOf(
                MinecraftLangSourceEntry("en_us", "tooltip.first", "en_us.lang", 2, 10)
            )
        )

        assertEquals(
            listOf("zh_cn.lang", "en_us.lang"),
            store.lookupAll("tooltip.first")!!.map(MinecraftLangSourceEntry::filePath)
        )
    }
}
