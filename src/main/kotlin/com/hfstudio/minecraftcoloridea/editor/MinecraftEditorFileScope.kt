package com.hfstudio.minecraftcoloridea.editor

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.vfs.VirtualFile

object MinecraftEditorFileScope {
    fun isProjectOwned(project: Project, file: VirtualFile): Boolean {
        return isProjectOwned(project.basePath, file.path)
    }

    fun isProjectOwned(projectBasePath: String?, filePath: String): Boolean {
        val normalizedBasePath = projectBasePath
            ?.takeIf(String::isNotBlank)
            ?.let(::normalizePath)
            ?: return false
        val normalizedFilePath = normalizePath(filePath)

        return normalizedFilePath == normalizedBasePath ||
            normalizedFilePath.startsWith("$normalizedBasePath/")
    }

    fun normalizePath(path: String): String {
        val normalized = path.replace('\\', '/').trimEnd('/')
        return if (SystemInfoRt.isFileSystemCaseSensitive) {
            normalized
        } else {
            normalized.lowercase()
        }
    }
}
