package com.hfstudio.minecraftcoloridea.navigation

data class MinecraftResolvedCodeUsageTarget(
    val key: String,
    val entries: List<MinecraftCodeUsageEntry>
)

object MinecraftCodeUsageNavigationResolver {
    sealed interface NavigationRequestResult {
        data object IndexNotReady : NavigationRequestResult
        data object NotFound : NavigationRequestResult
        data class Target(val target: MinecraftResolvedCodeUsageTarget) : NavigationRequestResult
    }

    fun resolve(
        key: String,
        usageIndexStamp: Long,
        lookup: (String) -> List<MinecraftCodeUsageEntry>?
    ): NavigationRequestResult {
        if (usageIndexStamp == 0L) {
            return NavigationRequestResult.IndexNotReady
        }

        val entries = lookup(key).orEmpty()
            .sortedWith(
                compareBy<MinecraftCodeUsageEntry>(
                    { confidenceRank(it.confidence) },
                    { it.filePath },
                    { it.lineNumber }
                )
            )

        if (entries.isEmpty()) {
            return NavigationRequestResult.NotFound
        }

        return NavigationRequestResult.Target(
            MinecraftResolvedCodeUsageTarget(
                key = key,
                entries = entries
            )
        )
    }

    private fun confidenceRank(confidence: MinecraftCodeUsageConfidence): Int {
        return when (confidence) {
            MinecraftCodeUsageConfidence.EXACT -> 0
            MinecraftCodeUsageConfidence.FAMILY_DERIVED -> 1
            MinecraftCodeUsageConfidence.ENUMERATED -> 2
        }
    }
}
