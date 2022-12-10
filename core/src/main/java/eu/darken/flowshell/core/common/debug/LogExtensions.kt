package eu.darken.flowshell.core.common.debug

internal fun logTag(vararg tags: String): String {
    val sb = StringBuilder("\uD83C\uDFDE️\uD83D\uDC1A:")
    for (i in tags.indices) {
        sb.append(tags[i])
        if (i < tags.size - 1) sb.append(":")
    }
    return sb.toString()
}