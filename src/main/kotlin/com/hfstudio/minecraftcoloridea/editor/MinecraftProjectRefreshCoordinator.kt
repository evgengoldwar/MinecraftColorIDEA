package com.hfstudio.minecraftcoloridea.editor

import com.intellij.openapi.components.Service
import com.intellij.openapi.editor.Document

@Service(Service.Level.PROJECT)
class MinecraftProjectRefreshCoordinator {
    private val documentToKeys = LinkedHashMap<Document, Set<String>>()
    private val keyToDocuments = LinkedHashMap<String, LinkedHashSet<Document>>()

    fun updateDependencies(document: Document, keys: Set<String>) {
        documentToKeys.remove(document)?.forEach { key ->
            keyToDocuments[key]?.remove(document)
            if (keyToDocuments[key].isNullOrEmpty()) {
                keyToDocuments.remove(key)
            }
        }

        if (keys.isEmpty()) {
            return
        }

        val normalizedKeys = LinkedHashSet(keys)
        documentToKeys[document] = normalizedKeys
        normalizedKeys.forEach { key ->
            keyToDocuments.getOrPut(key) { LinkedHashSet() }.add(document)
        }
    }

    fun affectedDocuments(changedKeys: Set<String>): Set<Document> {
        if (changedKeys.isEmpty()) {
            return emptySet()
        }

        val affected = LinkedHashSet<Document>()
        changedKeys.forEach { key ->
            affected += keyToDocuments[key].orEmpty()
        }
        return affected
    }
}
