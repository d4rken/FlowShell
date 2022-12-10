package eu.darken.flowshell.core.common

import android.os.Build

// Can't be const because that prevents them from being mocked in tests
@Suppress("MayBeConstant")
internal object BuildWrap {

    val VERSION = VersionWrap

    internal object VersionWrap {
        val SDK_INT = Build.VERSION.SDK_INT
    }
}

internal fun hasApiLevel(level: Int): Boolean = BuildWrap.VERSION.SDK_INT >= level
