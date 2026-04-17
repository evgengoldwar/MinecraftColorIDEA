package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.MinecraftColorBundle
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.SimpleListCellRenderer

class MinecraftGotoCodeUsageAction : AnAction(), DumbAware {
    private data class ActionContext(
        val project: Project,
        val editor: Editor,
        val key: String
    )

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        updatePresentationForResolvedKey(event.presentation, locatedKeyFor(event))
    }

    override fun actionPerformed(event: AnActionEvent) {
        val context = contextFor(event) ?: return
        when (val result = resolveNavigationRequest(context)) {
            MinecraftCodeUsageNavigationResolver.NavigationRequestResult.IndexNotReady -> {
                showHint(context.editor, "notification.goto.code.usage.index.not.ready")
            }

            MinecraftCodeUsageNavigationResolver.NavigationRequestResult.NotFound -> {
                showHint(context.editor, "notification.goto.code.usage.not.found")
            }

            is MinecraftCodeUsageNavigationResolver.NavigationRequestResult.Target -> {
                val target = result.target
                if (target.entries.size == 1) {
                    navigate(context.project, context.editor, target.entries.single())
                    return
                }

                JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(target.entries)
                    .setTitle(MinecraftColorBundle.message("chooser.goto.code.usage.title"))
                    .setRenderer(SimpleListCellRenderer.create("") { entry ->
                        entry?.let(::chooserPresentation)?.toHtml().orEmpty()
                    })
                    .setItemChosenCallback { entry ->
                        navigate(context.project, context.editor, entry)
                    }
                    .createPopup()
                    .showInBestPositionFor(context.editor)
            }
        }
    }

    internal fun updatePresentationForResolvedKey(
        presentation: Presentation,
        locatedKey: MinecraftLocatedLangKey?
    ) {
        presentation.isEnabledAndVisible = locatedKey != null
    }

    internal fun resolveNavigationRequest(
        key: String,
        usageIndexStamp: Long,
        lookup: (String) -> MinecraftCodeUsageNavigationResolver.NavigationRequestResult
    ): MinecraftCodeUsageNavigationResolver.NavigationRequestResult {
        if (usageIndexStamp == 0L) {
            return MinecraftCodeUsageNavigationResolver.NavigationRequestResult.IndexNotReady
        }
        return lookup(key)
    }

    internal fun chooserPresentation(entry: MinecraftCodeUsageEntry): MinecraftNavigationPresentation {
        return navigationPresentation(entry)
    }

    private fun resolveNavigationRequest(context: ActionContext): MinecraftCodeUsageNavigationResolver.NavigationRequestResult {
        val index = context.project.service<MinecraftCodeUsageIndexService>()
        return resolveNavigationRequest(context.key, index.usageIndexStamp()) { key ->
            MinecraftCodeUsageNavigationResolver.resolve(
                key = key,
                usageIndexStamp = index.usageIndexStamp(),
                lookup = index::lookup
            )
        }
    }

    private fun contextFor(event: AnActionEvent): ActionContext? {
        val project = event.project ?: return null
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
        val key = locatedKeyFor(event)?.key ?: return null
        return ActionContext(project, editor, key)
    }

    private fun locatedKeyFor(event: AnActionEvent): MinecraftLocatedLangKey? {
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
        return locateKey(editor)
    }

    private fun locateKey(editor: Editor): MinecraftLocatedLangKey? {
        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
        val normalizedPath = file.path.replace('\\', '/')
        if (!normalizedPath.contains("/lang/")) {
            return null
        }

        val document = editor.document
        val caretOffset = editor.caretModel.offset
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

    private fun navigate(project: Project, editor: Editor, entry: MinecraftCodeUsageEntry) {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(entry.filePath.replace('\\', '/'))
        if (file == null) {
            showHint(editor, "notification.goto.code.usage.not.found")
            return
        }

        OpenFileDescriptor(project, file, entry.matchStartOffset).navigate(true)
    }

    private fun showHint(editor: Editor, messageKey: String) {
        HintManager.getInstance().showInformationHint(
            editor,
            MinecraftColorBundle.message(messageKey)
        )
    }
}
