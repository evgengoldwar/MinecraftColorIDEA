package com.hfstudio.minecraftcoloridea.core

private val rgbExp = Regex(
    """^rgba?\s*\(\s*([01]?\d?\d|2[0-4]\d|25[0-5])\s*,\s*([01]?\d?\d|2[0-4]\d|25[0-5])\s*,\s*([01]?\d?\d|2[0-4]\d|25[0-5])\s*(?:,\s*([\d.]+)\s*)?\)""",
    RegexOption.IGNORE_CASE
)

private val hexExp = Regex("""^(?:#|0[xX])?(?:[a-fA-F0-9]{3}|[a-fA-F0-9]{6}|[a-fA-F0-9]{8})$""")

fun getColorContrast(color: String): String {
    val rgbMatch = rgbExp.matchEntire(color)
    val normalized = color.trim()

    val (r, g, b) = when {
        rgbMatch != null -> Triple(
            rgbMatch.groupValues[1].toInt(),
            rgbMatch.groupValues[2].toInt(),
            rgbMatch.groupValues[3].toInt()
        )

        hexExp.matches(normalized) -> {
            var hex = normalized
                .removePrefix("#")
                .removePrefix("0x")
                .removePrefix("0X")

            if (hex.length == 3) {
                hex = buildString(6) {
                    append(hex[0]).append(hex[0])
                    append(hex[1]).append(hex[1])
                    append(hex[2]).append(hex[2])
                }
            }

            if (hex.length == 8) {
                hex = hex.substring(2)
            }

            Triple(
                hex.substring(0, 2).toInt(16),
                hex.substring(2, 4).toInt(16),
                hex.substring(4, 6).toInt(16)
            )
        }

        else -> return "#FFFFFF"
    }

    val luminance = relativeLuminance(r, g, b)
    val contrastWhite = contrastRatio(luminance, 1.0)
    val contrastBlack = contrastRatio(luminance, 0.0)
    return if (contrastWhite > contrastBlack) "#FFFFFF" else "#000000"
}

private fun contrastRatio(l1: Double, l2: Double): Double {
    return if (l2 < l1) {
        (0.05 + l1) / (0.05 + l2)
    } else {
        (0.05 + l2) / (0.05 + l1)
    }
}

private fun relativeLuminance(r8: Int, g8: Int, b8: Int): Double {
    val r = srgb8ToLinear(r8)
    val g = srgb8ToLinear(g8)
    val b = srgb8ToLinear(b8)
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

private val srgbLookupTable = DoubleArray(256) { index ->
    val c = index / 255.0
    if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).let { Math.pow(it, 2.4) }
}

private fun srgb8ToLinear(c8: Int): Double {
    val index = c8.coerceIn(0, 255) and 0xff
    return srgbLookupTable[index]
}
