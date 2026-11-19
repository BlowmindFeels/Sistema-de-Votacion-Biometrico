package com.example.devices.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devices.data.OpcionResultado
import com.example.devices.data.Resultados
import com.example.devices.ui.components.EstadoVacio
import com.example.devices.viewmodel.ResultadosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultadosScreen(
    onVolver: () -> Unit,
    viewModel: ResultadosViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val resultados = state.resultados
    val error = state.error

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultados") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::cargar, enabled = !state.cargando) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                error != null -> EstadoVacio(
                    icono = Icons.Filled.CloudOff,
                    titulo = "No pudimos cargar los resultados",
                    mensaje = error,
                    textoAccion = "Reintentar",
                    onAccion = viewModel::cargar,
                    modifier = Modifier.align(Alignment.Center)
                )

                resultados != null -> ContenidoResultados(state.titulo, resultados)
            }
        }
    }
}

@Composable
private fun ContenidoResultados(titulo: String, r: Resultados) {
    val maximo = r.opciones.maxOfOrNull { it.votos } ?: 0
    val ganadoras = r.opciones.count { it.votos == maximo }
    val empate = r.total > 0 && ganadoras > 1

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            if (titulo.isNotBlank()) {
                Text(titulo, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(12.dp))
            }
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.HowToVote, contentDescription = null, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text("${r.total}", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            if (r.total == 1) "voto emitido" else "votos emitidos",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            if (empate) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Hay un empate entre $ganadoras opciones.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        items(r.opciones.sortedByDescending { it.votos }, key = { it.id }) { op ->
            FilaResultado(op, r.total, esGanadora = r.total > 0 && op.votos == maximo)
        }
        item {
            Text(
                "Los resultados no muestran quién votó: solo el total por opción.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilaResultado(op: OpcionResultado, total: Int, esGanadora: Boolean) {
    val fraccion = if (total > 0) op.votos.toFloat() / total else 0f
    val porcentaje = Math.round(fraccion * 100)
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = cs.surfaceContainerLowest,
        border = BorderStroke(1.dp, if (esGanadora) cs.primary else cs.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (esGanadora) {
                    Icon(
                        Icons.Filled.EmojiEvents,
                        contentDescription = "Opción con más votos",
                        tint = cs.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    op.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (esGanadora) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "$porcentaje %",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (esGanadora) cs.primary else cs.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { fraccion },
                modifier = Modifier.fillMaxWidth().height(10.dp),
                color = if (esGanadora) cs.primary else cs.secondary,
                trackColor = cs.surfaceContainerHighest,
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (op.votos == 1) "1 voto" else "${op.votos} votos",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant
            )
        }
    }
}
