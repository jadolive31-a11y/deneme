package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

@Composable
fun AdminDashboardScreenContent(
    viewModel: MainViewModel
) {
    val users by viewModel.users.collectAsState()
    val clubs by viewModel.clubs.collectAsState()
    val tournaments by viewModel.tournaments.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) } // 0=Genel Bakış, 1=Kullanıcılar, 2=Kulüpler, 3=Turnuvalar, 4=Haberler & Bildirim

    val subTabs = listOf("Genel Bakış", "Kullanıcılar", "Kulüpler", "Turnuvalar", "Haber/Bildirim")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AlmostBlack)
    ) {
        // Horizontal scrollable premium sub-tabs
        ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = AlmostBlack,
            contentColor = NeonCyan,
            edgePadding = 12.dp
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = activeSubTab == index,
                    onClick = { activeSubTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeSubTab == index) NeonCyan else TextGray
                        )
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            when (activeSubTab) {
                0 -> OverviewTabContent(users, tournaments, viewModel)
                1 -> UsersManagementTabContent(users, viewModel)
                2 -> ClubsManagementTabContent(clubs, viewModel)
                3 -> TournamentsManagementTabContent(tournaments, viewModel)
                4 -> AnnouncementsTabContent(viewModel)
            }
        }
    }
}

@Composable
fun OverviewTabContent(
    users: List<AppUser>,
    tournaments: List<Tournament>,
    viewModel: MainViewModel
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "ADMİN METRİKLERİ",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }

        item {
            // Stats Grid (Total Users, Active Tournaments)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSlate)
                        .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Toplam Üye", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${users.size}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Aktif Futbol Ekosistemi", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSlate)
                        .border(1.dp, NeonCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Turnuva / Arena", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("${tournaments.size}", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tam Gaz Mücadele", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            // Role breakdown list
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("ROL DAĞILIMI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(12.dp))

                val roles = listOf(
                    Triple("⚽ Futbolcu", users.count { it.role == UserRole.PLAYER }, "Oyuncu profili aktif"),
                    Triple("🕵️ Scout", users.count { it.role == UserRole.SCOUT }, "Gözlemci onaylı"),
                    Triple("👨‍🏫 Antrenör", users.count { it.role == UserRole.COACH }, "Lisanslı teknik hoca"),
                    Triple("🏟️ Kulüp", users.count { it.role == UserRole.CLUB }, "Tescilli organizasyon"),
                    Triple("🎥 Medya Ekibi", users.count { it.role == UserRole.MEDIA }, "Medya ve çekim ekibi")
                )

                roles.forEach { (roleName, count, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(roleName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(desc, color = TextGray, fontSize = 10.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AlmostBlack)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("$count Üye", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Divider(color = DarkBorder, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }

        item {
            var showResetDialog by remember { mutableStateOf(false) }
            var isResetting by remember { mutableStateOf(false) }
            var resetError by remember { mutableStateOf<String?>(null) }

            Text(
                text = "⚡ ADMİN SİSTEM ARACI",
                color = OrangeWarning,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = null,
                            tint = OrangeWarning,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tüm Ekosistemi Temizle / Sıfırla",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Oluşturulan tüm oyuncuları, kulüpleri, beğenileri ve mesajları veritabanından TAMAMEN SİLER ve ekosistemi sıfırdan başlatır.",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    NeonButton(
                        text = if (isResetting) "Temizleniyor..." else "Tüm Ekosistemi Temizle",
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
                            text = "Bu işlem, veritabanındaki tüm oyuncuları, kulüpleri, beğenileri, takip ilişkilerini ve konuşmaları TAMAMEN SİLEREK ekosistemi sıfırlayacaktır. Sizin admin hesabınız dışındaki tüm özel oluşturulmuş kullanıcılar silinecektir.\n\nEmin misiniz?",
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
                                    },
                                    onFailure = { err ->
                                        isResetting = false
                                        resetError = "Hata oluştu: ${err.message}"
                                    }
                                )
                            }
                        ) {
                            Text("Evet, Sıfırla", color = OrangeWarning, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showResetDialog = false }) {
                            Text("Vazgeç", color = Color.White)
                        }
                    },
                    containerColor = DarkSlate,
                    titleContentColor = Color.White,
                    textContentColor = TextGray
                )
            }
        }

        item {
            Text(
                text = "YENİ KAYITLAR & ONAYLAR",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Show last 3 users needing verification/moderation
        val recentUsers = users.takeLast(4).reversed()
        if (recentUsers.isEmpty()) {
            item {
                Text("Yeni kayıt bulunmuyor.", color = TextGray, fontSize = 12.sp)
            }
        } else {
            items(recentUsers) { user ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSlate)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(user.photoUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = "Rol: ${user.role.name} • Şehir: ${user.city}",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                        }

                        // Quick Actions
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { viewModel.toggleVerifyUser(user.id) },
                                modifier = Modifier.size(32.dp).background(AlmostBlack, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (user.isVerified) Icons.Default.Close else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = if (user.isVerified) OrangeWarning else NeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleBanUser(user.id) },
                                modifier = Modifier.size(32.dp).background(AlmostBlack, CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = null,
                                    tint = if (user.isBanned) OrangeWarning else TextGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UsersManagementTabContent(
    users: List<AppUser>,
    viewModel: MainViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf<UserRole?>(null) }
    var showRoleDropdownForUser by remember { mutableStateOf<String?>(null) }

    val filteredUsers = users.filter {
        it.name.contains(searchQuery, ignoreCase = true) &&
        (selectedRoleFilter == null || it.role == selectedRoleFilter) &&
        it.role != UserRole.ADMIN // Admin never listed
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            CustomTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "Kullanıcı Adı Ara...",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
                testTag = "admin_user_search"
            )
        }

        // Role quick filters
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(null) + listOf(
                    UserRole.PLAYER, UserRole.SCOUT, UserRole.COACH,
                    UserRole.CLUB, UserRole.MEDIA
                )

                filters.forEach { filter ->
                    val isSelected = selectedRoleFilter == filter
                    val label = when (filter) {
                        null -> "Hepsi"
                        UserRole.PLAYER -> "Futbolcu"
                        UserRole.SCOUT -> "Scout"
                        UserRole.COACH -> "Antrenör"
                        UserRole.CLUB -> "Kulüp"
                        UserRole.MEDIA -> "Medya"
                        else -> "Diğer"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) NeonCyan else DarkSlate)
                            .border(1.dp, if (isSelected) Color.Transparent else DarkBorder, RoundedCornerShape(12.dp))
                            .clickable { selectedRoleFilter = filter }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) AlmostBlack else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        items(filteredUsers) { user ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (user.isBanned) DarkSlate.copy(alpha = 0.5f) else DarkSlate)
                    .border(1.dp, if (user.isBanned) OrangeWarning.copy(alpha = 0.3f) else DarkBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(user.photoUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = user.name,
                                    color = if (user.isBanned) TextGray else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (user.isVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(14.dp))
                                }
                            }
                            Text(
                                text = "Rol: ${user.role.name} • Şehir: ${user.city}",
                                color = TextGray,
                                fontSize = 11.sp
                            )
                            if (user.isBanned) {
                                Text(
                                    text = "ENGELLEMİŞ KULLANICI",
                                    color = OrangeWarning,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Options Column
                        Column(horizontalAlignment = Alignment.End) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Toggle verification
                                IconButton(
                                    onClick = { viewModel.toggleVerifyUser(user.id) },
                                    modifier = Modifier.size(32.dp).background(AlmostBlack, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = if (user.isVerified) NeonCyan else TextGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                // Toggle ban
                                IconButton(
                                    onClick = { viewModel.toggleBanUser(user.id) },
                                    modifier = Modifier.size(32.dp).background(AlmostBlack, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Block,
                                        contentDescription = null,
                                        tint = if (user.isBanned) OrangeWarning else TextGray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                // Delete
                                IconButton(
                                    onClick = {
                                        viewModel.deleteUser(
                                            userId = user.id,
                                            onSuccess = {
                                                android.widget.Toast.makeText(context, "Kullanıcı başarıyla silindi.", android.widget.Toast.LENGTH_SHORT).show()
                                            },
                                            onFailure = { err ->
                                                android.widget.Toast.makeText(context, "Silme başarısız: Yetki yetersiz veya sunucu hatası oluştu.", android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        )
                                    },
                                    modifier = Modifier.size(32.dp).background(AlmostBlack, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = OrangeWarning,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Role Modifier Button
                            Text(
                                text = "Rolü Değiştir",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        showRoleDropdownForUser = if (showRoleDropdownForUser == user.id) null else user.id
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }

                    if (showRoleDropdownForUser == user.id) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("YENİ ROL ATAYIN:", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                UserRole.PLAYER, UserRole.SCOUT, UserRole.COACH,
                                UserRole.CLUB, UserRole.MEDIA
                            ).forEach { role ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AlmostBlack)
                                        .clickable {
                                            viewModel.changeUserRole(user.id, role)
                                            showRoleDropdownForUser = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(role.name, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClubsManagementTabContent(
    clubs: List<Club>,
    viewModel: MainViewModel
) {
    var selectedClubIdForNews by remember { mutableStateOf<String?>(null) }
    var newsTitle by remember { mutableStateOf("") }
    var newsContent by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "KULÜP LİSANS & ONAY YÖNETİMİ",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
        }

        items(clubs) { club ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSlate)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                        ) {
                            Image(
                                painter = rememberAsyncImagePainter(club.logoUrl),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(club.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Şehir: ${club.city} • Durum: ${club.licenseStatus}", color = TextGray, fontSize = 11.sp)
                            Text("Tip: ${club.clubType}", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        // Actions
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AlmostBlack)
                                        .clickable { viewModel.approveClub(club.id) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("Onayla", color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AlmostBlack)
                                        .clickable { viewModel.featureClub(club.id) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("Öne Çıkar", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AlmostBlack)
                                        .clickable { viewModel.passivateClub(club.id) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text("Pasif Yap", color = OrangeWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "✍️ Kulüp Haberini Paylaş",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                selectedClubIdForNews = if (selectedClubIdForNews == club.id) null else club.id
                            }
                            .padding(vertical = 4.dp)
                    )

                    if (selectedClubIdForNews == club.id) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CustomTextField(value = newsTitle, onValueChange = { newsTitle = it }, label = "Haber Başlığı")
                            CustomTextField(value = newsContent, onValueChange = { newsContent = it }, label = "Haber İçeriği", modifier = Modifier.height(80.dp))
                            
                            NeonButton(
                                text = "Haber Yayınla",
                                onClick = {
                                    if (newsTitle.isNotBlank() && newsContent.isNotBlank()) {
                                        viewModel.addClubNews(club.id, newsTitle, newsContent)
                                        newsTitle = ""
                                        newsContent = ""
                                        selectedClubIdForNews = null
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TournamentsManagementTabContent(
    tournaments: List<Tournament>,
    viewModel: MainViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showCreateForm by remember { mutableStateOf(false) }

    // Create Form fields
    var tTitle by remember { mutableStateOf("") }
    var tDesc by remember { mutableStateOf("") }
    var tCountdown by remember { mutableStateOf("7 Gün Kaldı") }
    var tPrize by remember { mutableStateOf("10,000 TL") }
    var tMaxPart by remember { mutableStateOf("16") }
    var tDiff by remember { mutableStateOf("Orta") }

    var selectedTournamentForWinner by remember { mutableStateOf<String?>(null) }
    var winnerName by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TURNUVA & KAZANAN YÖNETİMİ",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan)
                        .clickable { showCreateForm = !showCreateForm }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (showCreateForm) "Formu Kapat" else "+ Yeni Turnuva",
                        color = AlmostBlack,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showCreateForm) {
            item {
                PremiumCard(modifier = Modifier.fillMaxWidth()) {
                    Text("YENİ TURNUVA OLUŞTUR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    CustomTextField(value = tTitle, onValueChange = { tTitle = it }, label = "Turnuva Başlığı")
                    Spacer(modifier = Modifier.height(8.dp))
                    CustomTextField(value = tDesc, onValueChange = { tDesc = it }, label = "Açıklama")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(value = tCountdown, onValueChange = { tCountdown = it }, label = "Geri Sayım")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(value = tPrize, onValueChange = { tPrize = it }, label = "Ödül Havuzu")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(value = tMaxPart, onValueChange = { tMaxPart = it }, label = "Max Katılımcı")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            CustomTextField(value = tDiff, onValueChange = { tDiff = it }, label = "Zorluk Derecesi")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    NeonButton(
                        text = "Turnuvayı Kaydet ve Yayınla",
                        onClick = {
                            if (tTitle.isNotBlank()) {
                                viewModel.addTournament(tTitle, tDesc, tCountdown, tPrize, tMaxPart.toIntOrNull() ?: 16, tDiff)
                                tTitle = ""
                                tDesc = ""
                                showCreateForm = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        items(tournaments) { t ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSlate)
                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Ödül: ${t.prize} • Zorluk: ${t.difficulty}", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("Durum: ${t.countdown}", color = TextGray, fontSize = 10.sp)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { selectedTournamentForWinner = if (selectedTournamentForWinner == t.id) null else t.id },
                                modifier = Modifier.size(32.dp).background(AlmostBlack, CircleShape)
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteTournament(
                                        id = t.id,
                                        onSuccess = {
                                            android.widget.Toast.makeText(context, "Turnuva başarıyla silindi.", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        onFailure = { err ->
                                            android.widget.Toast.makeText(context, "Silme başarısız: Yetki yetersiz veya sunucu hatası oluştu.", android.widget.Toast.LENGTH_LONG).show()
                                        }
                                    )
                                },
                                modifier = Modifier.size(32.dp).background(AlmostBlack, CircleShape)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = OrangeWarning, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (selectedTournamentForWinner == t.id) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("KAZANANI BELİRLE VE KAPAT:", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1.5f)) {
                                CustomTextField(value = winnerName, onValueChange = { winnerName = it }, label = "Kazanan Oyuncu Adı")
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                NeonButton(
                                    text = "Kaydet",
                                    onClick = {
                                        if (winnerName.isNotBlank()) {
                                            viewModel.selectTournamentWinner(t.id, winnerName)
                                            winnerName = ""
                                            selectedTournamentForWinner = null
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnnouncementsTabContent(
    viewModel: MainViewModel
) {
    var newsTitle by remember { mutableStateOf("") }
    var newsContent by remember { mutableStateOf("") }

    var pushTitle by remember { mutableStateOf("") }
    var pushDesc by remember { mutableStateOf("") }

    var successMessage by remember { mutableStateOf("") }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "HABER & BİLDİRİM MERKEZİ",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }

        if (successMessage.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(successMessage, color = NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Global News posting
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("ANA SAYFADA HABER YAYINLA", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("Ana sayfa akışında tüm kullanıcıların göreceği duyuru/haber paylaşımı yapın.", color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(10.dp))
                CustomTextField(value = newsTitle, onValueChange = { newsTitle = it }, label = "Haber Başlığı")
                Spacer(modifier = Modifier.height(8.dp))
                CustomTextField(value = newsContent, onValueChange = { newsContent = it }, label = "Haber Detayları", modifier = Modifier.height(100.dp))
                Spacer(modifier = Modifier.height(16.dp))
                NeonButton(
                    text = "Haberi Yayınla",
                    onClick = {
                        if (newsTitle.isNotBlank() && newsContent.isNotBlank()) {
                            viewModel.addHaber(newsTitle, newsContent)
                            successMessage = "Haber ana sayfada başarıyla yayınlandı!"
                            newsTitle = ""
                            newsContent = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Push notification sending
        item {
            PremiumCard(modifier = Modifier.fillMaxWidth()) {
                Text("TÜM KULLANICILARA BİLDİRİM GÖNDER", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
                Text("Her kullanıcının bildirim kutusuna anlık mesaj iletin.", color = TextGray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 4.dp))
                Spacer(modifier = Modifier.height(10.dp))
                CustomTextField(value = pushTitle, onValueChange = { pushTitle = it }, label = "Bildirim Başlığı")
                Spacer(modifier = Modifier.height(8.dp))
                CustomTextField(value = pushDesc, onValueChange = { pushDesc = it }, label = "Bildirim Açıklaması")
                Spacer(modifier = Modifier.height(16.dp))
                NeonButton(
                    text = "Bildirimi Gönder",
                    onClick = {
                        if (pushTitle.isNotBlank() && pushDesc.isNotBlank()) {
                            viewModel.addNotificationToAll(pushTitle, pushDesc)
                            successMessage = "Bildirim tüm üyelere başarıyla gönderildi!"
                            pushTitle = ""
                            pushDesc = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
