package com.hfstudio.minecraftcoloridea.navigation

import com.intellij.mock.MockProjectEx
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightVirtualFile
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
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

    @Test
    fun refreshChangedFilesUsesVirtualFileCharsetForMatchOffsets() {
        val disposable = Disposer.newDisposable()
        try {
            val service = MinecraftCodeUsageIndexService(MockProjectEx(disposable))
            val content = """
                // header
                val text = I18n.format("tooltip.backpack")
            """.trimIndent()
            val file = TestVirtualFile(
                pathValue = "E:/Github/ExampleMod/src/main/kotlin/example/BackpackItem.kt",
                text = content,
                charset = StandardCharsets.UTF_16LE
            )

            service.refreshChangedFiles(sequenceOf(file), maxEnumeratedKeys = 8)

            val entry = service.lookup("tooltip.backpack")!!.single()
            assertEquals(2, entry.lineNumber)
            assertEquals(content.indexOf("tooltip.backpack"), entry.matchStartOffset)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    @Test
    fun refreshChangedFilesNormalizesCrlfOffsetsForCodeNavigation() {
        val disposable = Disposer.newDisposable()
        try {
            val service = MinecraftCodeUsageIndexService(MockProjectEx(disposable))
            val content = """
                // header
                desc.add(StatCollector.translateToLocal("Tooltip_Other"));
                desc.add(StatCollector.translateToLocal("Tooltip_NinefoldInputHatch_00"));
            """.trimIndent().replace("\n", "\r\n")
            val normalized = content.replace("\r\n", "\n")
            val file = TestVirtualFile(
                pathValue = "E:/Github/ExampleMod/src/main/java/example/NinefoldInputHatch.java",
                text = content
            )

            service.refreshChangedFiles(sequenceOf(file), maxEnumeratedKeys = 8)

            val entry = service.lookup("Tooltip_NinefoldInputHatch_00")!!.single {
                it.filePath.endsWith("NinefoldInputHatch.java")
            }
            assertEquals(3, entry.lineNumber)
            assertEquals(
                normalized.indexOf("Tooltip_NinefoldInputHatch_00"),
                entry.matchStartOffset
            )
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private class TestVirtualFile(
        private val pathValue: String,
        text: String = "",
        charset: Charset = StandardCharsets.UTF_8
    ) : LightVirtualFile(pathValue.substringAfterLast('/')) {
        private val fileCharset = charset
        private val bytes = text.toByteArray(charset)

        override fun getPath(): String = pathValue

        override fun contentsToByteArray(): ByteArray = bytes

        override fun getInputStream(): InputStream = ByteArrayInputStream(bytes)

        override fun getCharset(): Charset = fileCharset
    }
}
