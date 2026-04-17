package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.core.MinecraftVirtualFileTextLoader
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

@Service(Service.Level.PROJECT)
class MinecraftCodeUsageIndexService(private val project: Project) {
    private var stamp: Long = 0
    private val store = MinecraftCodeUsageStore()
    private val parser = MinecraftCodeUsageParser()

    fun usageIndexStamp(): Long = stamp

    fun lookup(key: String): List<MinecraftCodeUsageEntry>? = store.lookup(key)

    fun refreshProjectResources(maxEnumeratedKeys: Int) {
        store.clear()
        MinecraftCodeUsageFileScope.projectFiles(project).forEach { file ->
            refreshFile(file, MinecraftVirtualFileTextLoader.load(file), maxEnumeratedKeys)
        }
        stamp += 1
    }

    fun refreshChangedFiles(files: Sequence<VirtualFile>, maxEnumeratedKeys: Int) {
        var changed = false
        files.forEach { file ->
            if (!MinecraftCodeUsageFileScope.isCandidate(file)) {
                return@forEach
            }
            refreshFile(file, MinecraftVirtualFileTextLoader.load(file), maxEnumeratedKeys)
            changed = true
        }
        if (changed) {
            stamp += 1
        }
    }

    fun refreshDocument(file: VirtualFile, text: String, maxEnumeratedKeys: Int) {
        if (!MinecraftCodeUsageFileScope.isCandidate(file)) {
            return
        }
        refreshFile(file, text, maxEnumeratedKeys)
        stamp += 1
    }

    fun removeProjectFile(path: String) {
        store.removeFile(path)
        stamp += 1
    }

    fun clear() {
        store.clear()
        stamp += 1
    }

    private fun refreshFile(file: VirtualFile, text: String, maxEnumeratedKeys: Int) {
        store.replaceFile(
            path = file.path,
            entries = parser.parseFile(
                filePath = file.path,
                text = text,
                maxEnumeratedKeys = maxEnumeratedKeys
            )
        )
    }
}
