package com.example.ui.screens

import android.net.Uri
import com.google.accompanist.permissions.isGranted
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import coil.compose.rememberAsyncImagePainter
import com.example.model.FootballPosition
import com.example.model.PreferredFoot
import com.example.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun ProfileWizardScreen(
    viewModel: MainViewModel,
    onComplete: () -> Unit
) {
    val currentRole by viewModel.currentUserRole.collectAsState()
    val scrollState = rememberScrollState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showCameraCapture by remember { mutableStateOf(false) }
    var bitmapToCrop by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var isUploadingCroppedImage by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val cameraPermissionState = com.google.accompanist.permissions.rememberPermissionState(android.Manifest.permission.CAMERA)

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val original = com.example.ui.components.loadUriToBitmap(context, uri)
            if (original != null) {
                bitmapToCrop = original
            }
        }
    }

    // ----------------------------------------------------
    // ROLE SPECIFIC LOCAL STATES
    // ----------------------------------------------------
    
    // Scout State
    var scoutFullName by remember { mutableStateOf("") }
    var scoutCity by remember { mutableStateOf("İstanbul") }
    var scoutClub by remember { mutableStateOf("") }
    var scoutDuty by remember { mutableStateOf("") }
    var scoutPhoto by remember { mutableStateOf("") }
    var scoutInstagram by remember { mutableStateOf("") }
    var scoutPhone by remember { mutableStateOf("") }

    // Coach State
    var coachFullName by remember { mutableStateOf("") }
    var coachLicense by remember { mutableStateOf("UEFA B Lisans") }
    var coachTeam by remember { mutableStateOf("") }
    var coachCity by remember { mutableStateOf("İstanbul") }
    var coachPhoto by remember { mutableStateOf("") }
    var coachInstagram by remember { mutableStateOf("") }
    var coachPhone by remember { mutableStateOf("") }

    // Club State
    var clubLogoUrl by remember { mutableStateOf("") }
    var clubCoverUrl by remember { mutableStateOf("") }
    var clubName by remember { mutableStateOf("") }
    var clubFoundYear by remember { mutableStateOf("") }
    var clubCity by remember { mutableStateOf("İstanbul") }
    var clubPhone by remember { mutableStateOf("") }
    var clubWhatsapp by remember { mutableStateOf("") }
    var clubInstagram by remember { mutableStateOf("") }
    var clubWebsite by remember { mutableStateOf("") }
    var clubMapLocation by remember { mutableStateOf("") }
    var clubIntro by remember { mutableStateOf("") }
    var clubDistrict by remember { mutableStateOf("") }
    var clubTrainingFacility by remember { mutableStateOf("") }
    var clubActiveStudentsCount by remember { mutableStateOf("") }
    var clubCoachesCount by remember { mutableStateOf("") }
    var clubTrophyCount by remember { mutableStateOf("") }
    var clubHasLicense by remember { mutableStateOf(false) }
    var clubHasSummerSchool by remember { mutableStateOf(false) }
    var clubHasWinterSchool by remember { mutableStateOf(false) }
    var selectedAgeGroups by remember { mutableStateOf(setOf<String>()) }

    // Media (Medya Ekibi) State
    var mediaPhoto by remember { mutableStateOf("") }
    var mediaFullName by remember { mutableStateOf("") }
    var mediaCity by remember { mutableStateOf("İstanbul") }
    var mediaPrice by remember { mutableStateOf("") }
    var mediaInstagram by remember { mutableStateOf("") }
    var mediaWhatsapp by remember { mutableStateOf("") }
    var mediaPortfolio by remember { mutableStateOf("") }
    var mediaServices by remember { mutableStateOf(setOf<String>()) }

    // Store (Spor Mağazası) State
    var storePhoto by remember { mutableStateOf("") }
    var storeName by remember { mutableStateOf("") }
    var storeCity by remember { mutableStateOf("İstanbul") }
    var storePhone by remember { mutableStateOf("") }
    var storeInstagram by remember { mutableStateOf("") }
    var storeAddress by remember { mutableStateOf("") }
    var storeWebsite by remember { mutableStateOf("") }

    // Pitch (Halı Saha) State
    var pitchPhoto by remember { mutableStateOf("") }
    var pitchName by remember { mutableStateOf("") }
    var pitchCity by remember { mutableStateOf("İstanbul") }
    var pitchPhone by remember { mutableStateOf("") }
    var pitchInstagram by remember { mutableStateOf("") }
    var pitchWorkingHours by remember { mutableStateOf("08:00 - 24:00") }
    var pitchPrice by remember { mutableStateOf("") }
    var pitchAddress by remember { mutableStateOf("") }

    // Organizer (Organizatör) State
    var organizerPhoto by remember { mutableStateOf("") }
    var organizerName by remember { mutableStateOf("") }
    var organizerCity by remember { mutableStateOf("İstanbul") }
    var organizerPhone by remember { mutableStateOf("") }
    var organizerInstagram by remember { mutableStateOf("") }
    var organizerType by remember { mutableStateOf("Turnuva ve Lig") }
    var organizerIntro by remember { mutableStateOf("") }

    // Pro Card Rating States
    var stat1 by remember { mutableStateOf(85) }
    var stat2 by remember { mutableStateOf(85) }
    var stat3 by remember { mutableStateOf(85) }
    var stat4 by remember { mutableStateOf(85) }
    var stat5 by remember { mutableStateOf(85) }
    var stat6 by remember { mutableStateOf(85) }

    // Error Message
    var formErrorMessage by remember { mutableStateOf("") }
    var selectedSecondaryList by remember { mutableStateOf<List<FootballPosition>>(emptyList()) }

    // Dynamic Multi-Step Setup
    var currentStepNonPlayer by remember { mutableStateOf(1) }
    val totalSteps = if (currentRole == UserRole.PLAYER) 7 else 2

    fun updatePhotoStateForRole(role: UserRole, path: String) {
        when (role) {
            UserRole.PLAYER -> viewModel.updateWizardPhoto(1f, 0f, 0f, 0f, path)
            UserRole.CLUB -> clubLogoUrl = path
            UserRole.SCOUT -> scoutPhoto = path
            UserRole.COACH -> coachPhoto = path
            UserRole.MEDIA -> mediaPhoto = path
            UserRole.STORE -> storePhoto = path
            UserRole.PITCH -> pitchPhoto = path
            UserRole.ORGANIZER -> organizerPhoto = path
            else -> scoutPhoto = path
        }
    }

    // Title text of TopBar based on Role
    val topBarTitle = when (currentRole) {
        UserRole.PLAYER -> "FUTBOLCU PROFİL SİHİRBAZI"
        UserRole.SCOUT -> "GÖZLEMCİ PROFİL SİHİRBAZI"
        UserRole.COACH -> "ANTRENÖR PROFİL SİHİRBAZI"
        UserRole.CLUB -> "KULÜP PROFİL SİHİRBAZI"
        UserRole.MEDIA -> "MEDYA EKİBİ PROFİL SİHİRBAZI"
        UserRole.STORE -> "SPOR MAĞAZASI PROFİL SİHİRBAZI"
        UserRole.PITCH -> "HALI SAHA PROFİL SİHİRBAZI"
        UserRole.ORGANIZER -> "ORGANİZATÖR PROFİL SİHİRBAZI"
        else -> "PROFİL SİHİRBAZI"
    }

@Composable
fun PhotoFramingReferenceGuide(
    isCorporate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val referenceBitmap = remember(isCorporate) {
        if (!isCorporate) {
            try {
                context.assets.open("transfermarkt.png").use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = DarkSlate,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 85.dp, height = 100.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AlmostBlack)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!isCorporate && referenceBitmap != null) {
                    Image(
                        bitmap = referenceBitmap,
                        contentDescription = "Doğru Kadraj Örneği",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (isCorporate) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "LOGO",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Text("📷", fontSize = 32.sp)
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isCorporate) "Logo Örneği" else "Doğru Kadraj",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = NeonCyan.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Rehber",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                if (isCorporate) {
                    Text("• Logo net olmalı", color = TextGray, fontSize = 11.sp)
                    Text("• Görsel merkezde olmalı", color = TextGray, fontSize = 11.sp)
                    Text("• Arka plan sade olmalı", color = TextGray, fontSize = 11.sp)
                } else {
                    Text("• Yüz tamamen görünmeli", color = TextGray, fontSize = 11.sp)
                    Text("• Kamera karşıdan olmalı", color = TextGray, fontSize = 11.sp)
                    Text("• Arka plan sade olmalı", color = TextGray, fontSize = 11.sp)
                }
            }
        }
    }
}
    val sampleAvatars = listOf(
        "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=300",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=300",
        "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=300",
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=300"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp
                    )
                },
                navigationIcon = {
                    val canGoBack = if (currentRole == UserRole.PLAYER) {
                        viewModel.wizardState.value.currentStep > 1
                    } else {
                        currentStepNonPlayer > 1
                    }
                    if (canGoBack) {
                        IconButton(onClick = {
                            if (currentRole == UserRole.PLAYER) {
                                viewModel.prevWizardStep()
                            } else {
                                currentStepNonPlayer--
                                formErrorMessage = ""
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = NeonCyan)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AlmostBlack)
            )
        },
        containerColor = AlmostBlack,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
        ) {
            // Horizontal Step Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val step = if (currentRole == UserRole.PLAYER) viewModel.wizardState.collectAsState().value.currentStep else currentStepNonPlayer
                repeat(totalSteps) { index ->
                    val isCompleted = index + 1 <= step
                    val stepColor = if (isCompleted) NeonCyan else DarkBorder
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(stepColor)
                    )
                }
            }

            val currentStep = if (currentRole == UserRole.PLAYER) viewModel.wizardState.collectAsState().value.currentStep else currentStepNonPlayer
            Text(
                text = "ADIM $currentStep / $totalSteps",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            // Screen content scroll body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentRole == UserRole.PLAYER) {
                    // ----------------------------------------------------
                    // ⚽ PLAYERS WIZARD (EXISTING SIX STEPS)
                    // ----------------------------------------------------
                    val playerState by viewModel.wizardState.collectAsState()
                    LaunchedEffect(playerState.currentStep) {
                        if (playerState.currentStep == 4) {
                            selectedSecondaryList = playerState.secondaryPositions
                        }
                    }
                    when (playerState.currentStep) {
                        1 -> {
                            val currentPhoto = playerState.photoUrl

                            Text(
                                text = "PROFİL FOTOĞRAFINI EKLE",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Text(
                                text = "Yüzünün net göründüğü sade bir fotoğraf seç.",
                                color = TextGray,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(top = 4.dp, bottom = 16.dp)
                            )

                            // Kompakt Örnek Rehber Kartı
                            PhotoFramingReferenceGuide(isCorporate = false)

                            if (currentPhoto.isEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                NeonButton(
                                    text = "FOTOĞRAF EKLE",
                                    onClick = { showBottomSheet = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("wizard_photo_trigger")
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 105.dp, height = 125.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(DarkSlate)
                                            .border(2.dp, NeonCyan, RoundedCornerShape(16.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = currentPhoto),
                                            contentDescription = "Önizleme",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            onClick = { showBottomSheet = true },
                                            shape = RoundedCornerShape(10.dp),
                                            color = DarkSlate,
                                            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Değiştir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Surface(
                                            onClick = { updatePhotoStateForRole(currentRole, "") },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0x33FF5252),
                                            border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Kaldır", color = Color(0xFFFF5252), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            Text(
                                text = "KİŞİSEL BİLGİLERİN",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Text(
                                text = "Temel bilgilerinizi kontrol edin ve doğum tarihinizi belirleyin.",
                                color = TextGray,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(top = 4.dp, bottom = 24.dp)
                            )

                            // Read-only premium passport styled display for Name
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSlate, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("AD SOYAD", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = TextGray, modifier = Modifier.size(14.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${playerState.firstName} ${playerState.lastName}".uppercase(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive Birth Date selector card using native DatePickerDialog
                            val context = androidx.compose.ui.platform.LocalContext.current
                            val calendar = java.util.Calendar.getInstance().apply {
                                set(2007, 5, 15) // Default starting birthdate
                                val parts = playerState.birthDate.split("-")
                                if (parts.size == 3) {
                                    val y = parts[0].toIntOrNull() ?: 2007
                                    val m = parts[1].toIntOrNull()?.minus(1) ?: 5
                                    val d = parts[2].toIntOrNull() ?: 15
                                    set(y, m, d)
                                }
                            }
                            val datePickerDialog = remember {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val formattedMonth = String.format("%02d", month + 1)
                                        val formattedDay = String.format("%02d", dayOfMonth)
                                        val newDateStr = "$year-$formattedMonth-$formattedDay"
                                        viewModel.updateWizardPersonal(playerState.firstName, playerState.lastName, newDateStr, playerState.nationality, playerState.city)
                                    },
                                    calendar.get(java.util.Calendar.YEAR),
                                    calendar.get(java.util.Calendar.MONTH),
                                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSlate, RoundedCornerShape(12.dp))
                                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable { datePickerDialog.show() }
                                    .padding(16.dp)
                            ) {
                                Text("DOĞUM TARİHİ", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatTurkishDate(playerState.birthDate.ifEmpty { "2007-06-15" }),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Seç", tint = NeonCyan, modifier = Modifier.size(18.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Locked country display with flag emoji
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSlate, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Text("UYRUK", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("TÜRKİYE 🇹🇷", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            SearchableCitySelector(
                                selectedCity = playerState.city.ifEmpty { "İstanbul" },
                                onCitySelected = { selectedCity ->
                                    viewModel.updateWizardPersonal(
                                        playerState.firstName,
                                        playerState.lastName,
                                        playerState.birthDate,
                                        playerState.nationality,
                                        selectedCity
                                    )
                                },
                                label = "Şehir",
                                modifier = Modifier.fillMaxWidth().testTag("wizard_city")
                            )
                        }
                        3 -> {
                            Text(
                                text = "FİZİKSEL ÖLÇÜMLER",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Text(
                                text = "Fiziksel özellikler sahada ikili mücadele gücünüzü doğrudan etkiler.",
                                color = TextGray,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(top = 4.dp, bottom = 24.dp)
                            )

                            // Boy (Height) Container with premium borders & clean alignment
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSlate, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("BOY", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("${playerState.height} CM", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = playerState.height.toFloat(),
                                    onValueChange = { viewModel.updateWizardPhysical(it.toInt(), playerState.weight, playerState.preferredFoot) },
                                    valueRange = 120f..220f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonCyan,
                                        activeTrackColor = NeonCyan,
                                        inactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Kilo (Weight) Container with premium borders & clean alignment
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(DarkSlate, RoundedCornerShape(12.dp))
                                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("KİLO", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("${playerState.weight} KG", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = playerState.weight.toFloat(),
                                    onValueChange = { viewModel.updateWizardPhysical(playerState.height, it.toInt(), playerState.preferredFoot) },
                                    valueRange = 30f..160f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeonCyan,
                                        activeTrackColor = NeonCyan,
                                        inactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Baskın Ayak (Preferred Foot) selection
                            Text(
                                text = "BASKIN AYAK",
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PreferredFoot.values().forEach { foot ->
                                    val isSelected = playerState.preferredFoot == foot
                                    val footLabel = when (foot) {
                                        PreferredFoot.LEFT -> "SOL"
                                        PreferredFoot.RIGHT -> "SAĞ"
                                        PreferredFoot.BOTH -> "ÇİFT AYAK"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) NeonCyan else DarkSlate)
                                            .border(1.dp, if (isSelected) Color.Transparent else Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                            .clickable { viewModel.updateWizardPhysical(playerState.height, playerState.weight, foot) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = footLabel,
                                            color = if (isSelected) AlmostBlack else TextWhite,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        4 -> {
                            Text(
                                text = "SAHADAKİ MEVKİLERİN",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Text(
                                text = "Sahada oynadığın mevkileri belirle. Scout aramalarında bu mevkilere göre listelenirsin.",
                                color = TextGray,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(top = 4.dp, bottom = 16.dp)
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "SAHADAKİ MEVKİLER (Dokunarak çoklu seçim yapın)",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                BoxWithConstraints(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(350.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSlate)
                                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                                        .padding(8.dp)
                                ) {
                                    val fieldWidth = maxWidth
                                    val fieldHeight = maxHeight

                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .drawBehind {
                                                val w = this.size.width
                                                val h = this.size.height

                                                // Deep green pitch atmosphere
                                                drawRect(
                                                    brush = Brush.verticalGradient(
                                                        listOf(
                                                            Color(0xFF0C1D14),
                                                            Color(0xFF060B08)
                                                        )
                                                    )
                                                )

                                                // Outer border line
                                                drawRect(
                                                    color = Color(0x20FFFFFF),
                                                    style = Stroke(width = 2f)
                                                )

                                                // Penalty Box Top
                                                drawRect(
                                                    color = Color(0x20FFFFFF),
                                                    topLeft = Offset(w * 0.22f, 0f),
                                                    size = Size(w * 0.56f, h * 0.18f),
                                                    style = Stroke(width = 1.5f)
                                                )
                                                drawRect(
                                                    color = Color(0x20FFFFFF),
                                                    topLeft = Offset(w * 0.35f, 0f),
                                                    size = Size(w * 0.3f, h * 0.06f),
                                                    style = Stroke(width = 1.5f)
                                                )

                                                // Center line
                                                drawLine(
                                                    color = Color(0x20FFFFFF),
                                                    start = Offset(0f, h / 2),
                                                    end = Offset(w, h / 2),
                                                    strokeWidth = 1.5f
                                                )

                                                // Center circle
                                                drawCircle(
                                                    color = Color(0x20FFFFFF),
                                                    radius = w * 0.15f,
                                                    center = Offset(w / 2, h / 2),
                                                    style = Stroke(width = 1.5f)
                                                )

                                                // Penalty Box Bottom
                                                drawRect(
                                                    color = Color(0x20FFFFFF),
                                                    topLeft = Offset(w * 0.22f, h * 0.82f),
                                                    size = Size(w * 0.56f, h * 0.18f),
                                                    style = Stroke(width = 1.5f)
                                                )
                                            }
                                    ) {
                                        val positions = listOf(
                                            Triple(FootballPosition.ST, 0.5f, 0.10f),
                                            Triple(FootballPosition.LW, 0.18f, 0.16f),
                                            Triple(FootballPosition.RW, 0.82f, 0.16f),
                                            Triple(FootballPosition.SS, 0.5f, 0.24f),
                                            Triple(FootballPosition.AM, 0.5f, 0.38f),
                                            Triple(FootballPosition.CM, 0.30f, 0.51f),
                                            Triple(FootballPosition.DM, 0.70f, 0.51f),
                                            Triple(FootballPosition.LB, 0.12f, 0.72f),
                                            Triple(FootballPosition.CB, 0.5f, 0.75f),
                                            Triple(FootballPosition.RB, 0.88f, 0.72f),
                                            Triple(FootballPosition.GK, 0.5f, 0.89f)
                                        )

                                        positions.forEach { (pos, fx, fy) ->
                                            val isMain = playerState.position == pos
                                            val isSecondary = selectedSecondaryList.contains(pos)
                                            val isSelected = isMain || isSecondary

                                            val nodeSize = 44.dp
                                            val xOffset = (fieldWidth * fx) - (nodeSize / 2)
                                            val yOffset = (fieldHeight * fy) - (nodeSize / 2)

                                            Box(
                                                modifier = Modifier
                                                    .offset(x = xOffset, y = yOffset)
                                                    .size(nodeSize)
                                                    .clip(CircleShape)
                                                    .background(if (isMain) NeonCyan else if (isSecondary) DarkNavy.copy(alpha = 0.9f) else Color(0xFF13151A))
                                                    .border(
                                                        width = if (isMain) 2.dp else if (isSecondary) 1.5.dp else 1.dp,
                                                        color = if (isMain) Color.White else if (isSecondary) NeonCyan.copy(alpha = 0.6f) else Color(0x33FFFFFF),
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        val newList = selectedSecondaryList.toMutableList()
                                                        if (newList.contains(pos)) {
                                                            newList.remove(pos)
                                                        } else {
                                                            newList.add(pos)
                                                        }
                                                        selectedSecondaryList = newList

                                                        val mainPos = if (playerState.position == pos && !newList.contains(pos)) {
                                                            newList.firstOrNull() ?: FootballPosition.ST
                                                        } else {
                                                            playerState.position
                                                        }
                                                        viewModel.updateWizardPositions(mainPos, newList)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = pos.shortName,
                                                    color = if (isMain) AlmostBlack else Color.White,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "ANA MEVKİNİZ (Seçtikleriniz arasından sadece BİR tane işaretleyin)",
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                val anaMevkiSecenekleri = if (selectedSecondaryList.isEmpty()) FootballPosition.values().toList() else selectedSecondaryList

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    anaMevkiSecenekleri.forEach { pos ->
                                        val isMain = playerState.position == pos
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(if (isMain) NeonCyan else DarkSlate)
                                                .border(1.dp, if (isMain) Color.Transparent else DarkBorder, RoundedCornerShape(20.dp))
                                                .clickable {
                                                    // Ensure the selected primary is also in the secondary list
                                                    if (!selectedSecondaryList.contains(pos)) {
                                                        val newList = selectedSecondaryList.toMutableList()
                                                        newList.add(pos)
                                                        selectedSecondaryList = newList
                                                        viewModel.updateWizardPositions(pos, newList)
                                                    } else {
                                                        viewModel.updateWizardPositions(pos, selectedSecondaryList)
                                                    }
                                                }
                                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = pos.name + if (isMain) " (ANA MEVKİ ★)" else "",
                                                color = if (isMain) AlmostBlack else TextWhite,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Box(modifier = Modifier.weight(1.5f)) {
                                    CustomTextField(
                                        value = playerState.club,
                                        onValueChange = { viewModel.updateWizardFootball(playerState.position, it, playerState.jerseyNumber) },
                                        label = "Aktif Kulüp (Örn. Serbest)",
                                        testTag = "wizard_club"
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    CustomTextField(
                                        value = if (playerState.jerseyNumber == 0) "" else playerState.jerseyNumber.toString(),
                                        onValueChange = { input ->
                                            val cleaned = input.filter { it.isDigit() }
                                            viewModel.updateWizardFootball(playerState.position, playerState.club, cleaned.toIntOrNull() ?: 0)
                                        },
                                        label = "Forma No",
                                        placeholder = "Örn: 10",
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        testTag = "wizard_jersey"
                                    )
                                }
                            }
                        }
                        5 -> {
                            Text(
                                text = "ÖZELLİKLERİNİ BELİRLE",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Text(
                                text = "EA FC Ultimate Team tarzı dijital kartın için özelliklerini değerlendir. Gerçekçi girmek scoutların ilgisini çeker.",
                                color = TextGray,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(top = 4.dp, bottom = 20.dp)
                            )

                            val statsList = listOf(
                                Triple("HIZ (PAC)", playerState.pace, { v: Int -> viewModel.updateWizardStats(v, playerState.shooting, playerState.passing, playerState.dribbling, playerState.defense, playerState.physical, playerState.weakFoot, playerState.skillMoves) }),
                                Triple("ŞUT (SHO)", playerState.shooting, { v: Int -> viewModel.updateWizardStats(playerState.pace, v, playerState.passing, playerState.dribbling, playerState.defense, playerState.physical, playerState.weakFoot, playerState.skillMoves) }),
                                Triple("PAS (PAS)", playerState.passing, { v: Int -> viewModel.updateWizardStats(playerState.pace, playerState.shooting, v, playerState.dribbling, playerState.defense, playerState.physical, playerState.weakFoot, playerState.skillMoves) }),
                                Triple("DRİPLİNG (DRI)", playerState.dribbling, { v: Int -> viewModel.updateWizardStats(playerState.pace, playerState.shooting, playerState.passing, v, playerState.defense, playerState.physical, playerState.weakFoot, playerState.skillMoves) }),
                                Triple("DEFANS (DEF)", playerState.defense, { v: Int -> viewModel.updateWizardStats(playerState.pace, playerState.shooting, playerState.passing, playerState.dribbling, v, playerState.physical, playerState.weakFoot, playerState.skillMoves) }),
                                Triple("FİZİKSEL (PHY)", playerState.physical, { v: Int -> viewModel.updateWizardStats(playerState.pace, playerState.shooting, playerState.passing, playerState.dribbling, playerState.defense, v, playerState.weakFoot, playerState.skillMoves) })
                            )

                            statsList.forEach { (label, value, onValChange) ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(DarkSlate, RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(label, color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(value.toString(), color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Slider(
                                        value = value.toFloat(),
                                        onValueChange = { onValChange(it.toInt()) },
                                        valueRange = 10f..99f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = NeonCyan,
                                            activeTrackColor = NeonCyan,
                                            inactiveTrackColor = Color.Gray.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkSlate, RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("ZAYIF AYAK", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            (1..5).forEach { star ->
                                                val active = star <= playerState.weakFoot
                                                Icon(
                                                    imageVector = if (active) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                    contentDescription = null,
                                                    tint = if (active) NeonCyan else TextGray.copy(alpha = 0.2f),
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clickable {
                                                            viewModel.updateWizardStats(
                                                                playerState.pace, playerState.shooting, playerState.passing,
                                                                playerState.dribbling, playerState.defense, playerState.physical,
                                                                star, playerState.skillMoves
                                                            )
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(DarkSlate, RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("BECERİ HAREKETİ", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            (1..5).forEach { star ->
                                                val active = star <= playerState.skillMoves
                                                Icon(
                                                    imageVector = if (active) Icons.Filled.Star else Icons.Filled.StarBorder,
                                                    contentDescription = null,
                                                    tint = if (active) NeonCyan else TextGray.copy(alpha = 0.2f),
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clickable {
                                                            viewModel.updateWizardStats(
                                                                playerState.pace, playerState.shooting, playerState.passing,
                                                                playerState.dribbling, playerState.defense, playerState.physical,
                                                                playerState.weakFoot, star
                                                            )
                                                        }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        6 -> {
                            Text(
                                text = "KENDİNİ TANIT",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Text(
                                text = "Gözlemcilerin ve kulüplerin seni daha iyi tanıması için kısa bir özet ekle ve sosyal medya hesaplarını paylaş.",
                                color = TextGray,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(top = 4.dp, bottom = 24.dp)
                            )

                            CustomTextField(
                                value = playerState.bio,
                                onValueChange = { viewModel.updateWizardBio(it, playerState.instagram, playerState.youtubeUrl) },
                                label = "Futbolcu Özeti / Biyografi",
                                modifier = Modifier.height(110.dp),
                                testTag = "wizard_bio"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            CustomTextField(
                                value = playerState.instagram,
                                onValueChange = { viewModel.updateWizardBio(playerState.bio, it, playerState.youtubeUrl) },
                                label = "Instagram Kullanıcı Adı (Örn: @futbolcu)",
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = NeonCyan) },
                                testTag = "wizard_instagram"
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            CustomTextField(
                                value = playerState.youtubeUrl,
                                onValueChange = { viewModel.updateWizardBio(playerState.bio, playerState.instagram, it) },
                                label = "YouTube Video veya Kanal Bağlantısı",
                                leadingIcon = { Icon(Icons.Default.PlayCircle, contentDescription = null, tint = NeonCyan) },
                                testTag = "wizard_youtube"
                            )
                        }
                        7 -> {
                            Text(
                                text = "FUTBOLCU KARTIN HAZIR!",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Text(
                                text = "Profil fotoğrafın ve özelliklerinle oluşturulan dijital lisans kartın. Sahalara adım atmaya hazırsın!",
                                color = TextGray,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(top = 4.dp, bottom = 24.dp)
                            )

                            // Dynamic calculations for FUTPlayerCard
                            val overall = ((playerState.pace + playerState.shooting + playerState.passing + playerState.dribbling + playerState.defense + playerState.physical) / 6).coerceIn(10, 99)

                            // FUTPlayerCard with user inputs with a premium glow background
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                FUTPlayerCard(
                                    name = "${playerState.firstName} ${playerState.lastName}",
                                    position = playerState.position.shortName,
                                    rating = overall,
                                    nationality = playerState.nationality.ifEmpty { "Türkiye" },
                                    club = playerState.club.ifEmpty { "Serbest" },
                                    stats = com.example.model.PhysicalStats(
                                        pace = playerState.pace,
                                        shooting = playerState.shooting,
                                        passing = playerState.passing,
                                        dribbling = playerState.dribbling,
                                        defense = playerState.defense,
                                        physical = playerState.physical
                                    ),
                                    glow = true,
                                    photoUrl = playerState.photoUrl
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Bu kart, scout listelerinde ve oyuncu aramalarında seni en prestijli şekilde temsil edecek.",
                                color = TextGray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    // ----------------------------------------------------
                    // 🕵️/👨‍🏫/🏟️/📹/📸 OTHER ROLES (2-STEP MODERN WIZARD)
                    // ----------------------------------------------------
                    if (currentStepNonPlayer == 1) {
                        val isCorporate = currentRole == UserRole.CLUB || currentRole == UserRole.STORE || currentRole == UserRole.PITCH
                        val currentPhoto = when (currentRole) {
                            UserRole.CLUB -> clubLogoUrl
                            UserRole.SCOUT -> scoutPhoto
                            UserRole.COACH -> coachPhoto
                            UserRole.MEDIA -> mediaPhoto
                            UserRole.STORE -> storePhoto
                            UserRole.PITCH -> pitchPhoto
                            UserRole.ORGANIZER -> organizerPhoto
                            else -> scoutPhoto
                        }

                        Text(
                            text = if (isCorporate) "KULÜP LOGOSUNU EKLE" else "PROFİL FOTOĞRAFINI EKLE",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Text(
                            text = if (isCorporate) "Kulübünü temsil eden net ve kaliteli logoyu yükle." else "Yüzünün net göründüğü sade bir fotoğraf seç.",
                            color = TextGray,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 4.dp, bottom = 16.dp)
                        )

                        // Kompakt Örnek Rehber Kartı
                        PhotoFramingReferenceGuide(isCorporate = isCorporate)

                        if (currentPhoto.isEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            NeonButton(
                                text = if (isCorporate) "LOGO EKLE" else "FOTOĞRAF EKLE",
                                onClick = { showBottomSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("wizard_photo_trigger")
                            )
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = if (isCorporate) 100.dp else 105.dp, height = if (isCorporate) 100.dp else 125.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(DarkSlate)
                                        .border(2.dp, NeonCyan, RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = currentPhoto),
                                        contentDescription = "Önizleme",
                                        contentScale = if (isCorporate) ContentScale.Fit else ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        onClick = { showBottomSheet = true },
                                        shape = RoundedCornerShape(10.dp),
                                        color = DarkSlate,
                                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Değiştir", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Surface(
                                        onClick = { updatePhotoStateForRole(currentRole, "") },
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0x33FF5252),
                                        border = BorderStroke(1.dp, Color(0xFFFF5252).copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Kaldır", color = Color(0xFFFF5252), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // STEP 2: DETAILS
                        Text(
                            text = "PROFESYONEL BİLGİLER",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Text(
                            text = "Dizinlerde listelenecek profesyonel bilgilerinizi ve iletişim kanallarınızı tanımlayın.",
                            color = TextGray,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(top = 4.dp, bottom = 24.dp)
                        )

                        when (currentRole) {
                            UserRole.SCOUT -> {
                                CustomTextField(value = scoutFullName, onValueChange = { scoutFullName = it }, label = "Ad Soyad", testTag = "scout_name_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                SearchableCitySelector(selectedCity = scoutCity, onCitySelected = { scoutCity = it }, label = "Şehir", modifier = Modifier.fillMaxWidth().testTag("scout_city_input"))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = scoutClub, onValueChange = { scoutClub = it }, label = "Çalıştığı Kulüp", testTag = "scout_club_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = scoutDuty, onValueChange = { scoutDuty = it }, label = "Görev (Örn. Baş Gözlemci)", testTag = "scout_duty_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = scoutInstagram, onValueChange = { scoutInstagram = it }, label = "Instagram (İsteğe Bağlı)", leadingIcon = { Icon(Icons.Default.Share, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                                                CustomTextField(value = scoutPhone, onValueChange = { scoutPhone = it }, label = "Telefon Numarası", leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                            }
                            UserRole.COACH -> {
                                CustomTextField(value = coachFullName, onValueChange = { coachFullName = it }, label = "Ad Soyad", testTag = "coach_name_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = coachLicense, onValueChange = { coachLicense = it }, label = "Lisans (Örn. UEFA Pro)", testTag = "coach_license_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = coachTeam, onValueChange = { coachTeam = it }, label = "Aktif Takım / Kulüp", testTag = "coach_team_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                SearchableCitySelector(selectedCity = coachCity, onCitySelected = { coachCity = it }, label = "Şehir", modifier = Modifier.fillMaxWidth().testTag("coach_city_input"))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = coachInstagram, onValueChange = { coachInstagram = it }, label = "Instagram", leadingIcon = { Icon(Icons.Default.Share, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = coachPhone, onValueChange = { coachPhone = it }, label = "Telefon Numarası", leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                            }
                            UserRole.CLUB -> {
                                CustomTextField(value = clubName, onValueChange = { clubName = it }, label = "Kulüp Adı", testTag = "club_name_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = clubFoundYear, onValueChange = { clubFoundYear = it }, label = "Kuruluş Yılı", placeholder = "Örn: 2012", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), testTag = "club_year_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                SearchableCitySelector(selectedCity = clubCity, onCitySelected = { clubCity = it }, label = "Şehir", modifier = Modifier.fillMaxWidth().testTag("club_city_input"))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = clubDistrict, onValueChange = { clubDistrict = it }, label = "İlçe (Örn: Talas)", testTag = "club_district_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = clubTrainingFacility, onValueChange = { clubTrainingFacility = it }, label = "Antrenman Sahası / Tesisleri", testTag = "club_facility_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = clubMapLocation, onValueChange = { clubMapLocation = it }, label = "Harita Konumu (Google Harita Linki)", leadingIcon = { Icon(Icons.Default.PinDrop, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = clubPhone, onValueChange = { clubPhone = it }, label = "Telefon Numarası", leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = clubWhatsapp, onValueChange = { clubWhatsapp = it }, label = "WhatsApp Numarası", leadingIcon = { Icon(Icons.Default.Chat, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = clubInstagram, onValueChange = { clubInstagram = it }, label = "Instagram Kullanıcı Adı (Örn: altinordu_fk)", leadingIcon = { Icon(Icons.Default.Share, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = clubWebsite, onValueChange = { clubWebsite = it }, label = "Website URL (Örn: altinordu.org.tr)", leadingIcon = { Icon(Icons.Default.Public, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = clubIntro, onValueChange = { clubIntro = it }, label = "Kulüp Kısa Tanıtım Yazısı", modifier = Modifier.height(100.dp))
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                Text("ALTYAPI & AKADEMİ BİLGİLERİ", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                                Spacer(modifier = Modifier.height(12.dp))

                                CustomTextField(value = clubActiveStudentsCount, onValueChange = { clubActiveStudentsCount = it }, label = "Aktif Öğrenci Sayısı", placeholder = "Örn: 185", leadingIcon = { Icon(Icons.Default.Group, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = clubCoachesCount, onValueChange = { clubCoachesCount = it }, label = "Antrenör Sayısı", placeholder = "Örn: 8", leadingIcon = { Icon(Icons.Default.SupervisedUserCircle, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Eğitim Verilen Yaş Grupları", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start).padding(vertical = 8.dp))
                                val allAgeGroups = listOf("U7", "U8", "U9", "U10", "U11", "U12", "U13", "U14", "U15", "U16", "U17", "U18", "U19")
                                val chunks = allAgeGroups.chunked(4)
                                chunks.forEach { chunk ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        chunk.forEach { age ->
                                            val isSelected = selectedAgeGroups.contains(age)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) NeonCyan else DarkSlate)
                                                    .border(1.dp, if (isSelected) NeonCyan else Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        selectedAgeGroups = if (isSelected) {
                                                            selectedAgeGroups - age
                                                        } else {
                                                            selectedAgeGroups + age
                                                        }
                                                    }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = age, color = if (isSelected) AlmostBlack else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        if (chunk.size < 4) {
                                            repeat(4 - chunk.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Lisanslı/Tescilli Kulüp mü?", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Switch(checked = clubHasLicense, onCheckedChange = { clubHasLicense = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = DarkBorder))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Yaz Spor Okulu Var mı?", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Switch(checked = clubHasSummerSchool, onCheckedChange = { clubHasSummerSchool = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = DarkBorder))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Kış Spor Okulu Var mı?", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Switch(checked = clubHasWinterSchool, onCheckedChange = { clubHasWinterSchool = it }, colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = DarkBorder))
                                }
                            }
                            UserRole.MEDIA -> {
                                CustomTextField(value = mediaFullName, onValueChange = { mediaFullName = it }, label = "Ad Soyad", testTag = "media_name_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                SearchableCitySelector(selectedCity = mediaCity, onCitySelected = { mediaCity = it }, label = "Hizmet Verdiği Şehir", modifier = Modifier.fillMaxWidth().testTag("media_city_input"))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = mediaPrice, onValueChange = { mediaPrice = it }, label = "Hizmet Ücreti / Aralığı (Örn. 2000 TL / Saat)", testTag = "media_price_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = mediaInstagram, onValueChange = { mediaInstagram = it }, label = "Instagram", leadingIcon = { Icon(Icons.Default.Share, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = mediaWhatsapp, onValueChange = { mediaWhatsapp = it }, label = "WhatsApp Numarası", leadingIcon = { Icon(Icons.Default.Phone, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = mediaPortfolio, onValueChange = { mediaPortfolio = it }, label = "Portfolyo / Örnek Çalışmalar Linki", leadingIcon = { Icon(Icons.Default.Link, null) })
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "SUNULAN HİZMET SEÇENEKLERİ",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                val servicesList = listOf(
                                    "Video Çekimi",
                                    "Fotoğraf Çekimi",
                                    "Drone Çekimi",
                                    "Tanıtım Filmi",
                                    "Maç Çekimi",
                                    "Röportaj"
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    servicesList.forEach { service ->
                                        val isSelected = mediaServices.contains(service)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) NeonCyan.copy(alpha = 0.12f) else DarkBorder)
                                                .border(1.dp, if (isSelected) NeonCyan else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    mediaServices = if (isSelected) {
                                                        mediaServices - service
                                                    } else {
                                                        mediaServices + service
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = {
                                                    mediaServices = if (isSelected) {
                                                        mediaServices - service
                                                    } else {
                                                        mediaServices + service
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(checkedColor = NeonCyan, uncheckedColor = TextGray)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = service, color = Color.White, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                            UserRole.STORE -> {
                                CustomTextField(value = storeName, onValueChange = { storeName = it }, label = "Mağaza Adı", testTag = "store_name_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                SearchableCitySelector(selectedCity = storeCity, onCitySelected = { storeCity = it }, label = "Şehir", modifier = Modifier.fillMaxWidth().testTag("store_city_input"))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = storePhone, onValueChange = { storePhone = it }, label = "Telefon Numarası", leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = storeInstagram, onValueChange = { storeInstagram = it }, label = "Instagram Kullanıcı Adı (İsteğe Bağlı)", leadingIcon = { Icon(Icons.Default.Share, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = storeWebsite, onValueChange = { storeWebsite = it }, label = "Website (İsteğe Bağlı)", leadingIcon = { Icon(Icons.Default.Public, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = storeAddress, onValueChange = { storeAddress = it }, label = "Mağaza Adresi", modifier = Modifier.height(100.dp))
                            }
                            UserRole.PITCH -> {
                                CustomTextField(value = pitchName, onValueChange = { pitchName = it }, label = "Halı Saha Tesis Adı", testTag = "pitch_name_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                SearchableCitySelector(selectedCity = pitchCity, onCitySelected = { pitchCity = it }, label = "Şehir", modifier = Modifier.fillMaxWidth().testTag("pitch_city_input"))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = pitchPhone, onValueChange = { pitchPhone = it }, label = "Rezervasyon / İletişim Telefonu", leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = pitchInstagram, onValueChange = { pitchInstagram = it }, label = "Instagram Kullanıcı Adı (İsteğe Bağlı)", leadingIcon = { Icon(Icons.Default.Share, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = pitchWorkingHours, onValueChange = { pitchWorkingHours = it }, label = "Çalışma Saatleri (Örn: 08:00 - 02:00)", leadingIcon = { Icon(Icons.Default.LockClock, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = pitchPrice, onValueChange = { pitchPrice = it }, label = "Tek Maç Ücreti (Örn: 1500 TL)", leadingIcon = { Icon(Icons.Default.AttachMoney, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = pitchAddress, onValueChange = { pitchAddress = it }, label = "Tesis Adresi", modifier = Modifier.height(100.dp))
                            }
                            UserRole.ORGANIZER -> {
                                CustomTextField(value = organizerName, onValueChange = { organizerName = it }, label = "Organizatör Adı / Şirketi", testTag = "organizer_name_input")
                                Spacer(modifier = Modifier.height(12.dp))
                                SearchableCitySelector(selectedCity = organizerCity, onCitySelected = { organizerCity = it }, label = "Şehir", modifier = Modifier.fillMaxWidth().testTag("organizer_city_input"))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = organizerPhone, onValueChange = { organizerPhone = it }, label = "İletişim Numarası", leadingIcon = { Icon(Icons.Default.Phone, null) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = organizerInstagram, onValueChange = { organizerInstagram = it }, label = "Instagram Kullanıcı Adı (İsteğe Bağlı)", leadingIcon = { Icon(Icons.Default.Share, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = organizerType, onValueChange = { organizerType = it }, label = "Organizasyon Türü (Örn: Turnuvalar, Lig, Halı Saha)", leadingIcon = { Icon(Icons.Default.EmojiEvents, null) })
                                Spacer(modifier = Modifier.height(12.dp))
                                CustomTextField(value = organizerIntro, onValueChange = { organizerIntro = it }, label = "Hakkınızda Kısa Açıklama / Vizyon", modifier = Modifier.height(100.dp))
                            }
                            else -> {}
                        }

                        if (formErrorMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(formErrorMessage, color = OrangeWarning, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Navigation Buttons (Back & Next/Finish)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary Action
                val canGoBack = if (currentRole == UserRole.PLAYER) {
                    viewModel.wizardState.value.currentStep > 1
                } else {
                    currentStepNonPlayer > 1
                }

                if (canGoBack) {
                    NeonButton(
                        text = "GERİ",
                        onClick = {
                            if (currentRole == UserRole.PLAYER) {
                                viewModel.prevWizardStep()
                            } else {
                                currentStepNonPlayer--
                                formErrorMessage = ""
                            }
                        },
                        isSecondary = true,
                        modifier = Modifier.width(110.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(110.dp))
                }

                // Primary Action
                val isLastStep = if (currentRole == UserRole.PLAYER) {
                    viewModel.wizardState.value.currentStep == 7
                } else {
                    currentStepNonPlayer == 2
                }

                NeonButton(
                    text = if (isLastStep) "PROFİLİ AKTİFLEŞTİR" else "İLERİ",
                    onClick = {
                        if (isLastStep) {
                            // Validate and finish
                            if (currentRole == UserRole.PLAYER) {
                                viewModel.finishWizard()
                                onComplete()
                            } else {
                                var isValid = true
                                when (currentRole) {
                                    UserRole.SCOUT -> {
                                        if (scoutFullName.isBlank() || scoutCity.isBlank() || scoutClub.isBlank() || scoutDuty.isBlank() || scoutPhone.isBlank()) isValid = false
                                    }
                                    UserRole.COACH -> {
                                        if (coachFullName.isBlank() || coachLicense.isBlank() || coachTeam.isBlank() || coachCity.isBlank() || coachPhone.isBlank()) isValid = false
                                    }
                                    UserRole.CLUB -> {
                                        if (clubName.isBlank() || clubFoundYear.isBlank() || clubCity.isBlank() || clubPhone.isBlank() || clubWhatsapp.isBlank()) isValid = false
                                    }
                                    UserRole.MEDIA -> {
                                        if (mediaFullName.isBlank() || mediaCity.isBlank() || mediaPrice.isBlank() || mediaWhatsapp.isBlank()) isValid = false
                                    }
                                    UserRole.STORE -> {
                                        if (storeName.isBlank() || storeCity.isBlank() || storePhone.isBlank() || storeAddress.isBlank()) isValid = false
                                    }
                                    UserRole.PITCH -> {
                                        if (pitchName.isBlank() || pitchCity.isBlank() || pitchPhone.isBlank() || pitchAddress.isBlank() || pitchPrice.isBlank()) isValid = false
                                    }
                                    UserRole.ORGANIZER -> {
                                        if (organizerName.isBlank() || organizerCity.isBlank() || organizerPhone.isBlank() || organizerIntro.isBlank()) isValid = false
                                    }
                                    else -> {}
                                }

                                if (!isValid) {
                                    formErrorMessage = "Lütfen zorunlu alanları doldurun."
                                } else {
                                    when (currentRole) {
                                        UserRole.SCOUT -> {
                                            viewModel.finishScoutWizard(scoutFullName, scoutCity, scoutClub, scoutDuty, scoutPhoto, scoutInstagram, scoutPhone, stat1, stat2, stat3, stat4, stat5, stat6)
                                        }
                                        UserRole.COACH -> {
                                            viewModel.finishCoachWizard(coachFullName, coachLicense, coachTeam, coachCity, coachPhoto, coachInstagram, coachPhone, stat1, stat2, stat3, stat4, stat5, stat6)
                                        }
                                        UserRole.CLUB -> {
                                            val activeStudents = clubActiveStudentsCount.toIntOrNull() ?: 0
                                            val coachesNum = clubCoachesCount.toIntOrNull() ?: 0
                                            val ageGroupsStr = selectedAgeGroups.sorted().joinToString(", ")
                                            viewModel.finishClubWizard(
                                                name = clubName,
                                                foundYear = clubFoundYear.toIntOrNull() ?: 2010,
                                                city = clubCity,
                                                district = clubDistrict,
                                                logoUrl = clubLogoUrl,
                                                phone = clubPhone,
                                                whatsapp = clubWhatsapp,
                                                instagram = clubInstagram,
                                                website = clubWebsite,
                                                mapLocation = clubMapLocation,
                                                bio = clubIntro,
                                                activeStudents = activeStudents,
                                                coachesCount = coachesNum,
                                                ageGroups = ageGroupsStr,
                                                hasLicense = clubHasLicense,
                                                hasSummerSchool = clubHasSummerSchool,
                                                hasWinterSchool = clubHasWinterSchool,
                                                trainingFacility = clubTrainingFacility
                                            )
                                        }
                                        UserRole.MEDIA -> {
                                            val servicesStr = mediaServices.sorted().joinToString(", ")
                                            viewModel.finishMediaWizard(mediaFullName, mediaCity, mediaPhoto, mediaPrice, mediaInstagram, mediaWhatsapp, mediaPortfolio, servicesStr, stat1, stat2, stat3, stat4, stat5, stat6)
                                        }
                                        UserRole.STORE -> {
                                            viewModel.finishStoreWizard(storeName, storeCity, storePhoto, storePhone, storeInstagram, storeAddress, storeWebsite)
                                        }
                                        UserRole.PITCH -> {
                                            viewModel.finishPitchWizard(pitchName, pitchCity, pitchPhoto, pitchPhone, pitchInstagram, pitchWorkingHours, pitchPrice, pitchAddress)
                                        }
                                        UserRole.ORGANIZER -> {
                                            viewModel.finishOrganizerWizard(organizerName, organizerCity, organizerPhoto, organizerPhone, organizerInstagram, organizerType, organizerIntro)
                                        }
                                        else -> {}
                                    }
                                    onComplete()
                                }
                            }
                        } else {
                            if (currentRole == UserRole.PLAYER) {
                                viewModel.nextWizardStep()
                            } else {
                                currentStepNonPlayer++
                                formErrorMessage = ""
                            }
                        }
                    },
                    modifier = Modifier.width(180.dp),
                    testTag = "wizard_next_button"
                )
            }
        }
    }

    if (showBottomSheet) {
        ProfileImagePickerBottomSheet(
            isCorporate = currentRole == UserRole.CLUB || currentRole == UserRole.STORE || currentRole == UserRole.PITCH,
            onCameraClick = {
                showBottomSheet = false
                if (cameraPermissionState.status.isGranted) {
                    showCameraCapture = true
                } else {
                    cameraPermissionState.launchPermissionRequest()
                }
            },
            onGalleryClick = {
                showBottomSheet = false
                galleryLauncher.launch("image/*")
            },
            onDismiss = {
                showBottomSheet = false
            }
        )
    }

    if (showCameraCapture) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showCameraCapture = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = AlmostBlack) {
                if (cameraPermissionState.status.isGranted) {
                    com.example.ui.components.CameraCaptureScreen(
                        onPhotoCaptured = { bitmap ->
                            showCameraCapture = false
                            bitmapToCrop = bitmap
                        },
                        onCancel = {
                            showCameraCapture = false
                        }
                    )
                } else {
                    com.example.ui.components.PermissionRequestScreen(
                        onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                    )
                }
            }
        }
    }

    if (bitmapToCrop != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { bitmapToCrop = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize(), color = AlmostBlack) {
                if (isUploadingCroppedImage) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = NeonCyan, strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("GÖRSEL KAYDEDİLİYOR...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                } else {
                    com.example.ui.components.CropperScreen(
                        bitmap = bitmapToCrop,
                        onCropCompleted = { cropped ->
                            isUploadingCroppedImage = true
                            com.example.ui.components.uploadBitmapToFirebaseStorage(
                                bitmap = cropped,
                                onSuccess = { downloadUrl ->
                                    isUploadingCroppedImage = false
                                    bitmapToCrop = null
                                    updatePhotoStateForRole(currentRole, downloadUrl)
                                },
                                onFailure = { e ->
                                    isUploadingCroppedImage = false
                                    val savedPath = com.example.ui.components.saveBitmapToProfileStorage(context, cropped)
                                    if (savedPath != null) {
                                        updatePhotoStateForRole(currentRole, savedPath)
                                    }
                                    bitmapToCrop = null
                                }
                            )
                        },
                        onCancel = {
                            bitmapToCrop = null
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileImagePickerBottomSheet(
    isCorporate: Boolean,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSlate,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isCorporate) "LOGO YÜKLEME YÖNTEMİ" else "FOTOĞRAF YÜKLEME YÖNTEMİ",
                color = NeonCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Camera option
            Surface(
                onClick = onCameraClick,
                shape = RoundedCornerShape(16.dp),
                color = AlmostBlack,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Kamerayla Çek",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Gallery option
            Surface(
                onClick = onGalleryClick,
                shape = RoundedCornerShape(16.dp),
                color = AlmostBlack,
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (isCorporate) "Galeriden Logo Seç" else "Galeriden Seç",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Cancel option
            Surface(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.05f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "İptal",
                        color = TextGray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun saveBitmapToCacheFile(context: android.content.Context, bitmap: android.graphics.Bitmap): String {
    val cacheDir = context.cacheDir
    val file = java.io.File(cacheDir, "player_crop_${System.currentTimeMillis()}.jpg")
    try {
        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return file.absolutePath
}

@Composable
fun StatSliderRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(value.toString(), color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Black)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 50f..99f,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun formatTurkishDate(dateStr: String): String {
    val months = listOf(
        "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
        "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
    )
    val parts = dateStr.split("-")
    if (parts.size == 3) {
        val y = parts[0]
        val mIndex = parts[1].toIntOrNull()?.minus(1) ?: return dateStr
        val d = parts[2].toIntOrNull() ?: return dateStr
        if (mIndex in 0..11) {
            return "$d ${months[mIndex]} $y"
        }
    }
    return dateStr
}

