package com.hfstudio.minecraftcoloridea.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MinecraftExternalFileSupportTest {
    @Test
    fun detectsProjectOwnedAndExternalFilesSeparately() {
        assertTrue(
            MinecraftEditorFileScope.isProjectOwned(
                projectBasePath = "E:/Github/ExampleMod",
                filePath = "E:/Github/ExampleMod/src/main/resources/assets/example/lang/en_us.lang"
            )
        )
        assertFalse(
            MinecraftEditorFileScope.isProjectOwned(
                projectBasePath = "E:/Github/ExampleMod",
                filePath = "D:/Temp/assets/example/lang/en_us.lang"
            )
        )
        assertFalse(
            MinecraftEditorFileScope.isProjectOwned(
                projectBasePath = "E:/Github/ExampleMod",
                filePath = "E:/Github/ExampleModBackup/src/main/resources/assets/example/lang/en_us.lang"
            )
        )
        assertFalse(
            MinecraftEditorFileScope.isProjectOwned(
                projectBasePath = null,
                filePath = "D:/Temp/assets/example/lang/en_us.lang"
            )
        )
    }

    @Test
    fun retainsReleasedExternalFilesOnlyForShortWindow() {
        var now = 1_000L
        val cache = MinecraftExternalFileCache(
            retentionMillis = 500L,
            nowMillis = { now }
        )
        val path = "D:/Temp/example/lang/en_us.lang"

        cache.markOpened(path)
        assertTrue(cache.isTracked(path))

        cache.markReleased(path)
        assertTrue(cache.isTracked(path))

        now += 499L
        assertTrue(cache.isTracked(path))

        now += 2L
        assertFalse(cache.isTracked(path))
        assertEquals(emptySet(), cache.trackedPaths())
    }
}
