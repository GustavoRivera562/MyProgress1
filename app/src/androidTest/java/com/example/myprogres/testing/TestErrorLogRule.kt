package com.example.myprogres.testing

import android.util.Log
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class TestErrorLogRule : TestWatcher() {
    override fun starting(description: Description) {
        Log.i(TAG, "[TEST_START] ${description.className}#${description.methodName}")
    }

    override fun succeeded(description: Description) {
        Log.i(TAG, "[TEST_PASS] ${description.className}#${description.methodName}")
    }

    override fun failed(exception: Throwable, description: Description) {
        val header = "[TEST_ERROR] ${description.className}#${description.methodName} " +
            "${exception::class.java.simpleName}: ${exception.message.orEmpty()}"
        Log.e(TAG, header, exception)
        System.err.println(header)
        exception.printStackTrace(System.err)
    }

    private companion object {
        const val TAG = "MyProgresTest"
    }
}
