package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class StoryCanvasExtendedColors(
    val canvasBackground: Color,
    val starFieldDot: Color,
    val connectorBranch: Color,
    val connectorRelationship: Color,
    val nodeAccentPalette: List<Color>,
)

private val LocalStoryCanvasExtendedColors = staticCompositionLocalOf {
    StoryCanvasExtendedColors(
        canvasBackground = BackgroundDark,
        starFieldDot = StarFieldDotDark,
        connectorBranch = ConnectorBranchDark,
        connectorRelationship = ConnectorRelationshipDark,
        nodeAccentPalette = NodeAccentPalette,
    )
}

object StoryCanvasExtras {
    val colors: StoryCanvasExtendedColors
        @Composable
        get() = LocalStoryCanvasExtendedColors.current
}

private val DarkColors = darkColorScheme(
    primary = DuskVioletDark,
    onPrimary = OnDuskVioletDark,
    primaryContainer = DuskVioletContainerDark,
    onPrimaryContainer = OnDuskVioletContainerDark,
    secondary = BrassDark,
    onSecondary = OnBrassDark,
    secondaryContainer = BrassContainerDark,
    onSecondaryContainer = OnBrassContainerDark,
    tertiary = NebulaTealDark,
    onTertiary = OnNebulaTealDark,
    tertiaryContainer = NebulaTealContainerDark,
    onTertiaryContainer = OnNebulaTealContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    inversePrimary = InversePrimaryDark,
    scrim = ScrimColor,
)

private val LightColors = lightColorScheme(
    primary = DuskVioletLight,
    onPrimary = OnDuskVioletLight,
    primaryContainer = DuskVioletContainerLight,
    onPrimaryContainer = OnDuskVioletContainerLight,
    secondary = BrassLight,
    onSecondary = OnBrassLight,
    secondaryContainer = BrassContainerLight,
    onSecondaryContainer = OnBrassContainerLight,
    tertiary = NebulaTealLight,
    onTertiary = OnNebulaTealLight,
    tertiaryContainer = NebulaTealContainerLight,
    onTertiaryContainer = OnNebulaTealContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    inversePrimary = InversePrimaryLight,
    scrim = ScrimColor,
)

@Composable
fun StoryCanvasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extendedColors = if (darkTheme) {
        StoryCanvasExtendedColors(
            canvasBackground = BackgroundDark,
            starFieldDot = StarFieldDotDark,
            connectorBranch = ConnectorBranchDark,
            connectorRelationship = ConnectorRelationshipDark,
            nodeAccentPalette = NodeAccentPalette,
        )
    } else {
        StoryCanvasExtendedColors(
            canvasBackground = BackgroundLight,
            starFieldDot = StarFieldDotLight,
            connectorBranch = ConnectorBranchLight,
            connectorRelationship = ConnectorRelationshipLight,
            nodeAccentPalette = NodeAccentPalette,
        )
    }

    CompositionLocalProvider(LocalStoryCanvasExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StoryCanvasTypography,
            shapes = StoryCanvasShapes,
            content = content,
        )
    }
}
