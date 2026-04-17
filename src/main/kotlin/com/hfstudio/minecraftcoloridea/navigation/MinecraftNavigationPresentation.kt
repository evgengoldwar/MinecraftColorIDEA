package com.hfstudio.minecraftcoloridea.navigation

import com.hfstudio.minecraftcoloridea.lang.MinecraftLangSourceEntry
import com.intellij.openapi.util.text.StringUtil

data class MinecraftNavigationPresentation(
    val locationText: String,
    val fileNameText: String
) {
    fun toHtml(): String {
        val escapedLocation = StringUtil.escapeXmlEntities(locationText)
        val escapedFileName = StringUtil.escapeXmlEntities(fileNameText)
        return "<html><table width='100%'><tr><td><b>$escapedLocation</b></td><td align='right'>$escapedFileName</td></tr></table></html>"
    }
}

internal fun navigationPresentation(entry: MinecraftLangSourceEntry): MinecraftNavigationPresentation {
    val normalizedPath = entry.filePath.replace('\\', '/')
    val fileName = normalizedPath.substringAfterLast('/')
    val parentPath = normalizedPath.substringBeforeLast('/', missingDelimiterValue = "")
    return MinecraftNavigationPresentation(
        locationText = "${fileName.ifEmpty { normalizedPath }}:${entry.lineNumber}",
        fileNameText = parentPath.ifEmpty { normalizedPath }
    )
}

internal fun navigationPresentation(entry: MinecraftCodeUsageEntry): MinecraftNavigationPresentation {
    val normalizedPath = entry.filePath.replace('\\', '/')
    val fileName = normalizedPath.substringAfterLast('/')
    val parentPath = normalizedPath.substringBeforeLast('/', missingDelimiterValue = "")
    return MinecraftNavigationPresentation(
        locationText = "${fileName.ifEmpty { normalizedPath }}:${entry.lineNumber}",
        fileNameText = parentPath.ifEmpty { normalizedPath }
    )
}
