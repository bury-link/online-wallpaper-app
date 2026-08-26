package link.bury.onlinewallpaper.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import link.bury.onlinewallpaper.R

private val Ink = Color(0xFF000000)
private val SignalOrange = Color(0xFFFF6C00)
private val Paper = Color(0xFFF7F5F0)
private val White = Color(0xFFFFFFFF)
private val Muted = Color(0xFF6D6A64)
private val DeepSurface = Color(0xFF151310)

private val SpaceGrotesk = FontFamily(
    Font(R.font.space_grotesk, FontWeight.Light),
    Font(R.font.space_grotesk, FontWeight.Normal),
    Font(R.font.space_grotesk, FontWeight.Medium),
    Font(R.font.space_grotesk, FontWeight.Bold),
)

private val LightColors = lightColorScheme(
    primary = SignalOrange,
    onPrimary = Ink,
    primaryContainer = Color(0xFFFFE2D1),
    onPrimaryContainer = Ink,
    secondary = Ink,
    onSecondary = White,
    secondaryContainer = Color(0xFFEDE9E1),
    onSecondaryContainer = Ink,
    tertiary = Muted,
    onTertiary = White,
    background = Paper,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDE9E1),
    onSurfaceVariant = Muted,
    outline = Color(0xFF89847B),
)

private val DarkColors = darkColorScheme(
    primary = SignalOrange,
    onPrimary = Ink,
    primaryContainer = Color(0xFF823500),
    onPrimaryContainer = White,
    secondary = White,
    onSecondary = Ink,
    secondaryContainer = Color(0xFF302D28),
    onSecondaryContainer = White,
    tertiary = Color(0xFFC9C3B9),
    onTertiary = Ink,
    background = Ink,
    onBackground = White,
    surface = DeepSurface,
    onSurface = White,
    surfaceVariant = Color(0xFF302D28),
    onSurfaceVariant = Color(0xFFC9C3B9),
    outline = Color(0xFF9A948B),
)

private val BrandTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)

private val BrandShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

@Composable
fun OnlineWallpaperTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BrandTypography,
        shapes = BrandShapes,
        content = content,
    )
}
