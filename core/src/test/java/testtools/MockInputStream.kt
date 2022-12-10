package testtools

import eu.darken.flowshell.core.common.debug.Logging.Priority.VERBOSE
import eu.darken.flowshell.core.common.debug.log
import java.io.IOException
import java.io.InputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class MockInputStream : InputStream() {
    private val writer: PipedOutputStream = PipedOutputStream()
    private val reader: PipedInputStream = PipedInputStream(writer)

    var isOpen = true
        private set

    fun queue(data: String) {
        try {
            writer.write(data.toByteArray())
            writer.flush()
            log(VERBOSE) { "Written&Flushed: $data" }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class) override fun read(): Int = reader.read()

    @Throws(IOException::class) override fun read(b: ByteArray): Int = reader.read(b)

    @Throws(IOException::class) override fun read(b: ByteArray, off: Int, len: Int): Int = reader.read(b, off, len)

    @Throws(IOException::class) override fun skip(n: Long): Long = reader.skip(n)

    @Throws(IOException::class) override fun available(): Int = reader.available()

    override fun mark(readlimit: Int) {
        reader.mark(readlimit)
    }

    @Throws(IOException::class) override fun reset() {
        reader.reset()
    }

    override fun markSupported(): Boolean = reader.markSupported()

    @Throws(IOException::class) override fun close() {
        log { "close() called" }
        if (!isOpen) return
        isOpen = false
        reader.close()
        writer.flush()
        writer.close()
    }
}