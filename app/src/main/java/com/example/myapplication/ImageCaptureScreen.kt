package com.example.myapplication

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.myapplication.ui.theme.*
import java.io.File

/**
 * Responsive Image Capture and Analysis Screen.
 * Adapts to phone (vertical stack) and tablet/landscape (side-by-side split view).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCaptureScreen(
    title: String,
    viewModel: MediReadViewModel,
    onBack: () -> Unit,
    onAnalyze: (Bitmap) -> Unit,
    uiState: UiState,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val isSpeaking by viewModel.isSpeaking.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    // Initialize TTS on launch
    LaunchedEffect(Unit) {
        viewModel.initTts(context)
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            capturedBitmap = loadBitmapFromUri(context, photoUri!!)
        }
    }

    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            capturedBitmap = loadBitmapFromUri(context, it)
        }
    }

    // Camera permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = createTempImageUri(context)
            photoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission is required to capture medicine labels", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            "Language: ${selectedLanguage.displayName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopSpeaking()
                        onReset()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (capturedBitmap != null && uiState !is UiState.Loading) {
                        IconButton(onClick = {
                            viewModel.stopSpeaking()
                            capturedBitmap = null
                            onReset()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Teal40,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideScreen = maxWidth > 720.dp

            if (isWideScreen && capturedBitmap != null) {
                // Tablet / Landscape Side-by-Side Responsive View
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Left Column: Image Preview & Actions
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ImagePreviewCard(
                            bitmap = capturedBitmap!!,
                            onRemove = {
                                viewModel.stopSpeaking()
                                capturedBitmap = null
                                onReset()
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ActionButtonsRow(
                            onRetake = {
                                viewModel.stopSpeaking()
                                capturedBitmap = null
                                onReset()
                            },
                            onAnalyze = { onAnalyze(capturedBitmap!!) },
                            isAnalyzing = uiState is UiState.Loading
                        )
                    }

                    // Right Column: State (Loading / Success / Error / Prompt)
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        RenderCurrentState(
                            uiState = uiState,
                            capturedBitmap = capturedBitmap,
                            isSpeaking = isSpeaking,
                            onSpeak = { text -> viewModel.speakText(text) },
                            onRetry = { capturedBitmap?.let { onAnalyze(it) } },
                            onScanAnother = {
                                viewModel.stopSpeaking()
                                capturedBitmap = null
                                onReset()
                            }
                        )
                    }
                }
            } else {
                // Mobile Portrait View (Clean vertical flow)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (capturedBitmap == null) {
                        // Image Picker & Camera Options
                        ImageSelectionPrompt(
                            onCaptureCamera = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            onPickGallery = {
                                galleryLauncher.launch("image/*")
                            }
                        )
                    } else {
                        // Image Captured Preview
                        ImagePreviewCard(
                            bitmap = capturedBitmap!!,
                            onRemove = {
                                viewModel.stopSpeaking()
                                capturedBitmap = null
                                onReset()
                            }
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (uiState is UiState.Initial) {
                            ActionButtonsRow(
                                onRetake = {
                                    capturedBitmap = null
                                    onReset()
                                },
                                onAnalyze = { onAnalyze(capturedBitmap!!) },
                                isAnalyzing = false
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Results / Progress / Error
                        RenderCurrentState(
                            uiState = uiState,
                            capturedBitmap = capturedBitmap,
                            isSpeaking = isSpeaking,
                            onSpeak = { text -> viewModel.speakText(text) },
                            onRetry = { capturedBitmap?.let { onAnalyze(it) } },
                            onScanAnother = {
                                viewModel.stopSpeaking()
                                capturedBitmap = null
                                onReset()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ImageSelectionPrompt(
    onCaptureCamera: () -> Unit,
    onPickGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Hero illustration box
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(Teal40.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.DocumentScanner,
                contentDescription = null,
                tint = Teal40,
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Capture or Upload Document",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Take a well-lit photo of the medicine strip, bottle label, or prescription slip.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Camera Action Button
        Button(
            onClick = onCaptureCamera,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(2.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal40)
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Open Camera", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Gallery Action Button
        OutlinedButton(
            onClick = onPickGallery,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true)
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(22.dp), tint = TealDark)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Choose from Gallery", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TealDark)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Best practices card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Tips for accurate AI reading:",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                InstructionBullet("Keep the medicine name and dosage text fully visible.")
                InstructionBullet("Avoid direct flash glare on shiny metallic foil blister strips.")
                InstructionBullet("Flatten folded prescriptions before taking the picture.")
            }
        }
    }
}

@Composable
fun InstructionBullet(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("• ", color = Teal40, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 18.sp)
    }
}

@Composable
fun ImagePreviewCard(
    bitmap: Bitmap,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Selected document image",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                contentScale = ContentScale.Fit
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}

@Composable
fun ActionButtonsRow(
    onRetake: () -> Unit,
    onAnalyze: () -> Unit,
    isAnalyzing: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onRetake,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            enabled = !isAnalyzing
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retake")
        }

        Button(
            onClick = onAnalyze,
            modifier = Modifier
                .weight(1.3f)
                .height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal40),
            enabled = !isAnalyzing
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Analyze with AI", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun RenderCurrentState(
    uiState: UiState,
    capturedBitmap: Bitmap?,
    isSpeaking: Boolean,
    onSpeak: (String) -> Unit,
    onRetry: () -> Unit,
    onScanAnother: () -> Unit
) {
    when (uiState) {
        is UiState.Loading -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(52.dp),
                        color = Teal40,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Analyzing Document with Gemini...",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Extracting medicine names, usage instructions, dosages & safety alerts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clip(RoundedCornerShape(8.dp)),
                        color = Teal40
                    )
                }
            }
        }

        is UiState.Success -> {
            DetailedResultsCard(
                resultText = uiState.outputText,
                isSpeaking = isSpeaking,
                onSpeak = { onSpeak(uiState.outputText) },
                onScanAnother = onScanAnother
            )
        }

        is UiState.Error -> {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F0))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Analysis Error",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = ErrorRed
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onScanAnother,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear")
                        }
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Teal40)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
        }

        is UiState.Initial -> {
            // Idle state, waiting for user to click "Analyze"
        }
    }
}

@Composable
fun DetailedResultsCard(
    resultText: String,
    isSpeaking: Boolean,
    onSpeak: () -> Unit,
    onScanAnother: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Top Header Row with Speech and Copy Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SuccessGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Analysis Results",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = TealDark
                        )
                    }

                    Row {
                        // Read Aloud / Speak Button
                        IconButton(
                            onClick = onSpeak,
                            modifier = Modifier
                                .background(
                                    if (isSpeaking) WarningOrange.copy(alpha = 0.15f) else Teal40.copy(alpha = 0.12f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isSpeaking) "Stop speech" else "Read Aloud",
                                tint = if (isSpeaking) WarningOrange else Teal40
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Copy Button
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("MediRead AI Analysis", resultText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Results copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.background(Color(0xFFEEEEEE), CircleShape)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy text", tint = TextSecondary)
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = Color(0xFFEEEEEE)
                )

                // Formatted Output Text
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 22.sp,
                        letterSpacing = 0.2.sp
                    ),
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Medical Verification Alert Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DisclaimerBg),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Always cross-check medicine names, doses, and directions with your doctor or pharmacist.",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Color(0xFF8D4B00)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onScanAnother,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Teal40)
        ) {
            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text("Scan Another Document", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Load a Bitmap from a content URI, scaled down if needed.
 */
fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream?.close()

        val maxDimension = 1200
        var sampleSize = 1
        if (options.outWidth > maxDimension || options.outHeight > maxDimension) {
            val widthRatio = options.outWidth.toFloat() / maxDimension
            val heightRatio = options.outHeight.toFloat() / maxDimension
            sampleSize = maxOf(widthRatio, heightRatio).toInt()
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val stream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(stream, null, decodeOptions)
        stream?.close()
        bitmap
    } catch (e: Exception) {
        null
    }
}

/**
 * Create a temporary file URI for camera capture.
 */
fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile("mediread_photo_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}
