package testtools

import eu.darken.flowshell.core.common.debug.Logging
import eu.darken.flowshell.core.common.debug.Logging.Priority.VERBOSE
import eu.darken.flowshell.core.common.debug.log
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

open class BaseTest {
    init {
        Logging.clearAll()
        Logging.install(JUnitLogger())
        testClassName = this.javaClass.simpleName
    }

    @BeforeEach open fun setup() {

    }

    @AfterEach open fun teardown() {

    }

    companion object {
        private var testClassName: String? = null

        @JvmStatic
        @AfterAll
        fun onTestClassFinished() {
            unmockkAll()
            log(testClassName!!, VERBOSE) { "onTestClassFinished()" }
            Logging.clearAll()
        }
    }
}