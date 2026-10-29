package com.repmomentum.ui.theme

import androidx.annotation.FontRes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.repmomentum.R

// Palette « Kinetic Ledger » (stitch_repmomentum_android_app/kinetic_ledger/DESIGN.md).
val Lime = Color(0xFFC3F400)
val LimeDim = Color(0xFFABD600)
val Ink = Color(0xFF161E00)
val Cyan = Color(0xFF00E3FD)
val Coral = Color(0xFFFF3366)

private val KineticColors = darkColorScheme(
    primary = Lime,
    onPrimary = Ink,
    primaryContainer = Color(0xFF3C4D00),
    onPrimaryContainer = Color(0xFFC3F400),
    inversePrimary = Color(0xFF506600),
    secondary = Cyan,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F58),
    onSecondaryContainer = Color(0xFF9CF0FF),
    tertiary = Color(0xFFFFB2BA),
    onTertiary = Color(0xFF670020),
    tertiaryContainer = Color(0xFF910030),
    onTertiaryContainer = Color(0xFFFFD9DC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF10141A),
    onBackground = Color(0xFFDFE2EB),
    surface = Color(0xFF10141A),
    onSurface = Color(0xFFDFE2EB),
    surfaceVariant = Color(0xFF31353C),
    onSurfaceVariant = Color(0xFFC4C9AC),
    surfaceTint = LimeDim,
    inverseSurface = Color(0xFFDFE2EB),
    inverseOnSurface = Color(0xFF2D3137),
    outline = Color(0xFF8E9379),
    outlineVariant = Color(0xFF444933),
    surfaceDim = Color(0xFF10141A),
    surfaceBright = Color(0xFF353940),
    surfaceContainerLowest = Color(0xFF0A0E14),
    surfaceContainerLow = Color(0xFF181C22),
    surfaceContainer = Color(0xFF1C2026),
    surfaceContainerHigh = Color(0xFF262A31),
    surfaceContainerHighest = Color(0xFF31353C),
)

/**
 * Polices variables embarquées (licence OFL, voir licenses/) :
 * titres en Space Grotesk, texte en Hanken Grotesk, chiffres en JetBrains Mono
 * pour que les valeurs ne bougent pas en changeant.
 */
@OptIn(ExperimentalTextApi::class)
private fun variableFamily(@FontRes res: Int, vararg weights: FontWeight) = FontFamily(
    weights.map { weight ->
        Font(res, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
    },
)

private val Display = variableFamily(R.font.space_grotesk, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
private val Body = variableFamily(R.font.hanken_grotesk, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)
val Mono = variableFamily(R.font.jetbrains_mono, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

private val KineticTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontFamily = Display, fontWeight = FontWeight.Bold, letterSpacing = (-0.03).em),
        headlineMedium = headlineMedium.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em),
        headlineSmall = headlineSmall.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = Body),
        bodyMedium = bodyMedium.copy(fontFamily = Body),
        bodySmall = bodySmall.copy(fontFamily = Body),
        labelLarge = labelLarge.copy(fontFamily = Display, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = Body, fontWeight = FontWeight.Medium),
        labelSmall = labelSmall.copy(fontFamily = Body, fontWeight = FontWeight.Medium),
    )
}

/** Styles des valeurs numériques (compteurs, heures, charges). */
object MetricText {
    val huge = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.04).em)
    val counter = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 30.sp, letterSpacing = (-0.02).em)
    val value = TextStyle(fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)
    val label = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.08.em)
}

private val KineticShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RepMomentumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KineticColors,
        typography = KineticTypography,
        shapes = KineticShapes,
        content = content,
    )
}
