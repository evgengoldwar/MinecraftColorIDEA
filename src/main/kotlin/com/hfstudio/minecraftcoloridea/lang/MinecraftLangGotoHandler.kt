package com.hfstudio.minecraftcoloridea.lang

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import org.jetbrains.annotations.Nls

class MinecraftLangGotoHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?,
        offset: Int,
        editor: Editor?
    ): Array<PsiElement>? {
        if (sourceElement == null || editor == null) return null
        if (!sourceElement.isValid) return null

        val elementText = sourceElement.text

        val key = extractKeyFromString(elementText) ?: return null

        val project = sourceElement.project
        val langFiles = FilenameIndex.getAllFilesByExt(project, "lang")
        val navigationElements = mutableListOf<PsiElement>()

        for (virtualFile in langFiles) {
            if (!virtualFile.isValid) continue

            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: continue
            if (!psiFile.isValid) continue

            val results = findKeyLineIndexes(psiFile, key)
            for (result in results) {
                navigationElements.add(
                    LangKeyNavigationElement(
                        psiFile = psiFile,
                        lineIndex = result.lineIndex,
                        matchedKey = result.matchedKey,
                        matchType = result.matchType
                    )
                )
            }
        }

        navigationElements.sortBy { element ->
            if (element is LangKeyNavigationElement) element.matchType.priority else Int.MAX_VALUE
        }

        return navigationElements.takeIf { it.isNotEmpty() }?.toTypedArray()
    }

    override fun getActionText(context: DataContext): @Nls String = "Go to .lang key"

    private fun extractKeyFromString(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.length < 2) return null

        val content = when {
            trimmed.startsWith("\"") && trimmed.endsWith("\"") -> trimmed.substring(1, trimmed.length - 1)
            trimmed.startsWith("'") && trimmed.endsWith("'") -> trimmed.substring(1, trimmed.length - 1)
            else -> trimmed
        }

        return content.split('.').lastOrNull() ?: content
    }

    private fun findKeyLineIndexes(psiFile: PsiFile, searchKey: String): List<SearchResult> {
        if (!psiFile.isValid) return emptyList()

        val results = mutableListOf<SearchResult>()
        val lines = psiFile.text.lines()

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            val equalsIndex = trimmed.indexOf('=')
            if (equalsIndex == -1) continue

            val lineKey = trimmed.substring(0, equalsIndex).trim()

            when {
                lineKey == searchKey -> {
                    results.add(SearchResult(index, lineKey, MatchType.EXACT))
                }
                lineKey.endsWith(".$searchKey") -> {
                    results.add(SearchResult(index, lineKey, MatchType.SUFFIX))
                }
                lineKey.contains(searchKey) -> {
                    results.add(SearchResult(index, lineKey, MatchType.CONTAINS))
                }
                lineKey.split('.').any { part -> part == searchKey } -> {
                    results.add(SearchResult(index, lineKey, MatchType.PARTIAL_MATCH))
                }
            }
        }

        return results.distinctBy { it.matchedKey }.sortedBy { it.matchType.priority }
    }

    private data class SearchResult(
        val lineIndex: Int,
        val matchedKey: String,
        val matchType: MatchType
    )
}