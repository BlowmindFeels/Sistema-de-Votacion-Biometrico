package com.example.devices.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Publish
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devices.ui.components.AvisoError
import com.example.devices.ui.components.BotonPrincipal
import com.example.devices.ui.components.CampoTexto
import com.example.devices.viewmodel.NuevaVotacionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NuevaVotacionScreen(
    onCreada: () -> Unit,
    onVolver: () -> Unit,
    viewModel: NuevaVotacionViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.creada) {
        if (state.creada) onCreada()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva votación") },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !state.guardando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Seccion("¿Qué se decide?")
                CampoTexto(
                    valor = state.titulo,
                    onCambio = viewModel::onTitulo,
                    etiqueta = "Título",
                    error = state.errorTitulo,
                    mayusculas = true,
                    ayuda = "Ej.: Cambio de empresa de vigilancia"
                )
                CampoTexto(
                    valor = state.descripcion,
                    onCambio = viewModel::onDescripcion,
                    etiqueta = "Descripción",
                    error = state.errorDescripcion,
                    unaLinea = false,
                    mayusculas = true,
                    ayuda = "Explica a los vecinos qué se vota y por qué"
                )

                Spacer(Modifier.height(8.dp))
                Seccion("Opciones")
                state.opciones.forEachIndexed { i, opcion ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CampoTexto(
                            valor = opcion,
                            onCambio = { viewModel.onOpcion(i, it) },
                            etiqueta = "Opción ${i + 1}",
                            error = null,
                            mayusculas = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.opciones.size > 2) {
                            IconButton(onClick = { viewModel.quitarOpcion(i) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Quitar opción ${i + 1}")
                            }
                        }
                    }
                }
                state.errorOpciones?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                    )
                }
                if (state.opciones.size < NuevaVotacionViewModel.MAX_OPCIONES) {
                    TextButton(onClick = viewModel::agregarOpcion) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Agregar opción")
                    }
                }

                Spacer(Modifier.height(8.dp))
                Seccion("Duración y apertura")
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Duración", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Días que estará abierta",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalIconButton(
                                onClick = { viewModel.onDuracion(state.duracionDias - 1) },
                                enabled = state.duracionDias > 1
                            ) { Icon(Icons.Filled.Remove, contentDescription = "Menos días") }
                            Text(
                                if (state.duracionDias == 1) "1 día" else "${state.duracionDias} días",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.widthIn(min = 64.dp).padding(horizontal = 8.dp)
                            )
                            FilledTonalIconButton(
                                onClick = { viewModel.onDuracion(state.duracionDias + 1) },
                                enabled = state.duracionDias < 90
                            ) { Icon(Icons.Filled.Add, contentDescription = "Más días") }
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Abrir ahora", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    if (state.abrirAhora) "Los vecinos podrán votar en cuanto la publiques."
                                    else "Quedará programada; la abres después desde el panel.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Switch(checked = state.abrirAhora, onCheckedChange = viewModel::onAbrirAhora)
                        }
                    }
                }

                state.errorGeneral?.let {
                    Spacer(Modifier.height(12.dp))
                    AvisoError(it)
                }
                Spacer(Modifier.height(16.dp))
            }

            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                BotonPrincipal(
                    texto = if (state.abrirAhora) "Publicar y abrir" else "Guardar como programada",
                    onClick = viewModel::guardar,
                    cargando = state.guardando,
                    icono = Icons.Filled.Publish,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun Seccion(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
    )
}
