package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.MinecraftColorBundle
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceEntry
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceIndexService
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorProjectSettingsState
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorSettingsState
import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.SimpleListCellRenderer

class MinecraftGotoLocalizedEntryAction : AnAction(), DumbAware {
    internal data class ChooserPresentation(
        val locationText: String,
        val fileNameText: String
    ) {
        fun toHtml(): String {
            return "<html><table width='100%'><tr><td>$locationText</td><td align='right'><b>$fileNameText</b></td></tr></table></html>"
        }
    }

    internal sealed interface NavigationRequestResult {
        data object IndexNotReady : NavigationRequestResult
        data object NotFound : NavigationRequestResult
        data class Target(val target: MinecraftResolvedNavigationTarget) : NavigationRequestResult
    }

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
            NavigationRequestResult.IndexNotReady -> {
                showHint(context.editor, "notification.goto.localized.entry.index.not.ready")
            }

            NavigationRequestResult.NotFound -> {
                showHint(context.editor, "notification.goto.localized.entry.not.found")
            }

            is NavigationRequestResult.Target -> {
                val target = result.target
                if (target.entries.size == 1) {
                    navigate(context.project, context.editor, target.entries.single())
                    return
                }

                JBPopupFactory.getInstance()
                    .createPopupChooserBuilder(target.entries)
                    .setTitle(MinecraftColorBundle.message("chooser.goto.localized.entry.title"))
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

    private fun contextFor(event: AnActionEvent): ActionContext? {
        val project = event.project ?: return null
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
        val key = locatedKeyFor(event)?.key ?: return null

        return ActionContext(
            project = project,
            editor = editor,
            key = key
        )
    }

    internal fun updatePresentationForResolvedKey(
        presentation: Presentation,
        locatedKey: MinecraftLocatedKey?
    ) {
        presentation.isEnabledAndVisible = locatedKey != null
    }

    private fun locatedKeyFor(event: AnActionEvent): MinecraftLocatedKey? {
        val project = event.project ?: return null
        val editor = event.getData(CommonDataKeys.EDITOR) ?: return null
        val document = editor.document
        val caretOffset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.charsSequence.subSequence(lineStart, lineEnd).toString()
        val baseConfig = project.service<MinecraftColorSettingsState>().toConfig()
        return MinecraftLocalizationKeyLocator(baseConfig.extraLocalizationMethods)
            .locate(lineText, caretOffset - lineStart)
    }

    internal fun resolveNavigationRequest(
        key: String,
        sourceIndexStamp: Long,
        resolveTarget: (String) -> MinecraftResolvedNavigationTarget?
    ): NavigationRequestResult {
        if (sourceIndexStamp == 0L) {
            return NavigationRequestResult.IndexNotReady
        }
        val target = resolveTarget(key) ?: return NavigationRequestResult.NotFound
        return NavigationRequestResult.Target(target)
    }

    private fun resolveNavigationRequest(context: ActionContext): NavigationRequestResult {
        val project = context.project
        val sourceIndex = project.service<MinecraftLangSourceIndexService>()
        val sourceIndexStamp = sourceIndex.sourceIndexStamp()
        if (sourceIndexStamp == 0L) {
            return NavigationRequestResult.IndexNotReady
        }

        val baseConfig = project.service<MinecraftColorSettingsState>().toConfig()
        val localeOrder = project.service<MinecraftColorProjectSettingsState>()
            .resolveLocaleTargetOrder(baseConfig)

        return resolveNavigationRequest(context.key, sourceIndexStamp) { key ->
            MinecraftLocalizedNavigationResolver.resolve(key, localeOrder, sourceIndex::lookupAll)
        }
    }

    internal fun chooserPresentation(entry: MinecraftLangSourceEntry): ChooserPresentation {
        val normalizedPath = entry.filePath.replace('\\', '/')
        val fileName = normalizedPath.substringAfterLast('/')
        val parentPath = normalizedPath.substringBeforeLast('/', missingDelimiterValue = "")
        val locationText = if (parentPath.isNotEmpty()) {
            "$parentPath:${entry.lineNumber}"
        } else {
            "Line ${entry.lineNumber}"
        }
        return ChooserPresentation(
            locationText = locationText,
            fileNameText = fileName.ifEmpty { normalizedPath }
        )
    }

    private fun navigate(project: Project, editor: Editor, entry: MinecraftLangSourceEntry) {
        val file = LocalFileSystem.getInstance().refreshAndFindFileByPath(entry.filePath.replace('\\', '/'))
        if (file == null) {
            showHint(editor, "notification.goto.localized.entry.not.found")
            return
        }

        OpenFileDescriptor(project, file, entry.lineStartOffset).navigate(true)
    }

    private fun showHint(editor: Editor, messageKey: String) {
        HintManager.getInstance().showInformationHint(
            editor,
            MinecraftColorBundle.message(messageKey)
        )
    }
}
