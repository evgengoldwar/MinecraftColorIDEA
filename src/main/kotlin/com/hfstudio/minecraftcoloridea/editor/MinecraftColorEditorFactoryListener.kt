package com.hfstudio.minecraftcoloridea.editor

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener

class MinecraftColorEditorFactoryListener : EditorFactoryListener {
    override fun editorCreated(event: EditorFactoryEvent) {
        service<MinecraftColorApplicationService>().editorCreated(event.editor)
    }

    override fun editorReleased(event: EditorFactoryEvent) {
        service<MinecraftColorApplicationService>().editorReleased(event.editor)
    }
}
