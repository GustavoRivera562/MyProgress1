package com.example.myprogres.core.error

interface CrashReporter {
    /** Receives only a stable error code; exception messages may contain private data. */
    fun record(errorCode: String)
}

object NoOpCrashReporter : CrashReporter {
    override fun record(errorCode: String) = Unit
}
