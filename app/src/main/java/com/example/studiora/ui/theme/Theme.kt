package com.example.studiora.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary              = PrimaryBlueDark,
    onPrimary            = OnPrimaryDark,
    primaryContainer     = PrimaryContainerDark,
    onPrimaryContainer   = PrimaryBlueDark,
    secondary            = AccentOrangeDark,
    onSecondary          = OnPrimary,
    secondaryContainer   = SecondaryContainerDark,
    onSecondaryContainer = AccentOrangeDark,
    tertiary             = AccentOrangeLight,
    onTertiary           = OnPrimary,
    background           = BackgroundDark,
    onBackground         = OnSurfaceDark,
    surface              = SurfaceDark,
    onSurface            = OnSurfaceDark,
    surfaceVariant       = SurfaceVariantDark,
    onSurfaceVariant     = OnSurfaceDark.copy(alpha = 0.7f),
    outline              = OutlineColor.copy(alpha = 0.4f),
    error                = ErrorColor
)

private val LightColorScheme = lightColorScheme(
    primary              = PrimaryBlue,
    onPrimary            = OnPrimary,
    primaryContainer     = PrimaryContainerLight,
    onPrimaryContainer   = PrimaryBlue,
    secondary            = AccentOrange,
    onSecondary          = OnPrimary,
    secondaryContainer   = SecondaryContainerLight,
    onSecondaryContainer = AccentOrange,
    tertiary             = AccentOrangeLight,
    onTertiary           = OnPrimary,
    background           = BackgroundLight,
    onBackground         = OnSurface,
    surface              = SurfaceLight,
    onSurface            = OnSurface,
    surfaceVariant       = SurfaceVariantLight,
    onSurfaceVariant     = OnSurfaceVariant,
    outline              = OutlineColor,
    error                = ErrorColor
)

@Composable
fun StudioraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled for consistent branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}