package com.example.devices.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.devices.data.EstadoVotacion

@Composable
fun EstadoBadge(estado: EstadoVotacion) {
    val cs = MaterialTheme.colorScheme
    val (texto, fondo, contenido) = when (estado) {
        EstadoVotacion.ACTIVA -> Triple("Activa", cs.primaryContainer, cs.onPrimaryContainer)
        EstadoVotacion.PROGRAMADA -> Triple("Programada", cs.tertiaryContainer, cs.onTertiaryContainer)
        EstadoVotacion.FINALIZADA -> Triple("Finalizada", cs.surfaceContainerHighest, cs.onSurfaceVariant)
        EstadoVotacion.CANCELADA -> Triple("Cancelada", cs.errorContainer, cs.onErrorContainer)
    }

    Surface(color = fondo, contentColor = contenido, shape = RoundedCornerShape(50)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 10.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Box(
                Modifier
                    .size(7.dp)
                    .background(contenido, CircleShape)
            )
            Text(text = texto, style = MaterialTheme.typography.labelMedium)
        }
    }
}
