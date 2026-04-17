package com.hfstudio.minecraftcoloridea.navigation

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class MinecraftGotoCodeUsageDeclarationHandler : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(sourceElement: PsiElement?, offset: Int, editor: Editor): Array<PsiElement>? {
        val project = editor.project ?: return null
        val key = locateKey(editor, offset)?.key ?: return null
        val index = project.service<MinecraftCodeUsageIndexService>()

        return resolveDeclarationTargets(
            key = key,
            usageIndexStamp = index.usageIndexStamp(),
            lookup = index::lookup,
            toPsiTarget = { entry -> entry.toPsiTarget(project) }
        )
    }

    override fun getActionText(context: DataContext): String? = null

    internal fun resolveDeclarationTargets(
        key: String,
        usageIndexStamp: Long,
        lookup: (String) -> List<MinecraftCodeUsageEntry>?,
        toPsiTarget: (MinecraftCodeUsageEntry) -> PsiElement?
    ): Array<PsiElement>? {
        val result = MinecraftCodeUsageNavigationResolver.resolve(
            key = key,
            usageIndexStamp = usageIndexStamp,
            lookup = lookup
        ) as? MinecraftCodeUsageNavigationResolver.NavigationRequestResult.Target ?: return null

        return result.target.entries
            .mapNotNull(toPsiTarget)
            .takeIf(List<PsiElement>::isNotEmpty)
            ?.toTypedArray()
    }

    private fun locateKey(editor: Editor, offset: Int): MinecraftLocatedLangKey? {
        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
        val normalizedPath = file.path.replace('\\', '/')
        if (!normalizedPath.contains("/lang/")) {
            return null
        }

        val document = editor.document
        val caretOffset = offset.coerceIn(0, document.textLength)
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.charsSequence.subSequence(lineStart, lineEnd).toString()
        val lineOffset = caretOffset - lineStart

        return when {
            normalizedPath.endsWith(".lang") -> MinecraftLangEntryKeyLocator.locateLang(lineText, lineOffset)
            normalizedPath.endsWith(".json") -> MinecraftLangEntryKeyLocator.locateJson(lineText, lineOffset)
            else -> null
        }
    }

    private fun MinecraftCodeUsageEntry.toPsiTarget(project: Project): PsiElement? {
        return MinecraftNavigationTargetElement(
            project = project,
            filePath = filePath,
            targetOffset = matchStartOffset,
            presentationData = navigationPresentation(this)
        )
    }
}
