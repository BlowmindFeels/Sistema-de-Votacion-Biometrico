package com.example.devices.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devices.data.EstadoVotacion
import com.example.devices.data.Votacion
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
                title = { Text("Detalle") },
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
            text = { Text("Vas a votar por «$nombreOpcion». Una vez emitido, no podrás cambiarlo.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarVoto) { Text("Votar") }
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

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        EstadoBadge(votacion.estado)
        Spacer(Modifier.height(8.dp))
        Text(votacion.titulo, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(votacion.descripcion, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Del ${votacion.fechaInicio} al ${votacion.fechaFin}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))
        Text("Opciones", style = MaterialTheme.typography.titleMedium)
        votacion.opciones.forEach { opcion ->
            FilaOpcion(
                nombre = opcion.nombre,
                seleccionada = state.opcionSeleccionada == opcion.id,
                habilitada = puedeVotar && !state.enviando,
                onClick = { onSeleccionar(opcion.id) }
            )
        }

        Spacer(Modifier.height(16.dp))
        when {
            state.yaVoto -> Text(
                "Ya emitiste tu voto en esta votación.",
                color = MaterialTheme.colorScheme.primary
            )
            votacion.estado != EstadoVotacion.ACTIVA -> Text(
                "Esta votación no está abierta para votar.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(16.dp))
        if (puedeVotar) {
            Button(
                onClick = onVotar,
                enabled = state.opcionSeleccionada != null && !state.enviando,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.enviando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Emitir voto")
                }
            }
        }
        TextButton(onClick = onVerResultados, modifier = Modifier.fillMaxWidth()) {
            Text("Ver resultados")
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = seleccionada,
                enabled = habilitada,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = seleccionada, onClick = null, enabled = habilitada)
        Spacer(Modifier.width(12.dp))
        Text(nombre, style = MaterialTheme.typography.bodyLarge)
    }
}