package com.hfstudio.minecraftcoloridea.core

data class MinecraftDefinition(
    val type: String,
    val start: String,
    val end: String,
    val multiline: Boolean = false,
    val escape: String? = null,
    val scope: MinecraftDefinition? = null
) {
    fun deepCopy(): MinecraftDefinition = copy(scope = scope?.deepCopy())
}

class MinecraftGrammar(
    val languageId: String,
    val definitions: MutableList<MinecraftDefinition> = mutableListOf()
) {
    companion object {
        fun comment(id: String = ""): String = "comment.$id"
        fun string(id: String = ""): String = "string.$id"
        fun scope(id: String = ""): String = "scope.$id"
    }

    fun addDefinition(
        type: String,
        start: String,
        end: String,
        multiline: Boolean = false,
        escape: String? = null,
        scope: MinecraftDefinition? = null
    ): MinecraftGrammar {
        definitions += MinecraftDefinition(
            type = type,
            start = start,
            end = end,
            multiline = multiline,
            escape = escape,
            scope = scope
        )
        return this
    }

    fun clone(languageId: String): MinecraftGrammar {
        return MinecraftGrammar(
            languageId = languageId,
            definitions = definitions.mapTo(mutableListOf()) { it.deepCopy() }
        )
    }
}
