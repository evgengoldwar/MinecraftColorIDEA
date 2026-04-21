package com.hfstudio.minecraftcoloridea.lang

enum class MatchType(val priority: Int) {
    EXACT(1),
    SUFFIX(2),
    PARTIAL_MATCH(3),
    CONTAINS(4)
}