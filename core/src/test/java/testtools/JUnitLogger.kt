package testtools

import eu.darken.flowshell.core.common.debug.Logging

internal class JUnitLogger(private val minLogLevel: Logging.Priority = Logging.Priority.VERBOSE) : Logging.Logger {

    override fun isLoggable(priority: Logging.Priority): Boolean = priority.intValue >= minLogLevel.intValue

    override fun log(priority: Logging.Priority, tag: String, message: String, metaData: Map<String, Any>?) {
        println("${System.currentTimeMillis()} ${priority.shortLabel}/$tag: $message")
    }

}
