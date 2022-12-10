package testtools

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset

fun String.toInputStream(): InputStream {
    return ByteArrayInputStream(this.toByteArray(Charset.defaultCharset()))
}