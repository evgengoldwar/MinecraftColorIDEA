package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.core.MinecraftVirtualFileTextLoader
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceEntry
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.LocalFileSystem

internal object MinecraftLocalizedEntryNavigation {
    fun resolveFileAndOffset(entry: MinecraftLangSourceEntry): Pair<com.intellij.openapi.vfs.VirtualFile, Int>? {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(entry.filePath.replace('\\', '/')) ?: return null
        val text = FileDocumentManager.getInstance().getDocument(file)
            ?.immutableCharSequence
            ?.toString()
            ?: MinecraftVirtualFileTextLoader.load(file)
        return file to resolveTargetOffset(text, entry)
    }

    fun resolveTargetOffset(content: String, entry: MinecraftLangSourceEntry): Int {
        val lineStart = entry.lineStartOffset.coerceIn(0, content.length)
        val lineEnd = content.indexOf('\n', lineStart).let { if (it >= 0) it else content.length }
        val lineText = content.substring(lineStart, lineEnd)
        val matchOffset = localizedKeySearchTerms(entry.key)
            .firstNotNullOfOrNull { term ->
                lineText.indexOf(term).takeIf { it >= 0 }
            }
        return if (matchOffset != null) {
            lineStart + matchOffset
        } else {
            lineStart
        }
    }

    private fun localizedKeySearchTerms(key: String): List<String> {
        val escaped = key
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return linkedSetOf(key, escaped).toList()
    }
}
