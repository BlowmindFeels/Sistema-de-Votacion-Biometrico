package com.example.devices.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

@Composable
private fun coloresCampo(): TextFieldColors =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )

@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    tipoTeclado: KeyboardType = KeyboardType.Text,
    icono: ImageVector? = null,
    modifier: Modifier = Modifier,
    unaLinea: Boolean = true,
    mayusculas: Boolean = false,
    ayuda: String? = null
) {
    val apoyo = error ?: ayuda
    val textoApoyo: (@Composable () -> Unit)? = apoyo?.let { t -> @Composable { Text(t) } }
    val iconoInicio: (@Composable () -> Unit)? =
        icono?.let { i -> @Composable { Icon(i, contentDescription = null) } }

    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = unaLinea,
        minLines = if (unaLinea) 1 else 3,
        isError = error != null,
        supportingText = textoApoyo,
        leadingIcon = iconoInicio,
        keyboardOptions = KeyboardOptions(
            keyboardType = tipoTeclado,
            capitalization = if (mayusculas) KeyboardCapitalization.Sentences else KeyboardCapitalization.None,
            imeAction = if (unaLinea) ImeAction.Next else ImeAction.Default
        ),
        colors = coloresCampo(),
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun CampoPassword(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    error: String?,
    ayuda: String? = null
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val apoyo = error ?: ayuda
    val textoApoyo: (@Composable () -> Unit)? = apoyo?.let { t -> @Composable { Text(t) } }

    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        isError = error != null,
        supportingText = textoApoyo,
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                )
            }
        },
        colors = coloresCampo(),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    )
}
