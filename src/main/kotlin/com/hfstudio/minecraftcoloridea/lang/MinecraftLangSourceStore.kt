package com.hfstudio.minecraftcoloridea.lang

class MinecraftLangSourceStore {
    private data class StoredFile(
        val locale: String,
        val entries: List<MinecraftLangSourceEntry>
    )

    private val filesByPath = LinkedHashMap<String, StoredFile>()
    private var entriesByLocaleKey: Map<String, Map<String, List<MinecraftLangSourceEntry>>> = emptyMap()
    private var dirty = false

    fun replaceFile(path: String, locale: String, entries: List<MinecraftLangSourceEntry>) {
        filesByPath[normalizePath(path)] = StoredFile(
            locale = locale.lowercase(),
            entries = entries.toList()
        )
        dirty = true
    }

    fun removeFile(path: String) {
        if (filesByPath.remove(normalizePath(path)) != null) {
            dirty = true
        }
    }

    fun clear() {
        if (filesByPath.isNotEmpty() || entriesByLocaleKey.isNotEmpty()) {
            filesByPath.clear()
            entriesByLocaleKey = emptyMap()
            dirty = false
        }
    }

    fun lookup(key: String, localeOrder: List<String>): List<MinecraftLangSourceEntry>? {
        val index = indexByLocaleKey()
        return localeOrder.firstNotNullOfOrNull { locale ->
            index[locale.lowercase()]?.get(key)
        }
    }

    fun lookupAll(key: String): List<MinecraftLangSourceEntry>? {
        val entries = indexByLocaleKey().values
            .mapNotNull { localeEntries -> localeEntries[key] }
            .flatten()
        return entries.takeIf(List<MinecraftLangSourceEntry>::isNotEmpty)
    }

    private fun indexByLocaleKey(): Map<String, Map<String, List<MinecraftLangSourceEntry>>> {
        if (!dirty) {
            return entriesByLocaleKey
        }

        val groupedEntries = linkedMapOf<String, LinkedHashMap<String, MutableList<MinecraftLangSourceEntry>>>()
        filesByPath.values.forEach { file ->
            val localeEntries = groupedEntries.getOrPut(file.locale) { linkedMapOf() }
            file.entries.forEach { entry ->
                localeEntries.getOrPut(entry.key) { mutableListOf() }.add(entry)
            }
        }

        entriesByLocaleKey = groupedEntries.mapValues { (_, localeEntries) ->
            localeEntries.mapValues { (_, entries) -> entries.toList() }
        }
        dirty = false
        return entriesByLocaleKey
    }

    private fun normalizePath(path: String): String = path.replace('\\', '/')
}
