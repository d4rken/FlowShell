package eu.darken.flowshell.core.shell

import java.io.Reader

class LineReader(val lineSeparator: String = System.lineSeparator()) {
    private val separator = lineSeparator.toCharArray()

    fun readLine(reader: Reader): String? {
        var curChar: Char
        val sb = StringBuilder(40)

        var charValue: Int
        while (reader.read().also { charValue = it } != -1) {
            curChar = charValue.toChar()
            when {
                curChar == '\n' && separator.size == 1 && curChar == separator[0] -> {
                    return sb.toString()
                }
                curChar == '\r' && separator.size == 1 && curChar == separator[0] -> {
                    return sb.toString()
                }
                curChar == '\n' && separator.size == 2 && curChar == separator[1] -> {
                    if (sb.isNotEmpty() && sb[sb.length - 1] == separator[0]) {
                        sb.deleteCharAt(sb.length - 1)
                        return sb.toString()
                    }
                }
            }
            sb.append(curChar)
        }
        return if (sb.isEmpty()) null else sb.toString()
    }
}