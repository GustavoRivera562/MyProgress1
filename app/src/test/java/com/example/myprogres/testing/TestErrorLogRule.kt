package com.example.myprogres.testing

import org.junit.rules.TestWatcher
import org.junit.runner.Description

class TestErrorLogRule : TestWatcher() {
    override fun starting(description: Description) {
        println("[TEST_START] ${description.className}#${description.methodName}")
    }

    override fun succeeded(description: Description) {
        println("[TEST_PASS] ${description.className}#${description.methodName}")
    }

    override fun failed(exception: Throwable, description: Description) {
        System.err.println(
            "[TEST_ERROR] ${description.className}#${description.methodName} " +
                "${exception::class.java.simpleName}: ${exception.message.orEmpty()}"
        )
        exception.printStackTrace(System.err)
    }
}
