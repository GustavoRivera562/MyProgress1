package com.example.myprogres.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MyProgresColorScheme = darkColorScheme(
    primary = ProgresLime,
    onPrimary = ProgresBackground,
    secondary = ProgresLimeBright,
    onSecondary = ProgresBackground,
    tertiary = ProgresBlue,
    onTertiary = ProgresBackground,
    background = ProgresBackground,
    onBackground = ProgresText,
    surface = ProgresSurface,
    onSurface = ProgresText,
    surfaceVariant = ProgresSurfaceVariant,
    onSurfaceVariant = ProgresMuted,
    secondaryContainer = ProgresSurfaceVariant,
    onSecondaryContainer = ProgresLime,
    outline = ProgresOutline,
    outlineVariant = ProgresOutline
)

@Composable
fun MyProgresTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MyProgresColorScheme,
        typography = Typography,
        content = content
    )
}
