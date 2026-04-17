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
        return resolve(key, localeOrder) { lookupKey, locale ->
            sourceStore.lookup(lookupKey, listOf(locale))
        }
    }

    fun resolve(
        key: String,
        localeOrder: List<String>,
        lookup: (String, String) -> List<MinecraftLangSourceEntry>?
    ): MinecraftResolvedNavigationTarget? {
        val normalizedOrder = localeOrder
            .map(String::trim)
            .filter(String::isNotEmpty)
            .map(String::lowercase)
            .distinct()

        for (locale in normalizedOrder) {
            val entries = lookup(key, locale)?.takeIf(List<MinecraftLangSourceEntry>::isNotEmpty) ?: continue
            return MinecraftResolvedNavigationTarget(
                key = key,
                locale = entries.first().locale.lowercase(),
                entries = entries
            )
        }

        return null
    }
}
