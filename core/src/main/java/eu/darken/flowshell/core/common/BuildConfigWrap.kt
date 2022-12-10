package eu.darken.flowshell.core.common

import androidx.annotation.Keep
import eu.darken.flowshell.core.BuildConfig


// Can't be const because that prevents them from being mocked in tests
@Suppress("MayBeConstant")
@Keep
object BuildConfigWrap {
    val DEBUG: Boolean = BuildConfig.DEBUG
}
