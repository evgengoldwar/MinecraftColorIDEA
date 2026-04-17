package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceEntry
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceStore

data class MinecraftResolvedNavigationTarget(
    val key: String,
    val locale: String,
    val entries: List<MinecraftLangSourceEntry>
)

object MinecraftLocalizedNavigationResolver {
    fun resolveIfReady(
        key: String,
        sourceIndexStamp: Long,
        resolveTarget: (String) -> MinecraftResolvedNavigationTarget?
    ): MinecraftResolvedNavigationTarget? {
        if (sourceIndexStamp == 0L) {
            return null
        }

        return resolveTarget(key)
    }

    fun resolve(
        key: String,
        localeOrder: List<String>,
        sourceStore: MinecraftLangSourceStore
    ): MinecraftResolvedNavigationTarget? {
        return resolve(key, localeOrder, sourceStore::lookupAll)
    }

    fun resolve(
        key: String,
        localeOrder: List<String>,
        lookupAll: (String) -> List<MinecraftLangSourceEntry>?
    ): MinecraftResolvedNavigationTarget? {
        val normalizedOrder = localeOrder
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(String::lowercase)
            .distinct()
        val localeRanks = normalizedOrder.withIndex().associate { (index, locale) -> locale to index }
        val entries = lookupAll(key).orEmpty()
            .sortedWith(
                compareBy<MinecraftLangSourceEntry>(
                    { localeRanks[it.locale.lowercase()] ?: Int.MAX_VALUE },
                    { if (it.locale.lowercase() in localeRanks) 0 else 1 },
                    { it.locale.lowercase() },
                    { it.filePath.lowercase() },
                    { it.lineNumber }
                )
            )

        if (entries.isEmpty()) {
            return null
        }

        return MinecraftResolvedNavigationTarget(
            key = key,
            locale = entries.first().locale.lowercase(),
            entries = entries
        )
    }
}
