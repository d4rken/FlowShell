package eu.darken.flowshell.core.common

import androidx.annotation.VisibleForTesting
import eu.darken.flowshell.core.common.debug.log
import eu.darken.flowshell.core.common.debug.logTag
import java.util.*

internal object FlowShellDebug {
    private val TAG = logTag("Debug")
    private var DEBUG = BuildConfigWrap.DEBUG

    var isDebug: Boolean
        get() = DEBUG
        set(debug) {
            log(TAG) { "setDebug(debug=$debug)" }
            DEBUG = debug
        }

    @VisibleForTesting
    val CALLBACKS = Collections.synchronizedSet(HashSet<Callback>())

    private val processCallbacks: Set<ProcessCallback>
        private get() {
            if (CALLBACKS.isEmpty()) return emptySet()
            var callbacks: MutableSet<ProcessCallback>
            synchronized(CALLBACKS) {
                callbacks = HashSet()
                for (callback in CALLBACKS) {
                    if (callback is ProcessCallback) {
                        callbacks.add(callback)
                    }
                }
            }
            return callbacks
        }

    fun notifyOnProcessStart(process: Process) {
        processCallbacks.forEach { it.onProcessStart(process) }
    }

    fun notifyOnProcessEnd(process: Process) {
        processCallbacks.forEach { it.onProcessEnd(process) }
    }

    fun addCallback(callback: Callback) {
        CALLBACKS.add(callback)
    }

    fun removeCallback(callback: Callback) {
        CALLBACKS.remove(callback)
    }

    interface Callback
    interface ProcessCallback : Callback {
        fun onProcessStart(process: Process)
        fun onProcessEnd(process: Process)
    }
}