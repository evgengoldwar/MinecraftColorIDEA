package com.hfstudio.minecraftcoloridea.navigation

class MinecraftCodeUsageStore {
    private val fileEntries = LinkedHashMap<String, List<MinecraftCodeUsageEntry>>()
    private var entriesByKey: Map<String, List<MinecraftCodeUsageEntry>> = emptyMap()
    private var dirty = false

    fun replaceFile(path: String, entries: List<MinecraftCodeUsageEntry>) {
        fileEntries[normalizePath(path)] = entries.toList()
        dirty = true
    }

    fun removeFile(path: String) {
        if (fileEntries.remove(normalizePath(path)) != null) {
            dirty = true
        }
    }

    fun clear() {
        if (fileEntries.isNotEmpty() || entriesByKey.isNotEmpty()) {
            fileEntries.clear()
            entriesByKey = emptyMap()
            dirty = false
        }
    }

    fun lookup(key: String): List<MinecraftCodeUsageEntry>? {
        return indexByKey()[key]
    }

    private fun indexByKey(): Map<String, List<MinecraftCodeUsageEntry>> {
        if (!dirty) {
            return entriesByKey
        }

        val grouped = linkedMapOf<String, MutableList<MinecraftCodeUsageEntry>>()
        fileEntries.values.forEach { entries ->
            entries.forEach { entry ->
                grouped.getOrPut(entry.key) { mutableListOf() }.add(entry)
            }
        }
        entriesByKey = grouped.mapValues { (_, entries) -> entries.toList() }
        dirty = false
        return entriesByKey
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/')
}
