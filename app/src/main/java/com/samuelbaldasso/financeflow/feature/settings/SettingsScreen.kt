package com.samuelbaldasso.financeflow.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samuelbaldasso.financeflow.designsystem.theme.FinanceGreen
import com.samuelbaldasso.financeflow.designsystem.theme.FinanceRed
import com.samuelbaldasso.financeflow.designsystem.theme.FinanceSlate700
import com.samuelbaldasso.financeflow.designsystem.theme.FinanceSlate800
import com.samuelbaldasso.financeflow.designsystem.theme.FinanceSlate900

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(state.userFeedbackMessage) {
        state.userFeedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onEvent(SettingsUiEvent.ClearFeedbackMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Configurações & Segurança",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FinanceSlate900
                )
            )
        },
        containerColor = FinanceSlate900,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Security
            SettingsSectionCard(title = "Segurança do Aplicativo", icon = Icons.Default.Security) {
                // PIN setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Código PIN de Acesso",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = if (state.securitySettings.isPinSet) "Ativado (4 dígitos)" else "Não configurado",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.securitySettings.isPinSet) FinanceGreen else Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (state.securitySettings.isPinSet) {
                            OutlinedButton(
                                onClick = { onEvent(SettingsUiEvent.RemovePin) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = FinanceRed)
                            ) {
                                Text("Remover")
                            }
                        }
                        Button(
                            onClick = { onEvent(SettingsUiEvent.OpenSetPinDialog) },
                            colors = ButtonDefaults.buttonColors(containerColor = FinanceSlate700)
                        ) {
                            Text(if (state.securitySettings.isPinSet) "Alterar" else "Definir PIN")
                        }
                    }
                }

                HorizontalDivider(color = FinanceSlate700.copy(alpha = 0.5f))

                // Biometrics toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Desbloqueio por Biometria",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Usar impressão digital ou reconhecimento facial",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = state.securitySettings.isBiometricEnabled,
                        onCheckedChange = { onEvent(SettingsUiEvent.ToggleBiometric(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = FinanceGreen
                        )
                    )
                }

                HorizontalDivider(color = FinanceSlate700.copy(alpha = 0.5f))

                // Lock Timeout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "Tempo para Bloqueio Automático",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Bloquear o app após período em segundo plano",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val timeouts = listOf(
                        0 to "Imediatamente",
                        1 to "1 minuto",
                        5 to "5 minutos",
                        15 to "15 minutos",
                        -1 to "Desativado"
                    )

                    timeouts.forEach { (minutes, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEvent(SettingsUiEvent.ChangeLockTimeout(minutes)) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.securitySettings.lockTimeoutMinutes == minutes,
                                onClick = { onEvent(SettingsUiEvent.ChangeLockTimeout(minutes)) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = FinanceGreen,
                                    unselectedColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                        }
                    }
                }

                HorizontalDivider(color = FinanceSlate700.copy(alpha = 0.5f))

                // Screenshot protection (FLAG_SECURE)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Proteção de Tela (FLAG_SECURE)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Bloqueia capturas de tela e oculta dados financeiros no alternador de tarefas",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = state.securitySettings.isScreenshotProtectionEnabled,
                        onCheckedChange = { onEvent(SettingsUiEvent.ToggleScreenshotProtection(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = FinanceGreen
                        )
                    )
                }
            }

            // Section 2: Privacy & LGPD
            SettingsSectionCard(title = "Privacidade & Conformidade (LGPD)", icon = Icons.Default.PrivacyTip) {
                Text(
                    text = "O FinanceFlow segue rigorosamente a Lei Geral de Proteção de Dados (LGPD). Todos os seus dados financeiros são processados e armazenados exclusivamente no dispositivo (offline-first). Não transmitimos dados de identificação pessoal (PII).",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Telemetry consent toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Consentimento para Telemetria",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Permitir envio anônimo de relatórios de falha sem dados financeiros",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = state.securitySettings.telemetryConsent,
                        onCheckedChange = { onEvent(SettingsUiEvent.ToggleTelemetryConsent(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = FinanceGreen
                        )
                    )
                }

                HorizontalDivider(color = FinanceSlate700.copy(alpha = 0.5f))

                // Portability (Export)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Portabilidade dos Dados",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "Exportar todos os registros em JSON estruturado (Art. 18 LGPD)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Button(
                        onClick = { onEvent(SettingsUiEvent.ExportAllData) },
                        colors = ButtonDefaults.buttonColors(containerColor = FinanceSlate700)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar")
                    }
                }

                HorizontalDivider(color = FinanceSlate700.copy(alpha = 0.5f))

                // Right to be forgotten (Wipe)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Direito ao Esquecimento",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = FinanceRed
                        )
                        Text(
                            text = "Excluir permanentemente todas as contas, transações e redefinir o app",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Button(
                        onClick = { onEvent(SettingsUiEvent.OpenWipeConfirmDialog) },
                        colors = ButtonDefaults.buttonColors(containerColor = FinanceRed)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Excluir Tudo")
                    }
                }
            }

            // Section 3: App Info
            SettingsSectionCard(title = "Sobre o FinanceFlow", icon = Icons.Default.Info) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Versão", color = Color.White.copy(alpha = 0.7f))
                    Text(text = "1.0.0-MVP (Build 1)", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Armazenamento", color = Color.White.copy(alpha = 0.7f))
                    Text(text = "SQLite Local (Room 2.7)", color = FinanceGreen, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Integridade", color = Color.White.copy(alpha = 0.7f))
                    Text(text = "Auditoria & SHA-256", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Dialogs
    if (state.showSetPinDialog) {
        SetPinModal(
            onDismiss = { onEvent(SettingsUiEvent.DismissSetPinDialog) },
            onConfirm = { pin -> onEvent(SettingsUiEvent.SetNewPin(pin)) }
        )
    }

    if (state.showWipeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.DismissWipeConfirmDialog) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = FinanceRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Excluir Todos os Dados?", color = Color.White)
                }
            },
            text = {
                Text(
                    text = "Esta ação é irreversível conforme o Artigo 18 da LGPD. Todas as contas, transações, orçamentos, metas e logs de auditoria serão apagados permanentemente deste dispositivo.",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEvent(SettingsUiEvent.ConfirmWipeAllData) },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceRed)
                ) {
                    Text("Confirmar Exclusão")
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.DismissWipeConfirmDialog) }) {
                    Text("Cancelar", color = Color.White)
                }
            },
            containerColor = FinanceSlate800
        )
    }

    state.exportedDataJson?.let { json ->
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.ClearExportedData) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = FinanceGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup LGPD Exportado", color = Color.White)
                }
            },
            text = {
                Column(modifier = Modifier.height(300.dp)) {
                    Text(
                        text = "Dados consolidados prontos para portabilidade:",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(FinanceSlate900)
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = json,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(json))
                        Toast.makeText(context, "JSON copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FinanceGreen)
                ) {
                    Text("Copiar JSON", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(SettingsUiEvent.ClearExportedData) }) {
                    Text("Fechar", color = Color.White)
                }
            },
            containerColor = FinanceSlate800
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, FinanceSlate700.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = FinanceSlate800)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = FinanceGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            content()
        }
    }
}

@Composable
private fun SetPinModal(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar PIN de Acesso", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Defina um código numérico de 4 dígitos para proteger o aplicativo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("PIN (4 dígitos)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            confirmPin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("Confirme o PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pin.length != 4) {
                        errorMessage = "O PIN deve conter exatamente 4 dígitos."
                    } else if (pin != confirmPin) {
                        errorMessage = "Os PINs digitados não conferem."
                    } else {
                        onConfirm(pin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = FinanceGreen)
            ) {
                Text("Salvar PIN", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        },
        containerColor = FinanceSlate800
    )
}
