package com.hfstudio.minecraftcoloridea.navigation

object MinecraftTranslationKeyFamilyExpander {
    fun expand(key: String): List<Pair<String, MinecraftCodeUsageConfidence>> {
        val derived = linkedSetOf<Pair<String, MinecraftCodeUsageConfidence>>()
        derived += key to MinecraftCodeUsageConfidence.EXACT

        if (shouldDeriveLegacyNameSuffix(key)) {
            derived += "$key.name" to MinecraftCodeUsageConfidence.FAMILY_DERIVED
        }

        return derived.toList()
    }

    fun expandLegacyFamilies(key: String): List<String> = expand(key).map { it.first }

    private fun shouldDeriveLegacyNameSuffix(key: String): Boolean {
        if (key.endsWith(".name")) {
            return false
        }
        if (!key.startsWith("item.") && !key.startsWith("tile.")) {
            return false
        }
        return key.split('.').size <= 2
    }
}
