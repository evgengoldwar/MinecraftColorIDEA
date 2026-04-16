package com.hfstudio.minecraftcoloridea.core

object EscapeNormalizer {
    private val unicodeSectionSign = Regex("""\\u00[aA]7""")

    fun normalize(text: String): ResolvedTextWithMapping {
        val builder = StringBuilder()
        val offsets = mutableListOf<Int>()
        var index = 0

        while (index < text.length) {
            val match = unicodeSectionSign.find(text, index)
            if (match == null || match.range.first != index) {
                builder.append(text[index])
                offsets += index
                index += 1
                continue
            }

            builder.append('\u00a7')
            offsets += index
            index = match.range.last + 1
        }

        return ResolvedTextWithMapping(builder.toString(), offsets.toIntArray())
    }
}
