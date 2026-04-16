package com.hfstudio.minecraftcoloridea.lang

data class MinecraftLangIndex(
    val projectLocales: Map<String, Map<String, String>>,
    val dependencyLocales: Map<String, Map<String, String>>
) {
    fun lookup(key: String, localeOrder: List<String>): String? {
        return lookupIn(projectLocales, key, localeOrder)
            ?: lookupIn(dependencyLocales, key, localeOrder)
    }

    private fun lookupIn(
        locales: Map<String, Map<String, String>>,
        key: String,
        localeOrder: List<String>
    ): String? {
        return localeOrder.firstNotNullOfOrNull { locale ->
            locales[locale.lowercase()]?.get(key)
        }
    }
}
