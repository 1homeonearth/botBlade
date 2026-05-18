package com.princess.royalscepter.data.api

import java.nio.charset.StandardCharsets

object UrlEncoding {
    private val unreservedCharacters = setOf('-', '.', '_', '~')

    fun pathSegment(value: String): String = value.encodePathComponent(encodeSlash = true)

    fun filePath(value: String): String = value
        .split('/')
        .joinToString(separator = "/") { segment -> segment.encodePathComponent(encodeSlash = true) }

    private fun String.encodePathComponent(encodeSlash: Boolean): String {
        val builder = StringBuilder()
        for (byte in toByteArray(StandardCharsets.UTF_8)) {
            val unsigned = byte.toInt() and 0xff
            val character = unsigned.toChar()
            val isUnreserved = character in 'A'..'Z' ||
                character in 'a'..'z' ||
                character in '0'..'9' ||
                character in unreservedCharacters ||
                (!encodeSlash && character == '/')
            if (isUnreserved) {
                builder.append(character)
            } else {
                builder.append('%')
                builder.append(unsigned.toString(16).uppercase().padStart(2, '0'))
            }
        }
        return builder.toString()
    }
}
