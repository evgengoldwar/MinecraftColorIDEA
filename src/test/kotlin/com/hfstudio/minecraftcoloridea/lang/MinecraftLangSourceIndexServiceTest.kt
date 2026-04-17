package com.hfstudio.minecraftcoloridea.lang

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

class MinecraftLangSourceIndexServiceTest {
    @Test
    fun storesExactLineNumbersAndRemovesDeletedFiles() {
        val store = MinecraftLangSourceStore()
        store.replaceFile(
            path = "src/main/resources/assets/example/lang/zh_cn.lang",
            locale = "zh_cn",
            entries = listOf(
                MinecraftLangSourceEntry("zh_cn", "tooltip.first", "a.lang", 1, 0),
                MinecraftLangSourceEntry("zh_cn", "tooltip.second", "a.lang", 2, 12)
            )
        )

        assertEquals(2, store.lookup("tooltip.second", listOf("zh_cn"))!!.single().lineNumber)

        store.removeFile("src/main/resources/assets/example/lang/zh_cn.lang")

        assertNull(store.lookup("tooltip.second", listOf("zh_cn")))
    }

    @Test
    fun refreshDocumentParsesRealLangContentAndReplacesOldEntries() {
        val disposable = Disposer.newDisposable()
        try {
            val service = MinecraftLangSourceIndexService(MockProjectEx(disposable))
            val file = TestVirtualFile("E:/Github/ExampleMod/src/main/resources/assets/example/lang/zh_cn.lang")
            val initialContent = """
                # comment

                tooltip.first=First
                tooltip.second=Second
            """.trimIndent()
            val updatedContent = """
                tooltip.second=Updated
            """.trimIndent()

            service.refreshDocument(file, initialContent)

            val originalEntry = service.lookup("tooltip.second", listOf("ZH_CN"))!!.single()
            assertEquals(4, originalEntry.lineNumber)
            assertEquals(initialContent.indexOf("tooltip.second"), originalEntry.lineStartOffset)

            service.refreshDocument(file, updatedContent)

            assertNull(service.lookup("tooltip.first", listOf("zh_cn")))
            val updatedEntry = service.lookup("tooltip.second", listOf("zh_cn"))!!.single()
            assertEquals(1, updatedEntry.lineNumber)
            assertEquals(updatedContent.indexOf("tooltip.second"), updatedEntry.lineStartOffset)

            service.removeProjectFile(file.path)

            assertNull(service.lookup("tooltip.second", listOf("zh_cn")))
        } finally {
            Disposer.dispose(disposable)
        }
    }

    @Test
    fun refreshDocumentParsesRealJsonContentWithExactLineMetadata() {
        val disposable = Disposer.newDisposable()
        try {
            val service = MinecraftLangSourceIndexService(MockProjectEx(disposable))
            val content = "{\n  \"tooltip.first\": \"First\",\n  \"tooltip.second\": \"Second\"\n}"
            val file = TestVirtualFile("E:/Github/ExampleMod/src/main/resources/assets/example/lang/EN_US.json")

            service.refreshDocument(file, content)

            val entry = service.lookup("tooltip.second", listOf("en_us"))!!.single()
            assertEquals("en_us", entry.locale)
            assertEquals(3, entry.lineNumber)
            assertEquals(content.indexOf("  \"tooltip.second\""), entry.lineStartOffset)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    @Test
    fun parseJsonAndRefreshDocumentKeepEntriesWhenOneSharesLineWithBrace() {
        val disposable = Disposer.newDisposable()
        try {
            val service = MinecraftLangSourceIndexService(MockProjectEx(disposable))
            val content = "{\"tooltip.first\": \"First\",\n  \"tooltip.second\": \"Second\"\n}"
            val file = TestVirtualFile("E:/Github/ExampleMod/src/main/resources/assets/example/lang/en_us.json")

            assertEquals(
                mapOf(
                    "tooltip.first" to "First",
                    "tooltip.second" to "Second"
                ),
                MinecraftLangFileParser.parseJson(content)
            )

            service.refreshDocument(file, content)

            val firstEntry = service.lookup("tooltip.first", listOf("en_us"))!!.single()
            assertEquals(1, firstEntry.lineNumber)
            assertEquals(0, firstEntry.lineStartOffset)

            val secondEntry = service.lookup("tooltip.second", listOf("en_us"))!!.single()
            assertEquals(2, secondEntry.lineNumber)
            assertEquals(content.indexOf("  \"tooltip.second\""), secondEntry.lineStartOffset)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    @Test
    fun refreshChangedFilesUsesVirtualFileCharsetForLangOffsets() {
        val disposable = Disposer.newDisposable()
        try {
            val service = MinecraftLangSourceIndexService(MockProjectEx(disposable))
            val content = """
                # header
                tooltip.backpack=Backpack
            """.trimIndent()
            val file = TestVirtualFile(
                pathValue = "E:/Github/ExampleMod/src/main/resources/assets/example/lang/zh_cn.lang",
                text = content,
                charset = StandardCharsets.UTF_16LE
            )

            service.refreshChangedFiles(sequenceOf(file))

            val entry = service.lookup("tooltip.backpack", listOf("zh_cn"))!!.single()
            assertEquals(2, entry.lineNumber)
            assertEquals(content.indexOf("tooltip.backpack"), entry.lineStartOffset)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    @Test
    fun refreshChangedFilesNormalizesCrlfOffsetsForLangNavigation() {
        val disposable = Disposer.newDisposable()
        try {
            val service = MinecraftLangSourceIndexService(MockProjectEx(disposable))
            val content = "# header\r\nfirst=One\r\nTooltip_NinefoldInputHatch_00=Value\r\n"
            val normalized = content.replace("\r\n", "\n")
            val file = TestVirtualFile(
                pathValue = "E:/Github/ExampleMod/src/main/resources/assets/example/lang/zh_cn.lang",
                text = content
            )

            service.refreshChangedFiles(sequenceOf(file))

            val entry = service.lookup("Tooltip_NinefoldInputHatch_00", listOf("zh_cn"))!!.single()
            assertEquals(3, entry.lineNumber)
            assertEquals(
                normalized.indexOf("Tooltip_NinefoldInputHatch_00"),
                entry.lineStartOffset
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
