package com.example.devices.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devices.data.OpcionResultado
import com.example.devices.data.Resultados
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

                error != null -> Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::cargar) { Text("Reintentar") }
                }

                resultados != null -> ContenidoResultados(state.titulo, resultados)
            }
        }
    }
}

@Composable
private fun ContenidoResultados(titulo: String, r: Resultados) {
    val maximo = r.opciones.maxOfOrNull { it.votos } ?: 0

    LazyColumn(
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (titulo.isNotBlank()) {
                Text(titulo, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
            }
            Text(
                if (r.total == 1) "1 voto en total" else "${r.total} votos en total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(r.opciones, key = { it.id }) { op ->
            FilaResultado(op, r.total, esGanadora = r.total > 0 && op.votos == maximo)
        }
        item {
            Text(
                "Los resultados se leen de la réplica de la base de votos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilaResultado(op: OpcionResultado, total: Int, esGanadora: Boolean) {
    val fraccion = if (total > 0) op.votos.toFloat() / total else 0f
    val porcentaje = (fraccion * 100).toInt()

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                op.nombre,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (esGanadora) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            Text(
                "$porcentaje % · ${op.votos}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { fraccion },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = if (esGanadora) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
