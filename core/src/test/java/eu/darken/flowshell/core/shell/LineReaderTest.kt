package eu.darken.flowshell.core.shell

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import testtools.BaseTest
import testtools.toInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader


class LineReaderTest : BaseTest() {

    @BeforeEach override fun setup() {
        super.setup()
    }

    @AfterEach override fun teardown() {
        super.teardown()
    }

    @Test fun testGetLineSeperator() {
        LineReader().lineSeparator shouldBe System.lineSeparator()
    }

    @Test @Throws(IOException::class) fun testLineEndings_linux() {
        val output = mutableListOf<String>()
        val reader = LineReader("\n")
        val stream: Reader = BufferedReader(InputStreamReader("line1\r\nline2\r\nli\rne\n\n".toInputStream()))
        var line: String?
        while (reader.readLine(stream).also { line = it } != null) {
            output.add(line!!)
        }

        output.size shouldBe 4
        output[0] shouldBe "line1\r"
        output[1] shouldBe "line2\r"
        output[2] shouldBe "li\rne"
        output[3] shouldBe ""
    }

    @Test @Throws(IOException::class) fun testLineEndings_windows() {
        val output = mutableListOf<String>()
        val reader = LineReader("\r\n")
        val stream: Reader = BufferedReader(InputStreamReader("line1\r\nline2\r\n\r\n".toInputStream()))
        var line: String?
        while (reader.readLine(stream).also { line = it } != null) {
            output.add(line!!)
        }

        output.size shouldBe 3
        output[0] shouldBe "line1"
        output[1] shouldBe "line2"
        output[2] shouldBe ""
    }

    @Test @Throws(IOException::class) fun testLineEndings_legacy() {
        val output = mutableListOf<String>()
        val reader = LineReader("\r")
        val stream: Reader = BufferedReader(InputStreamReader("line1\n\rline2\n\rli\nne\r\r".toInputStream()))
        var line: String?
        while (reader.readLine(stream).also { line = it } != null) {
            output.add(line!!)
        }

        output.size shouldBe 4
        output[0] shouldBe "line1\n"
        output[1] shouldBe "line2\n"
        output[2] shouldBe "li\nne"
        output[3] shouldBe ""
    }
}