package testtools

import eu.darken.flowshell.core.common.debug.Logging.Priority.ERROR
import eu.darken.flowshell.core.common.debug.Logging.Priority.VERBOSE
import eu.darken.flowshell.core.common.debug.asLog
import eu.darken.flowshell.core.common.debug.log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class MockProcess(
    private val lineSeparator: String = "\n"
) : Process() {
    private val cmdListeners: MutableList<CmdListener> = ArrayList()
    private val dataStream = MockInputStream()
    private val errorStream = MockInputStream()
    private val cmdStream = MockOutputStream(object : MockOutputStream.Listener {
        override fun onNewLine(line: String?) {
            cmdLines.add(line)
            processorQueue.add(line)
        }

        override fun onClose() {
            log(TAG) { "CmdStream: onClose()" }
        }
    })
    val cmdLines = LinkedBlockingQueue<String?>()
    val processorQueue = LinkedBlockingQueue<String?>()

    @Volatile
    var exitCode: Int? = null

    @Volatile
    var isDestroyed = false
    private val exitLatch = CountDownLatch(1)
    private val processor = Thread {

        // We keep processing while there is more input or could be more input
        while (cmdStream.isOpen || processorQueue.size > 0) {
//                Timber.v("CommandProcessor: isInputOpen=%b, queued='%s", cmdStream.isOpen(), processorQueue);
            var line: String? = null
            try {
                line = processorQueue.poll(100, TimeUnit.MILLISECONDS)
            } catch (e: InterruptedException) {
                log(TAG, ERROR) { "Error polling: ${e.asLog()}" }
            }
            if (line == null) continue
            var alreadyProcessed = false
            for (l in cmdListeners) {
                if (l.onNewCmd(line)) alreadyProcessed = true
            }
            if (!alreadyProcessed) {
                if (line.endsWith(" $?" + lineSeparator)) {

                    // By default we assume all commands exit OK
                    val split = line.split(" ").toTypedArray()
                    printData("${split[1]} 0")
                } else if (line.endsWith(" >&2$lineSeparator")) {
                    val split = line.split(" ").toTypedArray()
                    printError(split[1])
                } else if (line.startsWith("sleep")) {
                    val split: Array<String> = line.replace(lineSeparator, "").split(" ").toTypedArray()
                    val delay = split[1].toLong()
                    log(TAG, VERBOSE) { "Sleeping for $delay" }
                    try {
                        Thread.sleep(delay)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                } else if (line.startsWith("echo")) {
                    val split: Array<String> =
                        line.replace(lineSeparator, "").split(" ").toTypedArray()
                    printData(split[1])
                } else if (line.startsWith("error")) {
                    val split: Array<String> =
                        line.replace(lineSeparator, "").split(" ").toTypedArray()
                    printError(split[1])
                } else if (line.startsWith("exit")) {
                    try {
                        cmdStream.close()
                    } catch (e: IOException) {
                        log(TAG, ERROR) { "Error closing cmdStream: ${e.asLog()}" }
                    }
                }
            }
        }
        if (exitCode == null && !isDestroyed) exitCode = 0

        // The processor isn't running anymore so no more output/errors
        try {
            dataStream.close()
            errorStream.close()
        } catch (e: IOException) {
            log(TAG, ERROR) { "Error closing data and error stream: ${e.asLog()}" }
        }
        exitLatch.countDown()
        log(TAG, VERBOSE) { "Processor finished." }
    }

    init {
        processor.start()
    }

    @get:Synchronized val lastCommandRaw: String?
        get() = cmdLines.poll()

    @Synchronized override fun getOutputStream(): OutputStream = cmdStream

    @Synchronized override fun getInputStream(): InputStream = dataStream

    @Synchronized override fun getErrorStream(): InputStream = errorStream

    @Throws(InterruptedException::class) override fun waitFor(): Int {
        log(TAG, VERBOSE) { "waitFor()" }
        exitLatch.await()
        return exitCode!!
    }

    @Synchronized override fun exitValue(): Int {
        return if (exitCode == null || exitLatch.count > 0) throw IllegalThreadStateException() else exitCode!!
    }

    override fun isAlive(): Boolean {
        return exitLatch.count > 0
    }

    override fun destroy() {
        synchronized(this) {
            if (isDestroyed) return
            isDestroyed = true
            log(TAG, VERBOSE) { "destroy()" }
        }
        if (exitCode == null) exitCode = 1
        try {
            cmdStream.close()
            dataStream.close()
            errorStream.close()
        } catch (e: IOException) {
            log(TAG, ERROR) { "destroy() failed to close everything: ${e.asLog()}" }
        }
        exitLatch.countDown()
    }

    override fun destroyForcibly(): Process {
        destroy()
        return this
    }

    fun printData(output: String) {
        dataStream.queue(output + lineSeparator)
    }

    fun printError(error: String) {
        errorStream.queue(error + lineSeparator)
    }

    fun addCmdListener(listener: CmdListener) {
        cmdListeners.add(listener)
    }

    interface CmdListener {
        fun onNewCmd(line: String): Boolean
    }

    companion object {
        const val TAG = "MockProcess"
    }
}