package com.example.devices.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devices.data.EstadoVotacion
import com.example.devices.data.Votacion
import com.example.devices.ui.components.AvisoError
import com.example.devices.ui.components.BotonPrincipal
import com.example.devices.ui.components.EstadoBadge
import com.example.devices.viewmodel.DetalleUiState
import com.example.devices.viewmodel.DetalleVotacionViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.devices.util.Biometria
import com.example.devices.util.findFragmentActivity
import com.example.devices.util.TokenSeguro
import com.example.devices.data.SesionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleVotacionScreen(
    onVerResultados: () -> Unit,
    onVolver: () -> Unit,
    viewModel: DetalleVotacionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val votacion = state.votacion
    val context = LocalContext.current

    LaunchedEffect(state.pedirHuella) {
        if (!state.pedirHuella) return@LaunchedEffect

        val motivo = Biometria.mensajeNoDisponible(context)
        val activity = context.findFragmentActivity()
        val preparado = if (motivo == null) TokenSeguro.preparar(context) else null

        when {
            motivo != null -> viewModel.huellaFallida(motivo)
            activity == null -> viewModel.huellaFallida("No se pudo abrir la verificación de huella")
            preparado == null -> viewModel.huellaFallida("No se pudo acceder al almacén seguro de claves")
            else -> Biometria.autenticar(
                activity = activity,
                cipher = preparado.cipher,
                onExito = { cipher ->
                    TokenSeguro.usar(context, TokenSeguro.Preparado(cipher, preparado.cifrar), SesionManager.token)
                        .onSuccess(viewModel::huellaExitosa)
                        .onFailure { e -> viewModel.huellaFallida(e.message) }
                },
                onCancelado = { viewModel.huellaFallida(null) },
                onError = { msg -> viewModel.huellaFallida(msg) }
            )
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Votación") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                votacion == null -> Text(
                    state.error ?: "No se encontró la votación",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                )

                else -> ContenidoDetalle(
                    votacion = votacion,
                    state = state,
                    onSeleccionar = viewModel::seleccionarOpcion,
                    onVotar = viewModel::pedirConfirmacion,
                    onVerResultados = onVerResultados
                )
            }
        }
    }

    if (state.mostrarConfirmacion && votacion != null) {
        val nombreOpcion = votacion.opciones.find { it.id == state.opcionSeleccionada }?.nombre ?: ""
        AlertDialog(
            onDismissRequest = viewModel::cancelarConfirmacion,
            title = { Text("Confirmar voto") },
            icon = { Icon(Icons.Filled.Fingerprint, contentDescription = null) },
            text = {
                Text(
                    "Vas a votar por «$nombreOpcion». Después confirmarás con tu huella. " +
                        "Una vez emitido, el voto no se puede cambiar."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarVoto) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarConfirmacion) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun ContenidoDetalle(
    votacion: Votacion,
    state: DetalleUiState,
    onSeleccionar: (Int) -> Unit,
    onVotar: () -> Unit,
    onVerResultados: () -> Unit
) {
    val puedeVotar = votacion.estado == EstadoVotacion.ACTIVA && !state.yaVoto

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            EstadoBadge(votacion.estado)
            Spacer(Modifier.height(12.dp))
            Text(votacion.titulo, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(votacion.descripcion, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Del ${votacion.fechaInicio} al ${votacion.fechaFin}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(20.dp))
            when {
                state.yaVoto -> Aviso(
                    icono = Icons.Filled.CheckCircle,
                    texto = "Tu voto quedó registrado. Gracias por participar.",
                    fondo = MaterialTheme.colorScheme.primaryContainer,
                    contenido = MaterialTheme.colorScheme.onPrimaryContainer
                )
                votacion.estado == EstadoVotacion.PROGRAMADA -> Aviso(
                    icono = Icons.Filled.Schedule,
                    texto = "Esta votación aún no ha abierto.",
                    fondo = MaterialTheme.colorScheme.tertiaryContainer,
                    contenido = MaterialTheme.colorScheme.onTertiaryContainer
                )
                votacion.estado != EstadoVotacion.ACTIVA -> Aviso(
                    icono = Icons.Filled.Lock,
                    texto = "Esta votación está cerrada. Puedes consultar los resultados.",
                    fondo = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contenido = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                if (puedeVotar) "Elige una opción" else "Opciones",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                votacion.opciones.forEach { opcion ->
                    FilaOpcion(
                        nombre = opcion.nombre,
                        seleccionada = state.opcionSeleccionada == opcion.id,
                        habilitada = puedeVotar && !state.enviando,
                        onClick = { onSeleccionar(opcion.id) }
                    )
                }
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                AvisoError(it)
            }
            Spacer(Modifier.height(16.dp))
        }

        // Acciones fijas abajo, siempre visibles aunque haya muchas opciones.
        Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                if (puedeVotar) {
                    BotonPrincipal(
                        texto = "Votar con mi huella",
                        onClick = onVotar,
                        cargando = state.enviando,
                        habilitado = state.opcionSeleccionada != null,
                        icono = Icons.Filled.Fingerprint
                    )
                    Spacer(Modifier.height(4.dp))
                }
                OutlinedButton(
                    onClick = onVerResultados,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Filled.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver resultados")
                }
            }
        }
    }
}

@Composable
private fun Aviso(
    icono: ImageVector,
    texto: String,
    fondo: Color,
    contenido: Color
) {
    Surface(
        color = fondo,
        contentColor = contenido,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun FilaOpcion(
    nombre: String,
    seleccionada: Boolean,
    habilitada: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (seleccionada) cs.primaryContainer else cs.surfaceContainerLowest,
        contentColor = if (seleccionada) cs.onPrimaryContainer else cs.onSurface,
        border = BorderStroke(
            width = if (seleccionada) 2.dp else 1.dp,
            color = if (seleccionada) cs.primary else cs.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = seleccionada,
                enabled = habilitada,
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = seleccionada, onClick = null, enabled = habilitada)
            Spacer(Modifier.width(12.dp))
            Text(nombre, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
