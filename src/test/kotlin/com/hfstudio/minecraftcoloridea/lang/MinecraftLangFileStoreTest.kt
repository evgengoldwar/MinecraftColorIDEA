package com.hfstudio.minecraftcoloridea.lang

import kotlin.test.Test
import kotlin.test.assertEquals

class MinecraftLangFileStoreTest {
    @Test
    fun replacingFileUpdatesLocaleSnapshotAndReturnsChangedKeys() {
        val store = MinecraftLangFileStore()

        val initialChanged = store.replaceFile(
            path = "assets/example/lang/en_us.lang",
            locale = "en_us",
            entries = mapOf(
                "tooltip.old" to "Old value"
            )
        )
        val changed = store.replaceFile(
            path = "assets/example/lang/en_us.lang",
            locale = "en_us",
            entries = mapOf(
                "tooltip.old" to "New value",
                "tooltip.added" to "Added value"
            )
        )

        assertEquals(setOf("tooltip.old"), initialChanged)
        assertEquals(setOf("tooltip.old", "tooltip.added"), changed)
        assertEquals(
            "New value",
            store.snapshot()["en_us"]?.get("tooltip.old")
        )
        assertEquals(
            "Added value",
            store.snapshot()["en_us"]?.get("tooltip.added")
        )
    }

    @Test
    fun removingFileDropsKeysFromSnapshot() {
        val store = MinecraftLangFileStore()
        store.replaceFile(
            path = "assets/example/lang/en_us.lang",
            locale = "en_us",
            entries = mapOf(
                "tooltip.old" to "Old value",
                "tooltip.other" to "Other value"
            )
        )

        val removed = store.removeFile("assets/example/lang/en_us.lang")

        assertEquals(setOf("tooltip.old", "tooltip.other"), removed)
        assertEquals(emptyMap(), store.snapshot())
    }
}
