package com.hfstudio.minecraftcoloridea.core

object MinecraftGrammars {
    private val javaScript = MinecraftGrammar("javascript")
        .addDefinition(MinecraftGrammar.comment("block"), "/*", "*/", multiline = true)
        .addDefinition(MinecraftGrammar.comment("line"), "//", "\n")
        .addDefinition(MinecraftGrammar.string("quote.single"), "'", "'", escape = "\\")
        .addDefinition(MinecraftGrammar.string("quote.double"), "\"", "\"", escape = "\\")
        .addDefinition(
            MinecraftGrammar.string("template"),
            "`",
            "`",
            multiline = true,
            escape = "\\",
            scope = MinecraftDefinition(
                type = MinecraftGrammar.scope("template"),
                start = "\${",
                end = "}",
                multiline = true,
                escape = "\\"
            )
        )

    private val java = MinecraftGrammar("java")
        .addDefinition(MinecraftGrammar.comment("block"), "/*", "*/", multiline = true)
        .addDefinition(MinecraftGrammar.comment("line"), "//", "\n")
        .addDefinition(MinecraftGrammar.string("quote.single"), "'", "'", escape = "\\")
        .addDefinition(MinecraftGrammar.string("quote.double"), "\"", "\"", escape = "\\")
        .addDefinition(MinecraftGrammar.string("block"), "\"\"\"", "\"\"\"", multiline = true, escape = "\\")

    private val kotlin = MinecraftGrammar("kotlin")
        .addDefinition(MinecraftGrammar.comment("block"), "/*", "*/", multiline = true)
        .addDefinition(MinecraftGrammar.comment("line"), "//", "\n")
        .addDefinition(MinecraftGrammar.string("quote.single"), "'", "'", escape = "\\")
        .addDefinition(
            MinecraftGrammar.string("interpolated"),
            "\"",
            "\"",
            escape = "\\",
            scope = MinecraftDefinition(
                type = MinecraftGrammar.scope("template"),
                start = "\${",
                end = "}",
                multiline = true,
                escape = "\\"
            )
        )
        .addDefinition(MinecraftGrammar.string("block"), "\"\"\"", "\"\"\"", multiline = true, escape = "\\")

    private val php = MinecraftGrammar("php")
        .addDefinition(MinecraftGrammar.comment("block"), "/*", "*/", multiline = true)
        .addDefinition(MinecraftGrammar.comment("line"), "//", "\n")
        .addDefinition(MinecraftGrammar.string("quote.single"), "'", "'", escape = "\\")
        .addDefinition(MinecraftGrammar.string("quote.double"), "\"", "\"", escape = "\\")
        .addDefinition(MinecraftGrammar.string("heredoc"), "<<<", "\n", multiline = true)
        .addDefinition(MinecraftGrammar.string("nowdoc"), "<<<'", "\n", multiline = true)
        .addDefinition(
            MinecraftGrammar.string("interpolated"),
            "\"",
            "\"",
            escape = "\\",
            scope = MinecraftDefinition(
                type = MinecraftGrammar.scope("interpolated"),
                start = "\${",
                end = "}",
                escape = "\\"
            )
        )

    private val rust = MinecraftGrammar("rust")
        .addDefinition(MinecraftGrammar.comment("block"), "/*", "*/", multiline = true)
        .addDefinition(MinecraftGrammar.comment("line"), "//", "\n")
        .addDefinition(MinecraftGrammar.string("quote.single"), "'", "'", escape = "\\")
        .addDefinition(MinecraftGrammar.string("quote.double"), "\"", "\"", escape = "\\")
        .addDefinition(MinecraftGrammar.string("raw"), "r#\"", "\"#", multiline = true)

    private val go = MinecraftGrammar("go")
        .addDefinition(MinecraftGrammar.comment("block"), "/*", "*/", multiline = true)
        .addDefinition(MinecraftGrammar.comment("line"), "//", "\n")
        .addDefinition(MinecraftGrammar.string("quote.single"), "'", "'", escape = "\\")
        .addDefinition(MinecraftGrammar.string("quote.double"), "\"", "\"", escape = "\\")
        .addDefinition(MinecraftGrammar.string("raw"), "`", "`", multiline = true)

    fun find(languageId: String?): MinecraftGrammar? {
        val normalized = languageId
            ?.trim()
            ?.lowercase()
            ?.replace('_', ' ')
            ?.replace('-', ' ')
            ?.replace(Regex("\\s+"), " ")
            ?: return null

        return when {
            normalized == "java" -> java
            normalized.contains("kotlin") -> kotlin
            normalized.contains("php") -> php
            normalized.contains("rust") -> rust
            normalized == "go" || normalized.contains("golang") -> go
            normalized.contains("typescript") || normalized == "ts" || normalized == "tsx" -> javaScript
            normalized.contains("javascript") || normalized == "js" || normalized == "jsx" || normalized.contains("ecmascript") -> javaScript
            else -> null
        }
    }
}
