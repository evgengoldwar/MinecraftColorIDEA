package com.hfstudio.minecraftcoloridea.version

data class MinecraftDetectedVersion(
    val versionId: String,
    val source: MinecraftVersionSource,
    val sourcePath: String,
    val evidence: String
)
