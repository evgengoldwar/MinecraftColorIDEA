package com.hfstudio.minecraftcoloridea.editor

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener

class MinecraftColorDocumentListener : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
        service<MinecraftColorApplicationService>().documentChanged(event)
    }
}
