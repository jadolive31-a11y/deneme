package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import coil.compose.rememberAsyncImagePainter
import com.example.model.*
import com.example.ui.components.NeonButton
import com.example.ui.components.PremiumCard
import com.example.ui.components.GallerySection
import com.example.ui.components.YoutubeVideosSection
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubProfileDetailOverlay(
    clubId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onPlayerClick: (String) -> Unit,
    isOwnProfileTab: Boolean = false,
    onSubTabChange: (Int) -> Unit = {}
) {
    if (clubId.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().background(AlmostBlack),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = NeonCyan)
        }
        return
    }

    val clubs by viewModel.clubs.collectAsState()
    val users by viewModel.users.collectAsState()
    val allPlayers by viewModel.players.collectAsState()
    val currentUser = remember(users, clubId) { users.find { it.id == clubId } }

    val fallbackClub = remember(clubId, currentUser) {
        Club(
            id = clubId,
            name = currentUser?.name ?: "Kulüp",
            city = currentUser?.city?.ifEmpty { "İstanbul" } ?: "İstanbul",
            district = "Merkez",
            foundationYear = 2024,
            coverPhotoUrl = "",
            logoUrl = currentUser?.photoUrl ?: "",
            followerCount = 0,
            activeAgeGroups = "U11, U12, U13",
            aboutText = "Kulübümüz hakkında bilgi eklemek için profilinizi düzenleyin.",
            trainingFacility = "Kendi Tesislerimiz",
            pitchInfo = "Doğal Çim",
            trainingDays = "Hafta içi her gün",
            licenseStatus = "Lisanslı Akademi",
            acceptedAgeGroups = "U11, U12, U13",
            phoneNumber = "Belirtilmemiş",
            whatsappNumber = "Belirtilmemiş",
            instagramUsername = "Belirtilmemiş",
            websiteUrl = "Belirtilmemiş",
            locationUrl = "",
            address = "${currentUser?.city?.ifEmpty { "İstanbul" } ?: "İstanbul"} / Merkez",
            coaches = emptyList(),
            players = emptyList(),
            achievements = emptyList(),
            news = emptyList(),
            activeStudentsCount = 50,
            coachesCount = 3,
            trophyCount = 0,
            hasLicense = true,
            hasSummerSchool = true,
            hasWinterSchool = true,
            ageGroups = "U11, U12, U13"
        )
    }

    val club = remember(clubs, clubId, fallbackClub) { clubs.find { it.id == clubId } ?: fallbackClub }

    val livePlayers = remember(allPlayers, club) {
        allPlayers.filter { it.club == club.id || it.club == club.name }
    }
    val liveCoaches = remember(users, club) {
        users.filter { it.role == UserRole.COACH && (it.club == club.id || it.club == club.name) }
    }

    LaunchedEffect(clubId, clubs) {
        if (clubs.find { it.id == clubId } == null) {
            viewModel.ensureClubProfileExists(clubId)
        }
    }

    val currentUserId by viewModel.currentUserId.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    // Determine tabs dynamically based on ownership
    val tabs = remember(club.id, currentUserId) {
        if (club.id == currentUserId) {
            listOf("Başvurular", "Oyuncular", "Antrenörler", "Başarılar", "Fotoğraflar", "Videolar", "Haberler", "İletişim")
        } else {
            listOf("Oyuncular", "Antrenörler", "Başarılar", "Fotoğraflar", "Videolar", "Haberler", "İletişim")
        }
    }
    var selectedTabName by remember(tabs) { mutableStateOf(tabs.first()) }

    var showApplyDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var userMessage by remember { mutableStateOf("") }

    BackHandler(enabled = showEditDialog) {
        showEditDialog = false
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(AlmostBlack)) {
                CenterAlignedTopAppBar(
                    title = { Text(text = club.name.uppercase(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) },
                    navigationIcon = {
                        if (!isOwnProfileTab) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Geri", tint = NeonCyan)
                            }
                        }
                    },
                    actions = {
                        if (club.id == currentUserId) {
                            IconButton(onClick = { showEditDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Profili Düzenle",
                                    tint = NeonCyan
                                )
                            }
                        } else {
                            IconButton(onClick = { viewModel.toggleFollowClub(club.id) }) {
                                Icon(
                                    imageVector = if (club.followedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Takip Et",
                                    tint = if (club.followedByMe) Color.Red else Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AlmostBlack)
                )

                if (isOwnProfileTab) {
                    // Profile & Settings top sub-tab selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSlate)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("PROFİLİM", "AYARLAR").forEachIndexed { index, label ->
                            val isSelected = index == 0
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan else Color.Transparent)
                                    .clickable { onSubTabChange(index) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) AlmostBlack else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = AlmostBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // CORPORATE LOGO & DETAILS HEADER (No cover photo as requested)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(DarkSlate, AlmostBlack)
                        )
                    )
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Club Logo
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSlate)
                        .border(2.dp, NeonCyan, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = club.logoUrl),
                        contentDescription = "Kulüp Logosu",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Club Name & Verification
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = club.name,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    if (club.hasLicense) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Lisanslı Akademi",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "👥 ${club.followerCount} TAKİPÇİ",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "•",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "📅 ${club.foundationYear} KURULUŞ",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ACTION BUTTONS (Edit Profile or Follow/Apply/Message selection)
                if (club.id == currentUserId) {
                    Button(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = AlmostBlack, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PROFİLİ DÜZENLE", color = AlmostBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.95f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showMessageDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mesaj", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = { viewModel.toggleFollowClub(club.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (club.followedByMe) NeonCyan.copy(alpha = 0.15f) else NeonCyan
                            ),
                            border = if (club.followedByMe) BorderStroke(1.dp, NeonCyan) else null,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (club.followedByMe) Icons.Default.Check else Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (club.followedByMe) NeonCyan else AlmostBlack,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (club.followedByMe) "Takip Ediliyor" else "Takip Et",
                                    color = if (club.followedByMe) NeonCyan else AlmostBlack,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Button(
                            onClick = { showApplyDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (club.registrationApplied) Color(0xFF1E5B32) else Color(0xFF1B5E20)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (club.registrationApplied) Icons.Default.CheckCircle else Icons.Default.AssignmentInd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (club.registrationApplied) "Başvuruldu" else "Başvuru Yap",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // KULÜP TANITIM / HAKKINDA TEXT
            PremiumCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "KULÜP HAKKINDA",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = club.aboutText.ifEmpty { "Henüz eklenmedi" },
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            // SECTION 4 - CORPORATE INFO BIG CARDS GRID (9 cards)
            Text(
                text = "KURUMSAL BİLGİ VE KANALLAR",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CorporateInfoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "👥",
                        title = "Oyuncu",
                        value = if (club.activeStudentsCount > 0) "${club.activeStudentsCount} Sporcu" else if (club.players.isNotEmpty()) "${club.players.size} Kayıtlı" else "Henüz eklenmedi"
                    )
                    CorporateInfoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "👨‍🏫",
                        title = "Antrenör",
                        value = if (club.coachesCount > 0) "${club.coachesCount} Antrenör" else if (club.coaches.isNotEmpty()) "${club.coaches.size} Antrenör" else "Henüz eklenmedi"
                    )
                    CorporateInfoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🏆",
                        title = "Kupa",
                        value = if (club.trophyCount > 0) "${club.trophyCount} Kupa" else "Henüz eklenmedi"
                    )
                }

                // Row 2
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CorporateInfoCard(
                        modifier = Modifier.weight(1.5f),
                        emoji = "📍",
                        title = "Şehir / İlçe",
                        value = "${club.city} / ${club.district.ifEmpty { "Henüz eklenmedi" }}"
                    )
                    CorporateInfoCard(
                        modifier = Modifier.weight(1.5f),
                        emoji = "⚽",
                        title = "Yaş Grupları",
                        value = club.ageGroups.ifEmpty { club.activeAgeGroups.ifEmpty { "Henüz eklenmedi" } }
                    )
                }

                // Row 3 (Interactive Buttons)
                val context = androidx.compose.ui.platform.LocalContext.current
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CorporateInfoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📞",
                        title = "Telefon",
                        value = club.phoneNumber.ifEmpty { "Henüz eklenmedi" },
                        clickable = club.phoneNumber.isNotEmpty(),
                        onClick = {
                            if (club.phoneNumber.isNotEmpty()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:${club.phoneNumber}"))
                                context.startActivity(intent)
                            }
                        }
                    )
                    CorporateInfoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🟢",
                        title = "WhatsApp",
                        value = club.whatsappNumber.ifEmpty { "Henüz eklenmedi" },
                        clickable = club.whatsappNumber.isNotEmpty(),
                        onClick = {
                            if (club.whatsappNumber.isNotEmpty()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/${club.whatsappNumber}"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }

                // Row 4 (Interactive Buttons)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CorporateInfoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📸",
                        title = "Instagram",
                        value = if (club.instagramUsername.isNotEmpty()) "@${club.instagramUsername}" else "Henüz eklenmedi",
                        clickable = club.instagramUsername.isNotEmpty(),
                        onClick = {
                            if (club.instagramUsername.isNotEmpty()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://instagram.com/${club.instagramUsername}"))
                                context.startActivity(intent)
                            }
                        }
                    )
                    CorporateInfoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🗺️",
                        title = "Haritada Aç",
                        value = if (club.locationUrl.isNotEmpty()) "Konumu Göster" else "Henüz eklenmedi",
                        clickable = club.locationUrl.isNotEmpty(),
                        onClick = {
                            if (club.locationUrl.isNotEmpty()) {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(club.locationUrl))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // HORIZONTAL TABS SELECTOR (Scrollable)
            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedTabName).coerceAtLeast(0),
                containerColor = AlmostBlack,
                contentColor = NeonCyan,
                edgePadding = 16.dp,
                divider = { Divider(color = DarkBorder, thickness = 0.5.dp) }
            ) {
                tabs.forEach { text ->
                    Tab(
                        selected = selectedTabName == text,
                        onClick = { selectedTabName = text },
                        text = { Text(text = text.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        selectedContentColor = NeonCyan,
                        unselectedContentColor = TextGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // TAB CONTENT DISPLAY
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                when (selectedTabName) {
                    "Başvurular" -> {
                        val players by viewModel.players.collectAsState()
                        val appliedPlayers = players.filter { club.appliedPlayerIds.contains(it.id) }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "KULÜBE GELEN AKADEMİ BAŞVURULARI",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            if (appliedPlayers.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Henüz gelen bir başvuru bulunmamaktadır.",
                                        color = TextGray,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else {
                                appliedPlayers.forEach { player ->
                                    PremiumCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(50.dp)
                                                        .clip(CircleShape)
                                                        .background(DarkBorder),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (!player.photoUrl.isNullOrEmpty()) {
                                                        Image(
                                                            painter = rememberAsyncImagePainter(model = player.photoUrl),
                                                            contentDescription = "${player.firstName} ${player.lastName}",
                                                            contentScale = ContentScale.Crop,
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = "${player.firstName} ${player.lastName}",
                                                            tint = TextGray,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(text = "${player.firstName} ${player.lastName}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    Text(text = "Mevki: ${player.position.shortName} | Yaş: ${player.age}", color = TextGray, fontSize = 11.sp)
                                                    Text(text = "Scout Puanı: ${player.scoutRating}", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                IconButton(
                                                    onClick = { viewModel.acceptClubApplication(club.id, player.id) },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1B5E20))
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = "Kabul Et", tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                                IconButton(
                                                    onClick = { viewModel.rejectClubApplication(club.id, player.id) },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFFB71C1C))
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Reddet", tint = Color.White, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "Oyuncular" -> {
                        val isMyClubProfile = club.id == currentUserId
                        TabPlayersContent(club, livePlayers, isMyClubProfile, viewModel, onPlayerClick)
                    }
                    "Antrenörler" -> {
                        val isMyClubProfile = club.id == currentUserId
                        TabCoachesContent(club, liveCoaches, isMyClubProfile, viewModel)
                    }
                    "Başarılar" -> TabAchievementsContent(club)
                    "Fotoğraflar" -> {
                        val isMyClubProfile = club.id == currentUserId
                        val context = androidx.compose.ui.platform.LocalContext.current
                        GallerySection(
                            galleryList = club.gallery,
                            isEditable = isMyClubProfile,
                            isPremium = club.isPremium,
                            onAddPhoto = { bitmap ->
                                viewModel.addPhotoToGallery(context, bitmap) { _ -> }
                            },
                            onDeletePhoto = { photoUrl ->
                                viewModel.removePhotoFromGallery(photoUrl)
                            },
                            onReplacePhoto = { oldUrl, bitmap ->
                                viewModel.replacePhotoInGallery(context, oldUrl, bitmap) { _ -> }
                            }
                        )
                    }
                    "Videolar" -> {
                        val isMyClubProfile = club.id == currentUserId
                        YoutubeVideosSection(
                            videoList = club.youtubeVideos,
                            isEditable = isMyClubProfile,
                            isPremium = club.isPremium,
                            onAddVideo = { videoUrl ->
                                viewModel.addYoutubeVideo(videoUrl) { _ -> }
                            },
                            onDeleteVideo = { videoUrl ->
                                viewModel.removeYoutubeVideo(videoUrl)
                            }
                        )
                    }
                    "Haberler" -> TabNewsContent(club)
                    "İletişim" -> TabContactContent(club)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showEditDialog) {
        var editLogoUrl by remember { mutableStateOf(club.logoUrl) }
        var editPhone by remember { mutableStateOf(club.phoneNumber) }
        var editInstagram by remember { mutableStateOf(club.instagramUsername) }
        var editActiveStudents by remember { mutableStateOf(club.activeStudentsCount.toString()) }
        var editCoachesCount by remember { mutableStateOf(club.coachesCount.toString()) }
        var editTrophyCount by remember { mutableStateOf(club.trophyCount.toString()) }
        var editAboutText by remember { mutableStateOf(club.aboutText) }
        var editTrainingFacility by remember { mutableStateOf(club.trainingFacility) }
        var editLocationUrl by remember { mutableStateOf(club.locationUrl) }
        var editWebsite by remember { mutableStateOf(club.websiteUrl) }
        var editDistrict by remember { mutableStateOf(club.district) }
        var editCity by remember { mutableStateOf(club.city) }
        var editHasLicense by remember { mutableStateOf(club.hasLicense) }
        var editHasSummerSchool by remember { mutableStateOf(club.hasSummerSchool) }
        var editHasWinterSchool by remember { mutableStateOf(club.hasWinterSchool) }
        var editAgeGroups by remember { mutableStateOf(club.ageGroups) }

        val editGalleryLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                editLogoUrl = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("KULÜP PROFİLİNİ DÜZENLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Logo Selector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBorder)
                                .border(1.dp, NeonCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = editLogoUrl),
                                contentDescription = "Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Button(
                            onClick = { editGalleryLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBorder)
                        ) {
                            Text("Logo Değiştir", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Telefon", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editInstagram,
                        onValueChange = { editInstagram = it },
                        label = { Text("Instagram Kullanıcı Adı", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editActiveStudents,
                        onValueChange = { editActiveStudents = it },
                        label = { Text("Aktif Öğrenci Sayısı", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCoachesCount,
                        onValueChange = { editCoachesCount = it },
                        label = { Text("Antrenör Sayısı", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTrophyCount,
                        onValueChange = { editTrophyCount = it },
                        label = { Text("Kupa Sayısı", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editTrainingFacility,
                        onValueChange = { editTrainingFacility = it },
                        label = { Text("Antrenman Sahası / Tesisleri", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editLocationUrl,
                        onValueChange = { editLocationUrl = it },
                        label = { Text("Google Harita Konumu Linki", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editWebsite,
                        onValueChange = { editWebsite = it },
                        label = { Text("Web Sitesi", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editCity,
                        onValueChange = { editCity = it },
                        label = { Text("Şehir", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editDistrict,
                        onValueChange = { editDistrict = it },
                        label = { Text("İlçe", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAgeGroups,
                        onValueChange = { editAgeGroups = it },
                        label = { Text("Yaş Grupları (Örn: U7, U8, U9)", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editAboutText,
                        onValueChange = { editAboutText = it },
                        label = { Text("Kulüp Tanıtım Yazısı", color = TextGray) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan, unfocusedBorderColor = DarkBorder, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lisanslı/Tescilli mi?", color = Color.White, fontSize = 13.sp)
                        Switch(checked = editHasLicense, onCheckedChange = { editHasLicense = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Yaz Spor Okulu Var mı?", color = Color.White, fontSize = 13.sp)
                        Switch(checked = editHasSummerSchool, onCheckedChange = { editHasSummerSchool = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Kış Spor Okulu Var mı?", color = Color.White, fontSize = 13.sp)
                        Switch(checked = editHasWinterSchool, onCheckedChange = { editHasWinterSchool = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateClubDetails(
                            clubId = club.id,
                            logoUrl = editLogoUrl,
                            phone = editPhone,
                            instagram = editInstagram,
                            activeStudents = editActiveStudents.toIntOrNull() ?: 0,
                            coachesCount = editCoachesCount.toIntOrNull() ?: 0,
                            trophyCount = editTrophyCount.toIntOrNull() ?: 0,
                            aboutText = editAboutText,
                            trainingFacility = editTrainingFacility,
                            locationUrl = editLocationUrl,
                            websiteUrl = editWebsite,
                            district = editDistrict,
                            city = editCity,
                            hasLicense = editHasLicense,
                            hasSummerSchool = editHasSummerSchool,
                            hasWinterSchool = editHasWinterSchool,
                            ageGroups = editAgeGroups
                        )
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("KAYDET", color = AlmostBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("VAZGEÇ", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSlate
        )
    }

    // Messages Dialog
    if (showMessageDialog) {
        AlertDialog(
            onDismissRequest = { showMessageDialog = false },
            title = { Text("KULÜBE MESAJ GÖNDER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Kulüp yetkilisi ile doğrudan iletişime geçmek için mesajınızı yazın.",
                        color = TextGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = userMessage,
                        onValueChange = { userMessage = it },
                        placeholder = { Text("Mesajınızı yazın...", color = TextGray) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMessageDialog = false
                        userMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("MESAJI GÖNDER", color = AlmostBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMessageDialog = false }) {
                    Text("VAZGEÇ", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSlate
        )
    }

    // Apply Dialog
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { showApplyDialog = false },
            title = { Text(if (club.registrationApplied) "BAŞVURUYU GERİ ÇEK" else "AKADEMİ KAYIT BAŞVURUSU", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text(
                    text = if (club.registrationApplied) {
                        "Akademi seçmelerine yaptığınız ön kayıt başvurusunu iptal etmek istediğinize emin misiniz?"
                    } else {
                        "Profilinizin futbolcu özgeçmişi, fiziksel istatistikleri ve scout puanı doğrudan kulüp koordinatörlerine iletilecektir. Altyapı seçmelerine ön başvuru yapmak istiyor musunuz?"
                    },
                    color = TextGray,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyToClub(club.id)
                        showApplyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (club.registrationApplied) Color.Red else NeonCyan)
                ) {
                    Text(
                        text = if (club.registrationApplied) "BAŞVURUYU İPTAL ET" else "BAŞVURU YAP",
                        color = if (club.registrationApplied) Color.White else AlmostBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showApplyDialog = false }) {
                    Text("KAPAT", color = TextGray)
                }
            },
            containerColor = DarkSlate
        )
    }
}

@Composable
fun ClubStatBox(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
        Text(text = label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SocialIconAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(DarkSlate)
                .border(0.5.dp, DarkBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = NeonCyan, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// -------------------------------------------------------------
// CLUB TABS CONTENTS
// -------------------------------------------------------------

@Composable
fun TabAboutContent(club: Club) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "KULÜP TANITIMI", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = club.aboutText.ifEmpty { "Henüz eklenmedi" }, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "TESİS VE ANTRENMAN BİLGİLERİ", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(10.dp))

            AboutDetailItem(label = "Antrenman Tesisi", value = club.trainingFacility, icon = Icons.Default.Business)
            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            AboutDetailItem(label = "Saha Bilgileri", value = club.pitchInfo, icon = Icons.Default.SportsSoccer)
            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            AboutDetailItem(label = "Antrenman Günleri", value = club.trainingDays, icon = Icons.Default.Schedule)
            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            AboutDetailItem(label = "Lisans Durumu", value = if (club.hasLicense) "LİSANSLI AKADEMİ" else club.licenseStatus, icon = Icons.Default.Verified)
            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            AboutDetailItem(label = "Kabul Edilen Yaşlar", value = club.ageGroups.ifEmpty { club.acceptedAgeGroups }, icon = Icons.Default.Face)
        }
    }
}

@Composable
fun AboutDetailItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = value.ifEmpty { "Henüz eklenmedi" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TabPlayersContent(
    club: Club,
    livePlayers: List<Player>,
    isMyClubProfile: Boolean,
    viewModel: MainViewModel,
    onPlayerClick: (String) -> Unit
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    val allPlayers by viewModel.players.collectAsState()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KULÜP ALTYAPI SPORCULARI",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (isMyClubProfile) {
                Button(
                    onClick = { showInviteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Oyuncu Davet Et",
                        tint = AlmostBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("OYUNCU DAVET ET", color = AlmostBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (livePlayers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Henüz eklenmedi", color = TextGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 1200.dp)
            ) {
                items(livePlayers) { p ->
                    val clubPlayer = ClubPlayer(
                        id = p.id,
                        name = "${p.firstName} ${p.lastName}",
                        position = p.position.shortName,
                        age = p.age,
                        overallRating = p.scoutRating.takeIf { it > 0 } ?: p.selfRating,
                        photoUrl = p.photoUrl ?: ""
                    )
                    FUTPlayerCard(
                        player = clubPlayer,
                        onClick = { onPlayerClick(p.id) },
                        onRemove = if (isMyClubProfile) { { viewModel.removePlayerFromClub(club.id, p.id) } } else null
                    )
                }
            }
        }
    }

    if (showInviteDialog) {
        val inviteCandidates = remember(allPlayers, livePlayers) {
            allPlayers.filter { p ->
                val alreadyInClub = livePlayers.any { lp -> lp.id == p.id }
                val hasNoClub = p.club.isEmpty() || p.club == "Kulüpsüz" || p.club == "Hiçbiri"
                !alreadyInClub && hasNoClub
            }
        }
        
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = {
                Text("OYUNCU DAVET ET", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Kulübüne davet etmek istediğin sporcuları seç. Oyuncuya onay bildirimi gidecektir.",
                        color = TextGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (inviteCandidates.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Davet edilebilecek kulüpsüz oyuncu bulunamadı.", color = TextGray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(inviteCandidates) { player ->
                                var sent by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSlate)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(DarkBorder),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(model = player.photoUrl),
                                                contentDescription = player.firstName,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("${player.firstName} ${player.lastName}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Mevki: ${player.position.shortName} | OVR ${player.scoutRating.takeIf { it > 0 } ?: player.selfRating}", color = TextGray, fontSize = 10.sp)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.sendClubPlayerInvite(club.id, club.name, player.id)
                                            sent = true
                                        },
                                        enabled = !sent,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonCyan,
                                            disabledContainerColor = DarkBorder
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            if (sent) "GÖNDERİLDİ" else "DAVET ET",
                                            color = if (sent) TextGray else AlmostBlack,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("KAPAT", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AlmostBlack,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun FUTPlayerCard(player: ClubPlayer, onClick: () -> Unit, onRemove: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(DarkSlate)
            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(DarkBorder),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = player.photoUrl),
                    contentDescription = player.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = player.name,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                text = "Mevki: ${player.position} | Yaş: ${player.age}",
                color = TextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            // Overall rating badge (EA SPORTS FC style hexagon look)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonCyan)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "OVR ${player.overallRating}",
                    color = AlmostBlack,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black
                )
            }

            if (onRemove != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onRemove() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text("KULÜPTEN ÇIKAR", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TabCoachesContent(
    club: Club,
    liveCoaches: List<AppUser>,
    isMyClubProfile: Boolean,
    viewModel: MainViewModel
) {
    var showInviteDialog by remember { mutableStateOf(false) }
    val allUsers by viewModel.users.collectAsState()
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KULÜBÜN LİSANSLI ANTRENÖRLERİ",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            if (isMyClubProfile) {
                Button(
                    onClick = { showInviteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Antrenör Davet Et",
                        tint = AlmostBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ANTRENÖR DAVET ET", color = AlmostBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (liveCoaches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Henüz eklenmedi", color = TextGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            liveCoaches.forEach { coach ->
                PremiumCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(DarkBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = coach.photoUrl),
                                contentDescription = coach.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = coach.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = "Antrenör", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = "UEFA A", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            if (isMyClubProfile) {
                                Button(
                                    onClick = { viewModel.removeCoachFromClub(club.id, coach.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("ÇIKAR", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInviteDialog) {
        val inviteCandidates = remember(allUsers, liveCoaches) {
            allUsers.filter { u ->
                val alreadyInClub = liveCoaches.any { lc -> lc.id == u.id }
                val isCoach = u.role == UserRole.COACH
                val hasNoClub = u.club.isEmpty() || u.club == "Kulüpsüz"
                isCoach && !alreadyInClub && hasNoClub
            }
        }
        
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = {
                Text("ANTRENÖR DAVET ET", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Kulübüne davet etmek istediğin antrenörleri seç. Antrenöre onay bildirimi gidecektir.",
                        color = TextGray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    if (inviteCandidates.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Davet edilebilecek kulüpsüz antrenör bulunamadı.", color = TextGray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(inviteCandidates) { coach ->
                                var sent by remember { mutableStateOf(false) }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSlate)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(DarkBorder),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = rememberAsyncImagePainter(model = coach.photoUrl),
                                                contentDescription = coach.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(coach.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text(coach.city, color = TextGray, fontSize = 10.sp)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.sendClubCoachInvite(club.id, club.name, coach.id)
                                            sent = true
                                        },
                                        enabled = !sent,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = NeonCyan,
                                            disabledContainerColor = DarkBorder
                                        ),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            if (sent) "GÖNDERİLDİ" else "DAVET ET",
                                            color = if (sent) TextGray else AlmostBlack,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text("KAPAT", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AlmostBlack,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun TabAchievementsContent(club: Club) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "KULÜBÜN KAZANDIĞI BAŞARILAR VE KUPALAR",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (club.achievements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Henüz eklenmedi", color = TextGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            club.achievements.forEach { ach ->
                PremiumCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = ach.icon, fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = ach.description, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(text = ach.title, color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "x${ach.count}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabNewsContent(club: Club) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "KULÜPTEN GÜNCEL GELİŞMELER & DUYURULAR",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (club.news.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Henüz eklenmedi", color = TextGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        } else {
            club.news.forEach { item ->
                PremiumCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column {
                        Image(
                            painter = rememberAsyncImagePainter(model = item.imageUrl),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = item.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(text = item.date, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = item.content, color = TextGray, fontSize = 12.sp, lineHeight = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TabContactContent(club: Club) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PremiumCard(modifier = Modifier.fillMaxWidth()) {
            Text(text = "İLETİŞİM BİLGİLERİ VE RESMİ KANALLAR", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))

            AboutDetailItem(label = "Telefon", value = club.phoneNumber, icon = Icons.Default.Phone)
            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            AboutDetailItem(label = "WhatsApp Hattı", value = club.whatsappNumber, icon = Icons.Default.Chat)
            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            AboutDetailItem(label = "Instagram Kullanıcı Adı", value = club.instagramUsername, icon = Icons.Default.AlternateEmail)
            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            AboutDetailItem(label = "Web Sitesi", value = club.websiteUrl, icon = Icons.Default.Language)
            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
            AboutDetailItem(label = "Kulüp Adresi", value = club.address, icon = Icons.Default.Place)
        }
    }
}

// Format Followers Count
fun formatCount(count: Int): String {
    return if (count >= 1000) {
        val kValue = count / 1000.0
        String.format(java.util.Locale.US, "%.1fK", kValue)
    } else {
        count.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableCitySelector(
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    label: String = "Şehir Seçin",
    showAllOption: Boolean = false,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val rawCities = if (showAllOption) {
        listOf("Tüm Şehirler") + TURKEY_CITIES.filter { it != "Tüm Şehirler" }
    } else {
        TURKEY_CITIES.filter { it != "Tüm Şehirler" }
    }

    val filteredCities = remember(searchQuery, rawCities) {
        if (searchQuery.isBlank()) {
            rawCities
        } else {
            rawCities.filter { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Box(modifier = modifier) {
        OutlinedTextField(
            value = selectedCity,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = TextGray) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = DarkSlate,
                unfocusedContainerColor = DarkSlate,
                disabledContainerColor = DarkSlate,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = NeonCyan,
                unfocusedIndicatorColor = DarkBorder,
                disabledIndicatorColor = DarkBorder
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(12.dp)
        )

        if (expanded) {
            Dialog(onDismissRequest = { expanded = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSlate,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = label.uppercase(),
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Şehir ara...", color = TextGray) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = NeonCyan) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = AlmostBlack,
                                unfocusedContainerColor = AlmostBlack,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = NeonCyan,
                                unfocusedIndicatorColor = DarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredCities) { city ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (city == selectedCity) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            onCitySelected(city)
                                            searchQuery = ""
                                            expanded = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = city,
                                        color = if (city == selectedCity) NeonCyan else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (city == selectedCity) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            if (filteredCities.isEmpty()) {
                                item {
                                    Text(
                                        text = "Eşleşen şehir bulunamadı.",
                                        color = TextGray,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubSearchLayout(
    viewModel: MainViewModel,
    clubQuery: String,
    onClubQueryChange: (String) -> Unit,
    onClubClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clubs by viewModel.clubs.collectAsState()
    
    // State filters
    var activeCityFilter by remember { mutableStateOf("Tümü") }
    var activeAgeFilter by remember { mutableStateOf("Tümü") }
    var activeTypeFilter by remember { mutableStateOf("Tümü") }
    var activePlayerCountFilter by remember { mutableStateOf("Tümü") }
    var isMapViewActive by remember { mutableStateOf(false) }
    var selectedMapClubId by remember { mutableStateOf<String?>(null) }
    var isFiltersExpanded by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }

    // Helper options for Turkey youth clubs
    val ageGroups = listOf("Tümü", "U11", "U12", "U13", "U15", "U17", "U19")
    val clubTypes = listOf("Tümü", "Özel Akademi", "Spor Kulübü", "Profesyonel Kulüp", "Belediye Kulübü")
    val playerCountOptions = listOf("Tümü", "0-50", "51-100", "101-200", "201+")

    // Filter Logic
    val filteredClubs = clubs.filter { club ->
        val matchesQuery = club.name.contains(clubQuery, ignoreCase = true) ||
                club.city.contains(clubQuery, ignoreCase = true)
        val matchesCity = activeCityFilter == "Tümü" || club.city.equals(activeCityFilter, ignoreCase = true)
        val matchesType = activeTypeFilter == "Tümü" || club.clubType.equals(activeTypeFilter, ignoreCase = true)
        val matchesAge = activeAgeFilter == "Tümü" || club.activeAgeGroups.contains(activeAgeFilter, ignoreCase = true)
        val matchesPlayerCount = activePlayerCountFilter == "Tümü" || when(activePlayerCountFilter) {
            "0-50" -> club.activeStudentsCount in 0..50
            "51-100" -> club.activeStudentsCount in 51..100
            "101-200" -> club.activeStudentsCount in 101..200
            "201+" -> club.activeStudentsCount > 200
            else -> true
        }
        
        matchesQuery && matchesCity && matchesType && matchesAge && matchesPlayerCount
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AlmostBlack)
    ) {
        val activeClubFiltersCount = listOf(
            activeCityFilter != "Tümü",
            activeTypeFilter != "Tümü",
            activeAgeFilter != "Tümü",
            activePlayerCountFilter != "Tümü"
        ).count { it }

        // 1. COMPACT SEARCH BAR WITH ALIGNED FILTER TOGGLE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = clubQuery,
                onValueChange = onClubQueryChange,
                placeholder = { Text("Kulüp adı veya şehir ara", color = TextGray, fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (clubQuery.isNotEmpty()) {
                        IconButton(onClick = { onClubQueryChange("") }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Temizle", tint = TextGray, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedContainerColor = DarkSlate,
                    unfocusedContainerColor = DarkSlate,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                textStyle = TextStyle(fontSize = 13.sp, color = Color.White)
            )

            // Cyberpunk Compact Filter Toggle Button with Badge
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isFiltersExpanded) NeonCyan else DarkSlate)
                    .border(1.dp, if (isFiltersExpanded) NeonCyan else DarkBorder, RoundedCornerShape(14.dp))
                    .clickable { isFiltersExpanded = !isFiltersExpanded },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filtreler",
                    tint = if (isFiltersExpanded) AlmostBlack else Color.White,
                    modifier = Modifier.size(20.dp)
                )
                
                if (activeClubFiltersCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(if (isFiltersExpanded) AlmostBlack else NeonCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeClubFiltersCount.toString(),
                            color = if (isFiltersExpanded) NeonCyan else AlmostBlack,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Active filter chips row for clubs
        if (activeClubFiltersCount > 0) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeCityFilter != "Tümü") {
                    item {
                        ClubActiveChip(label = "Şehir: $activeCityFilter", onClear = { activeCityFilter = "Tümü" })
                    }
                }
                if (activeTypeFilter != "Tümü") {
                    item {
                        ClubActiveChip(label = "Tür: $activeTypeFilter", onClear = { activeTypeFilter = "Tümü" })
                    }
                }
                if (activeAgeFilter != "Tümü") {
                    item {
                        ClubActiveChip(label = "Yaş: $activeAgeFilter", onClear = { activeAgeFilter = "Tümü" })
                    }
                }
                if (activePlayerCountFilter != "Tümü") {
                    item {
                        ClubActiveChip(label = "Oyuncu: $activePlayerCountFilter", onClear = { activePlayerCountFilter = "Tümü" })
                    }
                }
            }
        }

        // Animated Filters Block
        AnimatedVisibility(
            visible = isFiltersExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                // 2. HUD HEADER & RADAR TOGGLE (Vibe of Google Maps + Instagram Explore)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSlate)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode Selectors
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isMapViewActive) NeonCyan else Color.Transparent)
                            .clickable { isMapViewActive = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = if (!isMapViewActive) AlmostBlack else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "📱 KULÜP KEŞFET",
                                color = if (!isMapViewActive) AlmostBlack else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isMapViewActive) NeonCyan else Color.Transparent)
                            .clickable { isMapViewActive = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Map,
                                contentDescription = null,
                                tint = if (isMapViewActive) AlmostBlack else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "🛰️ RADAR GÖRÜNÜMÜ",
                                color = if (isMapViewActive) AlmostBlack else Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Live Radar Status line (cyberpunk elegance)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isMapViewActive) Color.Green else NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isMapViewActive) "RADAR SCANNER ACTIVE • 39.93° N" else "EXPLORE GRID SYNCHRONIZED",
                            color = TextGray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "${filteredClubs.size} KULÜP",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // 3. HORIZONTAL FILTERS SECTIONS (Horizontal Scrollable Chips)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Searchable City Selection Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Şehir:",
                            color = TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (activeCityFilter != "Tümü") NeonCyan.copy(alpha = 0.15f) else DarkSlate)
                                .border(1.dp, if (activeCityFilter != "Tümü") NeonCyan else DarkBorder, RoundedCornerShape(12.dp))
                                .clickable { showCityDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Place, null, tint = if (activeCityFilter != "Tümü") NeonCyan else Color.White, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (activeCityFilter == "Tümü") "TÜM ŞEHİRLER (SEÇMEK İÇİN TIKLAYIN)" else activeCityFilter.uppercase(),
                                    color = if (activeCityFilter != "Tümü") NeonCyan else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (activeCityFilter != "Tümü") {
                            Text(
                                text = "TEMİZLE",
                                color = Color.Red,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .clickable { activeCityFilter = "Tümü" }
                                    .padding(start = 8.dp)
                            )
                        }
                    }

                    // Club Type Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tür:",
                            color = TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        clubTypes.forEach { type ->
                            val isSelected = activeTypeFilter == type
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkSlate)
                                    .border(1.dp, if (isSelected) NeonCyan else DarkBorder, RoundedCornerShape(12.dp))
                                    .clickable { activeTypeFilter = type }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = type.uppercase(),
                                    color = if (isSelected) NeonCyan else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Age Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Yaş:",
                            color = TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        ageGroups.forEach { age ->
                            val isSelected = activeAgeFilter == age
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkSlate)
                                    .border(1.dp, if (isSelected) NeonCyan else DarkBorder, RoundedCornerShape(12.dp))
                                    .clickable { activeAgeFilter = age }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = age,
                                    color = if (isSelected) NeonCyan else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Oyuncu Sayısı Filter Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Oyuncu:",
                            color = TextGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        playerCountOptions.forEach { option ->
                            val isSelected = activePlayerCountFilter == option
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.15f) else DarkSlate)
                                    .border(1.dp, if (isSelected) NeonCyan else DarkBorder, RoundedCornerShape(12.dp))
                                    .clickable { activePlayerCountFilter = option }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (option == "Tümü") "TÜMÜ" else "$option OYUNCU",
                                    color = if (isSelected) NeonCyan else Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Searchable City Selector Dialog
        if (showCityDialog) {
            Dialog(onDismissRequest = { showCityDialog = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 450.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSlate,
                    border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "ŞEHİR SEÇİN",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        var searchQuery by remember { mutableStateOf("") }
                        val rawCities = listOf("Tümü") + TURKEY_CITIES.filter { it != "Tüm Şehirler" }
                        val filteredCities = remember(searchQuery) {
                            if (searchQuery.isBlank()) rawCities else rawCities.filter { it.contains(searchQuery, ignoreCase = true) }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Şehir ara...", color = TextGray) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = NeonCyan) },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = AlmostBlack,
                                unfocusedContainerColor = AlmostBlack,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = NeonCyan,
                                unfocusedIndicatorColor = DarkBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(filteredCities) { city ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (city == activeCityFilter) NeonCyan.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable {
                                            activeCityFilter = city
                                            showCityDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = city,
                                        color = if (city == activeCityFilter) NeonCyan else Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = if (city == activeCityFilter) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. MAIN CONTENT AREA - PROFESSIONAL HIGH DENSITY GRID
        if (clubs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Kayıtlı kulüp bulunamadı.",
                        color = TextWhite,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else if (filteredClubs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Kulüp bulunamadı.",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Farklı bir kulüp adı veya şehirle tekrar ara.",
                        color = TextGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            activeCityFilter = "Tümü"
                            activeAgeFilter = "Tümü"
                            activeTypeFilter = "Tümü"
                            activePlayerCountFilter = "Tümü"
                            onClubQueryChange("")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan.copy(alpha = 0.15f),
                            contentColor = NeonCyan
                        ),
                        border = BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "ARAMA VE FİLTRELERİ TEMİZLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        } else {
            if (isMapViewActive) {
                // CYBERPUNK RADAR MAP VIEW (Google Maps styled with cyberpunk details)
                ClubRadarMapView(
                    clubs = filteredClubs,
                    selectedClubId = selectedMapClubId,
                    onSelectClub = { selectedMapClubId = it },
                    onClubClick = onClubClick
                )
            } else {
                // MODERN FULL-WIDTH CLUB LIST
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 2.dp, end = 2.dp, bottom = 12.dp)
                ) {
                    items(filteredClubs) { club ->
                        ClubDiscoverRowCard(
                            club = club,
                            onClick = { onClubClick(club.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClubActiveChip(label: String, onClear: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSlate)
            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Temizle",
                tint = NeonCyan,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onClear() }
            )
        }
    }
}

@Composable
fun ClubDiscoverRowCard(
    club: Club,
    onClick: () -> Unit
) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        borderGlow = (club.clubType == "Profesyonel Kulüp" || club.clubType == "Özel Akademi"),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Club logo
            Box(
                modifier = Modifier.size(72.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = club.logoUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(AlmostBlack)
                        .border(1.5.dp, NeonCyan.copy(alpha = 0.7f), CircleShape)
                )
                
                if (club.clubType == "Profesyonel Kulüp" || club.clubType == "Özel Akademi") {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                            .border(1.dp, AlmostBlack, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Doğrulanmış",
                            tint = AlmostBlack,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info Center
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = club.name.uppercase(),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = club.city,
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val students = if (club.activeStudentsCount > 0) club.activeStudentsCount else club.players.size
                val coaches = if (club.coachesCount > 0) club.coachesCount else club.coaches.size
                val countText = when {
                    students > 0 && coaches > 0 -> "$students oyuncu • $coaches antrenör"
                    students > 0 -> "$students oyuncu"
                    coaches > 0 -> "$coaches antrenör"
                    else -> ""
                }
                
                if (countText.isNotEmpty()) {
                    Text(
                        text = countText,
                        color = TextGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
            
            // Right Arrow
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Map simulation with radar effects and marker selection (combines Google Maps feel with premium UI)
@Composable
fun ClubRadarMapView(
    clubs: List<Club>,
    selectedClubId: String?,
    onSelectClub: (String) -> Unit,
    onClubClick: (String) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val radarSweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Sweep"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(460.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(AlmostBlack)
            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
    ) {
        // RADAR CANVAS (Draw tactical coordinate grid)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f
            val centerY = height / 2f
            
            // Draw grid lines
            val numLines = 8
            for (i in 1..numLines) {
                val x = (width / numLines) * i
                drawLine(
                    color = DarkBorder.copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, height),
                    strokeWidth = 1f
                )
            }
            for (i in 1..numLines) {
                val y = (height / numLines) * i
                drawLine(
                    color = DarkBorder.copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(width, y),
                    strokeWidth = 1f
                )
            }

            // Draw Radar Circles
            drawCircle(
                color = NeonCyan.copy(alpha = 0.05f),
                radius = minOf(width, height) * 0.45f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = Stroke(width = 1.5f)
            )
            drawCircle(
                color = NeonCyan.copy(alpha = 0.1f),
                radius = minOf(width, height) * 0.3f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = NeonCyan.copy(alpha = 0.15f),
                radius = minOf(width, height) * 0.15f,
                center = androidx.compose.ui.geometry.Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )

            // Draw radar sweep line
            val angleRad = Math.toRadians(radarSweepAngle.toDouble())
            val targetX = centerX + (minOf(width, height) * 0.48f) * cos(angleRad).toFloat()
            val targetY = centerY + (minOf(width, height) * 0.48f) * sin(angleRad).toFloat()
            drawLine(
                color = NeonCyan.copy(alpha = 0.25f),
                start = androidx.compose.ui.geometry.Offset(centerX, centerY),
                end = androidx.compose.ui.geometry.Offset(targetX, targetY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // PLACING MAP PINS (Simulating tactical city positions)
        clubs.forEach { club ->
            // Distribute clubs based on ID and city
            val (pctX, pctY) = when (club.id) {
                "c_1" -> 0.35f to 0.30f // Beşiktaş (Istanbul)
                "c_3" -> 0.48f to 0.22f // GS (Istanbul)
                "c_4" -> 0.42f to 0.40f // Kadıköy (Istanbul)
                "c_2" -> 0.22f to 0.65f // Altınordu (Izmir)
                "c_5" -> 0.28f to 0.76f // Karşıyaka (Izmir)
                "c_6" -> 0.16f to 0.70f // Bornova (Izmir)
                else -> {
                    // Spread out dynamically using hash
                    val hX = (club.id.hashCode() % 40 + 40) / 100f
                    val hY = (club.name.hashCode() % 40 + 45) / 100f
                    hX to hY
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val posX = maxWidth * pctX
                val posY = maxHeight * pctY
                val isSelected = club.id == selectedClubId

                Box(
                    modifier = Modifier
                        .absoluteOffset(x = posX, y = posY)
                        .size(48.dp)
                        .clickable { onSelectClub(club.id) },
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing glow ring
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.Red.copy(alpha = 0.2f * pulseScale)
                                else NeonCyan.copy(alpha = 0.15f * pulseScale)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Red.copy(alpha = 0.7f) else NeonCyan.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                    )

                    // Pin core: Logo container
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(AlmostBlack)
                            .border(
                                1.5.dp,
                                if (isSelected) Color.Red else NeonCyan,
                                CircleShape
                            )
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(model = club.logoUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Floating Mini Name tag
                    Box(
                        modifier = Modifier
                            .offset(y = 20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(AlmostBlack.copy(alpha = 0.85f))
                            .border(0.5.dp, if (isSelected) Color.Red else TextGray, RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = club.name.take(4).uppercase(),
                            color = if (isSelected) Color.Red else Color.White,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // FLOATING PREVIEW OVERLAY CARD (Sliding up from the bottom when selected)
        val selectedClub = clubs.find { it.id == selectedClubId }
        AnimatedVisibility(
            visible = selectedClub != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            if (selectedClub != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
                        .border(1.5.dp, NeonCyan, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = DarkSlate),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        // Left: Club Logo and cover image
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = selectedClub.coverPhotoUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AlmostBlack.copy(alpha = 0.4f))
                            )
                            // Mini Logo in front
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AlmostBlack)
                                    .padding(2.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = selectedClub.logoUrl),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Right: Info & Actions
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedClub.name.uppercase(),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        maxLines = 1
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(
                                            text = "${selectedClub.city} • ${selectedClub.clubType}",
                                            color = TextGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Close Button
                                IconButton(
                                    onClick = { onSelectClub("") },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Kapat",
                                        tint = TextGray,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // Horizontal Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🎂 ${selectedClub.activeAgeGroups}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "👥 ${formatCount(selectedClub.followerCount)} Takipçi",
                                    color = NeonCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // View Profile Mini Button
                            Button(
                                onClick = { onClubClick(selectedClub.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = NeonCyan,
                                    contentColor = AlmostBlack
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "PROFİLİ GÖR",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getClubPlayerCountText(club: Club): String {
    return when (club.id) {
        "c_1" -> "150+ Akademi Sporcusu"
        "c_2" -> "220+ Genç Yetenek"
        "c_3" -> "180+ Akademi Sporcusu"
        "c_4" -> "85+ Lisanslı Sporcu"
        "c_5" -> "120+ Akademi Oyuncusu"
        "c_6" -> "310+ Spor Okulu Öğrencisi"
        else -> "${maxOf(club.players.size, 45)} Akademi Sporcusu"
    }
}

@Composable
fun CorporateInfoCard(
    emoji: String,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    clickable: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (clickable && value != "Henüz eklenmedi") Modifier.clickable { onClick() } else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSlate),
        border = BorderStroke(0.5.dp, if (clickable && value != "Henüz eklenmedi") NeonCyan.copy(alpha = 0.5f) else DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = emoji, fontSize = 16.sp)
                Text(
                    text = title.uppercase(),
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = if (value == "Henüz eklenmedi") TextGray else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

