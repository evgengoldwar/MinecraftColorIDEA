package com.hfstudio.minecraftcoloridea.lang

data class MinecraftLangSourceEntry(
    val locale: String,
    val key: String,
    val filePath: String,
    val lineNumber: Int,
    val lineStartOffset: Int
)
