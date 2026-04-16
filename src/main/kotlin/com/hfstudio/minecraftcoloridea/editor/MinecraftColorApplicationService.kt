package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.core.MinecraftHighlightEngine
import com.hfstudio.minecraftcoloridea.lang.MinecraftLangIndexService
import com.hfstudio.minecraftcoloridea.settings.MinecraftColorSettingsState
import com.hfstudio.minecraftcoloridea.version.MinecraftVersionDetectionCache
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
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
                sessions.computeIfAbsent(editor) {
                    MinecraftColorEditorSession(it, settings, engine)
                }.scheduleRefresh(change)
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

    private fun handleCrossFileRefresh(project: Project, file: com.intellij.openapi.vfs.VirtualFile, text: String) {
        val path = file.path.replace('\\', '/')

        if (isLangFile(path)) {
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

        if (isVersionSignalFile(path)) {
            project.service<MinecraftVersionDetectionCache>().invalidate()
            refreshProject(project)
        }
    }

    private fun isLangFile(path: String): Boolean {
        return path.contains("/lang/") && (path.endsWith(".lang") || path.endsWith(".json"))
    }

    private fun isVersionSignalFile(path: String): Boolean {
        return path.endsWith("/mcmod.info") ||
            path.endsWith("/mods.toml") ||
            path.endsWith("/fabric.mod.json") ||
            path.endsWith("/quilt.mod.json") ||
            path.endsWith("/build.gradle") ||
            path.endsWith("/build.gradle.kts") ||
            path.endsWith("/gradle.properties")
    }
}
