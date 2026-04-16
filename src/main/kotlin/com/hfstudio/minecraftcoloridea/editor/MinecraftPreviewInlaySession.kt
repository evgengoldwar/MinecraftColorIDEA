package com.hfstudio.minecraftcoloridea.editor

import com.hfstudio.minecraftcoloridea.lang.MinecraftResolvedPreview
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.Inlay

class MinecraftPreviewInlaySession(private val editor: Editor) {
    private val inlays = mutableListOf<Inlay<*>>()

    fun replace(previews: List<MinecraftResolvedPreview>) {
        clear()
        previews.forEach { preview ->
            editor.inlayModel.addInlineElement(
                preview.anchorOffset,
                true,
                MinecraftPreviewRenderer(preview)
            )?.let(inlays::add)
        }
    }

    fun clear() {
        inlays.forEach(Inlay<*>::dispose)
        inlays.clear()
    }
}
