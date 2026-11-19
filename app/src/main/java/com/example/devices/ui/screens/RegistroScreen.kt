package com.example.devices.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devices.ui.components.AvisoError
import com.example.devices.ui.components.BotonPrincipal
import com.example.devices.ui.components.CampoPassword
import com.example.devices.ui.components.CampoTexto
import com.example.devices.viewmodel.RegistroViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroScreen(
    onRegistroExitoso: () -> Unit,
    onVolver: () -> Unit,
    viewModel: RegistroViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.registroExitoso) {
        if (state.registroExitoso) onRegistroExitoso()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear cuenta") },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !state.cargando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                "Regístrate con tus datos para participar en las votaciones de tu comunidad.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(20.dp))

            CampoTexto(
                state.nombre, viewModel::onNombreChange, "Nombre completo", state.errorNombre,
                icono = Icons.Filled.Person, mayusculas = true
            )
            CampoTexto(
                state.documento, viewModel::onDocumentoChange, "Número de documento",
                state.errorDocumento, KeyboardType.Number, icono = Icons.Filled.Badge,
                ayuda = "Entre 6 y 10 dígitos, sin puntos"
            )
            CampoTexto(
                state.correo, viewModel::onCorreoChange, "Correo electrónico",
                state.errorCorreo, KeyboardType.Email, icono = Icons.Filled.Email
            )
            CampoPassword(
                state.password, viewModel::onPasswordChange, "Contraseña", state.errorPassword,
                ayuda = "Mínimo 8 caracteres"
            )

            state.errorGeneral?.let {
                Spacer(Modifier.height(8.dp))
                AvisoError(it)
            }

            Spacer(Modifier.height(16.dp))
            BotonPrincipal(
                texto = "Crear mi cuenta",
                onClick = viewModel::registrar,
                cargando = state.cargando
            )

            Spacer(Modifier.height(20.dp))
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Tu contraseña se guarda cifrada y tu huella nunca sale del teléfono. " +
                            "Nadie puede ver por qué opción votaste.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            TextButton(
                onClick = onVolver,
                enabled = !state.cargando,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Ya tengo cuenta")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
