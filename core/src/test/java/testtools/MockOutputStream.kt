package testtools

import eu.darken.flowshell.core.common.debug.log
import java.io.IOException
import java.io.OutputStream

class MockOutputStream(private val listener: Listener?) : OutputStream() {
    private val dataSync = Any()
    private var data = StringBuilder()
    var isOpen = true
        private set
    private val stringBuffer = StringBuffer()
    var closeException: IOException? = null

    @Throws(IOException::class) override fun write(i: Int) {
        synchronized(dataSync) {
            val c = i.toChar()
            data.append(c)
            stringBuffer.append(i.toChar())
            if (c == '\n') {
                val line = data.toString()
                log { "Line: $line" }
                listener?.onNewLine(line)
                data = StringBuilder()
            }
        }
    }

    @Synchronized @Throws(IOException::class) override fun close() {
        log { "close() called" }
        closeException?.let {
            isOpen = false
            closeException = null
            throw it
        }

        if (!isOpen) return
        isOpen = false
        listener?.onClose()
    }


    fun getData(): StringBuffer {
        return stringBuffer
    }

    interface Listener {
        fun onNewLine(line: String?)
        fun onClose()
    }
}