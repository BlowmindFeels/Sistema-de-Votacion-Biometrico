package com.example.devices.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = VerdePrimario,
    onPrimary = Color.White,
    primaryContainer = VerdeContenedor,
    onPrimaryContainer = VerdeSobreContenedor,
    secondary = SalviaSecundario,
    onSecondary = Color.White,
    secondaryContainer = SalviaContenedor,
    onSecondaryContainer = SalviaSobreContenedor,
    tertiary = AzulTerciario,
    onTertiary = Color.White,
    tertiaryContainer = AzulContenedor,
    onTertiaryContainer = AzulSobreContenedor,
    background = FondoClaro,
    onBackground = TextoClaro,
    surface = FondoClaro,
    onSurface = TextoClaro,
    surfaceVariant = VarianteClaro,
    onSurfaceVariant = SobreVarianteClaro,
    outline = BordeClaro,
    outlineVariant = BordeSuaveClaro,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F5F0),
    surfaceContainer = Color(0xFFEAEFEA),
    surfaceContainerHigh = Color(0xFFE4EAE5),
    surfaceContainerHighest = Color(0xFFDFE4DF)
)

private val DarkColorScheme = darkColorScheme(
    primary = VerdePrimarioOsc,
    onPrimary = VerdeSobrePrimarioOsc,
    primaryContainer = VerdeContenedorOsc,
    onPrimaryContainer = VerdeSobreContenedorOsc,
    secondary = SalviaSecundarioOsc,
    onSecondary = Color(0xFF1E352A),
    secondaryContainer = SalviaContenedorOsc,
    onSecondaryContainer = SalviaContenedor,
    tertiary = AzulTerciarioOsc,
    onTertiary = Color(0xFF0B3443),
    tertiaryContainer = AzulContenedorOsc,
    onTertiaryContainer = AzulContenedor,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = FondoOscuro,
    onSurface = TextoOscuro,
    surfaceVariant = VarianteOscuro,
    onSurfaceVariant = SobreVarianteOscuro,
    outline = BordeOscuro,
    outlineVariant = BordeSuaveOscuro,
    surfaceContainerLowest = Color(0xFF0A0F0D),
    surfaceContainerLow = Color(0xFF171D1A),
    surfaceContainer = Color(0xFF1B211E),
    surfaceContainerHigh = Color(0xFF252B28),
    surfaceContainerHighest = Color(0xFF303633)
)

private val Formas = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DevicesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Colores propios por defecto, para que la app se vea igual en todos los teléfonos.
    dynamicColor: Boolean = false,
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
        shapes = Formas,
        content = content
    )
}
