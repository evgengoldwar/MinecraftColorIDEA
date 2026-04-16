package com.hfstudio.minecraftcoloridea.lang

data class MinecraftCollectedPreview(
    val anchorOffset: Int,
    val preview: MinecraftResolvedPreview
)

class MinecraftPreviewCollector(
    private val index: MinecraftLangIndex,
    private val extraMethodNames: Set<String> = emptySet()
) {
    fun collect(text: String, localeOrder: List<String>): List<MinecraftCollectedPreview> {
        if (text.isEmpty()) {
            return emptyList()
        }

        val resolver = MinecraftLocalizationResolver(index, extraMethodNames)
        val previews = mutableListOf<MinecraftCollectedPreview>()
        var lineStart = 0
        var cursor = 0

        while (cursor <= text.length) {
            if (cursor == text.length || text[cursor] == '\n') {
                val lineEnd = if (cursor > lineStart && text[cursor - 1] == '\r') cursor - 1 else cursor
                val line = text.substring(lineStart, lineEnd)

                if (isCandidateLine(line)) {
                    resolver.resolveExpression(line, localeOrder)
                        ?.takeIf { it.previewText.isNotEmpty() }
                        ?.let { preview ->
                            val anchorOffset = linePreviewAnchor(line)
                            previews += MinecraftCollectedPreview(
                                anchorOffset = lineStart + anchorOffset,
                                preview = preview.copy(anchorOffset = lineStart + anchorOffset)
                            )
                        }
                }

                lineStart = cursor + 1
            }

            cursor += 1
        }

        return previews
    }

    private fun isCandidateLine(line: String): Boolean {
        if (!line.contains('"')) {
            return false
        }

        val trimmed = line.trimStart()
        return !trimmed.startsWith("//") &&
            !trimmed.startsWith("/*") &&
            !trimmed.startsWith("*")
    }

    private fun linePreviewAnchor(line: String): Int {
        var anchor = line.length
        while (anchor > 0 && (line[anchor - 1].isWhitespace() || line[anchor - 1] == ';')) {
            anchor -= 1
        }
        return anchor
    }
}
