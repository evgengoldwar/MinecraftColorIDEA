package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.version.MinecraftVersionSignalFiles
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

internal object MinecraftProjectFileRules {
    fun isLangFile(path: String): Boolean {
        return path.contains("/lang/") && (path.endsWith(".lang") || path.endsWith(".json"))
    }

    fun isVersionSignalFile(path: String): Boolean {
        return MinecraftVersionSignalFiles.isVersionSignalFile(path)
    }

    fun staleProjectLangPaths(
        projectBasePath: String,
        events: List<VFileEvent>
    ): Set<String> {
        return events.mapNotNullTo(linkedSetOf()) { event ->
            staleProjectLangPath(projectBasePath, event)
        }
    }

    fun staleProjectLangPath(projectBasePath: String, event: VFileEvent): String? {
        val oldPath = when (event) {
            is VFileDeleteEvent -> event.path
            is VFileMoveEvent -> event.oldPath
            is VFilePropertyChangeEvent -> event.oldPath.takeIf { event.isRename }
            else -> null
        } ?: return null

        val normalizedPath = normalizePath(oldPath)
        return normalizedPath.takeIf { path ->
            MinecraftEditorFileScope.isProjectOwned(projectBasePath, path) && isLangFile(path)
        }
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/')
}
