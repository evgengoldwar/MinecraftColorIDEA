package com.hfstudio.minecraftcoloridea.navigation

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.FakePsiElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MinecraftGotoCodeUsageDeclarationHandlerTest {
    @Test
    fun declarationHandlerUsesSameTargetsAsActionResolver() {
        val entries = listOf(
            MinecraftCodeUsageEntry(
                key = "tooltip.backpack",
                filePath = "src/main/java/example/BackpackItem.java",
                lineNumber = 8,
                lineStartOffset = 120,
                matchStartOffset = 132,
                matchEndOffset = 148,
                snippet = """I18n.format("tooltip.backpack")""",
                confidence = MinecraftCodeUsageConfidence.EXACT
            )
        )

        val result = MinecraftGotoCodeUsageDeclarationHandler().resolveDeclarationTargets(
            key = "tooltip.backpack",
            usageIndexStamp = 1L,
            lookup = { entries },
            toPsiTarget = { TestNavigationElement(it.filePath) }
        )

        val targets = assertNotNull(result)
        assertEquals(listOf("src/main/java/example/BackpackItem.java"), targets.map(PsiElement::toString))
    }

    @Test
    fun declarationHandlerReturnsNullWhenIndexIsCold() {
        val result = MinecraftGotoCodeUsageDeclarationHandler().resolveDeclarationTargets(
            key = "tooltip.backpack",
            usageIndexStamp = 0L,
            lookup = { emptyList() },
            toPsiTarget = { TestNavigationElement(it.filePath) }
        )

        assertNull(result)
    }

    private class TestNavigationElement(private val label: String) : FakePsiElement() {
        override fun getName(): String = label

        override fun getParent(): PsiElement? = null

        override fun toString(): String = label
    }
}
