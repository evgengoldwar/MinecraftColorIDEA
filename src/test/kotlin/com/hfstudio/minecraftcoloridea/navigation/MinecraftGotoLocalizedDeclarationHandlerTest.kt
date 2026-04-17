package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceEntry
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceStore
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.FakePsiElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MinecraftGotoLocalizedDeclarationHandlerTest {
    @Test
    fun declarationHandlerUsesSamePreferredTargetAsPopupAction() {
        val store = MinecraftLangSourceStore().apply {
            replaceFile(
                path = "assets/example/lang/zh_cn.json",
                locale = "zh_cn",
                entries = listOf(MinecraftLangSourceEntry("zh_cn", "tooltip.backpack", "assets/example/lang/zh_cn.json", 8, 120))
            )
            replaceFile(
                path = "assets/example/lang/en_us.json",
                locale = "en_us",
                entries = listOf(MinecraftLangSourceEntry("en_us", "tooltip.backpack", "assets/example/lang/en_us.json", 2, 24))
            )
        }
        val localeOrder = listOf("zh_cn", "en_us")
        val actionTarget = MinecraftGotoLocalizedEntryAction().resolveNavigationRequest(
            key = "tooltip.backpack",
            sourceIndexStamp = 1L
        ) { key ->
            MinecraftLocalizedNavigationResolver.resolve(key, localeOrder, store)
        }

        val actionResolved = actionTarget as MinecraftGotoLocalizedEntryAction.NavigationRequestResult.Target
        val declarationTargets = MinecraftGotoLocalizedDeclarationHandler().resolveDeclarationTargets(
            key = "tooltip.backpack",
            sourceIndexStamp = 1L,
            resolveTarget = { key: String ->
                MinecraftLocalizedNavigationResolver.resolve(key, localeOrder, store)
            },
            toPsiTarget = { entry: MinecraftLangSourceEntry -> TestNavigationElement(entry.filePath) }
        )

        val targets = assertNotNull(declarationTargets)
        assertEquals(
            actionResolved.target.entries.map(MinecraftLangSourceEntry::filePath),
            targets.map(PsiElement::toString)
        )
    }

    @Test
    fun multipleSamePriorityMatchesStayAvailableToDeclarationNavigation() {
        val store = MinecraftLangSourceStore().apply {
            replaceFile(
                path = "assets/example/lang/first.json",
                locale = "zh_cn",
                entries = listOf(MinecraftLangSourceEntry("zh_cn", "tooltip.backpack", "assets/example/lang/first.json", 4, 64))
            )
            replaceFile(
                path = "assets/example/lang/second.json",
                locale = "zh_cn",
                entries = listOf(MinecraftLangSourceEntry("zh_cn", "tooltip.backpack", "assets/example/lang/second.json", 9, 140))
            )
            replaceFile(
                path = "assets/example/lang/en_us.json",
                locale = "en_us",
                entries = listOf(MinecraftLangSourceEntry("en_us", "tooltip.backpack", "assets/example/lang/en_us.json", 1, 0))
            )
        }

        val declarationTargets = MinecraftGotoLocalizedDeclarationHandler().resolveDeclarationTargets(
            key = "tooltip.backpack",
            sourceIndexStamp = 1L,
            resolveTarget = { key: String ->
                MinecraftLocalizedNavigationResolver.resolve(key, listOf("zh_cn", "en_us"), store)
            },
            toPsiTarget = { entry: MinecraftLangSourceEntry -> TestNavigationElement(entry.filePath) }
        )

        val targets = assertNotNull(declarationTargets)
        assertEquals(
            listOf("assets/example/lang/first.json", "assets/example/lang/second.json"),
            targets.map(PsiElement::toString)
        )
    }

    @Test
    fun coldIndexPathReturnsNullWithoutTriggeringLookup() {
        var resolveCalls = 0
        var conversionCalls = 0

        val declarationTargets = MinecraftGotoLocalizedDeclarationHandler().resolveDeclarationTargets(
            key = "tooltip.backpack",
            sourceIndexStamp = 0L,
            resolveTarget = { _: String ->
                resolveCalls += 1
                null
            },
            toPsiTarget = { entry: MinecraftLangSourceEntry ->
                conversionCalls += 1
                TestNavigationElement(entry.filePath)
            }
        )

        assertNull(declarationTargets)
        assertEquals(0, resolveCalls)
        assertEquals(0, conversionCalls)
    }

    @Test
    fun declarationHandlerResolvesActualKeyOffsetInsteadOfLineStart() {
        val content = """
            {
              "tooltip.backpack": "Backpack"
            }
        """.trimIndent()
        val entry = MinecraftLangSourceEntry(
            locale = "en_us",
            key = "tooltip.backpack",
            filePath = "assets/example/lang/en_us.json",
            lineNumber = 2,
            lineStartOffset = content.indexOf("  \"tooltip.backpack\"")
        )

        val resolvedOffset = MinecraftGotoLocalizedDeclarationHandler().resolveTargetOffset(
            content = content,
            entry = entry
        )

        assertEquals(content.indexOf("tooltip.backpack"), resolvedOffset)
    }

    @Test
    fun declarationHandlerFallsBackToLineStartWhenKeyTextIsMissingOnLine() {
        val content = """
            tooltip.other=Backpack
            tooltip.third=Satchel
        """.trimIndent()
        val entry = MinecraftLangSourceEntry(
            locale = "en_us",
            key = "tooltip.backpack",
            filePath = "assets/example/lang/en_us.lang",
            lineNumber = 1,
            lineStartOffset = 0
        )

        val resolvedOffset = MinecraftGotoLocalizedDeclarationHandler().resolveTargetOffset(
            content = content,
            entry = entry
        )

        assertEquals(entry.lineStartOffset, resolvedOffset)
    }

    private class TestNavigationElement(private val label: String) : FakePsiElement() {
        override fun getName(): String = label

        override fun getParent(): PsiElement? = null

        override fun toString(): String = label
    }
}
