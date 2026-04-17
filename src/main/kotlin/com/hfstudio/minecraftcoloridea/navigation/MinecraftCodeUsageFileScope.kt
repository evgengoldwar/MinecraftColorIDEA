package com.hfstudio.minecraftcoloridea.navigation

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

object MinecraftCodeUsageFileScope {
    private val primaryExtensions = setOf("java", "kt", "kts", "groovy", "scala")
    private val secondaryExtensions = setOf(
        "js", "ts", "jsx", "tsx",
        "lua", "py", "rb",
        "json", "json5", "xml", "toml", "yml", "yaml", "properties", "cfg", "txt", "md"
    )
    private const val MAX_FILE_BYTES = 512 * 1024L

    fun isCandidate(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        val extension = normalized.substringAfterLast('.', "").lowercase()
        if (normalized.contains("/lang/")) {
            return false
        }
        if (normalized.contains("/build/") || normalized.contains("/.gradle/") || normalized.contains("/out/")) {
            return false
        }
        return extension in primaryExtensions || extension in secondaryExtensions
    }

    fun isCandidate(file: VirtualFile): Boolean {
        val fileLength = runCatching { file.length }.getOrDefault(0L)
        val filePath = runCatching { file.path }.getOrDefault("")
        val isDirectory = runCatching { file.isDirectory }.getOrDefault(false)
        return !isDirectory &&
            fileLength <= MAX_FILE_BYTES &&
            isCandidate(filePath)
    }

    fun projectFiles(project: Project): Sequence<VirtualFile> = sequence {
        val root = project.basePath
            ?.let { LocalFileSystem.getInstance().findFileByPath(it.replace('\\', '/')) }
            ?: return@sequence

        val matches = mutableListOf<VirtualFile>()
        VfsUtilCore.iterateChildrenRecursively(root, null) { file ->
            if (isCandidate(file)) {
                matches += file
            }
            true
        }
        yieldAll(matches)
    }
}
