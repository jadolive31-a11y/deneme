package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil.compose.rememberAsyncImagePainter
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.SearchFilters

sealed interface ScreenState {
    data class Tab(val tabIndex: Int) : ScreenState
    data class PlayerProfile(val playerId: String) : ScreenState
    data class ClubProfile(val clubId: String) : ScreenState
    data class Chat(val chatId: String) : ScreenState
    data class ScoutReport(val playerId: String) : ScreenState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: MainViewModel,
    onLogout: () -> Unit
) {
    val navigationStack = remember { mutableStateListOf<ScreenState>(ScreenState.Tab(0)) }

    // Derive current states from the stack
    val currentScreenState = navigationStack.lastOrNull() ?: ScreenState.Tab(0)

    val selectedTab = navigationStack.filterIsInstance<ScreenState.Tab>().lastOrNull()?.tabIndex ?: 0
    var searchInitialMode by remember { mutableStateOf(0) }
    var exploreInitialCategory by remember { mutableStateOf<String?>(null) }
    val activePlayerId = (currentScreenState as? ScreenState.PlayerProfile)?.playerId
    val activeClubId = (currentScreenState as? ScreenState.ClubProfile)?.clubId
    val activeChatId = (currentScreenState as? ScreenState.Chat)?.chatId
    val showWriteScoutReportPlayerId = (currentScreenState as? ScreenState.ScoutReport)?.playerId

    fun navigateTo(state: ScreenState) {
        if (navigationStack.lastOrNull() != state) {
            navigationStack.add(state)
        }
    }

    fun navigateBack() {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.size - 1)
        }
    }

    fun selectTab(index: Int) {
        if (index == 0) {
            navigationStack.clear()
            navigationStack.add(ScreenState.Tab(0))
        } else {
            navigationStack.removeAll { it is ScreenState.Tab && it.tabIndex == index }
            navigationStack.removeAll { it !is ScreenState.Tab }
            if (navigationStack.isEmpty() || (navigationStack.first() as? ScreenState.Tab)?.tabIndex != 0) {
                navigationStack.add(0, ScreenState.Tab(0))
            }
            navigationStack.add(ScreenState.Tab(index))
        }
    }

    BackHandler(enabled = navigationStack.size > 1) {
        navigateBack()
    }

    val userRole by viewModel.currentUserRole.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    Scaffold(
        topBar = {
            if (activePlayerId == null && activeClubId == null && activeChatId == null && showWriteScoutReportPlayerId == null) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "FUTBOLCUM",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(NeonCyan)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                val roleLabel = when (userRole) {
                                    UserRole.PLAYER -> "FUTBOLCU"
                                    UserRole.SCOUT -> "GÖZLEMCİ"
                                    UserRole.COACH -> "ANTRENÖR"
                                    UserRole.CLUB -> "KULÜP"
                                    UserRole.MEDIA -> "MEDYA EKİBİ"
                                    UserRole.STORE -> "SPOR MAĞAZASI"
                                    UserRole.PITCH -> "HALI SAHA"
                                    UserRole.ORGANIZER -> "ORGANİZATÖR"
                                    UserRole.ADMIN -> "ADMİN"
                                }
                                Text(
                                    text = roleLabel,
                                    color = AlmostBlack,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    },
                    actions = {
                        // Quick logout button
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Hızlı Çıkış",
                                tint = Color(0xFFFF5252)
                            )
                        }

                        // Quick badge indicator
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSlate)
                                .clickable { selectTab(3) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (notifications.isNotEmpty()) NeonCyan else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            if (notifications.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(top = 6.dp, end = 6.dp)
                                        .clip(CircleShape)
                                        .background(OrangeWarning)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AlmostBlack)
                )
            }
        },
        bottomBar = {
            if (activePlayerId == null && activeClubId == null && activeChatId == null && showWriteScoutReportPlayerId == null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 1.dp, color = Color(0x1AFFFFFF)),
                    color = AlmostBlack,
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(58.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val tabs = mutableListOf(
                            Triple("Ana Sayfa", Icons.Default.Home, 0),
                            Triple("Arama", Icons.Default.Search, 1),
                            Triple("Keşfet", Icons.Default.Explore, 2),
                            Triple("Mesajlar", Icons.Default.Chat, 3),
                            Triple("Profilim", Icons.Default.Person, 4)
                        )
                        if (userRole == UserRole.ADMIN) {
                            tabs.add(Triple("Admin", Icons.Default.AdminPanelSettings, 5))
                        }
                        
                        tabs.forEach { (label, icon, index) ->
                            val isSelected = selectedTab == index
                            val contentColor by animateColorAsState(
                                targetValue = if (isSelected) NeonCyan else TextGray,
                                animationSpec = tween(durationMillis = 150),
                                label = "navColor"
                            )
                            val scaleFactor by animateFloatAsState(
                                targetValue = if (isSelected) 1.05f else 1f,
                                animationSpec = tween(durationMillis = 150),
                                label = "navScale"
                            )
                            
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        selectTab(index)
                                    }
                                    .graphicsLayer {
                                        scaleX = scaleFactor
                                        scaleY = scaleFactor
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = label,
                                    color = contentColor,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    letterSpacing = 0.2.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 4.dp else 0.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = AlmostBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main sub-page routing based on selectedTab
            when (selectedTab) {
                0 -> HomeScreenContent(
                    viewModel = viewModel,
                    onPlayerClick = { navigateTo(ScreenState.PlayerProfile(it)) },
                    onClubClick = { navigateTo(ScreenState.ClubProfile(it)) },
                    onTabSelect = { selectTab(it) },
                    onNavigate = { tab, mode, cat ->
                        if (mode != null) searchInitialMode = mode
                        if (cat != null) exploreInitialCategory = cat
                        selectTab(tab)
                    },
                    onChatClick = { navigateTo(ScreenState.Chat(it)) }
                )
                1 -> SearchScreenContent(
                    viewModel = viewModel,
                    initialSearchMode = searchInitialMode,
                    onPlayerClick = { navigateTo(ScreenState.PlayerProfile(it)) },
                    onClubClick = { navigateTo(ScreenState.ClubProfile(it)) }
                )
                2 -> ExploreHubScreenContent(
                    viewModel = viewModel,
                    initialCategory = exploreInitialCategory,
                    onPlayerClick = { navigateTo(ScreenState.PlayerProfile(it)) }
                )
                3 -> MessagesScreenContent(
                    viewModel = viewModel,
                    onChatClick = { navigateTo(ScreenState.Chat(it)) }
                )
                4 -> SettingsScreenContent(
                    viewModel = viewModel,
                    onLogout = onLogout,
                    userRole = userRole,
                    onPlayerClick = { navigateTo(ScreenState.PlayerProfile(it)) }
                )
                5 -> AdminDashboardScreenContent(
                    viewModel = viewModel
                )
            }

            // High Fidelity Overlay: Player Profile Screen
            activePlayerId?.let { playerId ->
                PlayerProfileDetailOverlay(
                    playerId = playerId,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onStartChat = { player ->
                        val chatId = viewModel.startChatWithPlayer(player)
                        navigateTo(ScreenState.Chat(chatId))
                    },
                    onWriteScoutReport = { pid ->
                        navigateTo(ScreenState.ScoutReport(pid))
                    }
                )
            }

            // High Fidelity Overlay: Club Profile Screen
            activeClubId?.let { clubId ->
                ClubProfileDetailOverlay(
                    clubId = clubId,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onPlayerClick = { playerId ->
                        navigateTo(ScreenState.PlayerProfile(playerId))
                    }
                )
            }

            // High Fidelity Overlay: WhatsApp Quality Chat Screen
            activeChatId?.let { chatId ->
                DirectChatOverlay(
                    chatId = chatId,
                    viewModel = viewModel,
                    onBack = { navigateBack() }
                )
            }

            // High Fidelity Overlay: Scout Evaluation Form Screen
            showWriteScoutReportPlayerId?.let { pid ->
                ScoutReportFormOverlay(
                    playerId = pid,
                    viewModel = viewModel,
                    onBack = { navigateBack() },
                    onSubmit = { navigateBack() }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-PAGE: HOME CONTENT
// -------------------------------------------------------------
@Composable
fun HubTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badge: String? = null,
    badgeColor: Color = NeonCyan,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSlate)
            .border(1.dp, NeonCyan.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
                if (badge != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge.uppercase(),
                            color = badgeColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.3.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = TextGray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    viewModel: MainViewModel,
    onPlayerClick: (String) -> Unit,
    onClubClick: (String) -> Unit,
    onTabSelect: (Int) -> Unit,
    onNavigate: (tabIndex: Int, searchMode: Int?, exploreCategory: String?) -> Unit = { tab, _, _ -> onTabSelect(tab) },
    onChatClick: (String) -> Unit
) {
    val players by viewModel.players.collectAsState()
    val clubs by viewModel.clubs.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val users by viewModel.users.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val currentUser = remember(users, currentUserId) {
        users.find { it.id == currentUserId }
    }

    val sortedUsers = remember(users) {
        users.sortedByDescending { it.createdAt }
    }

    val featuredUser = remember(users) {
        users.filter { it.isFeatured }
            .sortedByDescending { it.featuredAt }
            .firstOrNull()
    }

    var selectedProfileUserForDialog by remember { mutableStateOf<AppUser?>(null) }
    var showAllUsersDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- PREMIUM BRAND HEADER ROW ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EmeraldGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "BAĞLANTI AKTİF • TRANSFER MERKEZİ",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "FUTBOLCUM",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // --- 1. KULLANICI KARŞILAMA ALANI ---
        if (currentUser == null) {
            // Skeleton Loader
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(140.dp)
                        .height(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = 0.15f))
                )
                Box(
                    modifier = Modifier
                        .width(190.dp)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = 0.1f))
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(
                    text = "Merhaba, ${currentUser.name}",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val roleNameLabel = when (currentUser.role) {
                    UserRole.PLAYER -> "Futbolcu Hesabı"
                    UserRole.CLUB -> "Kulüp Hesabı"
                    UserRole.COACH -> "Antrenör Hesabı"
                    UserRole.SCOUT -> "Scout Hesabı"
                    UserRole.MEDIA -> "Medya Hesabı"
                    UserRole.STORE -> "Spor Mağazası"
                    UserRole.PITCH -> "Halı Saha"
                    UserRole.ORGANIZER -> "Turnuva Organizatörü"
                    UserRole.ADMIN -> "Yönetici Hesabı"
                }
                val subtitle = remember(currentUser) {
                    if (currentUser.role == UserRole.PLAYER) {
                        val matchingPlayer = players.find { it.id == currentUser.id }
                        if (matchingPlayer != null) {
                            "${matchingPlayer.city} • ${matchingPlayer.position.shortName}"
                        } else {
                            "${currentUser.city} • $roleNameLabel"
                        }
                    } else if (currentUser.role == UserRole.CLUB && currentUser.club.isNotEmpty()) {
                        "${currentUser.club} • $roleNameLabel"
                    } else {
                        "${currentUser.city} • $roleNameLabel"
                    }
                }
                Text(
                    text = subtitle,
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // --- 2. ROL BAZLI TEK AKILLI KART ---
        if (currentUser != null) {
            SmartActionCard(
                currentUser = currentUser,
                viewModel = viewModel,
                onNavigate = onNavigate
            )
        }

        // --- 3. SON KATILANLAR ---
        SonKatilanlarSection(
            users = sortedUsers,
            viewModel = viewModel,
            onUserClick = { clickedUser ->
                when (clickedUser.role) {
                    UserRole.PLAYER -> onPlayerClick(clickedUser.id)
                    UserRole.CLUB -> onClubClick(clickedUser.id)
                    else -> {
                        selectedProfileUserForDialog = clickedUser
                    }
                }
            },
            onSeeAllClick = {
                showAllUsersDialog = true
            }
        )

        // --- 4. ÖNE ÇIKAN PROFİL ---
        featuredUser?.let { featured ->
            FeaturedProfileSection(
                featuredUser = featured,
                viewModel = viewModel,
                onPlayerClick = onPlayerClick,
                onClubClick = onClubClick,
                onUserClick = { clickedUser ->
                    selectedProfileUserForDialog = clickedUser
                }
            )
        }
    }

    // Dialogs
    selectedProfileUserForDialog?.let { dialogUser ->
        AppUserProfileDialog(
            user = dialogUser,
            viewModel = viewModel,
            onDismiss = { selectedProfileUserForDialog = null },
            onStartChat = { name, role ->
                selectedProfileUserForDialog = null
                val chatId = viewModel.startChatWithUser(name, role)
                onChatClick(chatId)
            }
        )
    }

    if (showAllUsersDialog) {
        AllSonKatilanlarDialog(
            users = sortedUsers,
            viewModel = viewModel,
            onUserClick = { clickedUser ->
                showAllUsersDialog = false
                when (clickedUser.role) {
                    UserRole.PLAYER -> onPlayerClick(clickedUser.id)
                    UserRole.CLUB -> onClubClick(clickedUser.id)
                    else -> {
                        selectedProfileUserForDialog = clickedUser
                    }
                }
            },
            onDismiss = { showAllUsersDialog = false }
        )
    }
}

@Composable
fun ClubHomeCard(
    club: Club,
    onClick: () -> Unit
) {
    PremiumCard(
        modifier = Modifier
            .width(170.dp)
            .height(180.dp),
        onClick = onClick,
        borderGlow = (club.hasLicense || club.trophyCount > 0)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper part: Logo / Avatar
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                if (club.logoUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = club.logoUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(DarkSlate),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = club.name.take(2).uppercase(),
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // Name & City
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = club.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    if (club.hasLicense) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = NeonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Text(
                    text = club.city.uppercase(),
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Badges / Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "👥 ${club.activeStudentsCount}",
                    color = TextGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "🏆 ${club.trophyCount}",
                    color = TextGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Kayıt Durumu Badge
            val registrationsOpen = !club.registrationApplied
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (registrationsOpen) EmeraldGreen.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (registrationsOpen) "Kayıtlar Açık" else "Kayıtlar Kapalı",
                    color = if (registrationsOpen) EmeraldGreen else Color.Red,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun NewPlayerHomeCard(
    player: Player,
    onClick: () -> Unit
) {
    PremiumCard(
        modifier = Modifier
            .width(170.dp)
            .height(180.dp),
        onClick = onClick,
        borderGlow = player.isVerified
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Upper part: Image / Avatar
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                val photoUrlVal = player.photoUrl
                if (!photoUrlVal.isNullOrEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                .data(photoUrlVal)
                                .listener(
                                    onStart = {
                                        android.util.Log.d("FutbolcuBul", "PHOTO URL : $photoUrlVal")
                                    },
                                    onSuccess = { _, _ ->
                                        android.util.Log.d("FutbolcuBul", "IMAGE LOADED")
                                    },
                                    onError = { _, _ ->
                                        android.util.Log.d("FutbolcuBul", "IMAGE FAILED")
                                    }
                                )
                                .build()
                        ),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(DarkSlate),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (player.firstName.take(1) + player.lastName.take(1)).uppercase(),
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // Name & City
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = player.fullName,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    if (player.isVerified) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Verified",
                            tint = NeonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Text(
                    text = player.city.uppercase(),
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Position & Overall Rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = player.position.shortName,
                    color = TextGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "OVR: ${player.overallRating}",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // "Yeni Kayıt" Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonCyan.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "YENİ FUTBOLCU",
                    color = NeonCyan,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun WeeklyPlayersSection(
    players: List<Player>,
    onPlayerClick: (String) -> Unit
) {
    // Only consider players who have real achievements / match results / news
    val playersWithAchievements = remember(players) {
        players.filter { it.achievements.isNotBlank() }
    }

    if (playersWithAchievements.isEmpty()) {
        // If no player has real data, hide the entire section completely
        return
    }

    var selectedCategoryTab by remember { mutableStateOf(0) }
    val categories = listOf("FORVET", "ORTA SAHA", "DEFANS", "KALECİ", "GENÇ YETENEK")

    val categoryPlayers = remember(playersWithAchievements) {
        listOf(
            playersWithAchievements.find { it.position == FootballPosition.ST || it.position == FootballPosition.LW || it.position == FootballPosition.RW } ?: playersWithAchievements.firstOrNull(),
            playersWithAchievements.find { it.position == FootballPosition.CM || it.position == FootballPosition.AM || it.position == FootballPosition.DM } ?: playersWithAchievements.firstOrNull(),
            playersWithAchievements.find { it.position == FootballPosition.CB || it.position == FootballPosition.LB || it.position == FootballPosition.RB } ?: playersWithAchievements.firstOrNull(),
            playersWithAchievements.find { it.position == FootballPosition.GK } ?: playersWithAchievements.firstOrNull(),
            playersWithAchievements.find { it.age < 20 } ?: playersWithAchievements.firstOrNull()
        )
    }

    val activePlayer = categoryPlayers.getOrNull(selectedCategoryTab)

    PremiumCard(
        borderGlow = true,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HAFTANIN ALTIN KADROSU",
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LİG 11'İ",
                    color = Color(0xFFFFD700),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Category Tab Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEachIndexed { index, label ->
                val isSelected = selectedCategoryTab == index
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFFFFD700) else DarkBorder)
                        .clickable { selectedCategoryTab = index }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) AlmostBlack else Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        activePlayer?.let { player ->
            val explanation = player.achievements

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlayerClick(player.id) },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Main Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSlate)
                            .border(1.5.dp, Color(0xFFFFD700), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (player.photoUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(model = player.photoUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextGray, modifier = Modifier.size(24.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = player.fullName,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                        }
                        Text(
                            text = "${getPositionTurkishName(player.position)} | ${player.club}",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFD700)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = player.overallRating.toString(),
                            color = AlmostBlack,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Text(
                    text = explanation,
                    color = TextWhite,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontStyle = FontStyle.Italic
                )

                // --- REALISTIC ATTRIBUTE STATS OVERVIEW ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkBorder, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        "HIZ" to player.stats.pace,
                        "ŞUT" to player.stats.shooting,
                        "PAS" to player.stats.passing,
                        "DRB" to player.stats.dribbling,
                        "DEF" to player.stats.defense,
                        "FİZ" to player.stats.physical
                    ).forEach { (statName, valScore) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = statName,
                                color = TextGray,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = valScore.toString(),
                                color = if (valScore >= 80) Color(0xFFFFD700) else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityFeedSection(
    viewModel: MainViewModel,
    onPlayerClick: (String) -> Unit
) {
    val activityItems by viewModel.activityFeed.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "⚡ SOSYAL MEDYA AKTİVİTE AKIŞI",
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        activityItems.forEach { activity ->
            PremiumCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activity.userName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (activity.userBadge != UserBadge.NONE) {
                                    val badgeColor = when (activity.userBadge) {
                                        UserBadge.SCOUT -> Color(0xFF00E5FF)
                                        UserBadge.VERIFIED_PLAYER -> Color(0xFF00FF88)
                                        UserBadge.CLUB_REPRESENTATIVE -> Color(0xFFFFCC00)
                                        UserBadge.NATIONAL_SCOUT -> Color(0xFFFF3366)
                                        else -> NeonCyan
                                    }
                                    val badgeLabel = when (activity.userBadge) {
                                        UserBadge.SCOUT -> "Scout"
                                        UserBadge.VERIFIED_PLAYER -> "Onaylı"
                                        UserBadge.CLUB_REPRESENTATIVE -> "Kulüp"
                                        UserBadge.NATIONAL_SCOUT -> "Milli Scout"
                                        else -> ""
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(badgeColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = badgeLabel.uppercase(),
                                            color = badgeColor,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${activity.userRole} • ${activity.timeAgo}",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = buildAnnotatedString {
                            append(activity.userName)
                            append(" ")
                            withStyle(style = SpanStyle(color = NeonCyan, fontWeight = FontWeight.Bold)) {
                                append(activity.actionText)
                            }
                            if (activity.detailText.isNotEmpty()) {
                                append(" ")
                                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                    append(activity.detailText)
                                }
                            }
                        },
                        color = TextWhite,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Divider(color = DarkBorder, thickness = 0.5.dp)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.toggleActivityLike(activity.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (activity.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (activity.likedByMe) Color.Red else TextGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${activity.likesCount} Beğeni",
                                color = if (activity.likedByMe) Color.Red else TextGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { 
                                    viewModel.players.value.find { it.fullName == activity.userName }?.let {
                                        onPlayerClick(it.id)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Profili Gör",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-PAGE: SEARCH CONTENT (THE HEART OF THE APP)
// -------------------------------------------------------------
@Composable
fun SearchScreenContent(
    viewModel: MainViewModel,
    initialSearchMode: Int = 0,
    onPlayerClick: (String) -> Unit,
    onClubClick: (String) -> Unit
) {
    val players by viewModel.filteredPlayers.collectAsState()
    val allPlayers by viewModel.players.collectAsState()
    val filters by viewModel.filters.collectAsState()
    var showFiltersDrawer by remember { mutableStateOf(false) }
    var showCityDialog by remember { mutableStateOf(false) }
    var citySearchQuery by remember { mutableStateOf("") }

    var searchMode by remember(initialSearchMode) { mutableStateOf(initialSearchMode) }
    var clubQuery by remember { mutableStateOf("") }

    val isPlayersLoaded by viewModel.isPlayersLoaded.collectAsState()
    var playerSearchQuery by remember { mutableStateOf("") }

    val playerChips = remember(filters) {
        val list = mutableListOf<Pair<String, () -> Unit>>()
        val currentFilters = filters
        val position = currentFilters.position
        if (position != null) {
            list.add("Mevki: ${position.shortName}" to {
                viewModel.updateFilters(currentFilters.copy(position = null))
            })
        }
        if (currentFilters.city.isNotEmpty()) {
            list.add("Şehir: ${currentFilters.city}" to {
                viewModel.updateFilters(currentFilters.copy(city = ""))
            })
        }
        val foot = currentFilters.foot
        if (foot != null) {
            val footTr = when (foot) {
                PreferredFoot.LEFT -> "Sol Ayak"
                PreferredFoot.RIGHT -> "Sağ Ayak"
                PreferredFoot.BOTH -> "İki Ayaklı"
            }
            list.add("Ayak: $footTr" to {
                viewModel.updateFilters(currentFilters.copy(foot = null))
            })
        }
        if (currentFilters.minAge != 6 || currentFilters.maxAge != 50) {
            list.add("Yaş: ${currentFilters.minAge}-${currentFilters.maxAge}" to {
                viewModel.updateFilters(currentFilters.copy(minAge = 6, maxAge = 50))
            })
        }
        if (currentFilters.minHeight != 100 || currentFilters.maxHeight != 220) {
            list.add("Boy: ${currentFilters.minHeight}-${currentFilters.maxHeight} cm" to {
                viewModel.updateFilters(currentFilters.copy(minHeight = 100, maxHeight = 220))
            })
        }
        if (currentFilters.minWeight != 20 || currentFilters.maxWeight != 120) {
            list.add("Kilo: ${currentFilters.minWeight}-${currentFilters.maxWeight} kg" to {
                viewModel.updateFilters(currentFilters.copy(minWeight = 20, maxWeight = 120))
            })
        }
        if (currentFilters.isVerifiedOnly) {
            list.add("Sadece Onaylı" to {
                viewModel.updateFilters(currentFilters.copy(isVerifiedOnly = false))
            })
        }
        list
    }

    val filteredPlayers = remember(players, playerSearchQuery) {
        if (playerSearchQuery.isEmpty()) {
            players
        } else {
            players.filter { player ->
                player.fullName.contains(playerSearchQuery, ignoreCase = true) ||
                player.city.contains(playerSearchQuery, ignoreCase = true) ||
                player.club.contains(playerSearchQuery, ignoreCase = true) ||
                getPositionTurkishName(player.position).contains(playerSearchQuery, ignoreCase = true)
            }
        }
    }

    if (showCityDialog) {
        AlertDialog(
            onDismissRequest = { 
                showCityDialog = false
                citySearchQuery = ""
            },
            title = {
                Text(
                    text = "ŞEHİR SEÇİN",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CustomTextField(
                        value = citySearchQuery,
                        onValueChange = { citySearchQuery = it },
                        label = "Şehir Ara...",
                        testTag = "city_search_input",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                    
                    val filteredCities = TURKEY_CITIES.filter {
                        it.lowercase()
                            .replace('ı', 'i')
                            .replace('ö', 'o')
                            .replace('ü', 'u')
                            .replace('ş', 's')
                            .replace('ç', 'c')
                            .replace('ğ', 'g')
                            .replace('İ', 'i')
                            .contains(
                                citySearchQuery.lowercase()
                                    .replace('ı', 'i')
                                    .replace('ö', 'o')
                                    .replace('ü', 'u')
                                    .replace('ş', 's')
                                    .replace('ç', 'c')
                                    .replace('ğ', 'g')
                                    .replace('İ', 'i')
                            )
                    }
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                    ) {
                        items(filteredCities) { city ->
                            val isSelected = (city == "Tüm Şehirler" && filters.city.isEmpty()) || (city == filters.city)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable {
                                        viewModel.updateFilters(
                                            filters.copy(city = if (city == "Tüm Şehirler") "" else city)
                                        )
                                        showCityDialog = false
                                        citySearchQuery = ""
                                    }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = city,
                                    color = if (isSelected) NeonCyan else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    showCityDialog = false 
                    citySearchQuery = ""
                }) {
                    Text("KAPAT", color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = DarkSlate,
            textContentColor = Color.White
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Sade sayfa başlığı
        Text(
            text = "OYUNCU VE KULÜP KEŞFET",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Oyuncular / Kulüpler sekmeleri
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSlate)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (searchMode == 0) NeonCyan else Color.Transparent)
                    .clickable { searchMode = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚽ OYUNCULAR",
                    color = if (searchMode == 0) AlmostBlack else TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (searchMode == 1) NeonCyan else Color.Transparent)
                    .clickable { searchMode = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🛡️ KULÜPLER",
                    color = if (searchMode == 1) AlmostBlack else TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (searchMode == 0) {
            // Arama alanı ve filtre butonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = playerSearchQuery,
                    onValueChange = { playerSearchQuery = it },
                    placeholder = { Text("Oyuncu adı, şehir, mevki veya kulüp ara", color = TextGray, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (playerSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { playerSearchQuery = "" }, modifier = Modifier.size(36.dp)) {
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

                // Filter button with active count badge
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (showFiltersDrawer) NeonCyan else DarkSlate)
                        .border(1.dp, if (showFiltersDrawer) NeonCyan else DarkBorder, RoundedCornerShape(14.dp))
                        .clickable { showFiltersDrawer = !showFiltersDrawer },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filtreler",
                        tint = if (showFiltersDrawer) AlmostBlack else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    
                    if (playerChips.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (showFiltersDrawer) AlmostBlack else NeonCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = playerChips.size.toString(),
                                color = if (showFiltersDrawer) NeonCyan else AlmostBlack,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }

            // Aktif filtreler varsa filtre chip’leri
            if (playerChips.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(playerChips) { (label, onClear) ->
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
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Expandable Filters Drawer (Apple Design Language Layout)
            AnimatedVisibility(
                visible = showFiltersDrawer,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
            PremiumCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "PROFESYONEL OYUNCU FİLTRELERİ",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Age Filter
                    Text(text = "YAŞ: ${filters.minAge} - ${filters.maxAge}", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    RangeSlider(
                        value = filters.minAge.toFloat()..filters.maxAge.toFloat(),
                        onValueChange = { viewModel.updateFilters(filters.copy(minAge = it.start.toInt(), maxAge = it.endInclusive.toInt())) },
                        valueRange = 6f..50f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Height Filter
                    Text(text = "BOY: ${filters.minHeight} - ${filters.maxHeight} CM", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    RangeSlider(
                        value = filters.minHeight.toFloat()..filters.maxHeight.toFloat(),
                        onValueChange = { viewModel.updateFilters(filters.copy(minHeight = it.start.toInt(), maxHeight = it.endInclusive.toInt())) },
                        valueRange = 100f..220f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Weight Filter
                    Text(text = "KİLO: ${filters.minWeight} - ${filters.maxWeight} KG", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    RangeSlider(
                        value = filters.minWeight.toFloat()..filters.maxWeight.toFloat(),
                        onValueChange = { viewModel.updateFilters(filters.copy(minWeight = it.start.toInt(), maxWeight = it.endInclusive.toInt())) },
                        valueRange = 20f..120f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // City Filter Selector Button
                    Text(text = "ŞEHİR (TÜRKİYE)", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder)
                            .clickable { showCityDialog = true }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (filters.city.isEmpty()) "Tüm Şehirler (Türkiye geneli)" else filters.city,
                            color = if (filters.city.isEmpty()) TextGray else NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (filters.city.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear City",
                                    tint = Color.Red,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { 
                                            viewModel.updateFilters(filters.copy(city = "")) 
                                        }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(
                                  imageVector = Icons.Default.ArrowDropDown,
                                  contentDescription = "Select City",
                                  tint = Color.White,
                                  modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Position Selector Chips
                    Text(text = "FUTBOL MEVKİSİ", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        items(FootballPosition.values()) { pos ->
                            val isSelected = filters.position == pos
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan else DarkBorder)
                                    .clickable {
                                        viewModel.updateFilters(filters.copy(position = if (isSelected) null else pos))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${pos.shortName} (${getPositionTurkishName(pos)})",
                                    color = if (isSelected) AlmostBlack else TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Preferred Foot Selector Chips
                    Text(text = "TERCİH EDİLEN AYAK", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        val footOptions = listOf(
                            null to "Tümü",
                            PreferredFoot.LEFT to "Sol Ayak",
                            PreferredFoot.RIGHT to "Sağ Ayak",
                            PreferredFoot.BOTH to "İki Ayaklı"
                        )
                        items(footOptions.size) { index ->
                            val option = footOptions[index]
                            val footVal = option.first
                            val label = option.second
                            val isSelected = filters.foot == footVal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan else DarkBorder)
                                    .clickable {
                                        viewModel.updateFilters(filters.copy(foot = footVal))
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) AlmostBlack else TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Verified Only Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "SADECE ONAYLI PROFİLLER", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = filters.isVerifiedOnly,
                            onCheckedChange = { viewModel.updateFilters(filters.copy(isVerifiedOnly = it)) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = NeonCyan.copy(alpha = 0.5f))
                        )
                    }

                    // Reset Action
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            text = "SIFIRLA",
                            color = Color.Red,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.resetFilters() }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

        // Live list results
        if (!isPlayersLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(top = 12.dp)
            ) {
                SearchLoadingSkeleton()
            }
        } else if (allPlayers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextGray, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Kayıtlı futbolcu bulunamadı.", color = TextGray, fontSize = 14.sp)
                }
            }
        } else if (filteredPlayers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                SearchEmptyState(
                    title = "Oyuncu bulunamadı.",
                    subtitle = "Arama metnini veya filtrelerini değiştirerek tekrar deneyebilirsin.",
                    onClearAll = {
                        playerSearchQuery = ""
                        viewModel.updateFilters(SearchFilters())
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 12.dp)
            ) {
                items(filteredPlayers) { player ->
                    PlayerDiscoverCard(
                        player = player,
                        onClick = {
                            viewModel.incrementViews(player.id)
                            onPlayerClick(player.id)
                        }
                    )
                }
            }
        }
    } else {
        ClubSearchLayout(
            viewModel = viewModel,
            clubQuery = clubQuery,
            onClubQueryChange = { clubQuery = it },
            onClubClick = onClubClick,
            modifier = Modifier.weight(1f)
        )
    }
}
}

@Composable
fun SearchEmptyState(
    title: String,
    subtitle: String,
    onClearAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Explore,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                color = TextGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onClearAll,
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
}

@Composable
fun SearchLoadingSkeleton() {
    val infiniteTransition = rememberInfiniteTransition(label = "SkeletonAlpha")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSlate)
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
                    .alpha(alpha)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(DarkBorder)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkBorder)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkBorder)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(100.dp)
                                .height(11.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkBorder)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder)
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerDiscoverCard(
    player: Player,
    onClick: () -> Unit
) {
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        borderGlow = player.isVerified,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(DarkBorder),
                contentAlignment = Alignment.Center
            ) {
                if (player.photoUrl != null && player.photoUrl.isNotEmpty()) {
                    Image(
                        painter = rememberAsyncImagePainter(model = player.photoUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = TextGray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = player.fullName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (player.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Doğrulanmış",
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val positionText = getPositionTurkishName(player.position)
                val ageText = if (player.age > 0) "${player.age} Yaş" else ""
                val line2 = when {
                    positionText.isNotEmpty() && ageText.isNotEmpty() -> "$positionText • $ageText"
                    positionText.isNotEmpty() -> positionText
                    ageText.isNotEmpty() -> ageText
                    else -> ""
                }
                if (line2.isNotEmpty()) {
                    Text(
                        text = line2,
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val clubText = if (player.club.isBlank() || player.club.lowercase() == "kulüpsüz") "Kulüpsüz" else player.club
                val line3 = if (player.city.isNotEmpty()) "$clubText • ${player.city}" else clubText
                Text(
                    text = line3,
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                if (player.scoutRating > 0) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SCOUT",
                            color = NeonCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = player.scoutRating.toString(),
                            color = NeonCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-PAGE: TOURNAMENTS & ARENAS (REDESIGNED)
// -------------------------------------------------------------
@Composable
fun ExploreHubScreenContent(
    viewModel: MainViewModel,
    initialCategory: String? = null,
    onPlayerClick: (String) -> Unit
) {
    val exploreItems by viewModel.exploreItems.collectAsState()
    val currentUserRole by viewModel.currentUserRole.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    val categories = listOf(
        "🔥 Haftanın Golü",
        "🏆 Turnuvalar",
        "🎯 Challenge",
        "🏪 Mağazalar",
        "🎥 Medya Ekibi",
        "🎓 Antrenör İlanları",
        "🏫 Futbol Okulları",
        "⚽ Halı Sahalar"
    )
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory ?: "🔥 Haftanın Golü") }
    var selectedDetailItem by remember { mutableStateOf<ExploreItem?>(null) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    var showAddPostDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AlmostBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Screen Header title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "KEŞFET",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Futbol ekosistemindeki tüm fırsatları yakala",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }

                // Elegant Add Posting Button
                Button(
                    onClick = { showAddPostDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = AlmostBlack
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "İlan Ekle",
                        modifier = Modifier.size(16.dp),
                        tint = AlmostBlack
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "İlan Ekle",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Category Horizontal Scroll List
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { category ->
                    val isSelected = selectedCategory == category
                    val bgCol by animateColorAsState(if (isSelected) NeonCyan else DarkSlate)
                    val txtCol by animateColorAsState(if (isSelected) AlmostBlack else Color.White)
                    val borderCol = if (isSelected) NeonCyan else DarkBorder

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bgCol)
                            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            color = txtCol,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Discover Items List
            val filteredItems = exploreItems.filter { it.category == selectedCategory }

            if (selectedCategory == "🔥 Haftanın Golü") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        GoalOfTheWeekVotingSection(viewModel = viewModel)
                    }
                }
            } else if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Bu kategoride henüz ilan bulunmamaktadır.",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredItems) { item ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSlate)
                                .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                                .clickable { selectedDetailItem = item }
                        ) {
                            Column {
                                // Big cover image
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = item.coverUrl),
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Price or reward badge on top right of the image
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(12.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AlmostBlack.copy(alpha = 0.85f))
                                            .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = item.priceOrReward,
                                            color = NeonCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    
                                    // Rating if applicable
                                    if (item.category in listOf("🎥 Medya Ekibi", "🎓 Antrenör İlanları", "🏫 Futbol Okulları", "⚽ Halı Sahalar")) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(12.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(AlmostBlack.copy(alpha = 0.75f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Star,
                                                    contentDescription = "Puan",
                                                    tint = Color(0xFFFFD700),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = item.rating.toString(),
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Card details below image
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = item.title,
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = item.description,
                                        color = TextGray,
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Primary Action Button
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NeonCyan)
                                            .clickable { selectedDetailItem = item }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.buttonText,
                                            color = AlmostBlack,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // FULLSCREEN/OVERLAY DETAIL MODAL DIALOG WITH ANIMATED VISIBILITY
        AnimatedVisibility(
            visible = selectedDetailItem != null,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.fillMaxSize()
        ) {
            val item = selectedDetailItem
            if (item != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AlmostBlack)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Image Header with Back Arrow
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = item.coverUrl),
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Top scrim gradient for close button visibility
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(AlmostBlack.copy(alpha = 0.6f), Color.Transparent)
                                        )
                                    )
                            )
                            // Back Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(16.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(AlmostBlack.copy(alpha = 0.6f))
                                    .clickable { selectedDetailItem = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Geri",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Detail Information Pane
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Category Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = item.category.uppercase(),
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            // Title & Price
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Prize/Reward Highlight Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSlate)
                                    .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "KAZANÇ / BEDEL / FIRSAT",
                                            color = TextGray,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = item.priceOrReward,
                                            color = NeonCyan,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }

                            // About section
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "AÇIKLAMA",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (item.details.isNotEmpty()) item.details else item.description,
                                    color = TextGray,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }

                            // Contact info section
                            if (item.phone.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "İLETİŞİM BİLGİLERİ",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                    
                                    // Phone
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Phone, contentDescription = "Telefon", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = item.phone, color = Color.White, fontSize = 13.sp)
                                    }
                                    
                                    // Instagram
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Place, contentDescription = "Instagram", tint = NeonCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = item.instagram, color = Color.White, fontSize = 13.sp)
                                    }
                                }
                            }

                            // Discount Code Box (Specific to stores)
                            if (item.code.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldGreen.copy(alpha = 0.1f))
                                        .border(1.dp, EmeraldGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "SPONSOR ÖZEL İNDİRİM KODU",
                                                color = TextGray,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = item.code,
                                                color = EmeraldGreen,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Black,
                                                letterSpacing = 1.sp
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(EmeraldGreen)
                                                .clickable {
                                                    // Copy code simulation
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "KODU AL",
                                                color = AlmostBlack,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            // Items List section (For Stores category)
                            if (item.itemsList.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "ÖNE ÇIKAN ÜRÜNLER VE MODELLER",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                    
                                    item.itemsList.forEach { storeItem ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(DarkSlate)
                                                .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                            ) {
                                                Image(
                                                    painter = rememberAsyncImagePainter(model = storeItem.imageUrl),
                                                    contentDescription = storeItem.name,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = storeItem.name,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = storeItem.price,
                                                    color = NeonCyan,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Communication Buttons (WhatsApp, Phone, Instagram)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // WhatsApp (EmeraldGreen)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldGreen)
                                        .clickable {
                                            uriHandler.openUri(item.whatsapp)
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "WHATSAPP",
                                            color = AlmostBlack,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                // Instagram / Arama
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(DarkSlate)
                                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            uriHandler.openUri("tel:${item.phone}")
                                        }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ARAMA YAP",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(30.dp))
                        }
                    }
                }
            }
        }
    }

    // New Post creation dialog
    if (showAddPostDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showAddPostDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AlmostBlack)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YENİ İLAN / DÜKKAN EKLE",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(onClick = { showAddPostDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                        }
                    }

                    var title by remember { mutableStateOf("") }
                    var description by remember { mutableStateOf("") }
                    var priceOrReward by remember { mutableStateOf("") }
                    var phone by remember { mutableStateOf("") }
                    var instagram by remember { mutableStateOf("") }
                    var details by remember { mutableStateOf("") }
                    
                    // Pre-select category based on active tab, unless it's Turnuva/Challenge and user is not admin
                    val defaultCategory = if (currentUserRole != UserRole.ADMIN && (selectedCategory == "🏆 Turnuvalar" || selectedCategory == "🎯 Challenge")) {
                        "🏪 Mağazalar"
                    } else {
                        selectedCategory
                    }
                    var postCategory by remember { mutableStateOf(defaultCategory) }

                    // Preset Cover Templates
                    val templates = listOf(
                        "🏆 Turnuva" to "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=600",
                        "🎯 Challenge" to "https://images.unsplash.com/photo-1431324155629-1a6edd1def2d?q=80&w=600",
                        "🏪 Mağaza" to "https://images.unsplash.com/photo-1551958219-acbc608c6377?q=80&w=600",
                        "🎥 Medya Ekibi" to "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=600",
                        "🎓 Antrenör" to "https://images.unsplash.com/photo-1508847154043-be12a327dc6f?q=80&w=600",
                        "🏫 Futbol Okulu" to "https://images.unsplash.com/photo-1526232761682-d26e03ac148e?q=80&w=600",
                        "⚽ Halı Saha" to "https://images.unsplash.com/photo-1459865264687-595d652de67e?q=80&w=600"
                    )
                    var selectedTemplateIdx by remember { mutableStateOf(2) } // default to store
                    var customCoverUrl by remember { mutableStateOf("") }
                    val activeCoverUrl = if (customCoverUrl.isNotEmpty()) customCoverUrl else templates[selectedTemplateIdx].second

                    var showErrorText by remember { mutableStateOf("") }

                    // Category Title
                    Text(
                        text = "İLAN KATEGORİSİ",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Admin only warning
                    if (currentUserRole != UserRole.ADMIN) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFFFA000).copy(alpha = 0.12f))
                                .border(1.dp, Color(0xFFFFA000).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "⚠️ Turnuvalar ve Challenge'lar sadece admin yetkisine sahip kullanıcılar tarafından oluşturulabilir.",
                                color = Color(0xFFFFA000),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Selectable categories row
                    val allowedCategories = if (currentUserRole == UserRole.ADMIN) categories else categories.filter { it != "🏆 Turnuvalar" && it != "🎯 Challenge" }
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allowedCategories) { cat ->
                            val isSel = postCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSel) NeonCyan else DarkSlate)
                                    .border(1.dp, if (isSel) NeonCyan else DarkBorder, RoundedCornerShape(20.dp))
                                    .clickable { postCategory = cat }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    color = if (isSel) AlmostBlack else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("İlan / Dükkan Başlığı") },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextGray
                        )
                    )

                    // Description Input
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Kısa Açıklama (Örn: UEFA C lisanslı antrenörüm, kulüp arıyorum...)") },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextGray
                        )
                    )

                    // Price/Reward Input
                    OutlinedTextField(
                        value = priceOrReward,
                        onValueChange = { priceOrReward = it },
                        label = { Text("Fiyat / Saatlik Ücret / Ödül (Örn: 1000 TL / Saat)") },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextGray
                        )
                    )

                    // Detailed details Input
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("Detaylı Açıklama / Tecrübeler ve Hizmetler") },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextGray
                        )
                    )

                    // Contact Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefon Numarası") },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextGray
                        )
                    )

                    // Instagram Username
                    OutlinedTextField(
                        value = instagram,
                        onValueChange = { instagram = it },
                        label = { Text("Instagram Kullanıcı Adı (İsteğe Bağlı)") },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextGray
                        )
                    )

                    // Cover Template
                    Text(
                        text = "KAPAK GÖRSELİ ŞABLONU",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(templates.size) { idx ->
                            val tpl = templates[idx]
                            val isSelected = selectedTemplateIdx == idx && customCoverUrl.isEmpty()
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(2.dp, if (isSelected) NeonCyan else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedTemplateIdx = idx
                                        customCoverUrl = ""
                                    }
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(model = tpl.second),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.4f))
                                )
                                Text(
                                    text = tpl.first,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(4.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Custom URL
                    OutlinedTextField(
                        value = customCoverUrl,
                        onValueChange = { customCoverUrl = it },
                        label = { Text("Özel Kapak Görseli URL'si (İsteğe Bağlı)") },
                        textStyle = TextStyle(color = Color.White),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedLabelColor = NeonCyan,
                            unfocusedLabelColor = TextGray
                        )
                    )

                    if (showErrorText.isNotEmpty()) {
                        Text(
                            text = showErrorText,
                            color = Color.Red,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (title.isEmpty() || description.isEmpty() || priceOrReward.isEmpty() || phone.isEmpty()) {
                                showErrorText = "Lütfen başlık, kısa açıklama, ücret/fiyat ve telefon numarası alanlarını doldurun."
                            } else {
                                val itemButtonText = when (postCategory) {
                                    "🏪 Mağazalar" -> "MAĞAZAYI GEZ"
                                    "🎥 Medya Ekibi" -> "REZERVASYON YAP"
                                    "🏫 Futbol Okulları" -> "DETAYLI BİLGİ"
                                    "⚽ Halı Sahalar" -> "SAHA REZERVE ET"
                                    else -> "İLETİŞİME GEÇ"
                                }
                                val formattedInsta = if (instagram.isNotEmpty()) {
                                    if (instagram.startsWith("@")) instagram else "@$instagram"
                                } else "@futbolcum"

                                val newItem = ExploreItem(
                                    id = "custom_" + java.util.UUID.randomUUID().toString().take(6),
                                    category = postCategory,
                                    title = title,
                                    description = description,
                                    coverUrl = activeCoverUrl,
                                    priceOrReward = priceOrReward,
                                    buttonText = itemButtonText,
                                    rating = 4.8,
                                    location = "Kayseri",
                                    phone = phone,
                                    instagram = formattedInsta,
                                    whatsapp = "https://wa.me/${phone.filter { it.isDigit() }}",
                                    details = details,
                                    creatorId = currentUserId
                                )
                                viewModel.addExploreItem(newItem)
                                showAddPostDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = AlmostBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = "İLANI YAYINLA 🚀",
                            color = AlmostBlack,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun GoalOfTheWeekVotingSection(viewModel: MainViewModel) {
    val candidates by viewModel.goalCandidates.collectAsState()
    val hasVotedGlobal by viewModel.hasVotedGoal.collectAsState()
    
    val totalVotes = remember(candidates) {
        val sum = candidates.sumOf { it.voteCount }
        if (sum == 0) 1 else sum
    }

    var showSubmitDialog by remember { mutableStateOf(false) }
    var submitName by remember { mutableStateOf("") }
    var submitPosition by remember { mutableStateOf("") }
    var submitClub by remember { mutableStateOf("") }
    var submitDesc by remember { mutableStateOf("") }
    var submitVideoUrl by remember { mutableStateOf("") }
    var submitSuccess by remember { mutableStateOf(false) }

    // Active video playing simulation state (Id of goal being simulated as playing)
    var activePlayingId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. HERO BANNER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        listOf(DarkNavy, AlmostBlack)
                    )
                )
                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "HAFTANIN GOLÜ OYLAMASI",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Kayseri genelinde amatör ve halı saha maçlarında atılan en spektaküler jeneriklik golleri izle, haftanın golünü sen seç! En çok oy alan gol sahibine haftalık 1.000 TL Değerinde Halı Saha Çeki verilecektir.",
                    color = TextGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📅 Kalan Süre: 2 Gün 14 Saat",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { showSubmitDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = AlmostBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Golümü Gönder 🎥",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // 2. CANDIDATES LIST
        Text(
            text = "BU HAFTANIN ADAY GOLÜ SAHİPLERİ ⚽",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        candidates.forEach { candidate ->
            val percentage = ((candidate.voteCount * 100) / totalVotes).coerceIn(0, 100)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSlate)
                    .border(
                        width = 1.dp,
                        color = if (candidate.hasVoted) NeonCyan.copy(alpha = 0.5f) else DarkBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Player Meta Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(DarkBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                if (candidate.playerPhotoUrl != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(model = candidate.playerPhotoUrl),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = TextGray
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = candidate.playerName,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${candidate.playerPosition} • ${candidate.playerClub}",
                                    color = TextGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (candidate.hasVoted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonCyan.copy(alpha = 0.15f))
                                    .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Oyunuz ✅",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // High Fidelity Interactive Video Player Box
                    val isPlaying = activePlayingId == candidate.id
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AlmostBlack)
                            .clickable {
                                activePlayingId = if (isPlaying) null else candidate.id
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isPlaying) {
                            // Video Cover Thumbnail Placeholder
                            Image(
                                painter = rememberAsyncImagePainter(
                                    model = when (candidate.id) {
                                        "goal_1" -> "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=400"
                                        "goal_2" -> "https://images.unsplash.com/photo-1544698310-74ea9d1c8258?q=80&w=400"
                                        else -> "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=400"
                                    }
                                ),
                                contentDescription = "Video Thumbnail",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.5f)
                            )
                            // Play Button Indicator overlay
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(AlmostBlack.copy(alpha = 0.7f))
                                    .border(1.5.dp, NeonCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Oynat",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        } else {
                            // Animated active playing simulator block
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(NeonCyan.copy(alpha = 0.15f), AlmostBlack)
                                        )
                                    )
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SportsFootball,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "GOL VİDEOSU OYNATILIYOR 🎬",
                                    color = NeonCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "0:12 / 0:30",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                // Real-time linear progress bar
                                LinearProgressIndicator(
                                    progress = { 0.4f },
                                    color = NeonCyan,
                                    trackColor = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f)
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Durdurmak için dokun",
                                    color = TextGray,
                                    fontSize = 9.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }

                    // Description of goal
                    Text(
                        text = "💬 \"${candidate.goalDescription}\"",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        lineHeight = 16.sp
                    )

                    // Vote Actions Block
                    if (!hasVotedGlobal) {
                        Button(
                            onClick = { viewModel.voteForGoal(candidate.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonCyan.copy(alpha = 0.12f),
                                contentColor = NeonCyan
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ThumbUp,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "BU GOLE OY VER ⚡",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        // Results view (Progress bar with statistics)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Mevcut Oy Oranı",
                                    color = TextGray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$percentage% (${candidate.voteCount} Oy)",
                                    color = if (candidate.hasVoted) NeonCyan else Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            LinearProgressIndicator(
                                progress = { percentage / 100f },
                                color = if (candidate.hasVoted) NeonCyan else Color.White.copy(alpha = 0.5f),
                                trackColor = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Submit Goal Candidate Dialog
    if (showSubmitDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showSubmitDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AlmostBlack)
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "HAFTANIN GOLÜNE BAŞVUR",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                        IconButton(onClick = { showSubmitDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Kapat", tint = Color.White)
                        }
                    }

                    if (submitSuccess) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldGreen.copy(alpha = 0.1f))
                                .border(1.dp, EmeraldGreen, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldGreen, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Başvuru Başarılı!", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Gol videonuz incelendikten sonra önümüzdeki haftanın aday golleri listesine eklenecektir.", color = TextGray, fontSize = 11.sp, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        showSubmitDialog = false
                                        submitSuccess = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = AlmostBlack)
                                ) {
                                    Text("KAPAT", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Kayseri genelinde haftanın golü seçmelerine katılmak için maçta çektiğin gol videosunun linkini ve gol detaylarını aşağıdaki forma doldurarak bize gönder!",
                            color = TextGray,
                            fontSize = 12.sp
                        )

                        OutlinedTextField(
                            value = submitName,
                            onValueChange = { submitName = it },
                            label = { Text("Adınız Soyadınız") },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextGray
                            )
                        )

                        OutlinedTextField(
                            value = submitPosition,
                            onValueChange = { submitPosition = it },
                            label = { Text("Pozisyonunuz (Örn: ST, LW, AM)") },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextGray
                            )
                        )

                        OutlinedTextField(
                            value = submitClub,
                            onValueChange = { submitClub = it },
                            label = { Text("Oynadığınız Kulüp / Halı Saha Takımı") },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextGray
                            )
                        )

                        OutlinedTextField(
                            value = submitDesc,
                            onValueChange = { submitDesc = it },
                            label = { Text("Golün Kısa Açıklaması") },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextGray
                            )
                        )

                        OutlinedTextField(
                            value = submitVideoUrl,
                            onValueChange = { submitVideoUrl = it },
                            label = { Text("Video Linki (YouTube, TikTok, Instagram vb.)") },
                            textStyle = TextStyle(color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedLabelColor = NeonCyan,
                                unfocusedLabelColor = TextGray
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (submitName.isNotEmpty() && submitDesc.isNotEmpty()) {
                                    viewModel.submitGoalCandidate(
                                        playerName = submitName,
                                        position = submitPosition.ifBlank { "ST" },
                                        club = submitClub.ifBlank { "Belirtilmemiş" },
                                        goalDesc = submitDesc,
                                        videoUrl = submitVideoUrl,
                                        photoUrl = null
                                    )
                                    submitSuccess = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = AlmostBlack),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("BAŞVURUYU TAMAMLA 🚀", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}






// =============================================================
// SUB-PAGE COMPONENT: SPOR MAĞAZALARI (SPONSORLUK VE EKİPMAN)
// =============================================================
@Composable
fun SportsStoresTabContent(
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var expandedStoreId by remember { mutableStateOf<String?>(null) }
    var copiedCouponCode by remember { mutableStateOf<String?>(null) }

    // Kayseri Local Sponsored Sports Stores data with cleats & gear
    val stores = remember {
        listOf(
            SponsoredStore(
                id = "store_1",
                name = "Kayseri Erciyes Spor Dünyası",
                tag = "ANA SPONSOR",
                tagColor = NeonCyan,
                address = "Cumhuriyet Mah. Millet Cad. No: 12, Melikgazi, Kayseri",
                phone = "+90 352 222 3838",
                instagram = "erciyesspordunyasi",
                description = "Kayseri'nin en köklü spor mağazası. Halı saha kramponları, orijinal Erciyesspor ve büyük kulüp formaları, koruyucu ekipmanlar ve kaliteli toplarda özel indirimler.",
                coupon = "ERCIYES15",
                discount = "%15 İNDİRİM",
                rating = 4.8f,
                products = listOf(
                    StoreProduct(
                        id = "p1_1",
                        name = "Nike Mercurial Vapor 16 'Elite'",
                        originalPrice = "4.850 TL",
                        sponsoredPrice = "4.120 TL",
                        category = "Krampon",
                        description = "Hız ve dar alanda kıvraklık için tasarlandı. Kayseri suni çim zeminlerine %100 uyumludur."
                    ),
                    StoreProduct(
                        id = "p1_2",
                        name = "Puma Future Ultimate FG",
                        originalPrice = "4.200 TL",
                        sponsoredPrice = "3.570 TL",
                        category = "Krampon",
                        description = "Yaratıcı oyun kurucular için mükemmel esneklik ve top kontrolü."
                    ),
                    StoreProduct(
                        id = "p1_3",
                        name = "Erciyesspor Özel Nostalji Forması",
                        originalPrice = "850 TL",
                        sponsoredPrice = "720 TL",
                        category = "Forma",
                        description = "Kayseri futbolunun efsanevi Erciyesspor nostalji forması. Özel nefes alan kumaş."
                    ),
                    StoreProduct(
                        id = "p1_4",
                        name = "Uhlsport Pro Latex Kaleci Eldiveni",
                        originalPrice = "1.100 TL",
                        sponsoredPrice = "935 TL",
                        category = "Ekipman",
                        description = "Halı sahanın en zorlu şutlarında üstün latex tutuşu."
                    )
                )
            ),
            SponsoredStore(
                id = "store_2",
                name = "Kayseri Gözde Spor",
                tag = "KRAMPON SPONSORU",
                tagColor = GoldChampionship,
                address = "Sivas Cad. No: 45, Kocasinan, Kayseri",
                phone = "+90 352 233 4545",
                instagram = "gozdespor_kayseri",
                description = "Dünyaca ünlü markaların (Nike, Adidas, Puma) profesyonel dişli kramponları, kaleci eldivenleri ve halı saha malzemeleri. Kayseri genç yeteneklerine özel taksit imkanları.",
                coupon = "GOZDEKRAMPON",
                discount = "%10 İNDİRİM",
                rating = 4.6f,
                products = listOf(
                    StoreProduct(
                        id = "p2_1",
                        name = "Adidas Predator FT Accuracy",
                        originalPrice = "5.100 TL",
                        sponsoredPrice = "4.590 TL",
                        category = "Krampon",
                        description = "Kusursuz falsolar ve ceza sahası dışı şutlar için özel kauçuk kontrol bölgeleri."
                    ),
                    StoreProduct(
                        id = "p2_2",
                        name = "Nike Phantom GX II Pro",
                        originalPrice = "4.400 TL",
                        sponsoredPrice = "3.960 TL",
                        category = "Krampon",
                        description = "Asimetrik bağcık yapısı ve geniş vuruş yüzeyi ile tam isabet şutlar."
                    ),
                    StoreProduct(
                        id = "p2_3",
                        name = "Adidas League Profesyonel Top",
                        originalPrice = "1.350 TL",
                        sponsoredPrice = "1.215 TL",
                        category = "Ekipman",
                        description = "Dikişsiz yapıştırma dikişler ve rüzgardan etkilenmeyen aerodinamik tasarım."
                    ),
                    StoreProduct(
                        id = "p2_4",
                        name = "Suni Çim Koruyucu Set (Dizlik + Bileklik)",
                        originalPrice = "450 TL",
                        sponsoredPrice = "405 TL",
                        category = "Ekipman",
                        description = "Suni çimde kayarak müdahalelerde yanmaları önleyen özel neopren koruyucular."
                    )
                )
            ),
            SponsoredStore(
                id = "store_3",
                name = "Kayseri Pasaj Spor Pazarı",
                tag = "FORMA VE BASKI ORTAĞI",
                tagColor = EmeraldGreen,
                address = "Meydan Kapalı Çarşı No: 88, Melikgazi, Kayseri",
                phone = "+90 352 221 1212",
                instagram = "pasajsporkayseri",
                description = "Halı saha takımları için forma baskı, özel yelek dikimi, krampon tamiri ve uygun fiyatlı profesyonel spor malzemeleri. Toplu alımlarda ücretsiz forma baskı!",
                coupon = "PASAJBASKI",
                discount = "BEDAVA BASKI",
                rating = 4.5f,
                products = listOf(
                    StoreProduct(
                        id = "p3_1",
                        name = "Lotto Solista 100 Gravity",
                        originalPrice = "2.800 TL",
                        sponsoredPrice = "2.520 TL",
                        category = "Krampon",
                        description = "Hafif yapısıyla sprintlerde yüksek performans sağlayan ekonomik tercih."
                    ),
                    StoreProduct(
                        id = "p3_2",
                        name = "Takım Dijital Forma Paketi (Forma+Şort+Çorap)",
                        originalPrice = "450 TL",
                        sponsoredPrice = "400 TL",
                        category = "Forma",
                        description = "Halı saha takımına özel dijital baskılı set. Minimum 10 adet alımlarda geçerlidir."
                    ),
                    StoreProduct(
                        id = "p3_3",
                        name = "Krampon Diş Tamir ve Bakım Kiti",
                        originalPrice = "300 TL",
                        sponsoredPrice = "270 TL",
                        category = "Bakım",
                        description = "Eriyen suni çim dişlerini korumak ve krampon ömrünü uzatmak için özel cila seti."
                    )
                )
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- SPONSORSHIP INTRODUCTION BANNER ---
        item {
            PremiumCard(
                borderGlow = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "KAYSERİ YEREL SPONSORLUK VE İNDİRİMLER",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.7.sp
                        )
                    }
                    Text(
                        text = "Kayseri'deki seçkin spor mağazalarıyla yaptığımız sponsorluk anlaşmaları sayesinde oyuncularımıza özel indirim kodları ve en yeni ekipman kataloğu listelenmektedir. Alışveriş yapmadan önce indirim kodunuzu almayı unutmayın!",
                        color = TextWhite,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // --- MAIN STORES LIST ---
        items(stores) { store ->
            val isExpanded = expandedStoreId == store.id
            
            PremiumCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                borderGlow = isExpanded
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header Row: Logo/Initial and Store Metadata
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            // Store Icon/Avatar placeholder
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = store.name.uppercase(),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = GoldChampionship, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${store.rating} / 5.0  | Kayseri",
                                        color = TextGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        // Badge Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(store.tagColor.copy(alpha = 0.15f))
                                .border(0.5.dp, store.tagColor, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = store.tag,
                                color = store.tagColor,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Description text
                    Text(
                        text = store.description,
                        color = TextGray,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )

                    // Address, Phone, Instagram metadata
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBorder, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = store.address,
                                color = Color.White,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Telefon: ${store.phone}", color = Color.White, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Instagram: @${store.instagram}", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // COUPON BOX SECTION (High visual feedback code copy!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AlmostBlack)
                            .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(NeonCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = store.discount,
                                    color = NeonCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(text = "SPONSORLU KUPON", color = TextGray, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                Text(text = store.coupon, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        
                        val isCopied = copiedCouponCode == store.coupon
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isCopied) EmeraldGreen else NeonCyan)
                                .clickable {
                                    copiedCouponCode = store.coupon
                                    // Copy to clipboard
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Sponsor Kupon", store.coupon)
                                    clipboard.setPrimaryClip(clip)
                                    
                                    android.widget.Toast.makeText(context, "${store.coupon} Kupon Kodu Kopyalandı!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isCopied) Icons.Default.CheckCircle else Icons.Default.ContentCopy,
                                    contentDescription = null,
                                    tint = AlmostBlack,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isCopied) "KOPYALANDI!" else "KODU AL",
                                    color = AlmostBlack,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }

                    // Bottom Row: Action buttons to dial, instagram or view catalog
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Instagram Action
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkBorder)
                                .clickable {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse("https://instagram.com/${store.instagram}")
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "INSTAGRAM", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        // Quick Call Action
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkBorder)
                                .clickable {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_DIAL,
                                        android.net.Uri.parse("tel:${store.phone}")
                                    )
                                    context.startActivity(intent)
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "ARA", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        // Product Catalog expand/collapse
                        Box(
                            modifier = Modifier
                                .weight(1.5f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isExpanded) AlmostBlack else NeonCyan)
                                .border(1.dp, if (isExpanded) NeonCyan else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable {
                                    expandedStoreId = if (isExpanded) null else store.id
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = if (isExpanded) NeonCyan else AlmostBlack,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isExpanded) "KATALOĞU KAPAT" else "KRAMPON & EKİPMANLAR",
                                    color = if (isExpanded) NeonCyan else AlmostBlack,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }

                    // --- EXPANDED INTERACTIVE CATALOG LAYOUT ---
                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Divider(color = DarkBorder, thickness = 1.dp)
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(4.dp, 12.dp).background(NeonCyan))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SPONSORLU ÜRÜN VE EKİPMAN KATALOĞU",
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            store.products.forEach { product ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AlmostBlack)
                                        .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Product Image Icon
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(DarkBorder),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (product.category) {
                                                "Krampon" -> Icons.Default.LocalFireDepartment
                                                "Forma" -> Icons.Default.WorkspacePremium
                                                else -> Icons.Default.ShoppingCart
                                            },
                                            contentDescription = null,
                                            tint = NeonCyan.copy(alpha = 0.6f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Product Info details
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = product.description,
                                            color = TextGray,
                                            fontSize = 9.sp,
                                            lineHeight = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = product.originalPrice,
                                                color = TextGray,
                                                fontSize = 10.sp,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = product.sponsoredPrice,
                                                color = EmeraldGreen,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Buy/Contact Whatsapp trigger
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(EmeraldGreen)
                                            .clickable {
                                                val msg = "Merhaba, Futbolcum uygulamasından gördüm. ${store.name} mağazanızdaki '${product.name}' (${product.sponsoredPrice}) ürünü hakkında bilgi almak veya sipariş vermek istiyorum."
                                                val intent = android.content.Intent(
                                                    android.content.Intent.ACTION_VIEW,
                                                    android.net.Uri.parse("https://wa.me/${store.phone.replace(" ", "").replace("+", "")}?text=${java.net.URLEncoder.encode(msg, "UTF-8")}")
                                                )
                                                context.startActivity(intent)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "SİPARİŞ",
                                            color = AlmostBlack,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

// Data models for sponsorship stores
private data class SponsoredStore(
    val id: String,
    val name: String,
    val tag: String,
    val tagColor: Color,
    val address: String,
    val phone: String,
    val instagram: String,
    val description: String,
    val coupon: String,
    val discount: String,
    val rating: Float,
    val products: List<StoreProduct>,
    val coverUrl: String = "https://images.unsplash.com/photo-1551958219-acbc608c6377?q=80&w=600"
)

private data class StoreProduct(
    val id: String,
    val name: String,
    val originalPrice: String,
    val sponsoredPrice: String,
    val category: String,
    val description: String
)

private data class RecentWinner(
    val playerId: String,
    val name: String,
    val avatarUrl: String,
    val tournamentName: String,
    val pointsEarned: Int,
    val badgeText: String
)

private data class ArenaMvp(
    val playerId: String,
    val name: String,
    val avatarUrl: String,
    val category: String,
    val badgeColor: Color,
    val description: String
)

private data class WeeklyGoal(
    val title: String,
    val playerName: String,
    val coverUrl: String,
    val youtubeUrl: String
)

private data class DetailedLeaderboardUser(
    val rank: Int,
    val playerId: String,
    val name: String,
    val position: String,
    val rating: Int,
    val matchCount: Int,
    val championshipsCount: Int,
    val winRate: String,
    val scoutInterest: Int,
    val badges: List<String>,
    val avatarUrl: String
)

// -------------------------------------------------------------
// SUB-PAGE: MESSAGES
// -------------------------------------------------------------
@Composable
fun MessagesScreenContent(
    viewModel: MainViewModel,
    onChatClick: (String) -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()

    var selectedSubTab by remember { mutableStateOf(0) } // 0: Sohbetler, 1: Bildirimler

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Upper Sub-Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSlate)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("SOHBETLER", "BİLDİRİMLER").forEachIndexed { index, label ->
                val isSelected = selectedSubTab == index
                val badgeCount = if (index == 0) {
                    chats.count { it.unreadCount > 0 }
                } else {
                    notifications.count { !it.isRead }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) NeonCyan else Color.Transparent)
                        .clickable { selectedSubTab = index }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = label,
                            color = if (isSelected) AlmostBlack else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (badgeCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (isSelected) AlmostBlack else NeonCyan)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = badgeCount.toString(),
                                    color = if (isSelected) NeonCyan else AlmostBlack,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectedSubTab == 0) {
            // Direct Messages Tab
            Text(
                text = "DİREKT MESAJLAR",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (chats.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aktif sohbet bulunmuyor.", color = TextGray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(chats) { chat ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSlate)
                                .clickable { onChatClick(chat.id) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(DarkBorder),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan)
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = chat.otherParticipantName,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = chat.timestamp,
                                        color = TextGray,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = chat.lastMessage,
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (chat.unreadCount > 0) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = chat.unreadCount.toString(),
                                        color = AlmostBlack,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Notifications Tab
            Text(
                text = "BİLDİRİMLER & DAVETİYELER",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Yeni bildirim bulunmuyor.", color = TextGray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(notifications) { notif ->
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (notif.type == NotificationType.CLUB_PLAYER_INVITE || notif.type == NotificationType.CLUB_COACH_INVITE) {
                                                Icons.Default.Group
                                            } else {
                                                Icons.Default.Notifications
                                            },
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = notif.title,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = notif.time,
                                        color = TextGray,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = notif.description,
                                    color = TextGray,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (notif.type == NotificationType.CLUB_PLAYER_INVITE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.rejectPlayerInvite(notif.id) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                                            border = BorderStroke(1.dp, Color(0xFFB71C1C)),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("REDDET", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                viewModel.acceptPlayerInvite(
                                                    notificationId = notif.id,
                                                    clubId = notif.senderId,
                                                    playerId = currentUserId ?: ""
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("KABUL ET", color = AlmostBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else if (notif.type == NotificationType.CLUB_COACH_INVITE) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.rejectCoachInvite(notif.id) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB71C1C)),
                                            border = BorderStroke(1.dp, Color(0xFFB71C1C)),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("REDDET", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                viewModel.acceptCoachInvite(
                                                    notificationId = notif.id,
                                                    clubId = notif.senderId,
                                                    coachId = currentUserId ?: ""
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("KABUL ET", color = AlmostBlack, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(
                                            onClick = { viewModel.markNotificationAsRead(notif.id) },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                            modifier = Modifier.height(26.dp)
                                        ) {
                                            Text("BİLDİRİMİ SİL", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SUB-PAGE: SETTINGS & PLAYER PROFILE EDITING
// -------------------------------------------------------------
@Composable
fun SettingsScreenContent(
    viewModel: MainViewModel,
    onLogout: () -> Unit,
    userRole: UserRole,
    onPlayerClick: (String) -> Unit
) {
    var selectedSubTab by remember { mutableStateOf(if (userRole == UserRole.ADMIN) 1 else 0) }
    val currentUserId by viewModel.currentUserId.collectAsState()
    val players by viewModel.players.collectAsState()
    val users by viewModel.users.collectAsState()
    val isPlayersLoaded by viewModel.isPlayersLoaded.collectAsState()
    val player = players.find { it.id == currentUserId }
    val currentUser = users.find { it.id == currentUserId }

    var showResetDialog by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var resetError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (selectedSubTab == 0 && userRole == UserRole.CLUB) {
            ClubProfileDetailOverlay(
                clubId = currentUserId ?: "",
                viewModel = viewModel,
                onBack = {},
                onPlayerClick = onPlayerClick,
                isOwnProfileTab = true,
                onSubTabChange = { selectedSubTab = it }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (userRole != UserRole.ADMIN) {
                    // Profile & Settings top sub-tab selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSlate)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("PROFİLİM", "AYARLAR").forEachIndexed { index, label ->
                            val isSelected = selectedSubTab == index
                            val roleColor = when (userRole) {
                                UserRole.SCOUT -> Color(0xFFE040FB)
                                UserRole.COACH -> Color(0xFF00E676)
                                UserRole.MEDIA -> Color(0xFFFF9100)
                                UserRole.CLUB -> Color(0xFFFFD700)
                                else -> NeonCyan
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) roleColor else Color.Transparent)
                                    .clickable { selectedSubTab = index }
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

                if (selectedSubTab == 0 && userRole == UserRole.PLAYER) {
                    // PLAYER PROFILE EDITOR
                    LaunchedEffect(currentUserId, players, users, isPlayersLoaded) {
                        viewModel.ensurePlayerProfileExists(currentUserId)
                    }

                    val fallbackPlayer = remember(currentUserId, currentUser) {
                        val nameParts = (currentUser?.name ?: "Futbolcu").split(" ")
                        val firstName = nameParts.firstOrNull() ?: "Futbolcu"
                        val lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""
                        Player(
                            id = currentUserId,
                            firstName = firstName,
                            lastName = lastName,
                            photoUrl = currentUser?.photoUrl ?: "",
                            age = 18,
                            birthDate = "2008-01-01",
                            nationality = "Türkiye",
                            city = currentUser?.city?.ifEmpty { "İstanbul" } ?: "İstanbul",
                            height = 175,
                            weight = 70,
                            preferredFoot = PreferredFoot.RIGHT,
                            position = FootballPosition.ST,
                            club = "Kulüpsüz",
                            jerseyNumber = 10,
                            bio = "Profilinizi düzenleyerek bilgilerinizi güncelleyin.",
                            isVerified = false,
                            stats = PhysicalStats(0, 0, 0, 0, 0, 0)
                        )
                    }

                    val displayPlayer = remember(players, currentUserId, fallbackPlayer) {
                        players.find { it.id == currentUserId } ?: fallbackPlayer
                    }

                    MyProfileTabContent(player = displayPlayer, viewModel = viewModel)
                } else if (selectedSubTab == 0 && (userRole == UserRole.SCOUT || userRole == UserRole.COACH || userRole == UserRole.MEDIA || userRole == UserRole.STORE || userRole == UserRole.PITCH || userRole == UserRole.ORGANIZER)) {
                    // GENERIC PROFILE EDITOR (SCOUT, COACH, MEDIA)
                    currentUser?.let {
                        GenericUserProfileTabContent(user = it, viewModel = viewModel)
                    }
                } else {
                    // STANDARD SETTINGS SCREEN
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Avatar visual
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(DarkSlate)
                                .border(2.dp, NeonCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(40.dp))
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val displayName = currentUser?.name ?: player?.fullName ?: "Futbolcum Profili"
                        Text(text = displayName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        val roleLabel = when (userRole) {
                            UserRole.PLAYER -> "Futbolcu"
                            UserRole.SCOUT -> "Gözlemci"
                            UserRole.COACH -> "Antrenör"
                            UserRole.CLUB -> "Kulüp"
                            UserRole.MEDIA -> "Medya Ekibi"
                            UserRole.STORE -> "Spor Mağazası"
                            UserRole.PITCH -> "Halı Saha"
                            UserRole.ORGANIZER -> "Organizatör"
                            UserRole.ADMIN -> "Yönetici"
                        }
                        Text(text = "Aktif rol: $roleLabel", color = TextGray, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(30.dp))

                        // Options List (Apple design style)
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            SettingsItem(icon = Icons.Default.Language, title = "Dil", value = "Türkçe")
                            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                            SettingsItem(icon = Icons.Default.Notifications, title = "Anlık Bildirimler", value = "Açık")
                            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                            SettingsItem(icon = Icons.Default.Lock, title = "Gizlilik Kilidi", value = "Kapalı")
                            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                            SettingsItem(icon = Icons.Default.Help, title = "Destek & Yardım Merkezi", value = "")
                            Divider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                            SettingsItem(icon = Icons.Default.Info, title = "Futbolcum Hakkında", value = "v1.0.0")
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Test/Developer Tools card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = "GELİŞTİRİCİ & YETKİLİ ARAÇLARI",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }

                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = OrangeWarning,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Tüm Oluşturulanları Sil",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Oluşturulan tüm oyuncuları, kulüpleri, beğenileri ve mesajları temizleyip sıfırdan başlar.",
                                            color = TextGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                NeonButton(
                                    text = if (isResetting) "Sıfırlanıyor..." else "Tüm Ekosistemi Temizle",
                                    onClick = { showResetDialog = true },
                                    enabled = !isResetting,
                                    isSecondary = false,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (resetError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = resetError ?: "",
                                        color = OrangeWarning,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        if (showResetDialog) {
                            AlertDialog(
                                onDismissRequest = { showResetDialog = false },
                                title = {
                                    Text(
                                        text = "EKOSİSTEMİ SIFIRLA?",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                },
                                text = {
                                    Text(
                                        text = "Bu işlem, veritabanındaki tüm oyuncuları, kulüpleri, beğenileri, takip ilişkilerini ve konuşmaları TAMAMEN SİLEREK ekosistemi sıfırlayacaktır. Tüm özel oluşturulmuş kullanıcılar silinecek ve oturumunuz kapatılacaktır.\n\nEmin misiniz?",
                                        color = TextGray,
                                        fontSize = 13.sp
                                    )
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            showResetDialog = false
                                            isResetting = true
                                            resetError = null
                                            viewModel.resetEcosystemData(
                                                onSuccess = {
                                                    isResetting = false
                                                    onLogout()
                                                },
                                                onFailure = { err ->
                                                    isResetting = false
                                                    resetError = "Hata oluştu: ${err.message}"
                                                }
                                            )
                                        }
                                    ) {
                                        Text("Evet, Her Şeyi Sıfırla", color = OrangeWarning, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showResetDialog = false }) {
                                        Text("İptal", color = Color.White)
                                    }
                                },
                                containerColor = DarkSlate,
                                titleContentColor = Color.White,
                                textContentColor = TextGray
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Logout
                        NeonButton(
                            text = "Hesaptan Çıkış Yap",
                            onClick = onLogout,
                            isSecondary = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text(text = value, color = TextGray, fontSize = 12.sp)
    }
}

// -------------------------------------------------------------
// OVERLAY DETAIL: PLAYER PROFILE DETAIL (TRANSFERMARKT STYLE)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerProfileDetailOverlay(
    playerId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onStartChat: (Player) -> Unit,
    onWriteScoutReport: (String) -> Unit
) {
    val players by viewModel.players.collectAsState()
    val player = players.find { it.id == playerId } ?: return
    
    val currentUserId by viewModel.currentUserId.collectAsState()
    val isMyProfile = player.id == currentUserId || player.id == "p_1"
    val tabs = remember(player.id, isMyProfile) {
        if (isMyProfile) {
            listOf("GENEL BAKIŞ", "İSTATİSTİKLER", "ZİYARETÇİLER", "VİDEOLAR", "DEĞERLENDİRMELER", "GEÇMİŞ")
        } else {
            listOf("GENEL BAKIŞ", "İSTATİSTİKLER", "VİDEOLAR", "DEĞERLENDİRMELER", "GEÇMİŞ")
        }
    }
    
    var selectedTabIndex by remember { mutableStateOf(0) }

    val userRole by viewModel.currentUserRole.collectAsState()

    var showAddReviewForm by remember { mutableStateOf(false) }
    var reviewerName by remember { mutableStateOf("") }
    var reviewerRole by remember { mutableStateOf("Futbolcu") }
    var reviewComment by remember { mutableStateOf("") }
    var selectedReviewTag by remember { mutableStateOf("🧠 Orta Sahanın Beyni") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = player.fullName.uppercase(), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = NeonCyan)
                    }
                },
                actions = {
                    // Like button on the profile header
                    IconButton(onClick = { viewModel.toggleLikePlayer(player.id) }) {
                        Icon(
                            imageVector = if (player.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Beğen",
                            tint = if (player.likedByMe) Color.Red else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AlmostBlack)
            )
        },
        containerColor = AlmostBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // EA Sports FC Dynamic FUT Card Top Header
            val calculatedOvr = player.overallRating
            val displayClubName = if (player.club.isBlank() || 
                player.club.trim().lowercase() == "free agent" || 
                player.club.trim().lowercase() == "serbest" || 
                player.club.trim().lowercase() == "kulüpsüz" ||
                player.club.trim() == "Belirtilmemiş"
            ) "Kulüpsüz" else player.club

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlmostBlack)
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FUTPlayerCard(
                    name = player.fullName,
                    position = player.position.shortName,
                    rating = calculatedOvr,
                    nationality = player.nationality,
                    club = displayClubName,
                    stats = player.stats,
                    glow = true,
                    photoUrl = player.photoUrl
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Beautiful sub-row with city, age, foot etc.
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎂 ${player.age} Yaş", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "•", color = TextGray, fontSize = 11.sp)
                    Text(text = "📍 ${player.city.ifBlank { "Kayseri" }}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "•", color = TextGray, fontSize = 11.sp)
                    Text(text = "🦶 ${getFootTurkishName(player.preferredFoot)} Ayak", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (player.isVerified) {
                        Text(text = "•", color = TextGray, fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "Onaylı", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

                    // Display speech bubble status if set
                    if (player.status.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonCyan.copy(alpha = 0.12f))
                                .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "💬 \"${player.status}\"",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Social counts bar (Instagram style)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = player.followsCount.toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text(text = "Takipçi", color = TextGray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = player.followingCount.toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text(text = "Takip Edilen", color = TextGray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = player.likesCount.toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text(text = "Beğeni", color = TextGray, fontSize = 11.sp)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(DarkBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = player.viewsCount.toString(), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                            Text(text = "Görüntüleme", color = TextGray, fontSize = 11.sp)
                        }
                    }

            // Follow Action Buttons if not viewing own profile
            if (!isMyProfile) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    NeonButton(
                        text = if (player.followedByMe) "Takip Ediliyor" else "Takip Et",
                        onClick = { viewModel.toggleFollowPlayer(player.id) },
                        isSecondary = player.followedByMe,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Quick Stats Bar: Self, Scout, Tournament Ratings
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Self Rating
                RatingColumnCard(title = "KENDİ PUANI", value = player.selfRating, description = "Oyuncunun beyan ettiği puan", modifier = Modifier.weight(1f), valueStr = if (player.selfRating > 0) null else "--")
                // Scout Rating
                RatingColumnCard(title = "GÖZLEMCİ", value = player.scoutRating, description = "Doğrulanmış ortalama puan", highlight = true, modifier = Modifier.weight(1f), valueStr = if (player.scoutRating > 0) null else "Henüz değerlendirilmedi")
                // Tournament Rating
                RatingColumnCard(title = "TURNUVA", value = player.tournamentRating, description = "Otomatik arena metriği", modifier = Modifier.weight(1f), valueStr = if (player.tournamentRating > 0) null else "Henüz katılmadı")
            }

            // Horizontal Tab Rows
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = DarkSlate,
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { idx, title ->
                    Tab(
                        selected = selectedTabIndex == idx,
                        onClick = { selectedTabIndex = idx },
                        text = { Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        selectedContentColor = NeonCyan,
                        unselectedContentColor = TextGray
                    )
                }
            }

            // Tab Content rendering
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Adjust index check because tabs size changes dynamically
                val currentTabName = tabs.getOrNull(selectedTabIndex) ?: ""
                
                when (currentTabName) {
                    "GENEL BAKIŞ" -> {
                        // Overview Tab
                        Column {
                            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                                Text(text = "ATLETİK BİYOMETRİK VERİLER", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Boy:", color = TextGray, fontSize = 12.sp)
                                    Text(text = "${player.height} cm", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Kilo:", color = TextGray, fontSize = 12.sp)
                                    Text(text = "${player.weight} kg", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Tercih Edilen Ayak:", color = TextGray, fontSize = 12.sp)
                                    Text(text = getFootTurkishName(player.preferredFoot), color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Aktif Kulüp:", color = TextGray, fontSize = 12.sp)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(text = if (player.club.isEmpty()) "Serbest / Kulüpsüz" else player.club, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        if (isMyProfile && player.club.isNotEmpty() && player.club != "Kulüpsüz" && player.club != "Serbest / Kulüpsüz" && player.club != "Hiçbiri") {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = { viewModel.playerLeaveClub(player.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("KULÜPTEN AYRIL", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Lisans Durumu:", color = TextGray, fontSize = 12.sp)
                                    Text(text = if (player.hasLicense) "Lisanslı (Aktif)" else "Lisansı Yok", color = if (player.hasLicense) EmeraldGreen else Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Piyasa Değeri:", color = TextGray, fontSize = 12.sp)
                                    Text(text = player.marketValue, color = EmeraldGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            if (player.previousClubs.isNotEmpty() || player.achievements.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                                    Text(text = "KULÜP GEÇMİŞİ & BAŞARILAR", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (player.previousClubs.isNotEmpty()) {
                                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                            Text(text = "Önceki Kulüpler:", color = TextGray, fontSize = 11.sp)
                                            Text(text = player.previousClubs, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                    if (player.achievements.isNotEmpty()) {
                                        Column {
                                            Text(text = "Kazanılan Başarılar:", color = TextGray, fontSize = 11.sp)
                                            Text(text = player.achievements, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                                Text(text = "BİYOGRAFİ BEYANI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = player.bio, color = TextGray, fontSize = 12.sp, lineHeight = 18.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val context = androidx.compose.ui.platform.LocalContext.current
                            GallerySection(
                                galleryList = player.gallery,
                                isEditable = isMyProfile,
                                isPremium = player.isPremium,
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
                    }
                    "İSTATİSTİKLER" -> {
                        // Stats Tab (EA stats)
                        Column {
                            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                                Text(text = "FİZİKSEL ÖZELLİK ORANLARI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                StatProgressBar(label = "HIZ", value = player.stats.pace)
                                StatProgressBar(label = "ŞUT", value = player.stats.shooting)
                                StatProgressBar(label = "PAS", value = player.stats.passing)
                                StatProgressBar(label = "DRİBLİNG", value = player.stats.dribbling)
                                StatProgressBar(label = "SAVUNMA", value = player.stats.defense)
                                StatProgressBar(label = "FİZİKSEL", value = player.stats.physical)
                            }
                        }
                    }
                    "ZİYARETÇİLER" -> {
                        // Profile visits & recent viewers list
                        Column {
                            Text(text = "PROFİL ETKİLEŞİM ANALİTİKLERİ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Visual Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSlate)
                                        .border(0.5.dp, DarkBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(text = "BUGÜN", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(text = player.profileVisits.today.toString(), color = NeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSlate)
                                        .border(0.5.dp, DarkBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(text = "SON 7 GÜN", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(text = player.profileVisits.last7Days.toString(), color = NeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSlate)
                                        .border(0.5.dp, DarkBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(text = "SON 30 GÜN", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(text = player.profileVisits.last30Days.toString(), color = NeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(DarkSlate)
                                        .border(0.5.dp, DarkBorder, RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(text = "TOPLAM", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        Text(text = player.profileVisits.total.toString(), color = NeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(text = "SON ZİYARET EDEN GÖZLEMCİLER & KULÜPLER", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(10.dp))

                            if (player.profileViewers.isEmpty()) {
                                Text(text = "Henüz son ziyaretçi verisi bulunmuyor.", color = TextGray, fontSize = 12.sp)
                            } else {
                                player.profileViewers.forEach { viewer ->
                                    PremiumCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(32.dp)
                                                        .clip(CircleShape)
                                                        .background(DarkBorder),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(text = viewer.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        if (viewer.badge != UserBadge.NONE) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(NeonCyan.copy(alpha = 0.15f))
                                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(text = "Scout", color = NeonCyan, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                    Text(text = viewer.role, color = TextGray, fontSize = 10.sp)
                                                }
                                            }
                                            Text(text = viewer.timeAgo, color = TextGray, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "VİDEOLAR" -> {
                        YoutubeVideosSection(
                            videoList = player.youtubeVideos,
                            isEditable = isMyProfile,
                            isPremium = player.isPremium,
                            onAddVideo = { videoUrl ->
                                viewModel.addYoutubeVideo(videoUrl) { _ -> }
                            },
                            onDeleteVideo = { videoUrl ->
                                viewModel.removeYoutubeVideo(videoUrl)
                            }
                        )
                    }
                    "DEĞERLENDİRMELER" -> {
                        // Reviews Tab
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "ONAYLI GÖZLEMCİ RAPORLARI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                                // Scout actions visible to scouts
                                if (userRole == UserRole.SCOUT) {
                                    Text(
                                        text = "+ RAPOR YAZ",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { onWriteScoutReport(player.id) }
                                            .padding(6.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            val hasReports = player.scoutReports.isNotEmpty()
                            if (hasReports) {
                                val avgTechnical = player.scoutReports.map { it.technical }.average().toInt()
                                val avgTactical = player.scoutReports.map { it.tactical }.average().toInt()
                                val avgMental = player.scoutReports.map { it.mental }.average().toInt()
                                val avgPositioning = player.scoutReports.map { it.positioning }.average().toInt()
                                val overallScoutAvg = (avgTechnical + avgTactical + avgMental + avgPositioning) / 4

                                val (gradeText, gradeColor) = when {
                                    overallScoutAvg >= 90 -> "A+ / ELİT PRO" to NeonCyan
                                    overallScoutAvg >= 80 -> "A / PROFESYONEL" to EmeraldGreen
                                    overallScoutAvg >= 70 -> "B / KULÜP SEVİYESİ" to GoldChampionship
                                    overallScoutAvg >= 60 -> "C / GENÇ AKADEMİ" to OrangeWarning
                                    else -> "D / AMATÖR POTANSİYEL" to Color.Gray
                                }

                                PremiumCard(modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                                    Text(
                                        text = "ORTALAMA GÖZLEMCİ PERFORMANS ANALİZİ",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(160.dp)
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            ScoutPerformanceRadarChart(
                                                technical = avgTechnical,
                                                tactical = avgTactical,
                                                mental = avgMental,
                                                positioning = avgPositioning
                                            )
                                        }

                                        Column(
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .padding(start = 12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "GENEL ORALAMA: ", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Text(text = overallScoutAvg.toString(), color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "SCOUT DERECE: ", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                Text(text = gradeText, color = gradeColor, fontSize = 10.sp, fontWeight = FontWeight.Black)
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            MiniStatRow(label = "TEKNİK (TEK)", value = avgTechnical, color = NeonCyan)
                                            MiniStatRow(label = "TAKTİK (TAK)", value = avgTactical, color = NeonCyan)
                                            MiniStatRow(label = "ZİHİNSEL (ZİH)", value = avgMental, color = NeonCyan)
                                            MiniStatRow(label = "POZİSYON (POZ)", value = avgPositioning, color = NeonCyan)
                                        }
                                    }
                                }

                                Text(
                                    text = "BİREYSEL GÖZLEMCİ DEĞERLENDİRMELERİ",
                                    color = TextGray,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }

                            if (player.scoutReports.isEmpty()) {
                                Text(text = "Henüz gözlemci raporu kaydedilmemiş. Gözlemciler, Onaylı Gözlemci olarak giriş yapıp rapor yazabilirler.", color = TextGray, fontSize = 12.sp)
                            } else {
                                player.scoutReports.forEach { report ->
                                    ExpandableScoutReportCard(report = report)
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Divider(color = DarkBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "HALK & SPORCU DEĞERLENDİRMELERİ",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (showAddReviewForm) "KAPAT" else "+ DEĞERLENDİRME YAP",
                                    color = NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { showAddReviewForm = !showAddReviewForm }
                                        .padding(6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (showAddReviewForm) {
                                PremiumCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp)
                                ) {
                                    Text(
                                        text = "SPORCU HAKKINDA DEĞERLENDİRME YAZ",
                                        color = NeonCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    CustomTextField(
                                        value = reviewerName,
                                        onValueChange = { reviewerName = it },
                                        label = "Adınız Soyadınız"
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Rolünüz",
                                        color = TextGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                    ) {
                                        listOf("Futbolcu", "Gözlemci", "Kulüp", "Ziyaretçi").forEach { role ->
                                            val isSelected = reviewerRole == role
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) NeonCyan else DarkBorder)
                                                    .clickable { reviewerRole = role }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = role,
                                                    color = if (isSelected) AlmostBlack else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "Öne Çıkan Özellik Etiketi",
                                        color = TextGray,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(bottom = 12.dp)
                                    ) {
                                        val tagsList = listOf(
                                            "🧠 Orta Sahanın Beyni",
                                            "⚡ Hız Canavarı",
                                            "🎯 Bitirici",
                                            "🪄 Pas Ustası",
                                            "🛡️ Geçilmez Duvar",
                                            "💪 Fizik Gücü",
                                            "🌟 Gelecek Vaat Ediyor",
                                            "🧤 Panter Kaleci"
                                        )
                                        tagsList.forEach { currentTag ->
                                            val isSelected = selectedReviewTag == currentTag
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) NeonCyan else DarkBorder)
                                                    .clickable { selectedReviewTag = currentTag }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = currentTag,
                                                    color = if (isSelected) AlmostBlack else Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    CustomTextField(
                                        value = reviewComment,
                                        onValueChange = { reviewComment = it },
                                        label = "Yorumunuz (Örn: Müthiş oyun zekası...)",
                                        modifier = Modifier.height(80.dp)
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    NeonButton(
                                        text = "DEĞERLENDİRMEYİ YAYINLA",
                                        onClick = {
                                            if (reviewerName.isNotBlank() && reviewComment.isNotBlank()) {
                                                viewModel.addSocialComment(
                                                    playerId = player.id,
                                                    commenterName = reviewerName,
                                                    commenterRole = reviewerRole,
                                                    commenterBadge = if (reviewerRole == "Gözlemci") UserBadge.SCOUT else if (reviewerRole == "Kulüp") UserBadge.CLUB_REPRESENTATIVE else UserBadge.NONE,
                                                    commentText = reviewComment,
                                                    tag = selectedReviewTag
                                                )
                                                reviewerName = ""
                                                reviewComment = ""
                                                showAddReviewForm = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            if (player.communityReviews.isEmpty()) {
                                Text(
                                    text = "Henüz halk değerlendirmesi yapılmamış. İlk değerlendirmeyi siz yazın ve sporcuyu öne çıkarın!",
                                    color = TextGray,
                                    fontSize = 12.sp
                                )
                            } else {
                                player.communityReviews.forEach { review ->
                                    var showReplyInput by remember { mutableStateOf(false) }
                                    var replyText by remember { mutableStateOf("") }

                                    PremiumCard(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 12.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = review.reviewerName,
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    
                                                    if (review.reviewerBadge != UserBadge.NONE) {
                                                        val badgeColor = when (review.reviewerBadge) {
                                                            UserBadge.SCOUT -> Color(0xFF00E5FF)
                                                            UserBadge.VERIFIED_PLAYER -> Color(0xFF00FF88)
                                                            UserBadge.CLUB_REPRESENTATIVE -> Color(0xFFFFCC00)
                                                            UserBadge.NATIONAL_SCOUT -> Color(0xFFFF3366)
                                                            else -> NeonCyan
                                                        }
                                                        val badgeLabel = when (review.reviewerBadge) {
                                                            UserBadge.SCOUT -> "Scout"
                                                            UserBadge.VERIFIED_PLAYER -> "Onaylı"
                                                            UserBadge.CLUB_REPRESENTATIVE -> "Kulüp"
                                                            UserBadge.NATIONAL_SCOUT -> "Milli Scout"
                                                            else -> ""
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(badgeColor.copy(alpha = 0.15f))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = badgeLabel.uppercase(),
                                                                color = badgeColor,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Black
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(NeonCyan.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = review.reviewerRole,
                                                            color = NeonCyan,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = review.date,
                                                    color = TextGray,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFFFD700).copy(alpha = 0.15f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = review.tag,
                                                    color = Color(0xFFFFD700),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = "\"${review.comment}\"",
                                                color = TextWhite,
                                                fontSize = 12.sp,
                                                lineHeight = 16.sp
                                            )

                                            Spacer(modifier = Modifier.height(12.dp))
                                            Divider(color = DarkBorder, thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    // Like Comment
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { viewModel.toggleLikeComment(player.id, review.id) }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (review.likedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                            contentDescription = null,
                                                            tint = if (review.likedByMe) Color.Red else TextGray,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = review.likesCount.toString(),
                                                            color = if (review.likedByMe) Color.Red else TextGray,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // Reply Toggle
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .clickable { showReplyInput = !showReplyInput }
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Reply,
                                                            contentDescription = null,
                                                            tint = TextGray,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = "Cevapla",
                                                            color = TextGray,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    // Report Button
                                                    IconButton(
                                                        onClick = { viewModel.reportComment(player.id, review.id) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Flag,
                                                            contentDescription = "Rapor Et",
                                                            tint = TextGray,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }

                                                    // Delete Button
                                                    val isCommentOwner = review.reviewerName == "Mert Yılmaz"
                                                    val isProfileOwner = player.id == "p_1"
                                                    if (isCommentOwner || isProfileOwner) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        IconButton(
                                                            onClick = { viewModel.deleteComment(player.id, review.id) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Delete,
                                                                contentDescription = "Sil",
                                                                tint = Color.Red,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Reply input
                                            if (showReplyInput) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    CustomTextField(
                                                        value = replyText,
                                                        onValueChange = { replyText = it },
                                                        label = "Cevabınızı yazın...",
                                                        modifier = Modifier.weight(1f).height(40.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    IconButton(
                                                        onClick = {
                                                            if (replyText.isNotBlank()) {
                                                                viewModel.replyToComment(
                                                                    playerId = player.id,
                                                                    commentId = review.id,
                                                                    replierName = "Mert Yılmaz",
                                                                    replierRole = "Oyuncu",
                                                                    replierBadge = UserBadge.VERIFIED_PLAYER,
                                                                    replyText = replyText
                                                                )
                                                                replyText = ""
                                                                showReplyInput = false
                                                            }
                                                        },
                                                        modifier = Modifier.size(32.dp).background(NeonCyan, CircleShape)
                                                    ) {
                                                        Icon(Icons.Default.Send, contentDescription = "Gönder", tint = AlmostBlack, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }

                                            // Existing replies
                                            if (review.replies.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                review.replies.forEach { reply ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(start = 16.dp, top = 6.dp)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(2.dp)
                                                                .height(44.dp)
                                                                .background(NeonCyan.copy(alpha = 0.3f))
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = reply.senderName,
                                                                    color = Color.White,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                if (reply.senderBadge != UserBadge.NONE) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(4.dp))
                                                                            .background(NeonCyan.copy(alpha = 0.15f))
                                                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Text(
                                                                            text = "Onaylı",
                                                                            color = NeonCyan,
                                                                            fontSize = 7.sp,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                    }
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                }
                                                                Text(
                                                                    text = reply.date,
                                                                    color = TextGray,
                                                                    fontSize = 10.sp
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            Text(
                                                                text = reply.text,
                                                                color = TextWhite,
                                                                fontSize = 11.sp,
                                                                lineHeight = 14.sp
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
                    }
                    "GEÇMİŞ" -> {
                        // Tournament History Tab
                        Column {
                            Text(text = "MÜSABAKA MAÇ GEÇMİŞİ", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))

                            if (player.tournamentHistory.isEmpty()) {
                                Text(text = "Henüz müsabaka kaydı bulunmuyor. Arenalarda mücadele ettikçe puanlar otomatik güncellenir.", color = TextGray, fontSize = 12.sp)
                            } else {
                                player.tournamentHistory.forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                            .border(0.5.dp, DarkBorder)
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(text = entry.tournamentTitle, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            val resultTurkish = when (entry.result.uppercase()) {
                                                "WINNER" -> "Kazanan"
                                                "FINALIST" -> "Finalist"
                                                "SEMI-FINALIST" -> "Yarı Finalist"
                                                "RUNNER-UP" -> "İkinci"
                                                else -> entry.result
                                            }
                                            Text(text = "Sonuç: $resultTurkish | Tarih: ${entry.date}", color = TextGray, fontSize = 11.sp)
                                        }
                                        Text(text = "${entry.matchRating} RTG", color = NeonCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // CTA Footers: Chat / Bookmarks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bookmark favorite
                IconButton(
                    onClick = { viewModel.toggleFavorite(player.id) },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSlate)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                ) {
                    val favIds by viewModel.favoritePlayerIds.collectAsState()
                    val isFavorite = favIds.contains(player.id)
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (isFavorite) NeonCyan else Color.White
                    )
                }

                // Chat button
                NeonButton(
                    text = "Sohbet Başlat",
                    onClick = { onStartChat(player) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun RatingColumnCard(
    title: String,
    value: Int,
    description: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
    valueStr: String? = null
) {
    val bgBrush = if (highlight) {
        Brush.verticalGradient(
            colors = listOf(
                DarkSlate,
                NeonCyan.copy(alpha = 0.12f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                DarkSlate,
                DarkNavy
            )
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(bgBrush)
            .border(
                width = 1.dp,
                color = if (highlight) NeonCyan.copy(alpha = 0.8f) else DarkBorder,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title.uppercase(),
                color = if (highlight) NeonCyan else TextGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            val displayValue = valueStr ?: (if (value > 0) value.toString() else "--")
            val valFontSize = if (displayValue.length > 3) 10.sp else 24.sp
            Text(
                text = displayValue,
                color = if (highlight) NeonCyan else Color.White,
                fontSize = valFontSize,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = TextGray,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun StatProgressBar(label: String, value: Int) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = value.toString(), color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { value / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape),
            color = NeonCyan,
            trackColor = DarkBorder,
            strokeCap = StrokeCap.Round
        )
    }
}

// -------------------------------------------------------------
// OVERLAY DETAIL: DIRECT CHAT (WHATSAPP QUALITY)
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectChatOverlay(
    chatId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val chats by viewModel.chats.collectAsState()
    val chat = chats.find { it.id == chatId } ?: return
    var messageText by remember { mutableStateOf("") }
    val scrollState = rememberLazyListState()

    // Scroll to bottom on load
    LaunchedEffect(chat.messages.size) {
        if (chat.messages.isNotEmpty()) {
            scrollState.animateScrollToItem(chat.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = NeonCyan)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = chat.otherParticipantName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val roleTurkish = when (chat.otherParticipantRole.uppercase()) {
                                "SCOUT" -> "Lisanslı Gözlemci"
                                "PLAYER" -> "Profesyonel Oyuncu"
                                "CLUB" -> "Kulüp Yetkilisi"
                                "ADMIN" -> "Yönetici"
                                else -> chat.otherParticipantRole
                            }
                            Text(text = roleTurkish, color = NeonCyan, fontSize = 10.sp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AlmostBlack)
            )
        },
        containerColor = AlmostBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Messages bubble log
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                if (chat.messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Henüz mesaj bulunmuyor.", color = TextGray)
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(chat.messages) { msg ->
                            val isMe = msg.senderId == "me"
                            val align = if (isMe) Alignment.End else Alignment.Start
                            val bubbleColor = if (isMe) NeonCyan else DarkSlate
                            val textColor = if (isMe) AlmostBlack else Color.White

                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
                                Box(
                                    modifier = Modifier
                                        .clip(
                                            RoundedCornerShape(
                                                topStart = 12.dp,
                                                topEnd = 12.dp,
                                                bottomStart = if (isMe) 12.dp else 0.dp,
                                                bottomEnd = if (isMe) 0.dp else 12.dp
                                            )
                                        )
                                        .background(bubbleColor)
                                        .padding(12.dp)
                                        .widthIn(max = 240.dp)
                                ) {
                                    Text(text = msg.text, color = textColor, fontSize = 13.sp)
                                }
                                Text(
                                    text = msg.timestamp,
                                    color = TextGray,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom message box with dynamic send button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(AlmostBlack),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text(text = "Mesajınızı yazın...", color = TextGray, fontSize = 13.sp) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = DarkSlate,
                        unfocusedContainerColor = DarkSlate,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, DarkBorder, RoundedCornerShape(24.dp)),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                viewModel.sendChatMessage(chat.id, messageText)
                                messageText = ""
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = NeonCyan)
                        }
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// OVERLAY DETAIL: SCOUT EVALUATION FORM
// -------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoutReportFormOverlay(
    playerId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    val players by viewModel.players.collectAsState()
    val player = players.find { it.id == playerId } ?: return

    var technical by remember { mutableStateOf(80) }
    var tactical by remember { mutableStateOf(80) }
    var mental by remember { mutableStateOf(80) }
    var positioning by remember { mutableStateOf(80) }
    var comment by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "GÖZLEMCİ DEĞERLENDİRME TASLAĞI", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AlmostBlack)
            )
        },
        containerColor = AlmostBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(text = "Oyuncu: ${player.fullName}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(text = "Rapor yetkili gözlemci statüsünde düzenlenmektedir.", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp))

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                ScoutRatingSlider(label = "TEKNİK PUAN (0-99)", value = technical, onValueChange = { technical = it })
                ScoutRatingSlider(label = "TAKTİK PUAN (0-99)", value = tactical, onValueChange = { tactical = it })
                ScoutRatingSlider(label = "ZİHİNSEL PUAN (0-99)", value = mental, onValueChange = { mental = it })
                ScoutRatingSlider(label = "POZİSYON ALMA PUANI (0-99)", value = positioning, onValueChange = { positioning = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            CustomTextField(
                value = comment,
                onValueChange = { comment = it },
                label = "Gözlemci Yorumları & Genel Değerlendirme",
                modifier = Modifier.height(110.dp),
                testTag = "scout_comments_input"
            )

            Spacer(modifier = Modifier.height(30.dp))

            NeonButton(
                text = "RAPORU ONAYLA VE GÖNDER",
                onClick = {
                    viewModel.addScoutReport(
                        playerId = playerId,
                        technical = technical,
                        tactical = tactical,
                        mental = mental,
                        positioning = positioning,
                        comment = comment.ifEmpty { "Olağanüstü savunma sezgisi ve teknik soğukkanlılık. Profesyonel akademi düzeyinde kesinlikle tavsiye edilir." }
                    )
                    onSubmit()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ScoutRatingSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = value.toString(), color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..99f,
            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
        )
    }
}

fun getPositionTurkishName(pos: FootballPosition): String {
    return when (pos) {
        FootballPosition.GK -> "Kaleci"
        FootballPosition.RB -> "Sağ Bek"
        FootballPosition.LB -> "Sol Bek"
        FootballPosition.CB -> "Stoper"
        FootballPosition.DM -> "Ön Libero"
        FootballPosition.CM -> "Merkez Orta Saha"
        FootballPosition.AM -> "On Numara"
        FootballPosition.RW -> "Sağ Kanat"
        FootballPosition.LW -> "Sol Kanat"
        FootballPosition.SS -> "Gizli Forvet"
        FootballPosition.ST -> "Santrafor"
    }
}

fun getFootTurkishName(foot: PreferredFoot): String {
    return when (foot) {
        PreferredFoot.RIGHT -> "Sağ Ayak"
        PreferredFoot.LEFT -> "Sol Ayak"
        PreferredFoot.BOTH -> "İki Ayaklı"
    }
}

val TURKEY_CITIES = listOf(
    "Tüm Şehirler",
    "Adana", "Adıyaman", "Afyonkarahisar", "Ağrı", "Amasya", "Ankara", "Antalya", "Artvin", "Aydın",
    "Balıkesir", "Bilecik", "Bingöl", "Bitlis", "Bolu", "Burdur", "Bursa", "Çanakkale", "Çankırı",
    "Çorum", "Denizli", "Diyarbakır", "Edirne", "Elazığ", "Erzincan", "Erzurum", "Eskişehir",
    "Gaziantep", "Giresun", "Gümüşhane", "Hakkari", "Hatay", "Isparta", "Mersin", "İstanbul", "İzmir",
    "Kars", "Kastamonu", "Kayseri", "Kırklareli", "Kırşehir", "Kocaeli", "Konya", "Kütahya",
    "Malatya", "Manisa", "Kahramanmaraş", "Mardin", "Muğla", "Muş", "Nevşehir", "Niğde", "Ordu",
    "Rize", "Sakarya", "Samsun", "Siirt", "Sinop", "Sivas", "Tekirdağ", "Tokat", "Trabzon",
    "Tunceli", "Şanlıurfa", "Uşak", "Van", "Yozgat", "Zonguldak", "Aksaray", "Bayburt", "Karaman",
    "Kırıkkale", "Batman", "Şırnak", "Bartın", "Ardahan", "Iğdır", "Yalova", "Karabük", "Kilis",
    "Osmaniye", "Düzce"
)

@Composable
fun PlayerRadarChart(
    pace: Int,
    shooting: Int,
    passing: Int,
    dribbling: Int,
    defense: Int,
    physical: Int,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(150.dp)
            .padding(10.dp)
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = size.width / 2f

        // Draw concentric hexagon rings
        val rings = listOf(0.25f, 0.5f, 0.75f, 1f)
        rings.forEach { scale ->
            val path = Path().apply {
                for (i in 0..5) {
                    val angle = -Math.PI / 2 + i * Math.PI / 3
                    val r = maxRadius * scale
                    val x = cx + r * Math.cos(angle).toFloat()
                    val y = cy + r * Math.sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.15f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }

        // Draw radial grid lines
        for (i in 0..5) {
            val angle = -Math.PI / 2 + i * Math.PI / 3
            val x = cx + maxRadius * Math.cos(angle).toFloat()
            val y = cy + maxRadius * Math.sin(angle).toFloat()
            drawLine(
                color = Color.White.copy(alpha = 0.15f),
                start = Offset(cx, cy),
                end = Offset(x, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw actual stats polygon
        val statsPath = Path().apply {
            val values = listOf(
                pace.toFloat() / 100f,
                dribbling.toFloat() / 100f,
                passing.toFloat() / 100f,
                physical.toFloat() / 100f,
                defense.toFloat() / 100f,
                shooting.toFloat() / 100f
            )
            for (i in 0..5) {
                val angle = -Math.PI / 2 + i * Math.PI / 3
                val r = maxRadius * values[i].coerceIn(0.1f, 1f)
                val x = cx + r * Math.cos(angle).toFloat()
                val y = cy + r * Math.sin(angle).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }

        drawPath(
            path = statsPath,
            color = NeonCyan.copy(alpha = 0.25f)
        )
        drawPath(
            path = statsPath,
            color = NeonCyan,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )

        // Dots on vertices
        val values = listOf(
            pace.toFloat() / 100f,
            dribbling.toFloat() / 100f,
            passing.toFloat() / 100f,
            physical.toFloat() / 100f,
            defense.toFloat() / 100f,
            shooting.toFloat() / 100f
        )
        for (i in 0..5) {
            val angle = -Math.PI / 2 + i * Math.PI / 3
            val r = maxRadius * values[i].coerceIn(0.1f, 1f)
            val x = cx + r * Math.cos(angle).toFloat()
            val y = cy + r * Math.sin(angle).toFloat()
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun StatPillRow(label: String, value: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(text = value.toString(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { value.toFloat() / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = color,
                trackColor = AlmostBlack
            )
        }
    }
}

@Composable
fun ScoutPerformanceRadarChart(
    technical: Int,
    tactical: Int,
    mental: Int,
    positioning: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(170.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // The radar polygon and concentric rings
        Canvas(
            modifier = Modifier.size(110.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxRadius = size.width / 2f

            // Draw concentric diamond rings (4 axes)
            val rings = listOf(0.25f, 0.5f, 0.75f, 1f)
            rings.forEach { scale ->
                val path = Path().apply {
                    // Top (Technical)
                    moveTo(cx, cy - maxRadius * scale)
                    // Right (Tactical)
                    lineTo(cx + maxRadius * scale, cy)
                    // Bottom (Mental)
                    lineTo(cx, cy + maxRadius * scale)
                    // Left (Positioning)
                    lineTo(cx - maxRadius * scale, cy)
                    close()
                }
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.12f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                )
            }

            // Draw diagonal grid lines
            drawLine(color = Color.White.copy(alpha = 0.12f), start = Offset(cx, cy - maxRadius), end = Offset(cx, cy + maxRadius), strokeWidth = 1.dp.toPx())
            drawLine(color = Color.White.copy(alpha = 0.12f), start = Offset(cx - maxRadius, cy), end = Offset(cx + maxRadius, cy), strokeWidth = 1.dp.toPx())

            // Calculate actual points (scaled 0..100)
            val techY = cy - maxRadius * (technical.toFloat() / 100f).coerceIn(0.1f, 1f)
            val tactX = cx + maxRadius * (tactical.toFloat() / 100f).coerceIn(0.1f, 1f)
            val mentY = cy + maxRadius * (mental.toFloat() / 100f).coerceIn(0.1f, 1f)
            val posX = cx - maxRadius * (positioning.toFloat() / 100f).coerceIn(0.1f, 1f)

            val statsPath = Path().apply {
                moveTo(cx, techY)
                lineTo(tactX, cy)
                lineTo(cx, mentY)
                lineTo(posX, cy)
                close()
            }

            // Draw translucent cyan filled area
            drawPath(
                path = statsPath,
                color = NeonCyan.copy(alpha = 0.2f)
            )
            // Outer stroke
            drawPath(
                path = statsPath,
                color = NeonCyan,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // Draw vertices dots
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(cx, techY))
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(tactX, cy))
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(cx, mentY))
            drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(posX, cy))
        }

        // Surrounding text labels
        Text(
            text = "TEK",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.TopCenter)
        )
        Text(
            text = "TAK",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
        Text(
            text = "ZİH",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        Text(
            text = "POZ",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterStart)
        )
    }
}

@Composable
fun ExpandableScoutReportCard(report: ScoutReport) {
    var isExpanded by remember { mutableStateOf(false) }
    
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable { isExpanded = !isExpanded }
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = report.scoutName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(GoldChampionship.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = report.scoutBadge,
                                color = GoldChampionship,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "RAPOR NO: SR-${report.id.take(6).uppercase()} • TARİH: ${report.date}",
                        color = TextGray,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                // Average Rating Bubble
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonCyan),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = report.averageRating.toString(),
                        color = AlmostBlack,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = "\"${report.comment}\"",
                color = TextWhite,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontStyle = FontStyle.Italic
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Expand / Collapse Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isExpanded) "DETAYLI PARAMETRELERİ GİZLE ▲" else "DETAYLI PARAMETRELERİ GÖSTER ▼",
                    color = NeonCyan,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0x11FFFFFF), thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MiniStatRow(label = "TEKNİK BECERİ", value = report.technical, color = NeonCyan)
                    MiniStatRow(label = "TAKTAK DİSİPLİN", value = report.tactical, color = NeonCyan)
                    MiniStatRow(label = "ZİHİNSEL GÜÇ / OYUN AKLI", value = report.mental, color = NeonCyan)
                    MiniStatRow(label = "POZİSYON ALMA & MOBİLİTE", value = report.positioning, color = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun MiniStatRow(label: String, value: Int, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { value.toFloat() / 100f },
                modifier = Modifier
                    .width(70.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp)),
                color = color,
                trackColor = AlmostBlack
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value.toString(),
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.width(16.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
fun GenericUserProfileTabContent(
    user: AppUser,
    viewModel: MainViewModel
) {
    var isEditMode by remember { mutableStateOf(false) }
    var showEditStudio by remember { mutableStateOf(false) }
    var editedPhotoUrlState by remember { mutableStateOf<String?>(null) }
    var showSuccessMessage by remember { mutableStateOf(false) }

    // Form States
    var nameState by remember { mutableStateOf(user.name) }
    var cityState by remember { mutableStateOf(user.city) }
    var bioState by remember { mutableStateOf(user.bio) }
    var instagramState by remember { mutableStateOf(user.instagram) }
    var phoneState by remember { mutableStateOf(user.phone) }
    var achievementsState by remember { mutableStateOf(user.achievements) }

    // Role-specific States
    var servicesState by remember { mutableStateOf(user.services) }
    var priceState by remember { mutableStateOf(user.price) }
    var licenseState by remember { mutableStateOf(user.license) }
    var dutyState by remember { mutableStateOf(user.duty) }
    var clubState by remember { mutableStateOf(user.club) }

    // Pro Stats States
    var stat1State by remember { mutableStateOf(user.stat1) }
    var stat2State by remember { mutableStateOf(user.stat2) }
    var stat3State by remember { mutableStateOf(user.stat3) }
    var stat4State by remember { mutableStateOf(user.stat4) }
    var stat5State by remember { mutableStateOf(user.stat5) }
    var stat6State by remember { mutableStateOf(user.stat6) }

    LaunchedEffect(user) {
        nameState = user.name
        cityState = user.city
        bioState = user.bio
        instagramState = user.instagram
        phoneState = user.phone
        achievementsState = user.achievements
        servicesState = user.services
        priceState = user.price
        licenseState = user.license
        dutyState = user.duty
        clubState = user.club
        stat1State = user.stat1
        stat2State = user.stat2
        stat3State = user.stat3
        stat4State = user.stat4
        stat5State = user.stat5
        stat6State = user.stat6
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    val roleColor = when (user.role) {
        UserRole.SCOUT -> Color(0xFFE040FB)
        UserRole.COACH -> Color(0xFF00E676)
        UserRole.MEDIA -> Color(0xFFFF9100)
        UserRole.STORE -> Color(0xFFFFEA00)
        UserRole.PITCH -> Color(0xFF00E5FF)
        UserRole.ORGANIZER -> Color(0xFFFF3D00)
        else -> NeonCyan
    }

    val roleLabel = when (user.role) {
        UserRole.SCOUT -> "LİSANSLI SCOUT"
        UserRole.COACH -> "ANTRENÖR"
        UserRole.MEDIA -> "MEDYA EKİBİ"
        UserRole.STORE -> "SPOR MAĞAZASI"
        UserRole.PITCH -> "HALI SAHA"
        UserRole.ORGANIZER -> "ORGANİZATÖR"
        else -> user.role.name
    }

    Crossfade(targetState = isEditMode, animationSpec = tween(500)) { edit ->
        if (edit) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "✏️ PROFİLİ DÜZENLE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { isEditMode = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, roleColor, CircleShape)
                        .clickable { showEditStudio = true }
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = editedPhotoUrlState ?: user.photoUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                Text(
                    text = "Profil resmini değiştirmek için tıklayın",
                    color = TextGray,
                    fontSize = 11.sp
                )

                if (showSuccessMessage) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, EmeraldGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Profiliniz başarıyla güncellendi!",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("GENEL BİLGİLER", color = roleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    
                    CustomTextField(
                        value = nameState,
                        onValueChange = { nameState = it },
                        label = "Ad Soyad",
                        testTag = "edit_profile_name",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    CustomTextField(
                        value = cityState,
                        onValueChange = { cityState = it },
                        label = "Şehir",
                        testTag = "edit_profile_city",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    CustomTextField(
                        value = bioState,
                        onValueChange = { bioState = it },
                        label = "Kısa Biyografi / Kendinizden Bahsedin",
                        testTag = "edit_profile_bio",
                        modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 8.dp)
                    )
                }

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("İLETİŞİM BİLGİLERİ", color = roleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    
                    CustomTextField(
                        value = instagramState,
                        onValueChange = { instagramState = it },
                        label = "Instagram Kullanıcı Adı (Örn: transfermarkt)",
                        testTag = "edit_profile_instagram",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )

                    CustomTextField(
                        value = phoneState,
                        onValueChange = { phoneState = it },
                        label = "Telefon / WhatsApp",
                        testTag = "edit_profile_phone",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    )
                }

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("UZMANLIK VE KARİYER BİLGİLERİ", color = roleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    
                    if (user.role == UserRole.SCOUT) {
                        CustomTextField(
                            value = clubState,
                            onValueChange = { clubState = it },
                            label = "Çalıştığınız Kulüp / Kurum",
                            testTag = "edit_profile_club",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        CustomTextField(
                            value = dutyState,
                            onValueChange = { dutyState = it },
                            label = "Görev Tanımı (Örn: Altyapı Scout)",
                            testTag = "edit_profile_duty",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    if (user.role == UserRole.COACH) {
                        CustomTextField(
                            value = clubState,
                            onValueChange = { clubState = it },
                            label = "Çalıştığınız Takım",
                            testTag = "edit_profile_club",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        CustomTextField(
                            value = licenseState,
                            onValueChange = { licenseState = it },
                            label = "Antrenörlük Lisansı (Örn: UEFA A)",
                            testTag = "edit_profile_license",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    if (user.role == UserRole.MEDIA) {
                        CustomTextField(
                            value = servicesState,
                            onValueChange = { servicesState = it },
                            label = "Sunulan Hizmetler (Virgülle ayırın)",
                            testTag = "edit_profile_services",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        CustomTextField(
                            value = priceState,
                            onValueChange = { priceState = it },
                            label = "Başlangıç Fiyatı (Örn: 1500 TL)",
                            testTag = "edit_profile_price",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    if (user.role == UserRole.STORE) {
                        CustomTextField(
                            value = achievementsState,
                            onValueChange = { achievementsState = it },
                            label = "Website (İsteğe Bağlı)",
                            testTag = "edit_profile_achievements",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    if (user.role == UserRole.PITCH) {
                        CustomTextField(
                            value = servicesState,
                            onValueChange = { servicesState = it },
                            label = "Çalışma Saatleri (Örn: 08:00 - 02:00)",
                            testTag = "edit_profile_services",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                        CustomTextField(
                            value = priceState,
                            onValueChange = { priceState = it },
                            label = "Tek Maç Ücreti (Örn: 1500 TL)",
                            testTag = "edit_profile_price",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    if (user.role == UserRole.ORGANIZER) {
                        CustomTextField(
                            value = servicesState,
                            onValueChange = { servicesState = it },
                            label = "Organizasyon Türü (Örn: Turnuva, Lig, Halı Saha)",
                            testTag = "edit_profile_services",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }

                    if (user.role != UserRole.STORE && user.role != UserRole.PITCH && user.role != UserRole.ORGANIZER) {
                        CustomTextField(
                            value = achievementsState,
                            onValueChange = { achievementsState = it },
                            label = if (user.role == UserRole.MEDIA) "Referanslar / Önceki Çalışmalar" else "Kariyer Başarıları & Sertifikalar",
                            testTag = "edit_profile_achievements",
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                    }
                }

                val labelsList = when (user.role) {
                    UserRole.SCOUT -> listOf(
                        "Yetenek / Talent (TAL)",
                        "Raporlama / Report (REP)",
                        "Network / Ağ (NET)",
                        "Gözlem / Eye (EYE)",
                        "Tecrübe / Experience (EXP)",
                        "Önsezi / Intuition (INT)"
                    )
                    UserRole.COACH -> listOf(
                        "Taktik / Tactics (TAC)",
                        "Antrenman / Training (TRN)",
                        "Yönetim / Man Management (MAN)",
                        "Analiz / Analysis (ALY)",
                        "Disiplin / Discipline (DIS)",
                        "Psikoloji / Psychology (PSY)"
                    )
                    UserRole.MEDIA -> listOf(
                        "Fotoğrafçılık / Photography (PHO)",
                        "Videografi / Videography (VID)",
                        "Drone Kullanımı / Drone (DRO)",
                        "Kurgu / Editing (EDT)",
                        "Portfolyo Gücü / Portfolio (PRT)",
                        "Saha Tecrübesi / Experience (EXP)"
                    )
                    UserRole.STORE -> listOf(
                        "Stok Gücü / Stock (STK)",
                        "Fiyat Avantajı / Price (PRC)",
                        "Müşteri Hizmetleri / Support (SUP)",
                        "Gönderim Hızı / Speed (SPD)",
                        "Güvenilirlik / Trust (TRU)",
                        "Yorum Derecesi / Rating (RAT)"
                    )
                    UserRole.PITCH -> listOf(
                        "Zemin Kalitesi / Turf (TRF)",
                        "Işıklandırma / Lighting (LGT)",
                        "Sosyal Tesisler / Amenities (AMN)",
                        "Fiyat Uygunluğu / Price (PRC)",
                        "Ulaşım Kolaylığı / Location (LOC)",
                        "Rezervasyon Kolaylığı / Booking (BOK)"
                    )
                    UserRole.ORGANIZER -> listOf(
                        "Turnuva Düzeni / Order (ORD)",
                        "Ödül Havuzu / Prizes (PRZ)",
                        "Katılımcı Memnuniyeti / Satisfaction (SAT)",
                        "Hakem Yönetimi / Refereeing (REF)",
                        "Disiplin ve Adalet / FairPlay (FAY)",
                        "Canlı Yayın Kalitesi / Streaming (STR)"
                    )
                    else -> listOf("Stat 1", "Stat 2", "Stat 3", "Stat 4", "Stat 5", "Stat 6")
                }
                val lbl1 = labelsList[0]
                val lbl2 = labelsList[1]
                val lbl3 = labelsList[2]
                val lbl4 = labelsList[3]
                val lbl5 = labelsList[4]
                val lbl6 = labelsList[5]

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("PRO KART GÜÇ VE YETENEKLERİ (50 - 99)", color = roleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        com.example.ui.screens.StatSliderRow(label = lbl1, value = stat1State, onValueChange = { stat1State = it })
                        com.example.ui.screens.StatSliderRow(label = lbl2, value = stat2State, onValueChange = { stat2State = it })
                        com.example.ui.screens.StatSliderRow(label = lbl3, value = stat3State, onValueChange = { stat3State = it })
                        com.example.ui.screens.StatSliderRow(label = lbl4, value = stat4State, onValueChange = { stat4State = it })
                        com.example.ui.screens.StatSliderRow(label = lbl5, value = stat5State, onValueChange = { stat5State = it })
                        com.example.ui.screens.StatSliderRow(label = lbl6, value = stat6State, onValueChange = { stat6State = it })
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { isEditMode = false },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSlate),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("İPTAL", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.updateUserProfile(
                                name = nameState,
                                city = cityState,
                                bio = bioState,
                                instagram = instagramState,
                                phone = phoneState,
                                achievements = achievementsState,
                                services = servicesState,
                                price = priceState,
                                duty = dutyState,
                                license = licenseState,
                                club = clubState,
                                photoUrl = editedPhotoUrlState,
                                stat1 = stat1State,
                                stat2 = stat2State,
                                stat3 = stat3State,
                                stat4 = stat4State,
                                stat5 = stat5State,
                                stat6 = stat6State
                            )
                            showSuccessMessage = true
                            isEditMode = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = roleColor),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("KAYDET", color = AlmostBlack, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSlate)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .border(2.5.dp, roleColor, CircleShape)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(model = user.photoUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = user.name,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black
                            )
                            if (user.isVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.Verified, contentDescription = "Verified", tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(roleColor.copy(alpha = 0.15f))
                                .border(1.dp, roleColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = roleLabel,
                                color = roleColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
                            Text(text = user.city, color = TextGray, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { isEditMode = true },
                            colors = ButtonDefaults.buttonColors(containerColor = roleColor.copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, roleColor),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("edit_profile_mode_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = roleColor, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("PROFİLİ DÜZENLE", color = roleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ProfessionalIdentityCard(
                    user = user,
                    roleColor = roleColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (user.bio.isNotBlank()) {
                    PremiumCard(modifier = Modifier.fillMaxWidth()) {
                        Text("HAKKINDA", color = roleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        Text(text = user.bio, color = Color.White, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }

                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("PROFİL DETAYLARI", color = roleColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                    if (user.role == UserRole.SCOUT) {
                        ProfileDetailRow(label = "Kulüp / Kurum", value = user.club.ifEmpty { "Serbest" })
                        ProfileDetailRow(label = "Görev Tanımı", value = user.duty.ifEmpty { "Gözlemci" })
                    }

                    if (user.role == UserRole.COACH) {
                        ProfileDetailRow(label = "Kulüp / Takım", value = user.club.ifEmpty { "Serbest" })
                        ProfileDetailRow(label = "Antrenör Lisansı", value = user.license.ifEmpty { "Belirtilmemiş" })
                    }

                    if (user.role == UserRole.STORE) {
                        if (user.achievements.isNotBlank()) {
                            ProfileDetailRow(label = "Website", value = user.achievements)
                        }
                        if (user.bio.isNotBlank()) {
                            ProfileDetailRow(label = "Mağaza Adresi", value = user.bio)
                        }
                    }

                    if (user.role == UserRole.PITCH) {
                        ProfileDetailRow(label = "Çalışma Saatleri", value = user.services.ifEmpty { "Belirtilmemiş" })
                        ProfileDetailRow(label = "Tek Maç Ücreti", value = user.price.ifEmpty { "Belirtilmemiş" })
                        if (user.bio.isNotBlank()) {
                            ProfileDetailRow(label = "Tesis Adresi", value = user.bio)
                        }
                    }

                    if (user.role == UserRole.ORGANIZER) {
                        ProfileDetailRow(label = "Organizasyon Türü", value = user.services.ifEmpty { "Belirtilmemiş" })
                    }

                    if (user.role == UserRole.MEDIA) {
                        ProfileDetailRow(label = "Başlangıç Ücreti", value = user.price.ifEmpty { "Belirtilmemiş" })
                        if (user.services.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sunulan Hizmetler", color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                user.services.split(", ").forEach { service ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(AlmostBlack)
                                            .border(0.5.dp, DarkBorder, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = service, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (user.achievements.isNotBlank() && user.role != UserRole.STORE && user.role != UserRole.PITCH && user.role != UserRole.ORGANIZER) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (user.role == UserRole.MEDIA) "REFERANSLAR / PORTFOLYO" else "BAŞARILAR & SERTİFİKALAR",
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(text = user.achievements, color = Color.White, fontSize = 12.sp, lineHeight = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("SOSYAL & İLETİŞİM", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    if (user.instagram.isNotBlank()) {
                        ProfileDetailRow(label = "Instagram", value = "@${user.instagram}")
                    }
                    if (user.phone.isNotBlank()) {
                        ProfileDetailRow(label = "Telefon / WhatsApp", value = user.phone)
                    }
                }

                Text(
                    text = when (user.role) {
                        UserRole.STORE -> "🛍️ MAĞAZA VE ÜRÜN FOTOĞRAFLARI"
                        UserRole.PITCH -> "🏟️ HALI SAHA TESİS FOTOĞRAFLARI"
                        UserRole.ORGANIZER -> "🏆 TURNUVA VE ETKİNLİK FOTOĞRAFLARI"
                        UserRole.MEDIA -> "🎥 ÖRNEK ÇALIŞMALAR (PORTFOLYO GALERİSİ)"
                        else -> "📷 ÇALIŞMA GALERİSİ / FOTOĞRAFLAR"
                    },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                GallerySection(
                    galleryList = user.gallery,
                    isEditable = true,
                    isPremium = user.isPremium,
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

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "📺 MAÇ & TANITIM VİDEOLARI",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                YoutubeVideosSection(
                    videoList = user.youtubeVideos,
                    isEditable = true,
                    isPremium = user.isPremium,
                    onAddVideo = { videoUrl ->
                        viewModel.addYoutubeVideo(videoUrl) { _ -> }
                    },
                    onDeleteVideo = { videoUrl ->
                        viewModel.removeYoutubeVideo(videoUrl)
                    }
                )
            }
        }
    }

    if (showEditStudio) {
        ProfilePhotoStudio(
            onPhotoCapturedAndCropped = { path ->
                editedPhotoUrlState = path
                showEditStudio = false
            },
            onDismiss = {
                showEditStudio = false
            }
        )
    }
}

@Composable
fun MyProfileTabContent(
    player: Player,
    viewModel: MainViewModel
) {
    var bioState by remember { mutableStateOf("") }
    var instagramState by remember { mutableStateOf("") }
    var youtubeState by remember { mutableStateOf("") }
    var clubState by remember { mutableStateOf("") }
    var hasLicenseState by remember { mutableStateOf(true) }
    var previousClubsState by remember { mutableStateOf("") }
    var achievementsState by remember { mutableStateOf("") }
    var ageState by remember { mutableStateOf(18) }
    var heightState by remember { mutableStateOf(175) }
    var weightState by remember { mutableStateOf(70) }
    var footState by remember { mutableStateOf(PreferredFoot.RIGHT) }
    var positionState by remember { mutableStateOf(FootballPosition.ST) }

    // FC stats
    var paceState by remember { mutableStateOf(75) }
    var shootingState by remember { mutableStateOf(70) }
    var passingState by remember { mutableStateOf(70) }
    var dribblingState by remember { mutableStateOf(75) }
    var defenseState by remember { mutableStateOf(50) }
    var physicalState by remember { mutableStateOf(70) }

    var isInitialized by remember { mutableStateOf(false) }
    var showSuccessMessage by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var showEditStudio by remember { mutableStateOf(false) }
    var editedPhotoUrlState by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = isEditMode) {
        isEditMode = false
    }

    LaunchedEffect(player) {
        if (!isInitialized) {
            bioState = player.bio
            instagramState = player.instagram
            youtubeState = player.youtubeUrl
            clubState = player.club
            hasLicenseState = player.hasLicense
            previousClubsState = player.previousClubs
            achievementsState = player.achievements
            ageState = player.age
            heightState = player.height
            weightState = player.weight
            footState = player.preferredFoot
            positionState = player.position
            paceState = player.stats.pace
            shootingState = player.stats.shooting
            passingState = player.stats.passing
            dribblingState = player.stats.dribbling
            defenseState = player.stats.defense
            physicalState = player.stats.physical
            isInitialized = true
        }
    }

    Crossfade(targetState = isEditMode, animationSpec = tween(500)) { edit ->
        if (!edit) {
            // ==========================================
            // VIEWER MODE (Transfermarkt + EA Sports FC)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Edit Profile Button (top-right of profile)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan, RoundedCornerShape(20.dp))
                            .clickable { isEditMode = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✏️ Profili Düzenle",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 1. TOP HEADER HERO CARD (FUT Card)
                val calculatedOvr = ((paceState + shootingState + passingState + dribblingState + defenseState + physicalState) / 6).coerceIn(1, 99)
                val displayClubName = if (clubState.isBlank() || 
                    clubState.trim().lowercase() == "free agent" || 
                    clubState.trim().lowercase() == "serbest" || 
                    clubState.trim().lowercase() == "kulüpsüz" ||
                    clubState.trim() == "Belirtilmemiş"
                ) "Kulüpsüz" else clubState

                FUTPlayerCard(
                    name = player.fullName,
                    position = positionState.shortName,
                    rating = calculatedOvr,
                    nationality = player.nationality,
                    club = displayClubName,
                    stats = PhysicalStats(
                        pace = paceState,
                        shooting = shootingState,
                        passing = passingState,
                        dribbling = dribblingState,
                        defense = defenseState,
                        physical = physicalState
                    ),
                    glow = true,
                    photoUrl = player.photoUrl
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Beautiful sub-row with city, age, foot etc.
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎂 ${player.age} Yaş", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "•", color = TextGray, fontSize = 11.sp)
                    Text(text = "📍 ${player.city.ifBlank { "Kayseri" }}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(text = "•", color = TextGray, fontSize = 11.sp)
                    Text(text = "🦶 ${getFootTurkishName(footState)} Ayak", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    if (player.isVerified) {
                        Text(text = "•", color = TextGray, fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "Onaylı", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. SPORCU FOTOĞRAFLARI & GALERİ (First-priority for profile visitor)
                val context = androidx.compose.ui.platform.LocalContext.current
                GallerySection(
                    galleryList = player.gallery,
                    isEditable = true,
                    isPremium = player.isPremium,
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

                Spacer(modifier = Modifier.height(16.dp))

                // 3. TRANSFERMARKT 4 KPI CARDS (Views, Scout interest, Followers, Market value)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // KPI 1: View counts
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSlate)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RemoveRedEye, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("GÖSTERİM", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${player.viewsCount} Göz", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text("Profil Görüntülenmesi", color = TextGray, fontSize = 10.sp)
                    }

                    // KPI 2: Scout Interest %
                    val interest = if (player.scoutRating > 0) (player.scoutRating + 12).coerceIn(40, 100) else 0
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSlate)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF8C00), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SCOUT İLGİSİ", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("%$interest", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text("Gözlemci Radarı", color = TextGray, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // KPI 3: Followers Count
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSlate)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Group, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("TAKİPÇİLER", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${player.followsCount} Takip", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text("İleride Kullanılacak", color = TextGray, fontSize = 10.sp)
                    }

                    // KPI 4: Market Value
                    val marketValue = if (player.marketValue.isEmpty() || player.marketValue == "N/A" || player.marketValue == "-" || player.marketValue == "Belirtilmemiş") "Belirtilmemiş" else player.marketValue
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSlate)
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("MARKET DEĞERİ", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(marketValue, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                        Text("Tahmini Piyasa Değeri", color = TextGray, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. PERFORMANCE RATINGS CARD
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DEĞERLENDİRME PUANI ÖZETİ",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Turnuva Puanı
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AlmostBlack)
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🏆 Turnuva Puanı", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("${player.tournamentRating}", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Scout Puanı
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AlmostBlack)
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🔍 Scout Puanı", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (player.scoutRating > 0) "${player.scoutRating}" else "--",
                                color = if (player.scoutRating > 0) NeonCyan else Color.Gray,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        // Kendi Puanı
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AlmostBlack)
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("👤 Kendi Puanı", color = TextGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            val selfOvr = ((paceState + shootingState + passingState + dribblingState + defenseState + physicalState) / 6).coerceIn(1, 99)
                            Text("$selfOvr", color = NeonCyan, fontSize = 16.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5. SOFASCORE EA SPORTS FC HEX RADAR & STATS SPLIT
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "EA SPORTS FC™ OYUNCU ANALİZ RADARI",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Radar Chart Canvas (Left side)
                        Box(
                            modifier = Modifier
                                .size(160.dp)
                                .weight(1.1f),
                            contentAlignment = Alignment.Center
                        ) {
                            PlayerRadarChart(
                                pace = paceState,
                                shooting = shootingState,
                                passing = passingState,
                                dribbling = dribblingState,
                                defense = defenseState,
                                physical = physicalState
                            )
                        }

                        // Text Stats Breakdown (Right side)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatPillRow(label = "HIZ (PAC)", value = paceState, color = NeonCyan)
                            StatPillRow(label = "ŞUT (SHO)", value = shootingState, color = NeonCyan)
                            StatPillRow(label = "PAS (PAS)", value = passingState, color = NeonCyan)
                            StatPillRow(label = "DRİBLİNG (DRI)", value = dribblingState, color = NeonCyan)
                            StatPillRow(label = "SAVUNMA (DEF)", value = defenseState, color = NeonCyan)
                            StatPillRow(label = "FİZİKSEL (PHY)", value = physicalState, color = NeonCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 6. BIOGRAPHY CARD
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notes, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "SPORCU BİYOGRAFİSİ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (bioState.trim().isEmpty()) "Bu sporcu henüz bir biyografi girmemiş." else bioState,
                        color = TextWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 7. YOUTUBE VIDEO CARD
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ÖNE ÇIKAN FUTBOL VİDEOLARI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    if (youtubeState.trim().isNotEmpty()) {
                        val context = LocalContext.current
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Red.copy(alpha = 0.1f))
                                .border(1.dp, Color.Red.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable {
                                    try {
                                        val intent = android.content.Intent(
                                            android.content.Intent.ACTION_VIEW,
                                            android.net.Uri.parse(youtubeState)
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // ignore or fallback
                                    }
                                }
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.Red, modifier = Modifier.size(36.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text("YouTube Tanıtım Videosu", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("Maç özetlerini ve performans videolarını izle", color = TextGray, fontSize = 10.sp)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Red)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Henüz bir YouTube veya maç özet videosu eklenmedi. Profili düzenleyerek ekleyebilirsiniz.",
                                color = TextGray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 8. PREVIOUS CLUBS CARD
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "OYNADIĞI ÖNCEKİ KULÜPLER", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (previousClubsState.trim().isEmpty()) "Kayıtlı önceki kulüp bilgisi yok." else previousClubsState,
                        color = TextWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 9. ACHIEVEMENTS CARD
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "KAZANILAN BAŞARILAR VE KUPALAR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (achievementsState.trim().isEmpty()) "Kayıtlı başarı bilgisi bulunmuyor." else achievementsState,
                        color = TextWhite,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 10. SCOUT & TOURNAMENT BADGES (Rozetler - moved to bottom)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSlate)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "KAZANILAN SPORCU ROZETLERİ",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val badges = listOf(
                        Triple("Doğrulanmış Oyuncu", "✔️", player.isVerified),
                        Triple("Resmi Scout Onayı", "🔍", player.scoutRating > 0 || player.scoutReports.isNotEmpty()),
                        Triple("Turnuva Şampiyonu", "🏆", player.achievements.contains("İlk Turnuva Şampiyonu", ignoreCase = true)),
                        Triple("Haftanın Golü", "⚽", player.achievements.contains("Haftanın Golü", ignoreCase = true)),
                        Triple("100 Profil Gösterimi", "👁️", player.profileVisits.total >= 100),
                        Triple("Bölge Şampiyonu", "🥇", player.achievements.contains("Bölge Şampiyonu", ignoreCase = true))
                    )
                    val unlockedCount = badges.count { it.third }
                    if (unlockedCount == 0) {
                        Text(
                            text = "Kazanılan Rozet: Yok",
                            color = NeonCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    badges.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { (title, emoji, unlocked) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AlmostBlack)
                                        .border(
                                            width = 1.dp,
                                            color = if (unlocked) NeonCyan.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .padding(10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = if (unlocked) emoji else "🔒",
                                            fontSize = 18.sp
                                        )
                                        Column {
                                            Text(
                                                text = title,
                                                color = if (unlocked) Color.White else Color.Gray,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (unlocked) "Açık" else "Kilitli",
                                                color = if (unlocked) NeonCyan else Color.Gray.copy(alpha = 0.6f),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        } else {
            // ==========================================
            // EDIT MODE (Inputs, TextFields, Sliders)
            // ==========================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title row with Cancel option
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROFİLİ DÜZENLE",
                        color = NeonCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp
                    )

                    Text(
                        text = "❌ Düzenlemeyi İptal Et",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { isEditMode = false }
                            .padding(4.dp)
                    )
                }

                // LIVE FIFA CARD PREVIEW
                Box(
                    modifier = Modifier.padding(bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FUTPlayerCard(
                        name = "${player.firstName} ${player.lastName}",
                        position = positionState.shortName,
                        rating = ((paceState + shootingState + passingState + dribblingState + defenseState + physicalState) / 6).coerceIn(1, 99),
                        nationality = player.nationality,
                        club = if (clubState.isEmpty()) "Serbest Oyuncu" else clubState,
                        stats = PhysicalStats(
                            pace = paceState,
                            shooting = shootingState,
                            passing = passingState,
                            dribbling = dribblingState,
                            defense = defenseState,
                            physical = physicalState
                        ),
                        glow = true,
                        photoUrl = editedPhotoUrlState ?: player.photoUrl
                    )
                }

                Button(
                    onClick = { showEditStudio = true },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.padding(bottom = 16.dp).testTag("edit_profile_photo_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📷 PROFİL FOTOĞRAFINI DEĞİŞTİR", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    text = "⚡ BU SPORCU TARAFINDAN KENDİ DEĞERLENDİRMESİDİR",
                    color = Color(0xFFFFD700),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (showSuccessMessage) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldGreen.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, EmeraldGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Profiliniz başarıyla güncellendi! Diğer kulüp ve gözlemciler yeni kartınızı canlı görebilir.",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // FIFA STATS SLIDERS
                PremiumCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "FIFA/FUT KART STATLARI (99 ÜZERİNDEN)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // PAC / HIZ Slider
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "HIZ (PAC):", color = TextGray, fontSize = 12.sp)
                            Text(text = "$paceState", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = paceState.toFloat(),
                            onValueChange = { paceState = it.toInt() },
                            valueRange = 10f..99f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // SHO / ŞUT Slider
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "ŞUT (SHO):", color = TextGray, fontSize = 12.sp)
                            Text(text = "$shootingState", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = shootingState.toFloat(),
                            onValueChange = { shootingState = it.toInt() },
                            valueRange = 10f..99f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // PAS / PAS Slider
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "PAS (PAS):", color = TextGray, fontSize = 12.sp)
                            Text(text = "$passingState", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = passingState.toFloat(),
                            onValueChange = { passingState = it.toInt() },
                            valueRange = 10f..99f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // DRI / DRİBLİNG Slider
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "DRİBLİNG (DRI):", color = TextGray, fontSize = 12.sp)
                            Text(text = "$dribblingState", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = dribblingState.toFloat(),
                            onValueChange = { dribblingState = it.toInt() },
                            valueRange = 10f..99f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // DEF / SAVUNMA Slider
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "SAVUNMA (DEF):", color = TextGray, fontSize = 12.sp)
                            Text(text = "$defenseState", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = defenseState.toFloat(),
                            onValueChange = { defenseState = it.toInt() },
                            valueRange = 10f..99f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // PHY / FİZİKSEL Slider
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "FİZİKSEL (PHY):", color = TextGray, fontSize = 12.sp)
                            Text(text = "$physicalState", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = physicalState.toFloat(),
                            onValueChange = { physicalState = it.toInt() },
                            valueRange = 10f..99f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }
                }

                // ATHLETE BIO & LINKS
                PremiumCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "BİYOGRAFİ, BAŞARILAR VE VİDEOLAR",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    CustomTextField(
                        value = bioState,
                        onValueChange = { bioState = it },
                        label = "Kişisel Biyografiniz",
                        modifier = Modifier.height(80.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = previousClubsState,
                        onValueChange = { previousClubsState = it },
                        label = "Önceden Oynadığınız Takımlar (Örn: Göztepe U16)",
                        modifier = Modifier.height(60.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = achievementsState,
                        onValueChange = { achievementsState = it },
                        label = "Kazandığınız Başarılar / Madalyalar",
                        modifier = Modifier.height(60.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = youtubeState,
                        onValueChange = { youtubeState = it },
                        label = "YouTube Öne Çıkanlar Videosu URL'si",
                        leadingIcon = { Icon(Icons.Default.PlayCircle, contentDescription = null, tint = Color.Red) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    CustomTextField(
                        value = instagramState,
                        onValueChange = { instagramState = it },
                        label = "Instagram Kullanıcı Adı (Örn: @merty9)"
                    )
                }

                // ATHLETIC BIOMETRICS & POSITION / FOOT
                PremiumCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "FİZİKSEL VE TAKTİKSEL VERİLER",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Position selection row
                    Text(text = "FUTBOL MEVKİSİ", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    ) {
                        items(FootballPosition.values().size) { idx ->
                            val pos = FootballPosition.values()[idx]
                            val isSelected = positionState == pos
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan else DarkBorder)
                                    .clickable { positionState = pos }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "${pos.shortName} (${getPositionTurkishName(pos)})",
                                    color = if (isSelected) AlmostBlack else TextWhite,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Preferred foot row
                    Text(text = "TERCİH EDİLEN AYAK", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PreferredFoot.values().forEach { foot ->
                            val isSelected = footState == foot
                            val label = when (foot) {
                                PreferredFoot.LEFT -> "Sol Ayak"
                                PreferredFoot.RIGHT -> "Sağ Ayak"
                                PreferredFoot.BOTH -> "İki Ayaklı"
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NeonCyan else DarkBorder)
                                    .clickable { footState = foot }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) AlmostBlack else TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Age slider
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "YAŞ:", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$ageState Yaş", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = ageState.toFloat(),
                            onValueChange = { ageState = it.toInt() },
                            valueRange = 6f..50f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // Height slider
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "BOY:", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$heightState CM", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = heightState.toFloat(),
                            onValueChange = { heightState = it.toInt() },
                            valueRange = 100f..220f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // Weight slider
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "KİLO:", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "$weightState KG", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = weightState.toFloat(),
                            onValueChange = { weightState = it.toInt() },
                            valueRange = 20f..120f,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                        )
                    }

                    // Current Club
                    CustomTextField(
                        value = clubState,
                        onValueChange = { clubState = it },
                        label = "Aktif Kulüp (Örn: Kulüpsüz)"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBorder)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "RESMİ LİSANS DURUMU", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "TFF veya diğer federasyon lisansınız var mı?", color = TextGray, fontSize = 9.sp)
                        }
                        Switch(
                            checked = hasLicenseState,
                            onCheckedChange = { hasLicenseState = it },
                            colors = SwitchDefaults.colors(
                                  checkedThumbColor = NeonCyan,
                                  checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                // SAVE BUTTON
                NeonButton(
                    text = "DEĞİŞİKLİKLERİ KAYDET",
                    onClick = {
                        viewModel.updatePlayerProfile(
                            bio = bioState,
                            instagram = instagramState,
                            youtubeUrl = youtubeState,
                            hasLicense = hasLicenseState,
                            club = clubState,
                            previousClubs = previousClubsState,
                            achievements = achievementsState,
                            pace = paceState,
                            shooting = shootingState,
                            passing = passingState,
                            dribbling = dribblingState,
                            defense = defenseState,
                            physical = physicalState,
                            height = heightState,
                            weight = weightState,
                            age = ageState,
                            preferredFoot = footState,
                            position = positionState,
                            photoUrl = editedPhotoUrlState
                        )
                        showSuccessMessage = true
                        isEditMode = false // Smoothly return to Viewer Mode after saving!
                        editedPhotoUrlState = null // Clear temporary state after successful save
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )

                // CANCEL BUTTON
                NeonButton(
                    text = "DÜZENLEMEYİ İPTAL ET",
                    onClick = {
                        isEditMode = false // Return without saving
                        editedPhotoUrlState = null // Discard temporary changes
                    },
                    isSecondary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                )
            }
        }
    }

    if (showEditStudio) {
        ProfilePhotoStudio(
            onPhotoCapturedAndCropped = { path ->
                editedPhotoUrlState = path
                showEditStudio = false
            },
            onDismiss = {
                showEditStudio = false
            }
        )
    }
}

private fun getRelativeTime(createdAt: String): String {
    if (createdAt.isBlank()) return "Yakın zamanda katıldı"
    if (createdAt.contains("önce") || createdAt.contains("Katıldı")) return createdAt
    try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        val date = sdf.parse(createdAt) ?: return "Az önce katıldı"
        val diffMs = System.currentTimeMillis() - date.time
        val diffMin = diffMs / (1000 * 60)
        return when {
            diffMin < 1 -> "Az önce katıldı"
            diffMin < 60 -> "$diffMin dakika önce"
            diffMin < 1440 -> "${diffMin / 60} saat önce"
            else -> "${diffMin / 1440} gün önce"
        }
    } catch (e: Exception) {
        return "Az önce katıldı"
    }
}

@Composable
fun SonKatilanlarSection(
    users: List<AppUser>,
    viewModel: MainViewModel,
    onUserClick: (AppUser) -> Unit,
    onSeeAllClick: () -> Unit
) {
    val players by viewModel.players.collectAsState()
    val clubs by viewModel.clubs.collectAsState()

    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        borderGlow = true
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "🆕 SON KATILANLAR",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "TÜMÜNÜ GÖR",
                color = NeonCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .clickable { onSeeAllClick() }
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val previewUsers = users.take(4)
            previewUsers.forEach { user ->
                val resolvedPhoto = when (user.role) {
                    UserRole.PLAYER -> players.find { it.id == user.id }?.photoUrl ?: user.photoUrl
                    UserRole.CLUB -> clubs.find { it.id == user.id }?.logoUrl ?: user.photoUrl
                    else -> user.photoUrl
                }
                SonKatilanUserRow(
                    user = user,
                    photoUrl = resolvedPhoto,
                    onClick = { onUserClick(user) }
                )
            }
        }
    }
}

@Composable
fun SonKatilanUserRow(
    user: AppUser,
    photoUrl: String? = null,
    onClick: () -> Unit
) {
    val roleColor = when (user.role) {
        UserRole.PLAYER -> NeonCyan
        UserRole.CLUB -> Color(0xFFFFD700)
        UserRole.SCOUT -> Color(0xFFE040FB)
        UserRole.COACH -> Color(0xFF00E676)
        UserRole.MEDIA -> Color(0xFFFF9100)
        UserRole.STORE -> Color(0xFFFFEA00)
        UserRole.PITCH -> Color(0xFF00E5FF)
        UserRole.ORGANIZER -> Color(0xFFFF3D00)
        UserRole.ADMIN -> Color.Red
    }

    val roleLabel = when (user.role) {
        UserRole.PLAYER -> "FUTBOLCU"
        UserRole.CLUB -> "KULÜP"
        UserRole.SCOUT -> "SCOUT"
        UserRole.COACH -> "ANTRENÖR"
        UserRole.MEDIA -> "MEDYA EKİBİ"
        UserRole.STORE -> "SPOR MAĞAZASI"
        UserRole.PITCH -> "HALI SAHA"
        UserRole.ORGANIZER -> "ORGANİZATÖR"
        UserRole.ADMIN -> "ADMİN"
    }

    val displayPhoto = photoUrl ?: user.photoUrl

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSlate)
            .border(0.5.dp, DarkBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(1.dp, roleColor.copy(alpha = 0.4f), CircleShape)
            ) {
                Image(
                    painter = rememberAsyncImagePainter(
                        model = displayPhoto.ifEmpty { "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=300" }
                    ),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (user.isVerified) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = NeonCyan,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = user.city,
                        color = TextGray,
                        fontSize = 10.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(TextGray)
                    )
                    Text(
                        text = getRelativeTime(user.createdAt),
                        color = TextGray,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(roleColor.copy(alpha = 0.12f))
                    .border(0.5.dp, roleColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = roleLabel,
                    color = roleColor,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun ProfessionalIdentityCard(
    user: AppUser,
    roleColor: Color,
    modifier: Modifier = Modifier,
    club: Club? = null
) {
    when (user.role) {
        UserRole.PITCH -> PitchProfileCard(user = user, roleColor = roleColor, modifier = modifier)
        UserRole.STORE -> StoreProfileCard(user = user, roleColor = roleColor, modifier = modifier)
        UserRole.CLUB -> ClubProfileCard(user = user, club = club, roleColor = roleColor, modifier = modifier)
        UserRole.ORGANIZER -> OrganizerProfileCard(user = user, roleColor = roleColor, modifier = modifier)
        UserRole.MEDIA -> MediaProfileCard(user = user, roleColor = roleColor, modifier = modifier)
        UserRole.SCOUT -> ScoutProfileCard(user = user, roleColor = roleColor, modifier = modifier)
        UserRole.COACH -> CoachProfileCard(user = user, roleColor = roleColor, modifier = modifier)
        else -> GenericCorporateProfileCard(user = user, roleColor = roleColor, modifier = modifier)
    }
}

@Composable
fun PitchProfileCard(user: AppUser, roleColor: Color, modifier: Modifier = Modifier) {
    CorporateBaseCard(
        user = user,
        roleLabel = "HALI SAHA TESİSİ",
        roleColor = roleColor,
        modifier = modifier
    ) { scale ->
        val details = mutableListOf<Pair<String, String>>()
        if (user.services.isNotBlank()) details.add("Çalışma Saatleri" to user.services)
        if (user.price.isNotBlank()) details.add("Saatlik Ücret" to user.price)
        if (user.phone.isNotBlank()) details.add("Rezervasyon" to user.phone)
        if (user.bio.isNotBlank()) details.add("Tesis Adresi" to user.bio)
        if (user.instagram.isNotBlank()) details.add("Instagram" to "@${user.instagram}")
        if (details.isEmpty()) details.add("Saha Türü" to "Kapalı / Açık Halı Saha")

        CorporateDetailsBox(details = details, roleColor = roleColor, scale = scale)
    }
}

@Composable
fun StoreProfileCard(user: AppUser, roleColor: Color, modifier: Modifier = Modifier) {
    CorporateBaseCard(
        user = user,
        roleLabel = "SPOR MAĞAZASI",
        roleColor = roleColor,
        modifier = modifier
    ) { scale ->
        val details = mutableListOf<Pair<String, String>>()
        if (user.services.isNotBlank()) details.add("Ürünler" to user.services)
        if (user.achievements.isNotBlank()) details.add("Website" to user.achievements)
        if (user.phone.isNotBlank()) details.add("İletişim" to user.phone)
        if (user.bio.isNotBlank()) details.add("Mağaza Adresi" to user.bio)
        if (user.instagram.isNotBlank()) details.add("Instagram" to "@${user.instagram}")
        if (details.isEmpty()) details.add("Hizmet" to "Forma & Ekipman Satışı")

        CorporateDetailsBox(details = details, roleColor = roleColor, scale = scale)
    }
}

@Composable
fun ClubProfileCard(user: AppUser, club: Club?, roleColor: Color, modifier: Modifier = Modifier) {
    CorporateBaseCard(
        user = user,
        roleLabel = "FUTBOL KULÜBÜ",
        roleColor = roleColor,
        modifier = modifier
    ) { scale ->
        val details = mutableListOf<Pair<String, String>>()
        if ((club?.foundationYear ?: 0) > 0) details.add("Kuruluş Yılı" to club!!.foundationYear.toString())
        if ((club?.activeStudentsCount ?: 0) > 0) details.add("Aktif Öğrenci" to "${club!!.activeStudentsCount} Sporcu")
        if ((club?.coachesCount ?: 0) > 0) details.add("Antrenör Sayısı" to "${club!!.coachesCount} Eğitmen")
        if ((club?.trophyCount ?: 0) > 0) details.add("Kupa Sayısı" to "${club!!.trophyCount} Kupa")
        if (club?.hasLicense == true) details.add("Akademi" to "Tescilli / Lisanslı")
        if (!club?.activeAgeGroups.isNullOrBlank()) details.add("Yaş Grupları" to club!!.activeAgeGroups)
        if (!club?.trainingFacility.isNullOrBlank()) details.add("Tesis" to club!!.trainingFacility)
        if (user.phone.isNotBlank() || !club?.phoneNumber.isNullOrBlank()) details.add("İletişim" to (user.phone.ifBlank { club?.phoneNumber ?: "" }))
        if (details.isEmpty()) details.add("Kulüp Statüsü" to "Resmi Futbol Kulübü")

        CorporateDetailsBox(details = details, roleColor = roleColor, scale = scale)
    }
}

@Composable
fun OrganizerProfileCard(user: AppUser, roleColor: Color, modifier: Modifier = Modifier) {
    CorporateBaseCard(
        user = user,
        roleLabel = "TURNUVA ORGANİZATÖRÜ",
        roleColor = roleColor,
        modifier = modifier
    ) { scale ->
        val details = mutableListOf<Pair<String, String>>()
        if (user.services.isNotBlank()) details.add("Organizasyon" to user.services)
        if (user.phone.isNotBlank()) details.add("İletişim Tel" to user.phone)
        if (user.instagram.isNotBlank()) details.add("Instagram" to "@${user.instagram}")
        if (user.bio.isNotBlank()) details.add("Hakkında" to user.bio)
        if (details.isEmpty()) details.add("Etkinlikler" to "Turnuva ve Lig Organizasyonu")

        CorporateDetailsBox(details = details, roleColor = roleColor, scale = scale)
    }
}

@Composable
fun MediaProfileCard(user: AppUser, roleColor: Color, modifier: Modifier = Modifier) {
    CorporateBaseCard(
        user = user,
        roleLabel = "MEDYA & İÇERİK EKİBİ",
        roleColor = roleColor,
        modifier = modifier
    ) { scale ->
        val details = mutableListOf<Pair<String, String>>()
        if (user.services.isNotBlank()) details.add("Hizmetler" to user.services)
        if (user.price.isNotBlank()) details.add("Hizmet Ücreti" to user.price)
        if (user.achievements.isNotBlank()) details.add("Portfolyo" to user.achievements)
        if (user.phone.isNotBlank()) details.add("İletişim Tel" to user.phone)
        if (user.instagram.isNotBlank()) details.add("Instagram" to "@${user.instagram}")
        if (details.isEmpty()) details.add("Hizmet" to "Spor Çekimi ve Prodüksiyon")

        CorporateDetailsBox(details = details, roleColor = roleColor, scale = scale)
    }
}

@Composable
fun ScoutProfileCard(user: AppUser, roleColor: Color, modifier: Modifier = Modifier) {
    CorporateBaseCard(
        user = user,
        roleLabel = "SCOUT / GÖZLEMCİ",
        roleColor = roleColor,
        modifier = modifier
    ) { scale ->
        val details = mutableListOf<Pair<String, String>>()
        if (user.club.isNotBlank()) details.add("Kulüp / Kurum" to user.club)
        if (user.duty.isNotBlank()) details.add("Görev" to user.duty)
        if (user.phone.isNotBlank()) details.add("İletişim" to user.phone)
        if (user.instagram.isNotBlank()) details.add("Instagram" to "@${user.instagram}")
        if (details.isEmpty()) details.add("Unvan" to "Futbol Scout / Gözlemci")

        CorporateDetailsBox(details = details, roleColor = roleColor, scale = scale)
    }
}

@Composable
fun CoachProfileCard(user: AppUser, roleColor: Color, modifier: Modifier = Modifier) {
    CorporateBaseCard(
        user = user,
        roleLabel = "ANTRENÖR",
        roleColor = roleColor,
        modifier = modifier
    ) { scale ->
        val details = mutableListOf<Pair<String, String>>()
        if (user.license.isNotBlank()) details.add("Lisans" to user.license)
        if (user.club.isNotBlank()) details.add("Aktif Takım" to user.club)
        if (user.phone.isNotBlank()) details.add("İletişim" to user.phone)
        if (user.instagram.isNotBlank()) details.add("Instagram" to "@${user.instagram}")
        if (details.isEmpty()) details.add("Görevi" to "Teknik Sorumlu / Antrenör")

        CorporateDetailsBox(details = details, roleColor = roleColor, scale = scale)
    }
}

@Composable
fun GenericCorporateProfileCard(user: AppUser, roleColor: Color, modifier: Modifier = Modifier) {
    CorporateBaseCard(
        user = user,
        roleLabel = user.role.name,
        roleColor = roleColor,
        modifier = modifier
    ) { scale ->
        val details = mutableListOf<Pair<String, String>>()
        if (user.city.isNotBlank()) details.add("Şehir" to user.city)
        if (user.phone.isNotBlank()) details.add("İletişim" to user.phone)
        if (details.isEmpty()) details.add("Statü" to "Onaylı Profil")

        CorporateDetailsBox(details = details, roleColor = roleColor, scale = scale)
    }
}

@Composable
private fun CorporateBaseCard(
    user: AppUser,
    roleLabel: String,
    roleColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable (scale: Float) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "proCardGlow")
    val animatedGlowColor by infiniteTransition.animateColor(
        initialValue = roleColor.copy(alpha = 0.4f),
        targetValue = roleColor.copy(alpha = 0.9f),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlow"
    )

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val containerWidth = maxWidth
        val cardWidth = minOf(280.dp, containerWidth - 16.dp)
        val cardHeight = cardWidth * (400f / 280f)
        val scaleFactor = (cardWidth / 280.dp)

        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .clip(RoundedCornerShape(16.dp * scaleFactor))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E2129),
                            AlmostBlack
                        )
                    )
                )
                .border(2.5.dp * scaleFactor, animatedGlowColor, RoundedCornerShape(16.dp * scaleFactor))
                .padding(14.dp * scaleFactor)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FUTBOLCUM",
                            color = Color.White,
                            fontSize = 11.sp * scaleFactor,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = roleLabel,
                            color = roleColor,
                            fontSize = 8.sp * scaleFactor,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(26.dp * scaleFactor)
                            .height(18.dp * scaleFactor)
                            .clip(RoundedCornerShape(4.dp * scaleFactor))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFFE5A93C), Color(0xFFF3D082), Color(0xFFC78B25))
                                )
                            )
                            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(4.dp * scaleFactor))
                    )
                }

                Spacer(modifier = Modifier.height(10.dp * scaleFactor))

                Box(
                    modifier = Modifier
                        .size(85.dp * scaleFactor)
                        .clip(CircleShape)
                        .background(Color(0xFF16171D))
                        .border(2.5.dp * scaleFactor, roleColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = user.photoUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp * scaleFactor))

                Text(
                    text = user.name.uppercase(),
                    color = TextWhite,
                    fontSize = 16.sp * scaleFactor,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                Text(
                    text = "${roleLabel.lowercase().replaceFirstChar { it.uppercase() }} • ${user.city}",
                    color = TextGray,
                    fontSize = 10.sp * scaleFactor,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp * scaleFactor)
                )

                Spacer(modifier = Modifier.height(10.dp * scaleFactor))

                Box(modifier = Modifier.weight(1f)) {
                    content(scaleFactor)
                }

                Spacer(modifier = Modifier.height(6.dp * scaleFactor))

                Text(
                    text = "ID: ${user.role.name}-${Math.abs(user.id.hashCode()) % 100000}",
                    color = Color.White.copy(alpha = 0.3f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.sp * scaleFactor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun CorporateDetailsBox(
    details: List<Pair<String, String>>,
    roleColor: Color,
    scale: Float = 1f
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp * scale))
            .background(Color(0xFF0F1115))
            .border(width = 0.5.dp, color = Color(0x1AFFFFFF), shape = RoundedCornerShape(10.dp * scale))
            .padding(horizontal = 10.dp * scale, vertical = 8.dp * scale)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp * scale)
        ) {
            details.take(5).forEach { (label, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = TextGray,
                        fontSize = 9.sp * scale,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = value,
                        color = Color.White,
                        fontSize = 9.5.sp * scale,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1.2f).padding(start = 4.dp * scale)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppUserProfileDialog(
    user: AppUser,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onStartChat: (String, String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val roleColor = when (user.role) {
        UserRole.PLAYER -> NeonCyan
        UserRole.CLUB -> Color(0xFFFFD700)
        UserRole.SCOUT -> Color(0xFFE040FB)
        UserRole.COACH -> Color(0xFF00E676)
        UserRole.MEDIA -> Color(0xFFFF9100)
        UserRole.STORE -> Color(0xFFFFEA00)
        UserRole.PITCH -> Color(0xFF00E5FF)
        UserRole.ORGANIZER -> Color(0xFFFF3D00)
        else -> NeonCyan
    }

    val roleName = when (user.role) {
        UserRole.PLAYER -> "FUTBOLCU"
        UserRole.CLUB -> "KULÜP"
        UserRole.SCOUT -> "GÖZLEMCİ (SCOUT)"
        UserRole.COACH -> "ANTRENÖR"
        UserRole.MEDIA -> "MEDYA EKİBİ"
        UserRole.STORE -> "SPOR MAĞAZASI"
        UserRole.PITCH -> "HALI SAHA"
        UserRole.ORGANIZER -> "ORGANİZATÖR"
        else -> "ÜYE"
    }

    // Capture system back press to close this overlay
    androidx.activity.compose.BackHandler(enabled = true) {
        onDismiss()
    }

    val tabs = remember(user.id) {
        listOf("GENEL BAKIŞ", "GALERİ / PORTFOLYO", "VİDEOLAR")
    }
    var selectedTabIndex by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = roleName, 
                        color = roleColor, 
                        fontSize = 13.sp, 
                        fontWeight = FontWeight.Black, 
                        letterSpacing = 1.5.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Geri", 
                            tint = roleColor
                        )
                    }
                },
                actions = {
                    if (user.isVerified) {
                        Icon(
                            imageVector = Icons.Default.Verified, 
                            contentDescription = "Onaylı Profil", 
                            tint = roleColor,
                            modifier = Modifier.padding(end = 16.dp).size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AlmostBlack)
            )
        },
        containerColor = AlmostBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Content: Custom role-specific FUT Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AlmostBlack)
                    .padding(top = 16.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ProfessionalIdentityCard(
                    user = user,
                    roleColor = roleColor
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Sub-row with city and membership year
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextGray, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = user.city, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(text = "•", color = TextGray, fontSize = 11.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = TextGray, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Katılım: ${if (user.createdAt.contains("T")) user.createdAt.substringBefore("T") else user.createdAt}", 
                            color = Color.White, 
                            fontSize = 11.sp, 
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (user.isPremium) {
                        Text(text = "•", color = TextGray, fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "PREMIUM", color = Color(0xFFFFD700), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Material 3 Tabs
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = AlmostBlack,
                contentColor = roleColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = roleColor
                    )
                },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTabIndex == index) roleColor else TextGray
                            )
                        }
                    )
                }
            }

            // Tab Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // GENEL BAKIŞ (Overview)
                        if (user.bio.isNotBlank()) {
                            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "HAKKINDA / BİYOGRAFİ",
                                    color = roleColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = user.bio,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "UZMANLIK VE DETAYLAR",
                                color = roleColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (user.role == UserRole.SCOUT) {
                                ProfileDetailRow(label = "Kulüp / Kurum", value = user.club.ifEmpty { "Serbest" })
                                ProfileDetailRow(label = "Görev Tanımı", value = user.duty.ifEmpty { "Gözlemci" })
                            }

                            if (user.role == UserRole.COACH) {
                                ProfileDetailRow(label = "Kulüp / Takım", value = user.club.ifEmpty { "Serbest" })
                                ProfileDetailRow(label = "Antrenör Lisansı", value = user.license.ifEmpty { "Belirtilmemiş" })
                            }

                            if (user.role == UserRole.STORE) {
                                if (user.achievements.isNotBlank()) {
                                    ProfileDetailRow(label = "Website", value = user.achievements)
                                }
                            }

                            if (user.role == UserRole.PITCH) {
                                ProfileDetailRow(label = "Tek Maç Ücreti", value = user.price.ifEmpty { "Belirtilmemiş" })
                            }

                            if (user.role == UserRole.MEDIA) {
                                ProfileDetailRow(label = "Başlangıç Ücreti", value = user.price.ifEmpty { "Belirtilmemiş" })
                            }
                            ProfileDetailRow(label = "E-posta", value = user.email)
                        }

                        if ((user.role == UserRole.MEDIA || user.role == UserRole.PITCH || user.role == UserRole.ORGANIZER) && user.services.isNotBlank()) {
                            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = when (user.role) {
                                        UserRole.PITCH -> "ÇALIŞMA SAATLERİ"
                                        UserRole.ORGANIZER -> "ORGANİZASYON TÜRÜ"
                                        else -> "SUNULAN HİZMETLER"
                                    },
                                    color = roleColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    user.services.split(",").forEach { service ->
                                        if (service.trim().isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(AlmostBlack)
                                                    .border(0.5.dp, DarkBorder, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = service.trim(),
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (user.achievements.isNotBlank() && user.role != UserRole.STORE && user.role != UserRole.PITCH && user.role != UserRole.ORGANIZER) {
                            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (user.role == UserRole.MEDIA) "REFERANSLAR / PORTFOLYO" else "BAŞARILAR & SERTİFİKALAR",
                                    color = roleColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Text(
                                    text = user.achievements,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    1 -> {
                        // GALERİ / PORTFOLYO
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "📷 FOTOĞRAFLAR VE ÇALIŞMALAR",
                                color = roleColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            if (user.gallery.isNotEmpty()) {
                                GallerySection(
                                    galleryList = user.gallery,
                                    isEditable = false,
                                    isPremium = user.isPremium,
                                    onAddPhoto = {},
                                    onDeletePhoto = {},
                                    onReplacePhoto = { _, _ -> }
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = TextGray, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Henüz fotoğraf eklenmemiş.", color = TextGray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // VİDEOLAR
                        PremiumCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "📺 TANITIM & ÇALIŞMA VİDEOLARI",
                                color = roleColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            if (user.youtubeVideos.isNotEmpty()) {
                                YoutubeVideosSection(
                                    videoList = user.youtubeVideos,
                                    isEditable = false,
                                    isPremium = user.isPremium,
                                    onAddVideo = {},
                                    onDeleteVideo = {}
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = TextGray, modifier = Modifier.size(40.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Henüz video eklenmemiş.", color = TextGray, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sticky Action Buttons for Communication
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (user.phone.isNotBlank()) {
                        Button(
                            onClick = {
                                try {
                                    val cleanPhone = user.phone.replace("+", "").replace(" ", "")
                                    val url = "https://wa.me/$cleanPhone"
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("WHATSAPP İLE SOHBET BAŞLAT", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                        }
                    }

                    if (user.instagram.isNotBlank()) {
                        Button(
                            onClick = {
                                try {
                                    val url = "https://instagram.com/${user.instagram}"
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1306C)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("INSTAGRAM PROFİLİNİ GÖR", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp, letterSpacing = 1.sp)
                        }
                    }

                    NeonButton(
                        text = "💬 UYGULAMADAN MESAJ GÖNDER",
                        onClick = {
                            onStartChat(user.name, roleName)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextGray, fontSize = 11.sp)
        Text(text = value, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AllSonKatilanlarDialog(
    users: List<AppUser>,
    viewModel: MainViewModel,
    onUserClick: (AppUser) -> Unit,
    onDismiss: () -> Unit
) {
    val players by viewModel.players.collectAsState()
    val clubs by viewModel.clubs.collectAsState()

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        PremiumCard(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(8.dp),
            borderGlow = true
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🆕 TÜM KATILANLAR",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(users) { user ->
                        val resolvedPhoto = when (user.role) {
                            UserRole.PLAYER -> players.find { it.id == user.id }?.photoUrl ?: user.photoUrl
                            UserRole.CLUB -> clubs.find { it.id == user.id }?.logoUrl ?: user.photoUrl
                            else -> user.photoUrl
                        }
                        SonKatilanUserRow(
                            user = user,
                            photoUrl = resolvedPhoto,
                            onClick = { onUserClick(user) }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// --- REDESIGNED COMPONENT: SMART ACTION CARD ---
// -------------------------------------------------------------
@Composable
fun SmartActionCard(
    currentUser: AppUser,
    viewModel: MainViewModel,
    onNavigate: (tabIndex: Int, searchMode: Int?, exploreCategory: String?) -> Unit
) {
    val cardData = remember(currentUser) {
        when (currentUser.role) {
            UserRole.PLAYER -> {
                SmartCardState(
                    title = "Futbol Kariyerini Güçlendir",
                    description = "Profilini tamamla ve kulüpler tarafından keşfedil.",
                    buttonText = "PROFİLİ TAMAMLA",
                    icon = Icons.Default.Person,
                    onClick = { onNavigate(4, null, null) }
                )
            }
            UserRole.CLUB -> {
                SmartCardState(
                    title = "Oyuncu Başvurularını İncele",
                    description = "Kulübüne gelen yeni başvuruları değerlendir.",
                    buttonText = "BAŞVURULARI GÖR",
                    icon = Icons.Default.Group,
                    onClick = { onNavigate(4, null, null) }
                )
            }
            UserRole.SCOUT -> {
                SmartCardState(
                    title = "Yeni Yetenekleri Keşfet",
                    description = "Aradığın özelliklere sahip futbolcuları incele.",
                    buttonText = "OYUNCU ARA",
                    icon = Icons.Default.Search,
                    onClick = { onNavigate(1, 0, null) }
                )
            }
            UserRole.COACH -> {
                SmartCardState(
                    title = "Futbol Ağını Büyüt",
                    description = "Kulüpleri ve futbolcuları keşfet.",
                    buttonText = "KEŞFET",
                    icon = Icons.Default.Search,
                    onClick = { onNavigate(2, null, null) }
                )
            }
            UserRole.MEDIA -> {
                SmartCardState(
                    title = "Futbol Dünyasını Keşfet",
                    description = "Yeni oyuncuları, kulüpleri ve organizasyonları incele.",
                    buttonText = "KEŞFET",
                    icon = Icons.Default.Search,
                    onClick = { onNavigate(2, null, null) }
                )
            }
            UserRole.ORGANIZER -> {
                SmartCardState(
                    title = "Yeni Bir Turnuva Oluştur",
                    description = "Takımları davet et, fikstürü oluştur ve turnuvanı yönet.",
                    buttonText = "TURNUVA BAŞLAT",
                    icon = Icons.Default.EmojiEvents,
                    onClick = { onNavigate(2, null, "🏆 Turnuvalar") }
                )
            }
            UserRole.STORE -> {
                SmartCardState(
                    title = "Spor Mağazanı Öne Çıkar",
                    description = "Spor ekipmanlarını ve ürünlerini sergile.",
                    buttonText = "KEŞFET",
                    icon = Icons.Default.Sports,
                    onClick = { onNavigate(2, null, "🏪 Mağazalar") }
                )
            }
            UserRole.PITCH -> {
                SmartCardState(
                    title = "Tesis Rezervasyonlarını Yönet",
                    description = "Halı saha saatlerini ve tesis bilgilerini güncelle.",
                    buttonText = "BİLGİLERİ GÜNCELLE",
                    icon = Icons.Default.Sports,
                    onClick = { onNavigate(4, null, null) }
                )
            }
            UserRole.ADMIN -> {
                SmartCardState(
                    title = "Sistem Yönetim Paneli",
                    description = "Kullanıcı onayları ve sistem genel durumunu yönet.",
                    buttonText = "YÖNETİCİ AYARLARI",
                    icon = Icons.Default.Settings,
                    onClick = { onNavigate(5, null, null) }
                )
            }
        }
    }

    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 125.dp)
            .testTag("smart_action_card"),
        borderGlow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCyan.copy(alpha = 0.12f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = cardData.icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = cardData.title,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = cardData.description,
                        color = TextGray,
                        fontSize = 12.sp,
                        maxLines = 2,
                        lineHeight = 16.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            NeonButton(
                text = cardData.buttonText,
                onClick = cardData.onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("smart_card_button")
            )
        }
    }
}

data class SmartCardState(
    val title: String,
    val description: String,
    val buttonText: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

// -------------------------------------------------------------
// --- REDESIGNED COMPONENT: FEATURED PROFILE SECTION ---
// -------------------------------------------------------------
@Composable
fun FeaturedProfileSection(
    featuredUser: AppUser,
    viewModel: MainViewModel,
    onPlayerClick: (String) -> Unit,
    onClubClick: (String) -> Unit,
    onUserClick: (AppUser) -> Unit
) {
    val players by viewModel.players.collectAsState()

    val roleColor = when (featuredUser.role) {
        UserRole.PLAYER -> NeonCyan
        UserRole.CLUB -> Color(0xFFFFD700)
        UserRole.SCOUT -> Color(0xFFE040FB)
        UserRole.COACH -> Color(0xFF00E676)
        UserRole.MEDIA -> Color(0xFFFF9100)
        UserRole.STORE -> Color(0xFFFFEA00)
        UserRole.PITCH -> Color(0xFF00E5FF)
        UserRole.ORGANIZER -> Color(0xFFFF3D00)
        UserRole.ADMIN -> Color.Red
    }

    val roleLabel = when (featuredUser.role) {
        UserRole.PLAYER -> "ÖNE ÇIKAN FUTBOLCU"
        UserRole.CLUB -> "ÖNE ÇIKAN KULÜP"
        UserRole.SCOUT -> "ÖNE ÇIKAN SCOUT"
        UserRole.COACH -> "ÖNE ÇIKAN ANTRENÖR"
        UserRole.MEDIA -> "ÖNE ÇIKAN MEDYA EKİBİ"
        UserRole.STORE -> "ÖNE ÇIKAN SPOR MAĞAZASI"
        UserRole.PITCH -> "ÖNE ÇIKAN HALI SAHA"
        UserRole.ORGANIZER -> "ÖNE ÇIKAN ORGANİZATÖR"
        UserRole.ADMIN -> "ÖNE ÇIKAN YÖNETİCİ"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("featured_profile_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(roleColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "⭐ ÖNE ÇIKAN PROFİL",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        PremiumCard(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, roleColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
            borderGlow = false
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile picture / Logo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBorder)
                        .border(1.5.dp, roleColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (featuredUser.photoUrl.isNotEmpty()) {
                        Image(
                            painter = rememberAsyncImagePainter(model = featuredUser.photoUrl),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = TextGray,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(roleColor.copy(alpha = 0.12f))
                            .border(0.5.dp, roleColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = roleLabel,
                            color = roleColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Name
                    Text(
                        text = featuredUser.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Subtitle / City / Details
                    val subtitleText = remember(featuredUser) {
                        var details = featuredUser.city
                        if (featuredUser.role == UserRole.PLAYER) {
                            val matchingPlayer = players.find { it.id == featuredUser.id }
                            if (matchingPlayer != null) {
                                details += " • ${matchingPlayer.position.shortName} • ${matchingPlayer.age} Yaş"
                            }
                        } else if (featuredUser.role == UserRole.COACH && featuredUser.club.isNotEmpty()) {
                            details += " • ${featuredUser.club}"
                        } else if (featuredUser.role == UserRole.SCOUT && featuredUser.club.isNotEmpty()) {
                            details += " • ${featuredUser.club}"
                        }
                        details
                    }

                    Text(
                        text = subtitleText,
                        color = TextGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                NeonButton(
                    text = "PROFİLİ GÖR",
                    onClick = {
                        when (featuredUser.role) {
                            UserRole.PLAYER -> onPlayerClick(featuredUser.id)
                            UserRole.CLUB -> onClubClick(featuredUser.id)
                            else -> onUserClick(featuredUser)
                        }
                    },
                    modifier = Modifier.testTag("featured_profile_button")
                )
            }
        }
    }
}
