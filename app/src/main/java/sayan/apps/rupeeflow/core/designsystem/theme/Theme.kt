package sayan.apps.rupeeflow.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class RupeeFlowColors(
    val income: Color = IncomeGreen,
    val expense: Color = ExpenseRed,
    val warning: Color = WarningOrange,
    val info: Color = InfoBlue
)

val LocalRupeeFlowColors = staticCompositionLocalOf { RupeeFlowColors() }

private val BrandedDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    secondary = SecondaryDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    onPrimary = OnPrimaryDark,
    onSecondary = OnPrimaryDark,
    onBackground = OnBackgroundDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    error = ExpenseRed
)

@Composable
fun RupeeFlowTheme(
    amoledBlack: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    // 1. Determine base color scheme (Dynamic or Branded)
    val baseColorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        BrandedDarkColorScheme
    }

    // 2. Determine background and surface overrides based on AMOLED setting
    val backgroundColor = if (amoledBlack) AmoledBlack else BackgroundDark
    
    // 3. Construct the final color scheme by overriding specific roles
    val colorScheme = baseColorScheme.copy(
        background = backgroundColor,
        surface = SurfaceDark,
        surfaceVariant = SurfaceVariantDark,
        onBackground = TextPrimary,
        onSurface = TextPrimary,
        onSurfaceVariant = TextSecondary,
        error = ExpenseRed
    )

    val rupeeFlowColors = RupeeFlowColors() // Always use branded financial colors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    CompositionLocalProvider(LocalRupeeFlowColors provides rupeeFlowColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

object RupeeFlowTheme {
    val colors: RupeeFlowColors
        @Composable
        get() = LocalRupeeFlowColors.current
}
