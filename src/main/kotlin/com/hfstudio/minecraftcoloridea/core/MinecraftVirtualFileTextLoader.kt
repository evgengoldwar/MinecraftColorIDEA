package com.hfstudio.minecraftcoloridea.core

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile

object MinecraftVirtualFileTextLoader {
    fun load(file: VirtualFile): String {
        return StringUtil.convertLineSeparators(
            String(file.contentsToByteArray(), file.charset)
        )
    }
}
