package com.hfstudio.minecraftcoloridea.navigation

import com.intellij.mock.MockProjectEx
import com.intellij.testFramework.LightVirtualFile
import com.intellij.openapi.util.Disposer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MinecraftCodeUsageIndexServiceTest {
    @Test
    fun refreshDocumentReplacesOnlyThatFilesExactUsageEntries() {
        val disposable = Disposer.newDisposable()
        try {
            val service = MinecraftCodeUsageIndexService(MockProjectEx(disposable))
            val file = TestVirtualFile("E:/Github/ExampleMod/src/main/java/example/BackpackItem.java")

            service.refreshDocument(file, """I18n.format("tooltip.first")""", maxEnumeratedKeys = 8)
            service.refreshDocument(file, """I18n.format("tooltip.second")""", maxEnumeratedKeys = 8)

            assertEquals(listOf("tooltip.second"), service.lookup("tooltip.second")!!.map(MinecraftCodeUsageEntry::key))
            assertNull(service.lookup("tooltip.first"))
        } finally {
            Disposer.dispose(disposable)
        }
    }

    @Test
    fun removingCodeFileRemovesReverseUsageEntries() {
        val disposable = Disposer.newDisposable()
        try {
            val service = MinecraftCodeUsageIndexService(MockProjectEx(disposable))
            val file = TestVirtualFile("E:/Github/ExampleMod/src/main/java/example/BackpackItem.java")

            service.refreshDocument(file, """I18n.format("tooltip.first")""", maxEnumeratedKeys = 8)
            service.removeProjectFile(file.path)

            assertNull(service.lookup("tooltip.first"))
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private class TestVirtualFile(private val pathValue: String) : LightVirtualFile(pathValue.substringAfterLast('/')) {
        override fun getPath(): String = pathValue
    }
}
