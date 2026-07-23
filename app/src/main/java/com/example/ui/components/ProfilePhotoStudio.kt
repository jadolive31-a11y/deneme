package com.example.ui.components

import coil.compose.rememberAsyncImagePainter

import android.content.Context
import android.graphics.*
import android.media.FaceDetector
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.media.ExifInterface
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.ClipOp
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class StudioStep {
    SELECT_METHOD,
    CAMERA,
    CROP,
    PRESETS
}

data class DetectedFaceInfo(
    val midPoint: PointF,
    val eyesDistance: Float,
    val imageWidth: Int,
    val imageHeight: Int
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ProfilePhotoStudio(
    onPhotoCapturedAndCropped: (String) -> Unit, // Returns the saved file path
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(StudioStep.SELECT_METHOD) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val original = loadUriToBitmap(context, uri)
            if (original != null) {
                selectedBitmap = original
                currentStep = StudioStep.CROP
            }
        }
    }

    // Camera Permission State
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .background(AlmostBlack),
            colors = CardDefaults.cardColors(containerColor = AlmostBlack),
            shape = RoundedCornerShape(0.dp) // Full screen coverage
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(AlmostBlack)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    if (currentStep == StudioStep.SELECT_METHOD) {
                        onDismiss()
                    } else {
                        currentStep = StudioStep.SELECT_METHOD
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Geri",
                        tint = NeonCyan
                    )
                }
                
                Text(
                    text = "PROFESYONEL VESİKALIK STÜDYOSU",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.2.sp
                )
                
                Spacer(modifier = Modifier.width(48.dp)) // Equalizer spacer
            }

            // Step Content
            Box(modifier = Modifier.weight(1f)) {
                when (currentStep) {
                    StudioStep.SELECT_METHOD -> {
                        SelectMethodScreen(
                            onCameraClick = {
                                if (cameraPermissionState.status.isGranted) {
                                    currentStep = StudioStep.CAMERA
                                } else {
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            },
                            onGalleryClick = {
                                galleryLauncher.launch("image/*")
                            }
                        )
                    }
                    StudioStep.CAMERA -> {
                        if (cameraPermissionState.status.isGranted) {
                            CameraCaptureScreen(
                                onPhotoCaptured = { bitmap ->
                                    selectedBitmap = bitmap
                                    currentStep = StudioStep.CROP
                                },
                                onCancel = {
                                    currentStep = StudioStep.SELECT_METHOD
                                }
                            )
                        } else {
                            // Request Permission UI
                            PermissionRequestScreen(
                                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                            )
                        }
                    }
                    StudioStep.CROP -> {
                        var isUploading by remember { mutableStateOf(false) }
                        
                        if (isUploading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AlmostBlack.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = NeonCyan,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "FOTOĞRAF BULUTA YÜKLENİYOR...",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Lütfen bekleyin, profil fotoğrafınız Firebase'e yükleniyor.",
                                        color = TextGray,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            CropperScreen(
                                bitmap = selectedBitmap,
                                onCropCompleted = { cropped ->
                                    isUploading = true
                                    uploadBitmapToFirebaseStorage(
                                        bitmap = cropped,
                                        onSuccess = { downloadUrl ->
                                            isUploading = false
                                            onPhotoCapturedAndCropped(downloadUrl)
                                        },
                                        onFailure = { e ->
                                            isUploading = false
                                            val savedPath = saveBitmapToProfileStorage(context, cropped)
                                            if (savedPath != null) {
                                                onPhotoCapturedAndCropped(savedPath)
                                            }
                                        }
                                    )
                                },
                                onCancel = {
                                    currentStep = StudioStep.SELECT_METHOD
                                }
                            )
                        }
                    }
                    StudioStep.PRESETS -> {
                        PresetsAvatarScreen(
                            onAvatarSelected = { path ->
                                onPhotoCapturedAndCropped(path)
                            },
                            onCancel = {
                                currentStep = StudioStep.SELECT_METHOD
                            }
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun SelectMethodScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Visual Anchor icon: Copyright-free sleek athletic silhouette with target reticle
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(DarkSlate)
                .border(2.dp, NeonCyan, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.05f),
                    radius = size.minDimension / 2f
                )
                // Draw dashed circular target reticle representing face alignment zone
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.3f),
                    radius = size.minDimension * 0.38f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )
                // Draw crosshair ticks
                val radius = size.minDimension * 0.38f
                val cx = size.width / 2f
                val cy = size.height / 2f
                val tickLength = 10.dp.toPx()
                drawLine(NeonCyan.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(cx, cy - radius), androidx.compose.ui.geometry.Offset(cx, cy - radius + tickLength), 1.5.dp.toPx())
                drawLine(NeonCyan.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(cx, cy + radius), androidx.compose.ui.geometry.Offset(cx, cy + radius - tickLength), 1.5.dp.toPx())
                drawLine(NeonCyan.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(cx - radius, cy), androidx.compose.ui.geometry.Offset(cx - radius + tickLength, cy), 1.5.dp.toPx())
                drawLine(NeonCyan.copy(alpha = 0.5f), androidx.compose.ui.geometry.Offset(cx + radius, cy), androidx.compose.ui.geometry.Offset(cx + radius - tickLength, cy), 1.5.dp.toPx())
            }
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = NeonCyan.copy(alpha = 0.8f),
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "TRANSFERMARKT STANDARDI",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Profesyonel Scout Profil Fotoğrafı",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Bütün futbolcuların profil fotoğrafları EA Sports FC ve Transfermarkt kulüp veritabanlarındaki gibi homojen ve profesyonel vesikalık standartlarında olmalıdır. Lütfen sadece yüz ve omuzları içeren sade bir görsel ekleyin.",
            color = TextGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Guidelines card with bullets
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF14161C), RoundedCornerShape(12.dp))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "FOTOĞRAF YÖNLENDİRMELERİ",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val guidelines = listOf(
                "Yüz tamamen görünmeli",
                "Omuzlar kadraja dahil olmalı",
                "Arka plan sade olmalı",
                "Transfermarkt tarzı vesikalık kullanılmalı"
            )
            guidelines.forEach { guideline ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = guideline,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Camera option button
        Button(
            onClick = onCameraClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .testTag("camera_option_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "KAMERA İLE FOTOĞRAF ÇEK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gallery option button
        Button(
            onClick = onGalleryClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .testTag("gallery_option_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "GALERİDEN FOTOĞRAF SEÇ",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun PresetsAvatarScreen(
    onAvatarSelected: (String) -> Unit,
    onCancel: () -> Unit
) {
    val presets = listOf(
        "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?auto=format&fit=crop&w=300&q=80",
        "https://images.unsplash.com/photo-1518063319789-7217e6706b04?auto=format&fit=crop&w=300&q=80",
        "https://images.unsplash.com/photo-1544698310-74ea9d1c8258?auto=format&fit=crop&w=300&q=80",
        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=300&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80",
        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=300&q=80",
        "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?auto=format&fit=crop&w=300&q=80"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PROFESYONEL SPORCU AVATARLARI",
            color = NeonCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Bir Görsel Seçin",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(presets.size) { index ->
                val url = presets[index]
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clickable { onAvatarSelected(url) }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = url),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onCancel,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("İPTAL ET", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Kamera İzni Gerekli",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Profil fotoğrafınızı ön kamera ile çekebilmek için kamera erişimine izin vermelisiniz.",
            color = TextGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
        ) {
            Text(text = "Kamera İzni Ver", color = AlmostBlack, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CameraCaptureScreen(
    onPhotoCaptured: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val executor = androidx.core.content.ContextCompat.getMainExecutor(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                    } catch (exc: Exception) {
                        Log.e("Camera", "Binding failed", exc)
                    }
                }, executor)
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Oval Face Guide Overlay & Instructions
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val centerX = w / 2f
            val centerY = h * 0.4f // Position center higher up to match head location
            val ovalWidth = w * 0.62f
            val ovalHeight = ovalWidth * 1.35f // Face aspect ratio is vertical oval

            // Draw darkened semi-transparent backdrop around the face oval
            val path = androidx.compose.ui.graphics.Path().apply {
                addOval(androidx.compose.ui.geometry.Rect(centerX - ovalWidth / 2f, centerY - ovalHeight / 2f, centerX + ovalWidth / 2f, centerY + ovalHeight / 2f))
            }
            
            // Draw transparent hole with dark outer overlay
            clipPath(path, clipOp = ClipOp.Difference) {
                drawRect(color = Color.Black.copy(alpha = 0.7f))
            }

            // Draw Neon Guide Line
            drawOval(
                color = NeonCyan,
                topLeft = androidx.compose.ui.geometry.Offset(centerX - ovalWidth / 2f, centerY - ovalHeight / 2f),
                size = androidx.compose.ui.geometry.Size(ovalWidth, ovalHeight),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(15f, 10f), 0f
                    )
                )
            )
        }

        // Guidelines Card & Trigger Panel at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, AlmostBlack.copy(alpha = 0.9f), AlmostBlack)
                    )
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Instructions Checklist
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSlate.copy(alpha = 0.9f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "⚡ VESİKALIK KURALLARI",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text("• Yüzünüz tamamen görünmelidir.", color = Color.White, fontSize = 11.sp)
                    Text("• Omuzlarınız kadraja dahil olmalıdır.", color = Color.White, fontSize = 11.sp)
                    Text("• Arka plan mümkün olduğunca sade olmalıdır.", color = Color.White, fontSize = 11.sp)
                    Text("• Gözlük, filtre ve efekt kullanılmamalıdır.", color = Color.White, fontSize = 11.sp)
                }
            }

            // Trigger & Cancel Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "İptal", tint = Color.White)
                }

                // Trigger Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(NeonCyan)
                        .clickable {
                            val tempFile = File(context.cacheDir, "temp_capture_${UUID.randomUUID()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                            imageCapture.takePicture(
                                outputOptions,
                                androidx.core.content.ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        val savedUri = Uri.fromFile(tempFile)
                                        val originalBitmap = loadUriToBitmap(context, savedUri)
                                        if (originalBitmap != null) {
                                            onPhotoCaptured(originalBitmap)
                                        }
                                    }
                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("Camera", "Capture failed", exception)
                                    }
                                }
                            )
                        }
                        .padding(4.dp)
                        .border(4.dp, AlmostBlack, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Çek",
                        tint = AlmostBlack,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Space Equalizer
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
fun CropperScreen(
    bitmap: Bitmap?,
    onCropCompleted: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    if (bitmap == null) return

    val density = LocalDensity.current
    val viewportSizeDp = 280.dp
    val viewportWidthPx = with(density) { viewportSizeDp.toPx() }

    var zoom by remember { mutableStateOf(1f) }
    var rotation by remember { mutableStateOf(0f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    var faceInfo by remember { mutableStateOf<DetectedFaceInfo?>(null) }
    var autoCenteredDone by remember { mutableStateOf(false) }
    var detectionRunning by remember { mutableStateOf(true) }

    // Run native FaceDetector in a separate thread
    LaunchedEffect(bitmap) {
        withContext(Dispatchers.Default) {
            try {
                // Must be RGB_565 for standard Android FaceDetector
                val rgb565 = bitmap.copy(Bitmap.Config.RGB_565, true)
                if (rgb565 != null) {
                    val detector = FaceDetector(rgb565.width, rgb565.height, 1)
                    val faces = arrayOfNulls<FaceDetector.Face>(1)
                    val count = detector.findFaces(rgb565, faces)
                    if (count > 0 && faces[0] != null) {
                        val face = faces[0]!!
                        val midPoint = PointF()
                        face.getMidPoint(midPoint)
                        faceInfo = DetectedFaceInfo(
                            midPoint = midPoint,
                            eyesDistance = face.eyesDistance(),
                            imageWidth = bitmap.width,
                            imageHeight = bitmap.height
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("FaceDetector", "Failed to detect face natively", e)
            } finally {
                detectionRunning = false
            }
        }
    }

    // Auto-align face if found
    LaunchedEffect(faceInfo) {
        val info = faceInfo
        if (info != null && !autoCenteredDone) {
            val baseScale = viewportWidthPx / kotlin.math.min(info.imageWidth.toFloat(), info.imageHeight.toFloat())
            
            // Formula to calculate ideal zoom: Face width (approx 2.2 * eyesDistance) covers 65% of viewport
            val idealZoom = (0.65f * viewportWidthPx) / (2.2f * info.eyesDistance * baseScale)
            zoom = idealZoom.coerceIn(1.0f, 3.5f)

            // Center midpoint coordinates
            val imgCenterX = info.imageWidth / 2f
            val imgCenterY = info.imageHeight / 2f
            
            offsetX = -(info.midPoint.x - imgCenterX) * baseScale * zoom
            offsetY = -(info.midPoint.y - imgCenterY) * baseScale * zoom
            
            autoCenteredDone = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "FOTOĞRAFI KIRP & ORTALA",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Sürükleyerek taşıyın, iki parmakla büyütün veya alttaki çubukları kullanın.",
            color = TextGray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Main 1:1 Crop Viewport Box
        Box(
            modifier = Modifier
                .size(viewportSizeDp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, NeonCyan, RoundedCornerShape(12.dp))
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoomAmount, rotationAmount ->
                        zoom = (zoom * zoomAmount).coerceIn(0.6f, 4f)
                        rotation += rotationAmount
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Live Preview of the Image being manipulated using 100% identical Matrix math
            Canvas(modifier = Modifier.fillMaxSize()) {
                val matrix = android.graphics.Matrix()
                
                // 1. Move image center to origin
                matrix.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f)
                
                // 2. Apply rotation
                matrix.postRotate(rotation)
                
                // 3. Scale by baseScale and zoom
                val baseScale = viewportWidthPx / kotlin.math.min(bitmap.width.toFloat(), bitmap.height.toFloat())
                val finalScale = baseScale * zoom
                matrix.postScale(finalScale, finalScale)
                
                // 4. Translate back to center of target preview canvas plus offset
                matrix.postTranslate(viewportWidthPx / 2f + offsetX, viewportWidthPx / 2f + offsetY)
                
                drawContext.canvas.nativeCanvas.drawBitmap(bitmap, matrix, android.graphics.Paint().apply {
                    isAntiAlias = true
                    isFilterBitmap = true
                    isDither = true
                })
            }

            // Crosshair overlay to help with alignment
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeColor = Color.White.copy(alpha = 0.2f)
                drawLine(
                    color = strokeColor,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = 1f
                )
                drawLine(
                    color = strokeColor,
                    start = androidx.compose.ui.geometry.Offset(size.width / 2f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height),
                    strokeWidth = 1f
                )
                // Draw oval representing correct head position in Transfermarkt standard
                drawOval(
                    color = NeonCyan.copy(alpha = 0.15f),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.12f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.70f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }
        }

        // Face Detection status bar
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .height(28.dp),
            contentAlignment = Alignment.Center
        ) {
            if (detectionRunning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = NeonCyan)
            } else if (faceInfo != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Yüz Algılandı & Otomatik Hizalandı!", color = EmeraldGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("⚠️ Yüz algılanamadı, lütfen manuel olarak ayarlayın.", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Adjustments Controls
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Zoom Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("YAKINLAŞTIR (ZOOM)", color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.2fx", zoom), color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = zoom,
                    onValueChange = { zoom = it },
                    valueRange = 0.6f..4f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }

            // Rotate Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("DÖNDÜR (ROTATION)", color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(String.format("%.1f°", rotation), color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = rotation,
                    onValueChange = { rotation = it },
                    valueRange = -180f..180f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }

            // Center face button (if face is available)
            if (faceInfo != null) {
                Button(
                    onClick = {
                        val info = faceInfo!!
                        val baseScale = viewportWidthPx / kotlin.math.min(info.imageWidth.toFloat(), info.imageHeight.toFloat())
                        val idealZoom = (0.65f * viewportWidthPx) / (2.2f * info.eyesDistance * baseScale)
                        zoom = idealZoom.coerceIn(1.0f, 3.5f)
                        offsetX = -(info.midPoint.x - info.imageWidth / 2f) * baseScale * zoom
                        offsetY = -(info.midPoint.y - info.imageHeight / 2f) * baseScale * zoom
                        rotation = 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSlate),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CenterFocusStrong, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("YÜZÜ TEKRAR OTOMATİK ORTALA", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSlate),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("İPTAL", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    // Crop standard is mathematically centered 1080x1080 output
                    val cropped = createCroppedBitmap(bitmap, zoom, rotation, offsetX, offsetY, viewportWidthPx)
                    onCropCompleted(cropped)
                },
                modifier = Modifier
                    .weight(1.5f)
                    .height(50.dp)
                    .testTag("apply_crop_button"),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("UYGULA & KAYDET", color = AlmostBlack, fontWeight = FontWeight.Black)
            }
        }
    }
}

// Fixed orientation reader & loaders
fun loadUriToBitmap(context: Context, uri: Uri): Bitmap? {
    return try {
        val contentResolver = context.contentResolver
        val maxDimension = 2048
        
        // Step 1: Decode image bounds only to find original dimensions (memory-safe)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
        
        // Calculate the power-of-two inSampleSize to keep memory usage safe
        var inSampleSize = 1
        if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
            val halfHeight = options.outHeight / 2
            val halfWidth = options.outWidth / 2
            while (halfHeight / inSampleSize >= maxDimension && halfWidth / inSampleSize >= maxDimension) {
                inSampleSize *= 2
            }
        }
        
        // Step 2: Decode the actual bitmap with inSampleSize
        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        
        var decodedBitmap = contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, decodeOptions)
        } ?: return null
        
        // Make sure it is mutable
        if (!decodedBitmap.isMutable) {
            val mutableCopy = decodedBitmap.copy(Bitmap.Config.ARGB_8888, true)
            if (mutableCopy != null) {
                decodedBitmap.recycle()
                decodedBitmap = mutableCopy
            }
        }
        
        // Step 3: Read EXIF orientation
        var orientation = ExifInterface.ORIENTATION_NORMAL
        try {
            contentResolver.openInputStream(uri)?.use { input ->
                val exifInterface = ExifInterface(input)
                orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        } catch (e: Exception) {
            Log.e("EXIF", "Failed to read EXIF", e)
        }
        
        // Step 4: Correct orientation using Matrix manually (Bypasses any API auto-rotation bugs)
        val matrix = Matrix()
        var needsRotation = false
        
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                matrix.postRotate(90f)
                needsRotation = true
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                matrix.postRotate(180f)
                needsRotation = true
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                matrix.postRotate(270f)
                needsRotation = true
            }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                matrix.postScale(-1f, 1f)
                needsRotation = true
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.postScale(1f, -1f)
                needsRotation = true
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
                needsRotation = true
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
                needsRotation = true
            }
        }
        
        if (needsRotation) {
            val rotated = Bitmap.createBitmap(
                decodedBitmap,
                0,
                0,
                decodedBitmap.width,
                decodedBitmap.height,
                matrix,
                true
            )
            if (rotated != decodedBitmap) {
                decodedBitmap.recycle()
            }
            rotated
        } else {
            decodedBitmap
        }
    } catch (e: Exception) {
        Log.e("BitmapLoad", "Failed to load and correct uri: $uri", e)
        null
    }
}

fun rotateImageIfRequired(context: Context, bitmap: Bitmap, uri: Uri): Bitmap {
    return bitmap // Already handled robustly in loadUriToBitmap, return as-is
}

// 1080x1080 high quality crop painter
fun createCroppedBitmap(
    original: Bitmap,
    zoom: Float,
    rotation: Float,
    offsetX: Float,
    offsetY: Float,
    viewportWidthPx: Float
): Bitmap {
    val targetSize = 1080
    val cropped = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(cropped)
    
    val matrix = Matrix()
    
    // 1. Move image center to origin
    matrix.postTranslate(-original.width / 2f, -original.height / 2f)
    
    // 2. Apply rotation
    matrix.postRotate(rotation)
    
    // 3. Scale by combined scale factor
    val baseScale = viewportWidthPx / kotlin.math.min(original.width.toFloat(), original.height.toFloat())
    val finalScale = baseScale * zoom * (targetSize.toFloat() / viewportWidthPx)
    matrix.postScale(finalScale, finalScale)
    
    // 4. Translate back to center of target 1080 canvas plus scaled offset
    val scaledOffsetX = offsetX * (targetSize.toFloat() / viewportWidthPx)
    val scaledOffsetY = offsetY * (targetSize.toFloat() / viewportWidthPx)
    matrix.postTranslate(targetSize / 2f + scaledOffsetX, targetSize / 2f + scaledOffsetY)
    
    val paint = Paint().apply {
        isAntiAlias = true
        isFilterBitmap = true
        isDither = true
    }
    canvas.drawBitmap(original, matrix, paint)
    
    return cropped
}

// Storage helper
fun saveBitmapToProfileStorage(context: Context, bitmap: Bitmap): String? {
    return try {
        val dir = File(context.filesDir, "profile_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "tm_profile_${UUID.randomUUID().toString().take(8)}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        file.absolutePath
    } catch (e: Exception) {
        Log.e("SaveCrop", "Failed to save cropped bitmap", e)
        null
    }
}

fun uploadBitmapToFirebaseStorage(
    bitmap: Bitmap,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    try {
        val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
        val storageRef = storage.reference
        val photoRef = storageRef.child("profile_photos/${UUID.randomUUID().toString()}.jpg")
        
        val baos = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val data = baos.toByteArray()
        
        photoRef.putBytes(data)
            .addOnSuccessListener { taskSnapshot ->
                photoRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        val downloadUrl = uri.toString()
                        android.util.Log.d("FutbolcuBul", "STORAGE SUCCESS")
                        onSuccess(downloadUrl)
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e("FutbolcuBul", "Failed to get download URL", e)
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FutbolcuBul", "Failed to upload bytes to Firebase Storage", e)
                onFailure(e)
            }
    } catch (e: Exception) {
        android.util.Log.e("FutbolcuBul", "Firebase Storage exception", e)
        onFailure(e)
    }
}

fun uploadBitmapToStorage(
    bitmap: Bitmap,
    storagePath: String,
    onSuccess: (String) -> Unit,
    onFailure: (Exception) -> Unit
) {
    try {
        val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
        val photoRef = storage.reference.child(storagePath)
        
        var quality = 90
        var data: ByteArray
        var sizeInKb: Double
        do {
            val baos = java.io.ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            data = baos.toByteArray()
            sizeInKb = data.size / 1024.0
            quality -= 10
        } while (sizeInKb > 500.0 && quality > 10)
        
        photoRef.putBytes(data)
            .addOnSuccessListener { taskSnapshot ->
                photoRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        onSuccess(uri.toString())
                    }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    } catch (e: Exception) {
        onFailure(e)
    }
}
