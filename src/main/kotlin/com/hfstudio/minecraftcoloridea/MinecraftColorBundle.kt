package com.hfstudio.minecraftcoloridea

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey
import java.util.Locale
import java.util.ResourceBundle

private const val BUNDLE = "messages.MinecraftColorBundle"

object MinecraftColorBundle : DynamicBundle(BUNDLE) {
    @Nls
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)

    @Nls
    fun messageForLocale(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        locale: Locale,
        vararg params: Any
    ): String = messageOrDefault(ResourceBundle.getBundle(BUNDLE, locale), key, null, *params) ?: key
}
