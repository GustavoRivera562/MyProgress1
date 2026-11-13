package com.example.myprogres

import com.example.myprogres.testing.TestErrorLogRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ExampleUnitTest {
    @get:Rule
    val testErrorLogRule = TestErrorLogRule()

    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
