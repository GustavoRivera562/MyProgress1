package com.example.myprogres

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.myprogres.feature.auth.AuthViewModel
import com.example.myprogres.feature.progress.ProgressViewModel
import com.example.myprogres.ui.navigation.ProgressApp
import com.example.myprogres.ui.theme.MyProgresTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val progressViewModel: ProgressViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            MyProgresTheme {
                ProgressApp(
                    authViewModel = authViewModel,
                    progressViewModel = progressViewModel
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyProgresPreview() {
    MyProgresTheme {
        androidx.compose.material3.Text("MyProgres")
    }
}
