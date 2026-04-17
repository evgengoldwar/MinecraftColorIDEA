package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceEntry
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceIndexService
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorProjectSettingsState
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorSettingsState
import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

class MinecraftGotoLocalizedDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor): Array<PsiElement>? {
        val project = editor.project ?: return null
        val baseConfig = project.service<MinecraftColorSettingsState>().toConfig()
        val key = locateKey(editor, offset, baseConfig.extraLocalizationMethods)?.key ?: return null
        val sourceIndex = project.service<MinecraftLangSourceIndexService>()
        val localeOrder = project.service<MinecraftColorProjectSettingsState>()
            .resolveLocaleTargetOrder(baseConfig)

        return resolveDeclarationTargets(
            key = key,
            sourceIndexStamp = sourceIndex.sourceIndexStamp(),
            resolveTarget = { resolvedKey ->
                MinecraftLocalizedNavigationResolver.resolve(resolvedKey, localeOrder) { lookupKey, locale ->
                    sourceIndex.lookup(lookupKey, listOf(locale))
                }
            },
            toPsiTarget = { entry -> entry.toPsiTarget(project) }
        )
    }

    override fun getActionText(context: DataContext): String? = null

    internal fun resolveDeclarationTargets(
        key: String,
        sourceIndexStamp: Long,
        resolveTarget: (String) -> MinecraftResolvedNavigationTarget?,
        toPsiTarget: (MinecraftLangSourceEntry) -> PsiElement?
    ): Array<PsiElement>? {
        val resolvedTarget = MinecraftLocalizedNavigationResolver.resolveIfReady(
            key = key,
            sourceIndexStamp = sourceIndexStamp,
            resolveTarget = resolveTarget
        ) ?: return null

        return resolvedTarget.entries
            .mapNotNull(toPsiTarget)
            .takeIf(List<PsiElement>::isNotEmpty)
            ?.toTypedArray()
    }

    private fun locateKey(
        editor: Editor,
        offset: Int,
        extraLocalizationMethods: Set<String>
    ): MinecraftLocatedKey? {
        val document = editor.document
        val caretOffset = offset.coerceIn(0, document.textLength)
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.charsSequence.subSequence(lineStart, lineEnd).toString()

        return MinecraftLocalizationKeyLocator(extraLocalizationMethods)
            .locateStrictly(lineText, caretOffset - lineStart)
    }

    private fun MinecraftLangSourceEntry.toPsiTarget(project: Project): PsiElement? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath.replace('\\', '/')) ?: return null
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null
        return psiFile.findElementAt(resolveTargetOffset(psiFile.text, this).coerceIn(0, psiFile.textLength))
            ?: psiFile
    }

    internal fun resolveTargetOffset(content: String, entry: MinecraftLangSourceEntry): Int {
        val lineStart = entry.lineStartOffset.coerceIn(0, content.length)
        val lineEnd = content.indexOf('\n', lineStart).let { if (it >= 0) it else content.length }
        val lineText = content.substring(lineStart, lineEnd)
        val matchOffset = localizedKeySearchTerms(entry.key)
            .firstNotNullOfOrNull { term ->
                lineText.indexOf(term).takeIf { it >= 0 }
            }
        return if (matchOffset != null) {
            lineStart + matchOffset
        } else {
            lineStart
        }
    }

    private fun localizedKeySearchTerms(key: String): List<String> {
        val escaped = key
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
        return linkedSetOf(key, escaped).toList()
    }
}
