package com.hfstudio.minecraftcoloridea.core

object MinecraftFormatInterpolator {
    fun interpolate(template: String, args: List<String>): String {
        val result = StringBuilder()
        var sequentialIndex = 0
        var index = 0

        while (index < template.length) {
            if (template[index] != '%') {
                result.append(template[index])
                index += 1
                continue
            }

            if (index + 1 < template.length && template[index + 1] == '%') {
                result.append('%')
                index += 2
                continue
            }

            var cursor = index + 1
            var explicitIndex: Int? = null
            val numberStart = cursor

            while (cursor < template.length && template[cursor].isDigit()) {
                cursor += 1
            }

            if (cursor > numberStart && cursor < template.length && template[cursor] == '$') {
                explicitIndex = template.substring(numberStart, cursor).toInt() - 1
                cursor += 1
            } else {
                cursor = index + 1
            }

            if (cursor < template.length && (template[cursor] == 's' || template[cursor] == 'd')) {
                val argumentIndex = explicitIndex ?: sequentialIndex++
                result.append(args.getOrNull(argumentIndex) ?: template.substring(index, cursor + 1))
                index = cursor + 1
                continue
            }

            result.append(template[index])
            index += 1
        }

        return result.toString()
    }
}
