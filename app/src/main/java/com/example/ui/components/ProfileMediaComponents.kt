package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

// Custom Theme Colors (Aligned with Dark Slate & Neon Cyan aesthetic)
private val AlmostBlack = Color(0xFF0A0E14)
private val DarkSlate = Color(0xFF151D2A)
private val NeonCyan = Color(0xFF00E5FF)
private val TextGray = Color(0xFF90A4AE)
private val TextWhite = Color(0xFFF5F7FA)
private val DarkBorder = Color(0xFF263238)

fun getYoutubeVideoId(url: String): String? {
    return try {
        val pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#&?\\s]*"
        val compiledPattern = java.util.regex.Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(url)
        if (matcher.find()) {
            matcher.group()
        } else {
            if (url.length == 11 && !url.contains("/") && !url.contains(".")) {
                url
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun GallerySection(
    galleryList: List<String>,
    isEditable: Boolean,
    isPremium: Boolean,
    onAddPhoto: (Bitmap) -> Unit,
    onDeletePhoto: (String) -> Unit,
    onReplacePhoto: (String, Bitmap) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val limit = if (isPremium) 20 else 5

    var showSourceSelector by remember { mutableStateOf(false) }
    var selectedPhotoToEdit by remember { mutableStateOf<String?>(null) }
    var photoToReplace by remember { mutableStateOf<String?>(null) }
    var zoomedPhotoUrl by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = loadUriToBitmap(context, it)
                if (bitmap != null) {
                    val currentToReplace = photoToReplace
                    if (currentToReplace != null) {
                        onReplacePhoto(currentToReplace, bitmap)
                    } else {
                        onAddPhoto(bitmap)
                    }
                }
                photoToReplace = null
            } catch (e: Exception) {
                android.util.Log.e("FutbolcuBul", "Failed to load image from gallery", e)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            val currentToReplace = photoToReplace
            if (currentToReplace != null) {
                onReplacePhoto(currentToReplace, it)
            } else {
                onAddPhoto(it)
            }
            photoToReplace = null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSlate),
        border = BorderStroke(0.5.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "FOTOĞRAF GALERİSİ",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "${galleryList.size}/$limit",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (isEditable) {
                // For owners: show exactly `limit` slots in 3-column grid
                val rows = (0 until limit).chunked(3)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (index in row) {
                                if (index < galleryList.size) {
                                    val photoUrl = galleryList[index]
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AlmostBlack)
                                            .clickable { zoomedPhotoUrl = photoUrl }
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = photoUrl),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    // Empty box
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AlmostBlack)
                                            .border(
                                                1.dp,
                                                NeonCyan.copy(alpha = 0.3f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                photoToReplace = null
                                                showSourceSelector = true
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Ekle",
                                            tint = NeonCyan.copy(alpha = 0.7f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                            // Fill remaining spaces in the last row to keep alignment consistent
                            val missingItems = 3 - row.size
                            if (missingItems > 0) {
                                repeat(missingItems) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            } else {
                // For visitors: only render actual photos, or show "empty" message
                if (galleryList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Henüz fotoğraf eklenmemiş.", color = TextGray, fontSize = 12.sp)
                    }
                } else {
                    val rows = galleryList.chunked(3)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (row in rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (photoUrl in row) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AlmostBlack)
                                            .clickable { zoomedPhotoUrl = photoUrl }
                                    ) {
                                         Image(
                                             painter = rememberAsyncImagePainter(model = photoUrl),
                                             contentDescription = null,
                                             contentScale = ContentScale.Crop,
                                             modifier = Modifier.fillMaxSize()
                                         )
                                    }
                                }
                                // Fill remaining spaces in the row to keep alignment consistent
                                val missingItems = 3 - row.size
                                if (missingItems > 0) {
                                    repeat(missingItems) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isPremium) "Premium üye olarak 20 adede kadar fotoğraf ekleyebilirsiniz." else "Maksimum 5 adet fotoğraf yükleyebilirsiniz.",
                color = TextGray,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }

    // Photo options dialog for editable photos
    selectedPhotoToEdit?.let { photoUrl ->
        AlertDialog(
            onDismissRequest = { selectedPhotoToEdit = null },
            title = { Text("FOTOĞRAF SEÇENEKLERİ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = { Text("Bu fotoğrafı değiştirmek mi yoksa silmek mi istiyorsunuz?", color = TextGray, fontSize = 13.sp) },
            containerColor = DarkSlate,
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            photoToReplace = photoUrl
                            selectedPhotoToEdit = null
                            showSourceSelector = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fotoğrafı Değiştir", color = AlmostBlack, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            onDeletePhoto(photoUrl)
                            selectedPhotoToEdit = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fotoğrafı Sil", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { selectedPhotoToEdit = null },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("İptal", color = TextGray)
                    }
                }
            }
        )
    }

    // Capture Source Dialog
    if (showSourceSelector) {
        AlertDialog(
            onDismissRequest = { showSourceSelector = false },
            title = { Text("Fotoğraf Yükle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = { Text("Lütfen fotoğraf kaynağı seçiniz.", color = TextGray, fontSize = 14.sp) },
            containerColor = DarkSlate,
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            showSourceSelector = false
                            cameraLauncher.launch(null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kamerayla Çek", color = AlmostBlack, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            showSourceSelector = false
                            galleryLauncher.launch("image/*")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        border = BorderStroke(1.dp, NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Galeriden Seç", color = NeonCyan, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { showSourceSelector = false },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("İptal", color = TextGray)
                    }
                }
            }
        )
    }

    // Full screen image viewer
    zoomedPhotoUrl?.let { photoUrl ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { zoomedPhotoUrl = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // High-res Image (clickable to zoom/dismiss)
                Image(
                    painter = rememberAsyncImagePainter(model = photoUrl),
                    contentDescription = "Büyütülmüş Fotoğraf",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { zoomedPhotoUrl = null }
                )

                // Header Overlay (Close and Edit buttons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Close button
                    IconButton(
                        onClick = { zoomedPhotoUrl = null },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = Color.White
                        )
                    }

                    if (isEditable) {
                        Button(
                            onClick = {
                                selectedPhotoToEdit = photoUrl
                                zoomedPhotoUrl = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "DÜZENLE / SİL",
                                color = AlmostBlack,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Bottom Tip
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Kapatmak için fotoğrafa dokunun",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun YoutubeVideosSection(
    videoList: List<String>,
    isEditable: Boolean,
    isPremium: Boolean,
    onAddVideo: (String) -> Unit,
    onDeleteVideo: (String) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val limit = if (isPremium) 50 else 10

    var showAddDialog by remember { mutableStateOf(false) }
    var videoUrlInput by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical =.8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSlate),
        border = BorderStroke(0.5.dp, DarkBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VİDEO OYNATMA LİSTESİ",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "${videoList.size}/$limit",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (videoList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz YouTube video linki eklenmemiş.",
                        color = TextGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (videoUrl in videoList) {
                        val videoId = getYoutubeVideoId(videoUrl)
                        val thumbnailUrl = if (videoId != null) {
                            "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                        } else {
                            null
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AlmostBlack)
                                .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    try {
                                        uriHandler.openUri(videoUrl)
                                    } catch (e: Exception) {}
                                }
                        ) {
                            if (thumbnailUrl != null) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = thumbnailUrl),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // Dark overlay + Play Arrow
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Oynat",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "YOUTUBE ÜZERİNDE OYNAT",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Delete overlay if editable
                            if (isEditable) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .clickable { onDeleteVideo(videoUrl) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Videoyu Sil",
                                        tint = Color.Red,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isEditable && videoList.size < limit) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        videoUrlInput = ""
                        inputError = ""
                        showAddDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, NeonCyan),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("YENİ YOUTUBE VİDEOSU EKLE", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (isPremium) "Premium üye olarak 50 adede kadar YouTube videosu ekleyebilirsiniz." else "YouTube video linklerini ekleyerek maç, antrenman veya aksiyon anlarınızı sergileyebilirsiniz (Maksimum 10 video).",
                color = TextGray,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )
        }
    }

    // Add Video Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("YouTube Videosu Ekle", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Lütfen eklemek istediğiniz YouTube video linkini yapıştırınız.", color = TextGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = videoUrlInput,
                        onValueChange = {
                            videoUrlInput = it
                            inputError = ""
                        },
                        label = { Text("YouTube URL (örn: https://youtu.be/...)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        isError = inputError.isNotEmpty()
                    )
                    if (inputError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(inputError, color = Color.Red, fontSize = 11.sp)
                    }
                }
            },
            containerColor = DarkSlate,
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("İptal", color = TextGray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val videoId = getYoutubeVideoId(videoUrlInput)
                        if (videoId == null) {
                            inputError = "Geçersiz YouTube linki! Lütfen geçerli bir link girin."
                        } else {
                            onAddVideo(videoUrlInput)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Ekle", color = AlmostBlack, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
