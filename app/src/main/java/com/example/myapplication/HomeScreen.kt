package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MediReadViewModel,
    onNavigateToMedicineReader: () -> Unit,
    onNavigateToPrescriptionReader: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val apiKey by viewModel.apiKey.collectAsState()
    val modelName by viewModel.modelName.collectAsState()
    var showKeyDialog by remember { mutableStateOf(false) }

    if (showKeyDialog) {
        ApiKeySettingsDialog(
            currentKey = apiKey,
            currentModel = modelName,
            onDismiss = { showKeyDialog = false },
            onSave = { newKey, newModel ->
                viewModel.updateApiKey(newKey)
                viewModel.updateModelName(newModel)
                showKeyDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Teal40),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "MediRead AI",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TealDark
                            )
                        )
                    }
                },
                actions = {
                    // API Key Settings action
                    IconButton(onClick = { showKeyDialog = true }) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "API Key Settings",
                            tint = if (apiKey.isNotBlank()) Teal40 else WarningOrange
                        )
                    }
                    IconButton(onClick = onNavigateToAbout) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "About",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val isWideScreen = maxWidth > 600.dp
            val horizontalPadding = if (isWideScreen) 32.dp else 20.dp

            Column(
                modifier = Modifier
                    .widthIn(max = 700.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Teal40.copy(alpha = 0.12f),
                                        Color.White
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Subtitle Tagline
                            Text(
                                text = "Intelligent Medicine & Prescription Reader",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = TealDark,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Snap a photo of any medicine strip or prescription for instant, plain-language guidance.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Language Selector Chips
                            Text(
                                text = "Output Language / భాష / भाषा:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = TextPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                AppLanguage.entries.forEach { lang ->
                                    val isSelected = selectedLanguage == lang
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.setLanguage(lang) },
                                        label = {
                                            Text(
                                                lang.displayName,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Teal40,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section Label
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Reader Mode",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Responsive layout for action cards (Side by side on wide screens, stacked on phones)
                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ModernActionCard(
                                title = "Medicine Strip",
                                subtitle = "Scan packaging, ingredients, dosage & expiry",
                                icon = Icons.Default.LocalPharmacy,
                                gradient = listOf(Teal40, TealDark),
                                badgeText = "Strips & Bottles",
                                onClick = onNavigateToMedicineReader
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ModernActionCard(
                                title = "Prescription Reader",
                                subtitle = "Decode doctor notes, dosage timing & instructions",
                                icon = Icons.Default.Description,
                                gradient = listOf(Blue40, Color(0xFF0D47A1)),
                                badgeText = "Handwritten & Print",
                                onClick = onNavigateToPrescriptionReader
                            )
                        }
                    }
                } else {
                    ModernActionCard(
                        title = "Medicine Strip Reader",
                        subtitle = "Scan blister packs, boxes & bottles for uses, ingredients and expiry",
                        icon = Icons.Default.LocalPharmacy,
                        gradient = listOf(Teal40, TealDark),
                        badgeText = "Packaging & Labels",
                        onClick = onNavigateToMedicineReader
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ModernActionCard(
                        title = "Doctor's Prescription Reader",
                        subtitle = "Understand doctor's handwriting, dosage frequency & schedules in simple words",
                        icon = Icons.Default.Description,
                        gradient = listOf(Blue40, Color(0xFF0D47A1)),
                        badgeText = "Prescription Slips",
                        onClick = onNavigateToPrescriptionReader
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Safety Notice Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DisclaimerBg),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = WarningOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Medical Safety Disclaimer",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = WarningOrange
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "MediRead AI provides educational assistance. It is not a licensed physician or pharmacist. Always verify medicines and dosages with your doctor or chemist.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ModernActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    badgeText: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Surface(
                    color = gradient[0].copy(alpha = 0.12f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = gradient[0],
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Start Scan",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = gradient[0]
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = gradient[0],
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun ApiKeySettingsDialog(
    currentKey: String,
    currentModel: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var keyText by remember { mutableStateOf(currentKey) }
    var modelText by remember { mutableStateOf(currentModel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VpnKey, contentDescription = null, tint = Teal40)
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI Studio API Configuration")
            }
        },
        text = {
            Column {
                Text(
                    "You can update your Google AI Studio API key and configured model directly here for live demoing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = modelText,
                    onValueChange = { modelText = it },
                    label = { Text("Model Name (e.g., gemini-1.5-flash)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(keyText, modelText) },
                colors = ButtonDefaults.buttonColors(containerColor = Teal40)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
