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
import com.intellij.psi.PsiElement

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
                MinecraftLocalizedNavigationResolver.resolve(resolvedKey, localeOrder, sourceIndex::lookupAll)
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
            .locateForDeclaration(lineText, caretOffset - lineStart)
    }

    private fun MinecraftLangSourceEntry.toPsiTarget(project: Project): PsiElement? {
        val (_, offset) = MinecraftLocalizedEntryNavigation.resolveFileAndOffset(this) ?: return null
        return MinecraftNavigationTargetElement(
            project = project,
            filePath = filePath,
            targetOffset = offset,
            presentationData = navigationPresentation(this)
        )
    }

    internal fun resolveTargetOffset(content: String, entry: MinecraftLangSourceEntry): Int {
        return MinecraftLocalizedEntryNavigation.resolveTargetOffset(content, entry)
    }
}
