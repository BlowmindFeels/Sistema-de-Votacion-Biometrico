package com.example.devices.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devices.data.EstadoVotacion
import com.example.devices.data.Usuario
import com.example.devices.data.Votacion
import com.example.devices.ui.components.EstadoBadge
import com.example.devices.ui.components.EstadoVacio
import com.example.devices.viewmodel.AccionAdmin
import com.example.devices.viewmodel.AdminUiState
import com.example.devices.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onVolver: () -> Unit,
    onNuevaVotacion: () -> Unit,
    onVerResultados: (Int) -> Unit,
    viewModel: AdminViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // Se recarga al entrar y al volver de crear una votación.
    LaunchedEffect(Unit) { viewModel.cargar() }

    LaunchedEffect(state.mensaje) {
        state.mensaje?.let {
            snackbar.showSnackbar(it)
            viewModel.mensajeMostrado()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it, withDismissAction = true, duration = SnackbarDuration.Long)
            viewModel.errorMostrado()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administración") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::cargar, enabled = !state.procesando) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            if (state.pestana == 0) {
                ExtendedFloatingActionButton(
                    onClick = onNuevaVotacion,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nueva votación") }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = state.pestana) {
                Tab(
                    selected = state.pestana == 0,
                    onClick = { viewModel.seleccionarPestana(0) },
                    text = { Text("Votaciones (${state.votaciones.size})") }
                )
                Tab(
                    selected = state.pestana == 1,
                    onClick = { viewModel.seleccionarPestana(1) },
                    text = { Text("Usuarios (${state.usuarios.size})") }
                )
            }
            if (state.procesando) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            Box(Modifier.fillMaxSize()) {
                if (state.pestana == 0) {
                    PestanaVotaciones(state, viewModel::pedir, onVerResultados)
                } else {
                    PestanaUsuarios(state, viewModel::pedir)
                }
            }
        }
    }

    state.pendiente?.let { accion ->
        DialogoConfirmacion(accion, onConfirmar = viewModel::confirmar, onCancelar = viewModel::cancelar)
    }
}

// ---------------------------------------------------------------------------
// Votaciones
// ---------------------------------------------------------------------------

@Composable
private fun BoxScope.PestanaVotaciones(
    state: AdminUiState,
    onAccion: (AccionAdmin) -> Unit,
    onVerResultados: (Int) -> Unit
) {
    when {
        state.cargandoVotaciones && state.votaciones.isEmpty() ->
            CircularProgressIndicator(Modifier.align(Alignment.Center))

        state.votaciones.isEmpty() -> EstadoVacio(
            icono = Icons.Filled.Inbox,
            titulo = "Aún no hay votaciones",
            mensaje = "Crea la primera con el botón «Nueva votación».",
            modifier = Modifier.align(Alignment.Center)
        )

        else -> LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Resumen(state.votaciones) }
            items(state.votaciones, key = { it.id }) { v ->
                TarjetaAdminVotacion(
                    votacion = v,
                    habilitado = !state.procesando,
                    onAccion = onAccion,
                    onVerResultados = { onVerResultados(v.id) }
                )
            }
        }
    }
}

@Composable
private fun Resumen(votaciones: List<Votacion>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Dato("Activas", votaciones.count { it.estado == EstadoVotacion.ACTIVA }, Modifier.weight(1f))
        Dato("Programadas", votaciones.count { it.estado == EstadoVotacion.PROGRAMADA }, Modifier.weight(1f))
        Dato(
            "Cerradas",
            votaciones.count { it.estado == EstadoVotacion.FINALIZADA || it.estado == EstadoVotacion.CANCELADA },
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun Dato(etiqueta: String, valor: Int, modifier: Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column(Modifier.padding(vertical = 12.dp, horizontal = 12.dp)) {
            Text("$valor", style = MaterialTheme.typography.headlineSmall)
            Text(
                etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaAdminVotacion(
    votacion: Votacion,
    habilitado: Boolean,
    onAccion: (AccionAdmin) -> Unit,
    onVerResultados: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                EstadoBadge(votacion.estado)
                Spacer(Modifier.weight(1f))
                Text(
                    "#${votacion.id}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                votacion.titulo,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "${votacion.fechaInicio} – ${votacion.fechaFin} · ${votacion.opciones.size} opciones",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                when (votacion.estado) {
                    EstadoVotacion.PROGRAMADA -> FilledTonalButton(
                        onClick = { onAccion(AccionAdmin.CambiarEstado(votacion, EstadoVotacion.ACTIVA)) },
                        enabled = habilitado
                    ) { IconoTexto(Icons.Filled.PlayArrow, "Abrir") }

                    EstadoVotacion.ACTIVA -> FilledTonalButton(
                        onClick = { onAccion(AccionAdmin.CambiarEstado(votacion, EstadoVotacion.FINALIZADA)) },
                        enabled = habilitado
                    ) { IconoTexto(Icons.Filled.Done, "Finalizar") }

                    else -> {}
                }
                if (votacion.estado == EstadoVotacion.PROGRAMADA || votacion.estado == EstadoVotacion.ACTIVA) {
                    TextButton(
                        onClick = { onAccion(AccionAdmin.CambiarEstado(votacion, EstadoVotacion.CANCELADA)) },
                        enabled = habilitado,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { IconoTexto(Icons.Filled.Close, "Cancelar") }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onVerResultados) { IconoTexto(Icons.Filled.BarChart, "Resultados") }
            }
        }
    }
}

@Composable
private fun IconoTexto(icono: ImageVector, texto: String) {
    Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(6.dp))
    Text(texto)
}

// ---------------------------------------------------------------------------
// Usuarios
// ---------------------------------------------------------------------------

@Composable
private fun BoxScope.PestanaUsuarios(state: AdminUiState, onAccion: (AccionAdmin) -> Unit) {
    when {
        state.cargandoUsuarios && state.usuarios.isEmpty() ->
            CircularProgressIndicator(Modifier.align(Alignment.Center))

        state.usuarios.isEmpty() -> EstadoVacio(
            icono = Icons.Filled.Person,
            titulo = "No hay usuarios",
            mensaje = "Los vecinos aparecerán aquí cuando se registren.",
            modifier = Modifier.align(Alignment.Center)
        )

        else -> LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.usuarios, key = { it.id }) { u ->
                FilaUsuario(
                    usuario = u,
                    esYo = u.documento == state.documentoPropio,
                    habilitado = !state.procesando,
                    onAccion = onAccion
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 72.dp)
                )
            }
        }
    }
}

@Composable
private fun FilaUsuario(
    usuario: Usuario,
    esYo: Boolean,
    habilitado: Boolean,
    onAccion: (AccionAdmin) -> Unit
) {
    ListItem(
        leadingContent = { Iniciales(usuario.nombre, usuario.esAdmin) },
        headlineContent = {
            Text(
                if (esYo) "${usuario.nombre} (tú)" else usuario.nombre,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column {
                Text("Doc. ${usuario.documento}", style = MaterialTheme.typography.bodySmall)
                Text(
                    usuario.correo,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (usuario.esAdmin) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Administrador",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        trailingContent = {
            if (!esYo) {
                if (usuario.esAdmin) {
                    OutlinedButton(
                        onClick = { onAccion(AccionAdmin.CambiarRol(usuario, hacerAdmin = false)) },
                        enabled = habilitado
                    ) { Text("Quitar admin") }
                } else {
                    FilledTonalButton(
                        onClick = { onAccion(AccionAdmin.CambiarRol(usuario, hacerAdmin = true)) },
                        enabled = habilitado
                    ) { Text("Hacer admin") }
                }
            }
        }
    )
}

@Composable
private fun Iniciales(nombre: String, esAdmin: Boolean) {
    val letras = nombre.trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }.ifBlank { "?" }
    Surface(
        shape = CircleShape,
        color = if (esAdmin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
        contentColor = if (esAdmin) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (esAdmin) Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(22.dp))
            else Text(letras, style = MaterialTheme.typography.titleSmall)
        }
    }
}

// ---------------------------------------------------------------------------
// Confirmación
// ---------------------------------------------------------------------------

@Composable
private fun DialogoConfirmacion(
    accion: AccionAdmin,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit
) {
    val (titulo, texto, boton) = when (accion) {
        is AccionAdmin.CambiarEstado -> when (accion.nuevo) {
            EstadoVotacion.ACTIVA -> Triple(
                "Abrir votación",
                "«${accion.votacion.titulo}» quedará abierta desde ahora y los vecinos podrán votar.",
                "Abrir"
            )
            EstadoVotacion.FINALIZADA -> Triple(
                "Finalizar votación",
                "Se cerrará «${accion.votacion.titulo}» y nadie más podrá votar. Esta acción no se puede deshacer.",
                "Finalizar"
            )
            EstadoVotacion.CANCELADA -> Triple(
                "Cancelar votación",
                "«${accion.votacion.titulo}» quedará cancelada. Esta acción no se puede deshacer.",
                "Cancelar votación"
            )
            EstadoVotacion.PROGRAMADA -> Triple("Cambiar estado", "", "Aceptar")
        }
        is AccionAdmin.CambiarRol -> if (accion.hacerAdmin) Triple(
            "Dar rol de administrador",
            "${accion.usuario.nombre} podrá crear, abrir, cerrar y cancelar votaciones, y gestionar usuarios.",
            "Hacer admin"
        ) else Triple(
            "Quitar rol de administrador",
            "${accion.usuario.nombre} volverá a ser un vecino sin permisos de administración.",
            "Quitar admin"
        )
    }
    val destructiva = accion is AccionAdmin.CambiarEstado &&
        (accion.nuevo == EstadoVotacion.CANCELADA || accion.nuevo == EstadoVotacion.FINALIZADA)

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            TextButton(
                onClick = onConfirmar,
                colors = if (destructiva) ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                         else ButtonDefaults.textButtonColors()
            ) { Text(boton) }
        },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Volver") } }
    )
}
