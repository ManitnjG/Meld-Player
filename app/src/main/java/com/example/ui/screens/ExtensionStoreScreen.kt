package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioExtension
import com.example.model.ExtensionType
import com.example.ui.theme.*
import com.example.ui.viewmodel.MeldViewModel

@Composable
fun ExtensionStoreScreen(
    viewModel: MeldViewModel,
    modifier: Modifier = Modifier
) {
    val extensions by viewModel.allExtensions.collectAsState()
    var showInstallDialog by remember { mutableStateOf(false) }
    val latencyResults = remember { mutableStateMapOf<String, Int>() }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 120.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SpotiFLAC Extension Store",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Audio stream resolvers, download engines & metadata plugins",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { showInstallDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = FlacCyan, contentColor = DeepBlack),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("install_extension_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Ext", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Info Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(FlacCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Extension, contentDescription = null, tint = FlacCyan, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Decentralized Audio Sources", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "Extensions seamlessly stream FLAC audio from Spotify, Tidal, Deezer & YouTube engines without API restrictions.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Active Primary Audio Source Section
        item {
            Text(
                text = "Primary Audio Stream Resolver",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val audioSourceExtensions = extensions.filter { it.type == ExtensionType.AUDIO_SOURCE }
        items(audioSourceExtensions) { extension ->
            val isPrimary = extension.isPrimarySource
            val liveLatency = latencyResults[extension.id] ?: extension.latencyMs

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isPrimary) DarkSurfaceElevated else DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setPrimaryAudioSource(extension.id) }
                    .border(
                        width = if (isPrimary) 1.5.dp else 1.dp,
                        color = if (isPrimary) FlacCyan else StrokeColor,
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isPrimary,
                        onClick = { viewModel.setPrimaryAudioSource(extension.id) },
                        colors = RadioButtonDefaults.colors(selectedColor = FlacCyan)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = extension.name,
                                color = if (isPrimary) FlacCyan else TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = extension.version,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = extension.description,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkSurfaceVariant
                            ) {
                                Text(
                                    text = "Ping: ${liveLatency}ms",
                                    color = if (liveLatency < 50) LosslessGreen else HiResGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            extension.supportedFormats.forEach { fmt ->
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = DarkSurfaceVariant
                                ) {
                                    Text(
                                        text = fmt,
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // All Installed Plugins & Extensions
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "All Extensions & Plugins (${extensions.size})",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(extensions) { extension ->
            val liveLatency = latencyResults[extension.id] ?: extension.latencyMs

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CardBackground,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (extension.type) {
                                        ExtensionType.AUDIO_SOURCE -> Icons.Filled.Audiotrack
                                        ExtensionType.METADATA -> Icons.Filled.List
                                        ExtensionType.LYRICS -> Icons.Filled.Mic
                                        ExtensionType.DOWNLOAD_ENGINE -> Icons.Filled.CloudDownload
                                    },
                                    contentDescription = null,
                                    tint = FlacCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = extension.name,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "by ${extension.author} • ${extension.type.label}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Switch(
                            checked = extension.isEnabled,
                            onCheckedChange = { viewModel.toggleExtension(extension) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FlacCyan,
                                checkedTrackColor = DarkSurfaceElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = extension.description,
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkSurfaceElevated
                            ) {
                                Text(
                                    text = "${liveLatency}ms latency",
                                    color = if (liveLatency < 50) LosslessGreen else HiResGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.testExtensionLatency(extension) { newLatency ->
                                        latencyResults[extension.id] = newLatency
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                            ) {
                                Text("Test Ping", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInstallDialog) {
        InstallExtensionDialog(
            onDismiss = { showInstallDialog = false },
            onInstall = { url ->
                viewModel.installCustomExtension(url)
                showInstallDialog = false
            }
        )
    }
}

@Composable
private fun InstallExtensionDialog(
    onDismiss: () -> Unit,
    onInstall: (String) -> Unit
) {
    var urlOrJson by remember { mutableStateOf("") }
    val exampleRepos = listOf(
        "https://github.com/spotiflacapp/SpotiFLAC-Extension/tree/main/extensions",
        "https://raw.githubusercontent.com/spotiflac/registry/v2/flac_master.json",
        "https://registry.spotiflac.org/extensions/qobuz_studio.spotiflac-ext"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceElevated,
        title = {
            Text("Install SpotiFLAC Extension", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Enter a SpotiFLAC Extension repository URL or .spotiflac-ext registry link:",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = urlOrJson,
                    onValueChange = { urlOrJson = it },
                    placeholder = { Text("https://github.com/.../my_plugin.spotiflac-ext", color = TextTertiary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FlacCyan,
                        unfocusedBorderColor = StrokeColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Quick templates from registry:", color = TextTertiary, fontSize = 11.sp)
                exampleRepos.forEach { repo ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { urlOrJson = repo }
                    ) {
                        Text(
                            text = repo.substringAfterLast("/"),
                            color = FlacCyan,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (urlOrJson.isNotBlank()) onInstall(urlOrJson) },
                enabled = urlOrJson.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FlacCyan, contentColor = DeepBlack)
            ) {
                Text("Install Extension")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}
