package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.MinecraftHighlightEngine
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorSettingsState
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class MinecraftColorApplicationService : Disposable {
    private val settings = service<MinecraftColorSettingsState>()
    private val engine = MinecraftHighlightEngine()
    private val sessions = ConcurrentHashMap<Editor, MinecraftColorEditorSession>()

    fun editorCreated(editor: Editor) {
        if (!isSupportedEditor(editor)) {
            return
        }

        sessions.computeIfAbsent(editor) {
            MinecraftColorEditorSession(it, settings, engine)
        }.scheduleRefresh()
    }

    fun editorReleased(editor: Editor) {
        sessions.remove(editor)?.dispose()
    }

    fun documentChanged(document: Document) {
        EditorFactory.getInstance().getEditors(document).forEach { editor ->
            if (isSupportedEditor(editor)) {
                sessions.computeIfAbsent(editor) {
                    MinecraftColorEditorSession(it, settings, engine)
                }.scheduleRefresh()
            }
        }
    }

    fun refreshAllEditors() {
        sessions.values.forEach(MinecraftColorEditorSession::scheduleRefresh)
    }

    fun refreshDocuments(project: Project, documents: Set<Document>) {
        if (documents.isEmpty()) {
            return
        }

        EditorFactory.getInstance().allEditors
            .asSequence()
            .filter { it.project == project && it.document in documents }
            .forEach(::editorCreated)
    }

    fun refreshProject(project: Project) {
        val editors = FileEditorManager.getInstance(project).allEditors
            .filterIsInstance<TextEditor>()
            .map(TextEditor::getEditor)

        editors.forEach(::editorCreated)
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
}
