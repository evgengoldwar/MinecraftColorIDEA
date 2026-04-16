package com.hfstudio.minecraftcoloridea.editor

import com.intellij.openapi.editor.impl.DocumentImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinecraftEditorRefreshTest {
    private val coordinator = MinecraftProjectRefreshCoordinator()

    @Test
    fun changedLangKeysReturnOnlyDependentDocuments() {
        val first = DocumentImpl("first")
        val second = DocumentImpl("second")

        coordinator.updateDependencies(first, setOf("tooltip.backpack"))
        coordinator.updateDependencies(second, setOf("tooltip.slot"))

        assertEquals(setOf(first), coordinator.affectedDocuments(setOf("tooltip.backpack")))
        assertEquals(setOf(second), coordinator.affectedDocuments(setOf("tooltip.slot")))
    }

    @Test
    fun updatingDependenciesReplacesPreviousKeys() {
        val document = DocumentImpl("single")

        coordinator.updateDependencies(document, setOf("tooltip.old"))
        coordinator.updateDependencies(document, setOf("tooltip.new"))

        assertTrue(coordinator.affectedDocuments(setOf("tooltip.old")).isEmpty())
        assertEquals(setOf(document), coordinator.affectedDocuments(setOf("tooltip.new")))
    }
}
