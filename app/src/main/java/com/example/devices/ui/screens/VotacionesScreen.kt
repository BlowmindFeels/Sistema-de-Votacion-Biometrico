package com.example.devices.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devices.data.EstadoVotacion
import com.example.devices.data.Votacion
import com.example.devices.ui.components.EstadoBadge
import com.example.devices.ui.components.EstadoVacio
import com.example.devices.viewmodel.VotacionesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotacionesScreen(
    onVotacionClick: (Int) -> Unit,
    onAdmin: () -> Unit,
    onSesionCerrada: () -> Unit,
    viewModel: VotacionesViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val error = state.error
    var confirmarSalida by remember { mutableStateOf(false) }

    // Se recarga cada vez que la pantalla aparece (al volver de votar o del panel admin).
    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Votaciones") },
                actions = {
                    if (state.esAdmin) {
                        IconButton(onClick = onAdmin) {
                            Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Panel de administración")
                        }
                    }
                    IconButton(onClick = { confirmarSalida = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.esAdmin) {
                ExtendedFloatingActionButton(
                    onClick = onAdmin,
                    icon = { Icon(Icons.Filled.AdminPanelSettings, contentDescription = null) },
                    text = { Text("Administrar") }
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refrescando,
            onRefresh = { viewModel.cargar(comoRefresco = true) },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                state.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                error != null && state.votaciones.isEmpty() -> EstadoVacio(
                    icono = Icons.Filled.CloudOff,
                    titulo = "No pudimos cargar las votaciones",
                    mensaje = error,
                    textoAccion = "Reintentar",
                    onAccion = { viewModel.cargar() },
                    modifier = Modifier.align(Alignment.Center)
                )

                else -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { Saludo(state.nombre, state.cuenta(EstadoVotacion.ACTIVA), state.esAdmin) }
                    item {
                        Filtros(
                            seleccionado = state.filtro,
                            cuenta = state::cuenta,
                            onSeleccionar = viewModel::filtrar
                        )
                    }
                    if (state.visibles.isEmpty()) {
                        item {
                            EstadoVacio(
                                icono = Icons.Filled.Inbox,
                                titulo = "No hay votaciones aquí",
                                mensaje = if (state.filtro == null) "Cuando se publique una votación aparecerá en esta lista."
                                          else "Prueba con otro filtro.",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    items(state.visibles, key = { it.id }) { votacion ->
                        TarjetaVotacion(votacion) { onVotacionClick(votacion.id) }
                    }
                }
            }
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
            title = { Text("Cerrar sesión") },
            text = { Text("Tendrás que ingresar de nuevo con tu documento y contraseña.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalida = false
                    viewModel.cerrarSesion()
                    onSesionCerrada()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun Saludo(nombre: String, activas: Int, esAdmin: Boolean) {
    val primerNombre = nombre.trim().substringBefore(" ").ifBlank { "vecino" }
    Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Text("Hola, $primerNombre", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp))
        Text(
            when (activas) {
                0 -> "No hay votaciones abiertas en este momento."
                1 -> "Hay 1 votación abierta esperando tu voto."
                else -> "Hay $activas votaciones abiertas esperando tu voto."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (esAdmin) {
            Spacer(Modifier.height(8.dp))
            AssistChip(
                onClick = {},
                label = { Text("Administrador") },
                leadingIcon = {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            )
        }
    }
}

@Composable
private fun Filtros(
    seleccionado: EstadoVotacion?,
    cuenta: (EstadoVotacion?) -> Int,
    onSeleccionar: (EstadoVotacion?) -> Unit
) {
    val opciones = listOf(
        null to "Todas",
        EstadoVotacion.ACTIVA to "Activas",
        EstadoVotacion.PROGRAMADA to "Programadas",
        EstadoVotacion.FINALIZADA to "Finalizadas"
    )
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(opciones, key = { it.second }) { (estado, texto) ->
            FilterChip(
                selected = seleccionado == estado,
                onClick = { onSeleccionar(estado) },
                label = { Text("$texto (${cuenta(estado)})") }
            )
        }
    }
}

@Composable
private fun TarjetaVotacion(votacion: Votacion, onClick: () -> Unit) {
    val activa = votacion.estado == EstadoVotacion.ACTIVA
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (activa) MaterialTheme.colorScheme.surfaceContainerLowest
                             else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                EstadoBadge(votacion.estado)
                Spacer(Modifier.height(10.dp))
                Text(
                    votacion.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    votacion.descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${votacion.fechaInicio} – ${votacion.fechaFin}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                    Icon(
                        Icons.Filled.HowToVote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "${votacion.opciones.size} opciones",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
