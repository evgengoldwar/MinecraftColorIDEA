package com.hfstudio.minecraftcoloridea.editor

import java.util.concurrent.ConcurrentHashMap

class MinecraftExternalFileCache(
    private val retentionMillis: Long = DEFAULT_RETENTION_MILLIS,
    private val nowMillis: () -> Long = System::currentTimeMillis
) {
    private data class Entry(
        val openEditors: Int,
        val releaseAtMillis: Long?
    )

    private val entries = ConcurrentHashMap<String, Entry>()

    fun markOpened(path: String) {
        evictExpired()
        val normalizedPath = MinecraftEditorFileScope.normalizePath(path)
        entries.compute(normalizedPath) { _, current ->
            Entry(
                openEditors = (current?.openEditors ?: 0) + 1,
                releaseAtMillis = null
            )
        }
    }

    fun markReleased(path: String) {
        evictExpired()
        val normalizedPath = MinecraftEditorFileScope.normalizePath(path)
        val releasedAt = nowMillis()

        entries.computeIfPresent(normalizedPath) { _, current ->
            val remainingEditors = (current.openEditors - 1).coerceAtLeast(0)
            if (remainingEditors == 0) {
                Entry(openEditors = 0, releaseAtMillis = releasedAt + retentionMillis)
            } else {
                Entry(openEditors = remainingEditors, releaseAtMillis = null)
            }
        }
        evictExpired()
    }

    fun isTracked(path: String): Boolean {
        evictExpired()
        val normalizedPath = MinecraftEditorFileScope.normalizePath(path)
        val entry = entries[normalizedPath] ?: return false
        return entry.openEditors > 0 || (entry.releaseAtMillis ?: Long.MIN_VALUE) > nowMillis()
    }

    fun trackedPaths(): Set<String> {
        evictExpired()
        val now = nowMillis()
        return entries.entries
            .asSequence()
            .filter { (_, entry) ->
                entry.openEditors > 0 || (entry.releaseAtMillis ?: Long.MIN_VALUE) > now
            }
            .map { it.key }
            .toCollection(LinkedHashSet())
    }

    private fun evictExpired() {
        val now = nowMillis()
        entries.entries.forEach { entry ->
            val releaseAtMillis = entry.value.releaseAtMillis ?: return@forEach
            if (entry.value.openEditors == 0 && releaseAtMillis <= now) {
                entries.remove(entry.key, entry.value)
            }
        }
    }

    companion object {
        const val DEFAULT_RETENTION_MILLIS: Long = 5_000L
    }
}
