package com.hfstudio.minecraftcoloridea.editor

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.testFramework.LightVirtualFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinecraftProjectFileRulesTest {
    @Test
    fun collectsStaleProjectLangPathsForDeleteRenameAndMoveEvents() {
        val oldParent = TestVirtualFile(
            pathValue = "E:/Github/ExampleMod/src/main/resources/assets/example/lang",
            directory = true
        )
        val newParent = TestVirtualFile(
            pathValue = "E:/Github/ExampleMod/src/generated/resources/assets/example/lang",
            directory = true
        )
        val externalParent = TestVirtualFile(
            pathValue = "D:/Temp/assets/example/lang",
            directory = true
        )
        val langFile = TestVirtualFile(
            pathValue = "${oldParent.path}/zh_cn.lang",
            parentFile = oldParent
        )

        val stalePaths = MinecraftProjectFileRules.staleProjectLangPaths(
            projectBasePath = "E:/Github/ExampleMod",
            events = listOf(
                VFileDeleteEvent(this, langFile, false),
                VFilePropertyChangeEvent(this, langFile, VirtualFile.PROP_NAME, "zh_cn.lang", "en_us.lang", false),
                VFileMoveEvent(this, langFile, newParent),
                VFileMoveEvent(this, langFile, externalParent)
            )
        )

        assertEquals(
            setOf("E:/Github/ExampleMod/src/main/resources/assets/example/lang/zh_cn.lang"),
            stalePaths
        )
    }

    @Test
    fun recognizesNeoforgeModsTomlAsVersionSignal() {
        assertTrue(
            MinecraftProjectFileRules.isVersionSignalFile(
                "E:/Github/ExampleMod/src/main/resources/META-INF/neoforge.mods.toml"
            )
        )
    }

    @Test
    fun collectsStaleProjectCodePathsForDeleteRenameAndMoveEvents() {
        val oldParent = TestVirtualFile(
            pathValue = "E:/Github/ExampleMod/src/main/java/example",
            directory = true
        )
        val newParent = TestVirtualFile(
            pathValue = "E:/Github/ExampleMod/build/generated/example",
            directory = true
        )
        val codeFile = TestVirtualFile(
            pathValue = "${oldParent.path}/BackpackItem.java",
            parentFile = oldParent
        )

        val stalePaths = MinecraftProjectFileRules.staleProjectCodeUsagePaths(
            projectBasePath = "E:/Github/ExampleMod",
            events = listOf(
                VFileDeleteEvent(this, codeFile, false),
                VFilePropertyChangeEvent(this, codeFile, VirtualFile.PROP_NAME, "BackpackItem.java", "SatchelItem.java", false),
                VFileMoveEvent(this, codeFile, newParent)
            )
        )

        assertEquals(
            setOf("E:/Github/ExampleMod/src/main/java/example/BackpackItem.java"),
            stalePaths
        )
    }

    private class TestVirtualFile(
        private val pathValue: String,
        private val directory: Boolean = false,
        private val parentFile: VirtualFile? = null
    ) : LightVirtualFile(pathValue.substringAfterLast('/')) {
        override fun getPath(): String = pathValue

        override fun getParent(): VirtualFile? = parentFile

        override fun isDirectory(): Boolean = directory
    }
}
