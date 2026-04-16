package com.hfstudio.minecraftcoloridea.lang

class MinecraftLangFileStore {
    private data class StoredFile(
        val locale: String,
        val entries: Map<String, String>
    )

    private val filesByPath = LinkedHashMap<String, StoredFile>()
    private var snapshotCache: Map<String, Map<String, String>> = emptyMap()
    private var dirty = false

    fun replaceFile(
        path: String,
        locale: String,
        entries: Map<String, String>
    ): Set<String> {
        val normalizedPath = normalizePath(path)
        val normalizedLocale = locale.lowercase()
        val previous = filesByPath.put(
            normalizedPath,
            StoredFile(
                locale = normalizedLocale,
                entries = LinkedHashMap(entries)
            )
        )
        dirty = true

        return changedKeys(previous?.entries.orEmpty(), entries)
    }

    fun removeFile(path: String): Set<String> {
        val removed = filesByPath.remove(normalizePath(path)) ?: return emptySet()
        dirty = true
        return removed.entries.keys
    }

    fun clear() {
        if (filesByPath.isEmpty()) {
            return
        }

        filesByPath.clear()
        snapshotCache = emptyMap()
        dirty = false
    }

    fun snapshot(): Map<String, Map<String, String>> {
        if (!dirty) {
            return snapshotCache
        }

        snapshotCache = filesByPath.values
            .groupBy { it.locale }
            .mapValues { (_, localeFiles) ->
                buildMap {
                    localeFiles.forEach { putAll(it.entries) }
                }
            }
        dirty = false
        return snapshotCache
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/')

    private fun changedKeys(
        previous: Map<String, String>,
        current: Map<String, String>
    ): Set<String> {
        val keys = linkedSetOf<String>()
        (previous.keys + current.keys).forEach { key ->
            if (previous[key] != current[key]) {
                keys += key
            }
        }
        return keys
    }
}
