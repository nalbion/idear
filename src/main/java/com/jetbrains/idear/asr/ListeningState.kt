package com.jetbrains.idear.asr

import com.jetbrains.idear.asr.ListeningState.Status.*
import java.util.concurrent.atomic.AtomicReference

/**
 * Created by breandan on 10/23/2015.
 */
object ListeningState {
    private val status = AtomicReference(INIT)

    enum class Status {
        INIT,
        ACTIVE,
        STANDBY,
        TERMINATED
    }

    private fun setStatus(s: Status) = status.getAndSet(s)

    fun getStatus() = status.get()

    val isTerminated: Boolean
        get() = getStatus() == TERMINATED

    val isInit: Boolean
        get() = getStatus() == INIT

    val isActive: Boolean
        get() = getStatus() == ACTIVE

    fun standBy() = STANDBY == setStatus(STANDBY)

    fun activate() = ACTIVE == setStatus(ACTIVE)
}
