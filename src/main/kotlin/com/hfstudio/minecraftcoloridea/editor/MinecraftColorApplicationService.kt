package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.MinecraftHighlightEngine
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangIndexService
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceIndexService
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorSettingsState
import com.hfstudio.minecraftcoloridea.version.MinecraftVersionDetectionCache
import com.hfstudio.minecraftcoloridea.version.MinecraftVersionSignalFiles
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class MinecraftColorApplicationService : Disposable {
    private val settings = service<MinecraftColorSettingsState>()
    private val engine = MinecraftHighlightEngine()
    private val sessions = ConcurrentHashMap<Editor, MinecraftColorEditorSession>()
    private val externalFiles = MinecraftExternalFileCache()

    fun editorCreated(editor: Editor) {
        if (!isSupportedEditor(editor)) {
            return
        }

        sessionFor(editor)?.scheduleRefresh()
    }

    fun editorReleased(editor: Editor) {
        trackExternalFile(editor, released = true)
        sessions.remove(editor)?.dispose()
    }

    fun documentChanged(event: DocumentEvent) {
        val document = event.document
        val editors = EditorFactory.getInstance().getEditors(document)
        val change = MinecraftDocumentChange(
            offset = event.offset,
            oldLength = event.oldLength,
            newLength = event.newLength
        )
        editors.forEach { editor ->
            if (isSupportedEditor(editor)) {
                sessionFor(editor)?.scheduleRefresh(change)
            }
        }

        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        editors.asSequence()
            .mapNotNull(Editor::getProject)
            .distinct()
            .forEach { project ->
                handleCrossFileRefresh(project, file, document.immutableCharSequence.toString())
            }
    }

    fun refreshAllEditors() {
        sessions.values.forEach { it.scheduleRefresh() }
    }

    fun refreshDocuments(project: Project, documents: Set<Document>) {
        if (documents.isEmpty()) {
            return
        }

        EditorFactory.getInstance().allEditors
            .asSequence()
            .filter { it.project == project && it.document in documents }
            .forEach { sessionFor(it)?.scheduleRefresh() }
    }

    fun refreshProject(project: Project) {
        val editors = EditorFactory.getInstance().allEditors
            .asSequence()
            .filter { it.project == project && isSupportedEditor(it) }
            .toList()

        editors.forEach { sessionFor(it)?.scheduleRefresh() }
    }

    override fun dispose() {
        sessions.values.forEach(MinecraftColorEditorSession::dispose)
        sessions.clear()
    }

    private fun isSupportedEditor(editor: Editor): Boolean {
        return editor.project != null &&
            !editor.isViewer &&
            editor.editorKind == EditorKind.MAIN_EDITOR
    }

    private fun handleCrossFileRefresh(project: Project, file: VirtualFile, text: String) {
        if (!MinecraftEditorFileScope.isProjectOwned(project, file)) {
            return
        }

        val path = file.path.replace('\\', '/')

        if (MinecraftProjectFileRules.isLangFile(path)) {
            project.service<MinecraftLangSourceIndexService>().refreshDocument(file, text)
            val changedKeys = project.service<MinecraftLangIndexService>().refreshDocument(file, text)
            if (changedKeys.isNotEmpty()) {
                val affectedDocuments = project.service<MinecraftProjectRefreshCoordinator>()
                    .affectedDocuments(changedKeys)
                if (affectedDocuments.isEmpty()) {
                    refreshProject(project)
                } else {
                    refreshDocuments(project, affectedDocuments)
                }
            }
        }

        if (MinecraftVersionSignalFiles.isVersionSignalFile(path)) {
            project.service<MinecraftVersionDetectionCache>().invalidate()
            refreshProject(project)
        }
    }

    private fun sessionFor(editor: Editor): MinecraftColorEditorSession? {
        if (!isSupportedEditor(editor)) {
            return null
        }

        sessions[editor]?.let { return it }
        trackExternalFile(editor, released = false)
        return sessions.computeIfAbsent(editor) {
            MinecraftColorEditorSession(it, settings, engine)
        }
    }

    private fun trackExternalFile(editor: Editor, released: Boolean) {
        val project = editor.project ?: return
        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return
        if (MinecraftEditorFileScope.isProjectOwned(project, file)) {
            return
        }

        if (released) {
            externalFiles.markReleased(file.path)
        } else {
            externalFiles.markOpened(file.path)
        }
    }
}
