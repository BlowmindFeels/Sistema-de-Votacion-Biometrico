package com.example.devices.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.devices.ui.components.AvisoError
import com.example.devices.ui.components.BotonPrincipal
import com.example.devices.ui.components.CampoPassword
import com.example.devices.ui.components.CampoTexto
import com.example.devices.ui.components.IconoCirculo
import com.example.devices.viewmodel.LoginViewModel

@Composable
fun LoginScreen(
    onLoginExitoso: () -> Unit,
    onIrARegistro: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.loginExitoso) {
        if (state.loginExitoso) onLoginExitoso()
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconoCirculo(Icons.Filled.HowToVote, tamano = 88)
            Spacer(Modifier.height(20.dp))
            Text(
                "Votación Comunitaria",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                "Chapinero · Bogotá",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Participa en las decisiones de tu comunidad desde tu teléfono.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("Ingresar", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    CampoTexto(
                        valor = state.documento,
                        onCambio = viewModel::onDocumentoChange,
                        etiqueta = "Número de documento",
                        error = state.errorDocumento,
                        tipoTeclado = KeyboardType.Number,
                        icono = Icons.Filled.Badge
                    )
                    Spacer(Modifier.height(4.dp))
                    CampoPassword(
                        valor = state.password,
                        onCambio = viewModel::onPasswordChange,
                        etiqueta = "Contraseña",
                        error = state.errorPassword
                    )

                    state.errorGeneral?.let {
                        Spacer(Modifier.height(8.dp))
                        AvisoError(it)
                    }

                    Spacer(Modifier.height(16.dp))
                    BotonPrincipal(
                        texto = "Ingresar",
                        onClick = viewModel::login,
                        cargando = state.cargando
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onIrARegistro, enabled = !state.cargando) {
                Text("¿No tienes cuenta? Regístrate")
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Tu huella se verifica en el teléfono y nunca se envía.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
