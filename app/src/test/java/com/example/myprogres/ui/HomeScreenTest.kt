package com.example.myprogres.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.myprogres.domain.model.DiagnosticSnapshot
import com.example.myprogres.feature.progress.ProgressUiState
import com.example.myprogres.testing.TestErrorLogRule
import com.example.myprogres.ui.screens.HomeScreen
import com.example.myprogres.ui.theme.MyProgresTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], manifest = "AndroidManifest.xml")
class HomeScreenTest {
    @get:Rule
    val testErrorLogRule = TestErrorLogRule()

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreen_showsTrainingAndWaterSummary() {
        composeRule.setContent {
            MyProgresTheme {
                HomeScreen(
                    state = ProgressUiState(
                        todayWaterMl = 500,
                        diagnostics = DiagnosticSnapshot(0, emptyList())
                    ),
                    onStartTraining = {},
                    onOpenWater = {}
                )
            }
        }

        composeRule.onNodeWithText("¿Listo para entrenar?").assertIsDisplayed()
        composeRule.onNodeWithText("500 ml").assertIsDisplayed()
    }
}
