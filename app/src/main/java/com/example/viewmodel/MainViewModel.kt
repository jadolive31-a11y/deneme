package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.model.*
import com.example.repository.FutbolcuBulRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.Bitmap
import com.example.ui.components.uploadBitmapToStorage

data class SearchFilters(
    val position: FootballPosition? = null,
    val club: String = "",
    val city: String = "",
    val foot: PreferredFoot? = null,
    val minAge: Int = 6,
    val maxAge: Int = 50,
    val minHeight: Int = 100,
    val maxHeight: Int = 220,
    val minWeight: Int = 20,
    val maxWeight: Int = 120,
    val minRating: Int = 0,
    val isVerifiedOnly: Boolean = false
)

data class WizardState(
    val currentStep: Int = 1,
    // Step 1: Photo & Adjustments
    val photoUrl: String = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=300",
    val zoom: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    // Step 2: Personal
    val firstName: String = "",
    val lastName: String = "",
    val birthDate: String = "2007-06-15",
    val nationality: String = "Türkiye",
    val city: String = "İstanbul",
    // Step 3: Physical
    val height: Int = 180,
    val weight: Int = 74,
    val preferredFoot: PreferredFoot = PreferredFoot.RIGHT,
    // Step 4: Football
    val position: FootballPosition = FootballPosition.ST,
    val secondaryPositions: List<FootballPosition> = emptyList(),
    val club: String = "Kulüpsüz",
    val jerseyNumber: Int = 0,
    // Step 5: Bio
    val bio: String = "",
    val instagram: String = "",
    val youtubeUrl: String = "",
    // Calculated rating for the card (Step 6)
    val pace: Int = 75,
    val shooting: Int = 70,
    val passing: Int = 68,
    val dribbling: Int = 73,
    val defense: Int = 45,
    val physical: Int = 70,
    val weakFoot: Int = 3,
    val skillMoves: Int = 3
)

class MainViewModel : ViewModel() {

    private val repository = FutbolcuBulRepository()

    // Screen States
    private val _currentUserRole = MutableStateFlow<UserRole>(UserRole.PLAYER)
    val currentUserRole: StateFlow<UserRole> = _currentUserRole.asStateFlow()

    private val _currentUserId = MutableStateFlow<String>("p_1") // Starts logged in as Mert Yılmaz initially
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    // Central lists
    val users: StateFlow<List<AppUser>> = repository.users

    val players: StateFlow<List<Player>> = repository.players
    val clubs: StateFlow<List<Club>> = repository.clubs
    val applications: StateFlow<List<ClubApplication>> = repository.applications

    val tournaments: StateFlow<List<Tournament>> = repository.tournaments
    val chats: StateFlow<List<Chat>> = repository.chats
    val notifications: StateFlow<List<Notification>> = repository.notifications
    val favoritePlayerIds: StateFlow<Set<String>> = repository.favoritePlayerIds
    val activityFeed: StateFlow<List<ActivityItem>> = repository.activityFeed
    val isPlayersLoaded: StateFlow<Boolean> = repository.isPlayersLoaded

    private val _goalCandidates = MutableStateFlow<List<GoalCandidate>>(
        listOf(
            GoalCandidate(
                id = "goal_1",
                playerName = "Mert Yılmaz",
                playerPosition = "AM",
                playerClub = "Kayserispor U21",
                playerPhotoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=300",
                goalDescription = "30 metreden tam doksana çatallık nefis frikik golü! Kaleci çaresiz kaldı.",
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                voteCount = 142,
                weekNumber = 28
            ),
            GoalCandidate(
                id = "goal_2",
                playerName = "Kaan Demir",
                playerPosition = "ST",
                playerClub = "Talasgücü Belediyespor",
                playerPhotoUrl = "https://images.unsplash.com/photo-1544698310-74ea9d1c8258?q=80&w=300",
                goalDescription = "Harika bir vole golü! Sol kanattan gelen ortayı gelişine sol ayakla ağlara gönderdi.",
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                voteCount = 125,
                weekNumber = 28
            ),
            GoalCandidate(
                id = "goal_3",
                playerName = "Yusuf Arslan",
                playerPosition = "LW",
                playerClub = "Melikgazi Halı Saha Karması",
                playerPhotoUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=300",
                goalDescription = "Müthiş akrobatik rövaşata golü! Rakip defansın arasından havada süzülerek spektaküler bitiriş.",
                videoUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                voteCount = 98,
                weekNumber = 28
            )
        )
    )
    val goalCandidates: StateFlow<List<GoalCandidate>> = _goalCandidates.asStateFlow()

    private val _hasVotedGoal = MutableStateFlow<Boolean>(false)
    val hasVotedGoal: StateFlow<Boolean> = _hasVotedGoal.asStateFlow()

    fun voteForGoal(candidateId: String) {
        if (_hasVotedGoal.value) return
        _goalCandidates.value = _goalCandidates.value.map {
            if (it.id == candidateId) {
                it.copy(voteCount = it.voteCount + 1, hasVoted = true)
            } else {
                it
            }
        }
        _hasVotedGoal.value = true
    }

    fun submitGoalCandidate(playerName: String, position: String, club: String, goalDesc: String, videoUrl: String, photoUrl: String?) {
        val newId = "goal_" + System.currentTimeMillis()
        val newCandidate = GoalCandidate(
            id = newId,
            playerName = playerName,
            playerPosition = position,
            playerClub = club,
            playerPhotoUrl = photoUrl ?: "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=300",
            goalDescription = goalDesc,
            videoUrl = videoUrl.ifBlank { "https://www.youtube.com/watch?v=dQw4w9WgXcQ" },
            voteCount = 0,
            weekNumber = 28
        )
        _goalCandidates.value = _goalCandidates.value + newCandidate
    }

    private val _exploreItems = MutableStateFlow<List<ExploreItem>>(
        listOf(
            // TURNUVALAR
            ExploreItem(
                id = "t_1",
                category = "🏆 Turnuvalar",
                title = "Kayseri Premier Halı Saha Ligi",
                description = "Kayseri'nin en elit takımlarının mücadele ettiği, profesyonel scoutların ve kamera ekibinin yer aldığı efsanevi lig.",
                coverUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=600",
                priceOrReward = "50,000 TL Ödül Havuzu",
                buttonText = "LİGE KATIL",
                location = "Melikgazi, Kayseri",
                details = "Kayseri Premier Halı Saha Ligi her sezon 16 takımın katılımıyla gerçekleşir. Maçlar HD kalitede kaydedilir, istatistikler anlık tutulur ve haftanın en iyi 7'si seçilir. Şampiyon takıma büyük nakit ödülü ve kupa verilir."
            ),
            ExploreItem(
                id = "t_2",
                category = "🏆 Turnuvalar",
                title = "Erciyes Genç Yetenekler Kupası",
                description = "U19 - U21 kategorisindeki gençlerin yeteneklerini sergileyebileceği, Kayserispor scout ekibinin izleyeceği turnuva.",
                coverUrl = "https://images.unsplash.com/photo-1517649763962-0c623066013b?q=80&w=600",
                priceOrReward = "Profesyonel Ekipman Seti",
                buttonText = "BAŞVURU YAP",
                location = "Talas, Kayseri",
                details = "Kayserispor altyapı scoutları tarafından yakından takip edilecek bu özel turnuvada bireysel ve takım halinde performanslar değerlendirilecektir. Geleceğin profesyonel futbolcusu olmak için büyük bir fırsat."
            ),
            
            // CHALLENGE
            ExploreItem(
                id = "c_1",
                category = "🎯 Challenge",
                title = "1v1 Kafes Turnuvası",
                description = "Bireysel yeteneğine güvenenlerin buluştuğu Kayseri Kafes Turnuvası. Rakibini geç, golünü at, şampiyon ol!",
                coverUrl = "https://images.unsplash.com/photo-1431324155629-1a6edd1def2d?q=80&w=600",
                priceOrReward = "Nike Phantom GX II Krampon",
                buttonText = "CHALLENGE BAŞLAT",
                details = "Tek teke mücadele, dar alanda yüksek tempo ve teknik beceri. 3 dakika süren maçlarda en çok gol atan bir üst tura yükselir."
            ),
            ExploreItem(
                id = "c_2",
                category = "🎯 Challenge",
                title = "Kayseri Ayak Tenisi Ligi Challenge",
                description = "En az 2 kişilik takımını kur, Kayseri genelindeki ayak tenisi turnuvasında rakiplerini alt et ve şampiyon ol!",
                coverUrl = "https://images.unsplash.com/photo-1544698310-74ea9d1c8258?q=80&w=600",
                priceOrReward = "3,000 TL Hediye Çeki",
                buttonText = "MEYDAN OKU",
                details = "Ayak tenisi becerilerini sergileyebileceğin, çiftler halinde yarışılan profesyonel organizasyon. Eleme usulü turnuva takvimi Kayseri'deki seçkin halı sahalarda gerçekleşecektir."
            ),
            ExploreItem(
                id = "c_3",
                category = "🎯 Challenge",
                title = "Kayseri En İyi Serbest Vuruş Yarışması",
                description = "Halı sahada serbest vuruş becerilerini sergile, en iyi frikik atan oyuncular arasına adını yazdır.",
                coverUrl = "https://images.unsplash.com/photo-1516515429572-bf32372f2409?q=80&w=600",
                priceOrReward = "Özel Kristal Kupa + Plaket",
                buttonText = "GOLÜNÜ YÜKLE",
                details = "Halı sahada attığın en iyi serbest vuruş golünün videosunu sisteme yükle, oylamada en yüksek puanı al ve ödülü kazan!"
            ),

            // MAĞAZALAR
            ExploreItem(
                id = "s_1",
                category = "🏪 Mağazalar",
                title = "Kayseri Futbol Dünyası",
                description = "Kayseri'nin en büyük futbol ekipman mağazası. En yeni kramponlar, formalar ve aksesuarlar burada.",
                coverUrl = "https://images.unsplash.com/photo-1551958219-acbc608c6377?q=80&w=600",
                priceOrReward = "%20 İndirim Kodu: ERCİYES20",
                buttonText = "MAĞAZAYI GEZ",
                phone = "+90 352 123 4567",
                instagram = "@kayserifutboldunyasi",
                whatsapp = "https://wa.me/903521234567",
                code = "ERCİYES20",
                details = "Talas Bulvarı üzerinde yer alan mağazamızda dünyaca ünlü markaların lisanslı krampon, forma ve kaleci ekipmanlarını bulabilirsiniz. Uygulama kullanıcılarına özel indirim kodu geçerlidir.",
                itemsList = listOf(
                    StoreItem("Nike Phantom GX II Elite", "4,999 TL", "https://images.unsplash.com/photo-1542291026-7eec264c27ff?q=80&w=200"),
                    StoreItem("Adidas Predator 24 Pro", "4,799 TL", "https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?q=80&w=200"),
                    StoreItem("Puma Future Ultimate FG", "4,499 TL", "https://images.unsplash.com/photo-1608231387042-66d1773070a5?q=80&w=200")
                )
            ),

            // MEDYA EKİBİ (MERGED)
            ExploreItem(
                id = "k_1",
                category = "🎥 Medya Ekibi",
                title = "Mehmet Demir - Profesyonel Spor Videografi & Drone",
                description = "4K kalitesinde drone ve saha kenarı tripod kameralar ile profesyonel maç çekimi ve Reels özetleri.",
                coverUrl = "https://images.unsplash.com/photo-1492691527719-9d1e07e534b4?q=80&w=600",
                priceOrReward = "1,500 TL / Maç",
                buttonText = "REZERVASYON YAP",
                rating = 4.9,
                phone = "+90 532 111 2233",
                instagram = "@mehmetdemirvideo",
                whatsapp = "https://wa.me/905321112233",
                details = "Kayseri genelinde tüm halı sahalarda profesyonel maç kaydı alıyorum. Drone ile havadan çekim ve maç sonu gollerin editlenerek teslimi fiyata dahildir."
            ),
            ExploreItem(
                id = "p_1",
                category = "🎥 Medya Ekibi",
                title = "Tolga Şahin - Kayseri Spor Fotoğrafçılığı",
                description = "Yüksek hızlı profesyonel spor lensleri ile maç içi aksiyon, depar ve gol sevinci kareleri.",
                coverUrl = "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?q=80&w=600",
                priceOrReward = "1,000 TL / Maç",
                buttonText = "RANDEVU AL",
                rating = 4.9,
                phone = "+90 544 333 4455",
                instagram = "@tolgasahinphoto",
                whatsapp = "https://wa.me/905443334455",
                details = "Maç boyu saha kenarından en dinamik anlarınızı donduruyorum. 100+ adet elenmiş ve editlenmiş yüksek çözünürlüklü dijital fotoğraf teslimi."
            ),

            // ANTRENÖR İLANLARI
            ExploreItem(
                id = "co_1",
                category = "🎓 Antrenör İlanları",
                title = "Gökhan Saygı - Profesyonel Kaleci Antrenörü",
                description = "TFF lisanslı kaleci antrenörü olarak amatör ve profesyonel kalecilere özel teknik ve reaksiyon idmanları.",
                coverUrl = "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=600",
                priceOrReward = "800 TL / Seans",
                buttonText = "İLETİŞİME GEÇ",
                rating = 4.9,
                phone = "+90 555 123 4567",
                instagram = "@gokhansaygicoach",
                whatsapp = "https://wa.me/905551234567",
                details = "Kaleci tekniği, ayak kullanımı, pozisyon alma ve zihinsel hazırlık alanlarında bireysel veya grup eğitimleri. Kayseri genelinde anlaşmalı tesislerde eğitim imkanı."
            ),
            ExploreItem(
                id = "co_2",
                category = "🎓 Antrenör İlanları",
                title = "Ahmet Yılmaz - UEFA B Lisanslı Bireysel Futbol Antrenörü",
                description = "Genç yeteneklerin taktiksel, fiziksel ve teknik gelişimini artırmaya yönelik kişiye özel antrenman programları.",
                coverUrl = "https://images.unsplash.com/photo-1508847154043-be12a327dc6f?q=80&w=600",
                priceOrReward = "600 TL / Saat",
                buttonText = "RANDEVU AL",
                rating = 4.8,
                phone = "+90 507 987 6543",
                instagram = "@ahmetyilmazfutbol",
                whatsapp = "https://wa.me/905079876543",
                details = "Halı saha takımları için taktiksel danışmanlık veya oyuncular için özel teknik ve kondisyon gelişim idmanları. Profesyonel ekipmanlarla modern antrenman metodolojisi."
            ),

            // FUTBOL OKULLARI (NEW CATEGORY WITH ADS)
            ExploreItem(
                id = "fo_1",
                category = "🏫 Futbol Okulları",
                title = "Kayseri Atlas Futbol Okulu - Geleceğin Yıldızları",
                description = "6-15 yaş arası çocuklar için profesyonel antrenörler eşliğinde futbol eğitimi ve lisanslı sporcu yetiştirme programı.",
                coverUrl = "https://images.unsplash.com/photo-1526232761682-d26e03ac148e?q=80&w=600",
                priceOrReward = "1,500 TL / Aylık",
                buttonText = "DETAYLI BİLGİ",
                rating = 4.9,
                phone = "+90 352 444 0338",
                instagram = "@kayseriatlasfutbol",
                whatsapp = "https://wa.me/903524440338",
                details = "Kayseri Atlas Futbol Okulu olarak her çocuğun gelişimini yakından takip ediyoruz. Tesislerimizde suni çim zemin, modern antrenman ekipmanları ve pedogojik formasyon sahibi antrenörlerimiz ile haftada iki gün eğitim verilmektedir."
            ),
            ExploreItem(
                id = "fo_2",
                category = "🏫 Futbol Okulları",
                title = "Erciyes Yıldızları Spor Kulübü Akademisi",
                description = "UEFA lisanslı hocalar gözetiminde altyapı taramaları, hazırlık turnuvaları ve profesyonel kulüplere seçilme imkanı.",
                coverUrl = "https://images.unsplash.com/photo-1517649763962-0c623066013b?q=80&w=600",
                priceOrReward = "1,200 TL / Aylık",
                buttonText = "KAYIT OL",
                rating = 4.8,
                phone = "+90 352 555 3838",
                instagram = "@erciyesyildizlarisk",
                whatsapp = "https://wa.me/903525553838",
                details = "Geleceğin elit futbolcularını yetiştirmeyi amaçlayan akademimizde, fiziksel kondisyonun yanı sıra takım ruhu, oyun zekası ve centilmenlik öncelikli ilkelerimizdir. Kayıtlarımız devam etmektedir."
            ),

            // HALI SAHALAR
            ExploreItem(
                id = "h_1",
                category = "⚽ Halı Sahalar",
                title = "Talas Arena Halı Saha",
                description = "Kayseri Talas'ta son nesil suni çim, mükemmel aydınlatma, lüks soyunma odaları ve cafe alanı ile premium saha keyfi.",
                coverUrl = "https://images.unsplash.com/photo-1459865264687-595d652de67e?q=80&w=600",
                priceOrReward = "1,200 TL / Saat",
                buttonText = "SAHA REZERVE ET",
                rating = 4.9,
                phone = "+90 352 222 1212",
                instagram = "@talasarenahalisaha",
                whatsapp = "https://wa.me/903522221212",
                details = "7v7 and 8v8 maçlar için ideal boyutlarda, kış aylarında ısıtmalı kapalı saha konsepti. Maç sonrası sıcak/soğuk duş imkanı ve otopark mevcuttur."
            )
        )
    )
    val exploreItems: StateFlow<List<ExploreItem>> = _exploreItems.asStateFlow()

    fun addExploreItem(item: ExploreItem) {
        val currentList = _exploreItems.value.toMutableList()
        currentList.add(0, item)
        _exploreItems.value = currentList
    }

    init {
        repository.setCurrentUserId(_currentUserId.value)
        viewModelScope.launch {
            _currentUserId.collect { id ->
                repository.setCurrentUserId(id)
            }
        }
    }

    // Search filters state
    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    // Wizard State
    private val _wizardState = MutableStateFlow(WizardState())
    val wizardState: StateFlow<WizardState> = _wizardState.asStateFlow()

    // Active screen filtering
    val filteredPlayers: StateFlow<List<Player>> = combine(players, _filters, repository.users) { playerList, filter, userList ->
        playerList.filter { player ->
            val matchesPos = filter.position == null || player.position == filter.position
            val matchesClub = filter.club.isEmpty() || player.club.contains(filter.club, ignoreCase = true)
            val playerCityNorm = player.city.lowercase()
                .replace('ı', 'i')
                .replace('ö', 'o')
                .replace('ü', 'u')
                .replace('ş', 's')
                .replace('ç', 'c')
                .replace('ğ', 'g')
                .replace('İ', 'i')
                .trim()
            val filterCityNorm = filter.city.lowercase()
                .replace('ı', 'i')
                .replace('ö', 'o')
                .replace('ü', 'u')
                .replace('ş', 's')
                .replace('ç', 'c')
                .replace('ğ', 'g')
                .replace('İ', 'i')
                .trim()
            val matchesCity = filter.city.isEmpty() || playerCityNorm.contains(filterCityNorm)
            val matchesFoot = filter.foot == null || player.preferredFoot == filter.foot
            val matchesAge = player.age in filter.minAge..filter.maxAge
            val matchesHeight = player.height in filter.minHeight..filter.maxHeight
            val matchesWeight = player.weight in filter.minWeight..filter.maxWeight
            val matchesRating = player.overallRating >= filter.minRating
            val matchesVerified = !filter.isVerifiedOnly || player.isVerified

            matchesPos && matchesClub && matchesCity && matchesFoot && matchesAge && matchesHeight && matchesWeight && matchesRating && matchesVerified
        }.sortedWith(compareByDescending<Player> { player ->
            val user = userList.find { it.id == player.id }
            user?.createdAt ?: "2026-07-11"
        }.thenByDescending { player ->
            player.id
        })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun setRole(role: UserRole) {
        _currentUserRole.value = role
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (firebaseUser != null) {
            _currentUserId.value = firebaseUser.uid
        } else {
            when (role) {
                UserRole.ADMIN -> _currentUserId.value = "admin_1"
                UserRole.PLAYER -> _currentUserId.value = "p_1"
                UserRole.SCOUT -> _currentUserId.value = "scout_hasan"
                UserRole.CLUB -> _currentUserId.value = "club_gs"
                UserRole.COACH -> _currentUserId.value = "coach_fatih"
                UserRole.MEDIA -> _currentUserId.value = "media_burak"
                UserRole.STORE -> _currentUserId.value = "store_hasan"
                UserRole.PITCH -> _currentUserId.value = "pitch_hasan"
                UserRole.ORGANIZER -> _currentUserId.value = "organizer_hasan"
            }
        }
    }

    fun login(email: String, role: UserRole) {
        setRole(role)
    }

    fun applyToTournament(tournamentId: String) {
        repository.applyToTournament(tournamentId)
    }

    fun toggleFavorite(playerId: String) {
        repository.toggleFavoritePlayer(playerId)
    }

    fun incrementViews(playerId: String) {
        repository.incrementPlayerViews(playerId)
    }

    fun sendChatMessage(chatId: String, text: String) {
        if (text.isNotBlank()) {
            repository.sendChatMessage(chatId, text)
        }
    }

    fun startChatWithPlayer(player: Player): String {
        val chat = repository.createChatWithPlayer(player.fullName, player.position.fullName)
        return chat.id
    }

    fun startChatWithUser(name: String, role: String): String {
        val chat = repository.createChatWithPlayer(name, role)
        return chat.id
    }

    fun addScoutReport(playerId: String, technical: Int, tactical: Int, mental: Int, positioning: Int, comment: String) {
        repository.addScoutReport(
            playerId = playerId,
            scoutName = "Hasan Şişman",
            scoutBadge = "UEFA A Scout",
            technical = technical,
            tactical = tactical,
            mental = mental,
            positioning = positioning,
            comment = comment
        )
    }

    fun updatePlayerProfile(
        bio: String,
        instagram: String,
        youtubeUrl: String,
        hasLicense: Boolean,
        club: String,
        previousClubs: String,
        achievements: String,
        pace: Int,
        shooting: Int,
        passing: Int,
        dribbling: Int,
        defense: Int,
        physical: Int,
        height: Int,
        weight: Int,
        age: Int,
        preferredFoot: PreferredFoot,
        position: FootballPosition,
        photoUrl: String? = null
    ) {
        val currentId = _currentUserId.value
        val currentPlayer = repository.players.value.find { it.id == currentId } ?: return

        val updatedPlayer = currentPlayer.copy(
            photoUrl = photoUrl ?: currentPlayer.photoUrl,
            bio = bio,
            instagram = instagram,
            youtubeUrl = youtubeUrl,
            hasLicense = hasLicense,
            club = club,
            previousClubs = previousClubs,
            achievements = achievements,
            height = height,
            weight = weight,
            age = age,
            preferredFoot = preferredFoot,
            position = position,
            selfRating = ((pace + shooting + passing + dribbling + defense + physical) / 6).coerceIn(1, 99),
            stats = PhysicalStats(
                pace = pace,
                shooting = shooting,
                passing = passing,
                dribbling = dribbling,
                defense = defense,
                physical = physical
            )
        )
         repository.updatePlayer(updatedPlayer)
         
         // Sync user details photoUrl with users collection too!
         val currentUserVal = repository.users.value.find { it.id == currentId }
         if (currentUserVal != null && updatedPlayer.photoUrl != null) {
             val updatedUser = currentUserVal.copy(
                 photoUrl = updatedPlayer.photoUrl
             )
             repository.saveUserToFirestore(updatedUser)
         }
    }

    fun updateUserProfile(
        name: String,
        city: String,
        bio: String,
        instagram: String,
        phone: String,
        achievements: String,
        services: String = "",
        price: String = "",
        duty: String = "",
        license: String = "",
        club: String = "",
        photoUrl: String? = null,
        stat1: Int = 85,
        stat2: Int = 85,
        stat3: Int = 85,
        stat4: Int = 85,
        stat5: Int = 85,
        stat6: Int = 85
    ) {
        val currentId = _currentUserId.value
        val currentUserVal = repository.users.value.find { it.id == currentId } ?: return
        val updatedUser = currentUserVal.copy(
            name = name,
            city = city,
            photoUrl = photoUrl ?: currentUserVal.photoUrl,
            bio = bio,
            instagram = instagram,
            phone = phone,
            achievements = achievements,
            services = services,
            price = price,
            duty = duty,
            license = license,
            club = club,
            stat1 = stat1,
            stat2 = stat2,
            stat3 = stat3,
            stat4 = stat4,
            stat5 = stat5,
            stat6 = stat6
        )
        repository.addUser(updatedUser)
    }

    fun uploadProfilePhoto(bitmap: Bitmap, onComplete: (Boolean) -> Unit) {
        val currentId = _currentUserId.value
        if (currentId.isEmpty()) {
            onComplete(false)
            return
        }
        val userRole = currentUserRole.value
        val storagePath = if (userRole == UserRole.CLUB) {
            "clubs/$currentId/logo.jpg"
        } else {
            "profiles/$currentId/profile.jpg"
        }

        uploadBitmapToStorage(
            bitmap = bitmap,
            storagePath = storagePath,
            onSuccess = { downloadUrl ->
                // 1. Update AppUser
                val currentUserVal = repository.users.value.find { it.id == currentId }
                if (currentUserVal != null) {
                    val updatedUser = currentUserVal.copy(photoUrl = downloadUrl)
                    repository.saveUserToFirestore(updatedUser)
                }

                // 2. Update Player/Club if exists
                if (userRole == UserRole.PLAYER) {
                    val currentPlayer = repository.players.value.find { it.id == currentId }
                    if (currentPlayer != null) {
                        val updatedPlayer = currentPlayer.copy(photoUrl = downloadUrl)
                        repository.updatePlayer(updatedPlayer)
                    }
                } else if (userRole == UserRole.CLUB) {
                    val currentClub = repository.clubs.value.find { it.id == currentId }
                    if (currentClub != null) {
                        val updatedClub = currentClub.copy(logoUrl = downloadUrl)
                        repository.saveClubToFirestore(updatedClub)
                    }
                }
                onComplete(true)
            },
            onFailure = {
                onComplete(false)
            }
        )
    }

    private fun updateGalleryModels(currentId: String, userRole: UserRole, newGallery: List<String>) {
        // Update models
        val currentUserVal = repository.users.value.find { it.id == currentId }
        if (currentUserVal != null) {
            val updatedUser = currentUserVal.copy(gallery = newGallery)
            repository.saveUserToFirestore(updatedUser)
        }

        if (userRole == UserRole.PLAYER) {
            val currentPlayer = repository.players.value.find { it.id == currentId }
            if (currentPlayer != null) {
                val updatedPlayer = currentPlayer.copy(gallery = newGallery)
                repository.updatePlayer(updatedPlayer)
            }
        } else if (userRole == UserRole.CLUB) {
            val currentClub = repository.clubs.value.find { it.id == currentId }
            if (currentClub != null) {
                val updatedClub = currentClub.copy(gallery = newGallery)
                repository.saveClubToFirestore(updatedClub)
            }
        }
    }

    fun addPhotoToGallery(context: android.content.Context, bitmap: Bitmap, onComplete: (Boolean) -> Unit) {
        val currentId = _currentUserId.value
        if (currentId.isEmpty()) {
            onComplete(false)
            return
        }
        val userRole = currentUserRole.value
        
        // Check limits based on Premium status
        val isPremium = repository.users.value.find { it.id == currentId }?.isPremium ?: false
        val maxPhotos = if (isPremium) 20 else 5

        val currentGallery = when (userRole) {
            UserRole.PLAYER -> repository.players.value.find { it.id == currentId }?.gallery ?: emptyList()
            UserRole.CLUB -> repository.clubs.value.find { it.id == currentId }?.gallery ?: emptyList()
            else -> repository.users.value.find { it.id == currentId }?.gallery ?: emptyList()
        }

        if (currentGallery.size >= maxPhotos) {
            android.util.Log.e("FutbolcuBul", "Gallery limit reached")
            onComplete(false)
            return
        }

        val uniqueId = java.util.UUID.randomUUID().toString().take(8)
        val storagePath = if (userRole == UserRole.CLUB) {
            "clubs/$currentId/gallery/$uniqueId.jpg"
        } else {
            "profiles/$currentId/gallery/$uniqueId.jpg"
        }

        uploadBitmapToStorage(
            bitmap = bitmap,
            storagePath = storagePath,
            onSuccess = { downloadUrl ->
                val newGallery = currentGallery + downloadUrl
                updateGalleryModels(currentId, userRole, newGallery)
                onComplete(true)
            },
            onFailure = { error ->
                // Fallback: save locally and add local file URI to gallery
                android.util.Log.e("FutbolcuBul", "Storage upload failed, falling back to local storage", error)
                val localPath = com.example.ui.components.saveBitmapToProfileStorage(context, bitmap)
                if (localPath != null) {
                    val localUri = "file://$localPath"
                    val newGallery = currentGallery + localUri
                    updateGalleryModels(currentId, userRole, newGallery)
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }
        )
    }

    fun replacePhotoInGallery(context: android.content.Context, oldUrl: String, newBitmap: Bitmap, onComplete: (Boolean) -> Unit = {}) {
        val currentId = _currentUserId.value
        if (currentId.isEmpty()) {
            onComplete(false)
            return
        }
        val userRole = currentUserRole.value
        val currentGallery = when (userRole) {
            UserRole.PLAYER -> repository.players.value.find { it.id == currentId }?.gallery ?: emptyList()
            UserRole.CLUB -> repository.clubs.value.find { it.id == currentId }?.gallery ?: emptyList()
            else -> repository.users.value.find { it.id == currentId }?.gallery ?: emptyList()
        }

        val uniqueId = java.util.UUID.randomUUID().toString().take(8)
        val storagePath = if (userRole == UserRole.CLUB) {
            "clubs/$currentId/gallery/$uniqueId.jpg"
        } else {
            "profiles/$currentId/gallery/$uniqueId.jpg"
        }

        uploadBitmapToStorage(
            bitmap = newBitmap,
            storagePath = storagePath,
            onSuccess = { downloadUrl ->
                val newGallery = currentGallery.map { if (it == oldUrl) downloadUrl else it }
                updateGalleryModels(currentId, userRole, newGallery)
                
                // Delete old photo from storage
                try {
                    val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
                    val ref = storage.getReferenceFromUrl(oldUrl)
                    ref.delete()
                } catch (e: Exception) {
                    // Ignore
                }
                
                onComplete(true)
            },
            onFailure = { error ->
                android.util.Log.e("FutbolcuBul", "Storage replace failed, falling back to local storage", error)
                val localPath = com.example.ui.components.saveBitmapToProfileStorage(context, newBitmap)
                if (localPath != null) {
                    val localUri = "file://$localPath"
                    val newGallery = currentGallery.map { if (it == oldUrl) localUri else it }
                    updateGalleryModels(currentId, userRole, newGallery)
                    
                    // Delete old local file if it was a local file
                    if (oldUrl.startsWith("file://")) {
                        try {
                            val file = java.io.File(oldUrl.removePrefix("file://"))
                            if (file.exists()) {
                                file.delete()
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                    onComplete(true)
                } else {
                    onComplete(false)
                }
            }
        )
    }

    fun removePhotoFromGallery(photoUrl: String, onComplete: (Boolean) -> Unit = {}) {
        val currentId = _currentUserId.value
        if (currentId.isEmpty()) {
            onComplete(false)
            return
        }
        val userRole = currentUserRole.value

        val currentGallery = when (userRole) {
            UserRole.PLAYER -> repository.players.value.find { it.id == currentId }?.gallery ?: emptyList()
            UserRole.CLUB -> repository.clubs.value.find { it.id == currentId }?.gallery ?: emptyList()
            else -> repository.users.value.find { it.id == currentId }?.gallery ?: emptyList()
        }

        val newGallery = currentGallery.filter { it != photoUrl }

        // Update AppUser
        val currentUserVal = repository.users.value.find { it.id == currentId }
        if (currentUserVal != null) {
            val updatedUser = currentUserVal.copy(gallery = newGallery)
            repository.saveUserToFirestore(updatedUser)
        }

        if (userRole == UserRole.PLAYER) {
            val currentPlayer = repository.players.value.find { it.id == currentId }
            if (currentPlayer != null) {
                val updatedPlayer = currentPlayer.copy(gallery = newGallery)
                repository.updatePlayer(updatedPlayer)
            }
        } else if (userRole == UserRole.CLUB) {
            val currentClub = repository.clubs.value.find { it.id == currentId }
            if (currentClub != null) {
                val updatedClub = currentClub.copy(gallery = newGallery)
                repository.saveClubToFirestore(updatedClub)
            }
        }

        // Try to delete from Firebase Storage asynchronously
        try {
            val storage = com.google.firebase.storage.FirebaseStorage.getInstance()
            val ref = storage.getReferenceFromUrl(photoUrl)
            ref.delete()
        } catch (e: Exception) {
            android.util.Log.e("FutbolcuBul", "Failed to delete storage file", e)
        }
        onComplete(true)
    }

    fun addYoutubeVideo(videoUrl: String, onComplete: (Boolean) -> Unit) {
        val currentId = _currentUserId.value
        if (currentId.isEmpty() || videoUrl.isEmpty()) {
            onComplete(false)
            return
        }
        val userRole = currentUserRole.value

        // Check limit based on Premium
        val isPremium = repository.users.value.find { it.id == currentId }?.isPremium ?: false
        val maxVideos = if (isPremium) 50 else 10

        val currentVideos = when (userRole) {
            UserRole.PLAYER -> repository.players.value.find { it.id == currentId }?.youtubeVideos ?: emptyList()
            UserRole.CLUB -> repository.clubs.value.find { it.id == currentId }?.youtubeVideos ?: emptyList()
            else -> repository.users.value.find { it.id == currentId }?.youtubeVideos ?: emptyList()
        }

        if (currentVideos.size >= maxVideos) {
            onComplete(false)
            return
        }

        val newVideos = currentVideos + videoUrl

        val currentUserVal = repository.users.value.find { it.id == currentId }
        if (currentUserVal != null) {
            val updatedUser = currentUserVal.copy(youtubeVideos = newVideos)
            repository.saveUserToFirestore(updatedUser)
        }

        if (userRole == UserRole.PLAYER) {
            val currentPlayer = repository.players.value.find { it.id == currentId }
            if (currentPlayer != null) {
                val updatedPlayer = currentPlayer.copy(youtubeVideos = newVideos)
                repository.updatePlayer(updatedPlayer)
            }
        } else if (userRole == UserRole.CLUB) {
            val currentClub = repository.clubs.value.find { it.id == currentId }
            if (currentClub != null) {
                val updatedClub = currentClub.copy(youtubeVideos = newVideos)
                repository.saveClubToFirestore(updatedClub)
            }
        }
        onComplete(true)
    }

    fun removeYoutubeVideo(videoUrl: String, onComplete: (Boolean) -> Unit = {}) {
        val currentId = _currentUserId.value
        if (currentId.isEmpty()) {
            onComplete(false)
            return
        }
        val userRole = currentUserRole.value

        val currentVideos = when (userRole) {
            UserRole.PLAYER -> repository.players.value.find { it.id == currentId }?.youtubeVideos ?: emptyList()
            UserRole.CLUB -> repository.clubs.value.find { it.id == currentId }?.youtubeVideos ?: emptyList()
            else -> repository.users.value.find { it.id == currentId }?.youtubeVideos ?: emptyList()
        }

        val newVideos = currentVideos.filter { it != videoUrl }

        val currentUserVal = repository.users.value.find { it.id == currentId }
        if (currentUserVal != null) {
            val updatedUser = currentUserVal.copy(youtubeVideos = newVideos)
            repository.saveUserToFirestore(updatedUser)
        }

        if (userRole == UserRole.PLAYER) {
            val currentPlayer = repository.players.value.find { it.id == currentId }
            if (currentPlayer != null) {
                val updatedPlayer = currentPlayer.copy(youtubeVideos = newVideos)
                repository.updatePlayer(updatedPlayer)
            }
        } else if (userRole == UserRole.CLUB) {
            val currentClub = repository.clubs.value.find { it.id == currentId }
            if (currentClub != null) {
                val updatedClub = currentClub.copy(youtubeVideos = newVideos)
                repository.saveClubToFirestore(updatedClub)
            }
        }
        onComplete(true)
    }

    fun addCommunityReview(playerId: String, reviewerName: String, reviewerRole: String, comment: String, tag: String) {
        repository.addCommunityReview(playerId, reviewerName, reviewerRole, comment, tag)
    }

    // Wizard Actions
    fun updateWizardPhoto(zoom: Float? = null, offsetX: Float? = null, offsetY: Float? = null, rotation: Float? = null, url: String? = null) {
        _wizardState.value = _wizardState.value.copy(
            zoom = zoom ?: _wizardState.value.zoom,
            offsetX = offsetX ?: _wizardState.value.offsetX,
            offsetY = offsetY ?: _wizardState.value.offsetY,
            rotation = rotation ?: _wizardState.value.rotation,
            photoUrl = url ?: _wizardState.value.photoUrl
        )
    }

    fun updateWizardPersonal(first: String, last: String, bdate: String, nat: String, city: String) {
        _wizardState.value = _wizardState.value.copy(
            firstName = first,
            lastName = last,
            birthDate = bdate,
            nationality = nat,
            city = city
        )
    }

    fun updateWizardPhysical(height: Int, weight: Int, foot: PreferredFoot) {
        // Recalculate stats based on physical features for authentic card feeling!
        val basePace = if (height < 175) 86 else if (height < 185) 78 else 68
        val basePhy = if (weight > 85) 84 else if (weight > 75) 75 else 62

        _wizardState.value = _wizardState.value.copy(
            height = height,
            weight = weight,
            preferredFoot = foot,
            pace = basePace,
            physical = basePhy
        )
    }

    fun updateWizardFootball(position: FootballPosition, club: String, jersey: Int) {
        // Position changes default stats
        val baseSho = when (position) {
            FootballPosition.ST, FootballPosition.SS -> 82
            FootballPosition.LW, FootballPosition.RW -> 76
            FootballPosition.AM -> 75
            else -> 45
        }
        val baseDef = when (position) {
            FootballPosition.CB -> 83
            FootballPosition.RB, FootballPosition.LB, FootballPosition.DM -> 76
            else -> 35
        }
        val basePas = when (position) {
            FootballPosition.AM, FootballPosition.CM -> 80
            FootballPosition.LW, FootballPosition.RW, FootballPosition.DM -> 72
            else -> 55
        }
        val baseDri = when (position) {
            FootballPosition.LW, FootballPosition.RW, FootballPosition.AM -> 83
            FootballPosition.ST, FootballPosition.SS, FootballPosition.CM -> 75
            else -> 58
        }

        _wizardState.value = _wizardState.value.copy(
            position = position,
            club = club,
            jerseyNumber = jersey,
            shooting = baseSho,
            defense = baseDef,
            passing = basePas,
            dribbling = baseDri
        )
    }

    fun updateWizardStats(pace: Int, shooting: Int, passing: Int, dribbling: Int, defense: Int, physical: Int, weakFoot: Int, skillMoves: Int) {
        _wizardState.value = _wizardState.value.copy(
            pace = pace,
            shooting = shooting,
            passing = passing,
            dribbling = dribbling,
            defense = defense,
            physical = physical,
            weakFoot = weakFoot,
            skillMoves = skillMoves
        )
    }

    fun updateWizardPositions(mainPosition: FootballPosition, secondary: List<FootballPosition>) {
        _wizardState.value = _wizardState.value.copy(
            position = mainPosition,
            secondaryPositions = secondary
        )
    }

    fun updateWizardBio(bio: String, insta: String, youtube: String) {
        _wizardState.value = _wizardState.value.copy(
            bio = bio,
            instagram = insta,
            youtubeUrl = youtube
        )
    }

    fun nextWizardStep() {
        if (_wizardState.value.currentStep < 7) {
            _wizardState.value = _wizardState.value.copy(currentStep = _wizardState.value.currentStep + 1)
        }
    }

    fun prevWizardStep() {
        if (_wizardState.value.currentStep > 1) {
            _wizardState.value = _wizardState.value.copy(currentStep = _wizardState.value.currentStep - 1)
        }
    }

    fun setWizardStep(step: Int) {
        if (step in 1..7) {
            _wizardState.value = _wizardState.value.copy(currentStep = step)
        }
    }

    fun finishWizard() {
        val w = _wizardState.value
        val id = _currentUserId.value.ifEmpty { "p_wizard_" + UUID.randomUUID().toString().take(6) }
        // Create dynamic Player in local DB
        val newPlayer = Player(
            id = id,
            firstName = w.firstName.ifEmpty { "New" },
            lastName = w.lastName.ifEmpty { "Player" },
            photoUrl = w.photoUrl,
            age = 19, // Simulated default
            birthDate = w.birthDate,
            nationality = w.nationality,
            city = w.city,
            height = w.height,
            weight = w.weight,
            preferredFoot = w.preferredFoot,
            position = w.position,
            club = w.club.ifEmpty { "Serbest" },
            jerseyNumber = w.jerseyNumber,
            bio = w.bio.ifEmpty { "Henüz biyografi eklenmedi." },
            instagram = w.instagram,
            youtubeUrl = w.youtubeUrl,
            selfRating = ((w.pace + w.shooting + w.passing + w.dribbling + w.defense + w.physical) / 6).coerceIn(10, 99),
            scoutRating = 0,
            tournamentRating = 0,
            marketValue = "Belirtilmemiş",
            isVerified = false,
            viewsCount = 0,
            followsCount = 0,
            stats = PhysicalStats(
                pace = w.pace,
                shooting = w.shooting,
                passing = w.passing,
                dribbling = w.dribbling,
                defense = w.defense,
                physical = w.physical
            ),
            secondaryPositions = w.secondaryPositions,
            zoom = w.zoom,
            offsetX = w.offsetX,
            offsetY = w.offsetY,
            weakFoot = w.weakFoot,
            skillMoves = w.skillMoves
        )

        // Write directly to Firestore "players" and update "users"
        try {
            repository.savePlayerToFirestore(newPlayer)
            val currentName = "${newPlayer.firstName} ${newPlayer.lastName}"
            val existingUser = repository.users.value.find { it.id == id }
            val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: ""
            val updatedUser = existingUser?.copy(name = currentName, city = newPlayer.city, photoUrl = newPlayer.photoUrl ?: "") ?: AppUser(
                id = id,
                name = currentName,
                email = email,
                role = UserRole.PLAYER,
                city = newPlayer.city,
                photoUrl = newPlayer.photoUrl ?: "",
                isVerified = false,
                isBanned = false,
                createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            )
            repository.saveUserToFirestore(updatedUser)
        } catch (e: Exception) {
            // Ignore if Firebase not fully initialized / offline
        }

        repository.addPlayer(newPlayer)
        _currentUserId.value = id
        setRole(UserRole.PLAYER)
        // Reset wizard state
        _wizardState.value = WizardState()
    }

    // Search Filter Actions
    fun updateFilters(newFilters: SearchFilters) {
        _filters.value = newFilters
    }

    fun resetFilters() {
        _filters.value = SearchFilters()
    }

    // Social actions
    fun toggleFollowPlayer(playerId: String) {
        repository.toggleFollowPlayer(playerId)
    }

    fun toggleLikePlayer(playerId: String) {
        repository.toggleLikePlayer(playerId)
    }

    fun addSocialComment(playerId: String, commenterName: String, commenterRole: String, commenterBadge: UserBadge, commentText: String, tag: String) {
        repository.addSocialComment(playerId, commenterName, commenterRole, commenterBadge, commentText, tag)
    }

    fun toggleLikeComment(playerId: String, commentId: String) {
        repository.toggleLikeComment(playerId, commentId)
    }

    fun deleteComment(playerId: String, commentId: String) {
        repository.deleteComment(playerId, commentId)
    }

    fun reportComment(playerId: String, commentId: String) {
        repository.reportComment(playerId, commentId)
    }

    fun replyToComment(playerId: String, commentId: String, replierName: String, replierRole: String, replierBadge: UserBadge, replyText: String) {
        repository.replyToComment(playerId, commentId, replierName, replierRole, replierBadge, replyText)
    }

    fun updatePlayerStatus(playerId: String, newStatus: String) {
        repository.updatePlayerStatus(playerId, newStatus)
    }

    fun toggleActivityLike(activityId: String) {
        repository.toggleActivityLike(activityId)
    }

    fun toggleFollowClub(clubId: String) {
        repository.toggleFollowClub(clubId)
    }

    fun applyToClub(clubId: String) {
        val playerId = _currentUserId.value.ifEmpty { "p_1" }
        repository.applyToClub(clubId, playerId)
    }

    fun acceptClubApplication(clubId: String, playerId: String) {
        repository.acceptClubApplication(clubId, playerId)
    }

    fun rejectClubApplication(clubId: String, playerId: String) {
        repository.rejectClubApplication(clubId, playerId)
    }

    fun updateClubDetails(
        clubId: String,
        logoUrl: String,
        phone: String,
        instagram: String,
        activeStudents: Int,
        coachesCount: Int,
        trophyCount: Int,
        aboutText: String,
        trainingFacility: String,
        locationUrl: String,
        websiteUrl: String,
        district: String,
        city: String,
        hasLicense: Boolean,
        hasSummerSchool: Boolean,
        hasWinterSchool: Boolean,
        ageGroups: String
    ) {
        repository.updateClubDetails(
            clubId, logoUrl, phone, instagram, activeStudents, coachesCount, trophyCount,
            aboutText, trainingFacility, locationUrl, websiteUrl, district, city,
            hasLicense, hasSummerSchool, hasWinterSchool, ageGroups
        )
    }

    fun addClubNews(clubId: String, title: String, content: String) {
        repository.addClubNews(clubId, title, content)
    }

    fun sendClubPlayerInvite(clubId: String, clubName: String, playerId: String) {
        repository.sendClubPlayerInvite(clubId, clubName, playerId)
    }

    fun sendClubCoachInvite(clubId: String, clubName: String, coachId: String) {
        repository.sendClubCoachInvite(clubId, clubName, coachId)
    }

    fun acceptPlayerInvite(notificationId: String, clubId: String, playerId: String) {
        repository.acceptPlayerInvite(notificationId, clubId, playerId)
    }

    fun rejectPlayerInvite(notificationId: String) {
        repository.rejectPlayerInvite(notificationId)
    }

    fun acceptCoachInvite(notificationId: String, clubId: String, coachId: String) {
        repository.acceptCoachInvite(notificationId, clubId, coachId)
    }

    fun rejectCoachInvite(notificationId: String) {
        repository.rejectCoachInvite(notificationId)
    }

    fun removePlayerFromClub(clubId: String, playerId: String) {
        repository.removePlayerFromClub(clubId, playerId)
    }

    fun removeCoachFromClub(clubId: String, coachId: String) {
        repository.removeCoachFromClub(clubId, coachId)
    }

    fun playerLeaveClub(playerId: String) {
        repository.playerLeaveClub(playerId)
    }

    fun markNotificationAsRead(notificationId: String) {
        repository.markNotificationAsRead(notificationId)
    }

    // Admin & User actions
    fun deleteUser(userId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        repository.deleteUser(userId, onSuccess, onFailure)
    }

    fun toggleBanUser(userId: String) {
        repository.toggleBanUser(userId)
    }

    fun toggleVerifyUser(userId: String) {
        repository.toggleVerifyUser(userId)
    }

    fun changeUserRole(userId: String, newRole: UserRole) {
        repository.changeUserRole(userId, newRole)
    }

    fun approveClub(clubId: String) {
        repository.approveClub(clubId)
    }

    fun featureClub(clubId: String) {
        repository.featureClub(clubId)
    }

    fun passivateClub(clubId: String) {
        repository.passivateClub(clubId)
    }

    fun addTournament(title: String, desc: String, countdown: String, prize: String, maxPart: Int, diff: String) {
        val id = "t_" + UUID.randomUUID().toString().take(6)
        repository.addTournament(
            Tournament(id, title, desc, countdown, prize, 0, maxPart, diff)
        )
    }

    fun editTournament(id: String, title: String, desc: String, countdown: String, prize: String, maxPart: Int, diff: String) {
        val t = repository.tournaments.value.find { it.id == id } ?: return
        repository.editTournament(id, t.copy(title = title, description = desc, countdown = countdown, prize = prize, maxParticipants = maxPart, difficulty = diff))
    }

    fun deleteTournament(id: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        repository.deleteTournament(id, onSuccess, onFailure)
    }

    fun selectTournamentWinner(tournamentId: String, winnerName: String) {
        repository.selectTournamentWinner(tournamentId, winnerName)
    }

    fun addNotificationToAll(title: String, description: String) {
        val notification = Notification(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            type = NotificationType.NEW_TOURNAMENT,
            time = "Şimdi",
            isRead = false
        )
        repository.addNotification(notification)
    }

    fun addHaber(title: String, content: String) {
        val activity = ActivityItem(
            id = "act_" + UUID.randomUUID().toString().take(6),
            userName = "Haber Merkezi",
            userRole = "ADMIN",
            userBadge = UserBadge.NONE,
            actionText = "haber paylaştı:",
            detailText = "$title - $content",
            timeAgo = "Şimdi"
        )
        repository.addActivityItem(activity)
    }

    // Role-specific Wizards Completion
    fun finishScoutWizard(name: String, city: String, club: String, duty: String, photo: String, instagram: String, phone: String, stat1: Int = 85, stat2: Int = 85, stat3: Int = 85, stat4: Int = 85, stat5: Int = 85, stat6: Int = 85) {
        val id = _currentUserId.value.ifEmpty { "scout_" + UUID.randomUUID().toString().take(6) }
        val existingUser = repository.users.value.find { it.id == id }
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: existingUser?.email ?: "$id@futbolcubul.com"
        val newUser = existingUser?.copy(
            name = name,
            city = city,
            photoUrl = photo,
            role = UserRole.SCOUT,
            club = club,
            duty = duty,
            instagram = instagram,
            phone = phone,
            stat1 = stat1,
            stat2 = stat2,
            stat3 = stat3,
            stat4 = stat4,
            stat5 = stat5,
            stat6 = stat6
        ) ?: AppUser(
            id = id,
            name = name,
            email = email,
            role = UserRole.SCOUT,
            city = city,
            photoUrl = photo,
            isVerified = false,
            club = club,
            duty = duty,
            instagram = instagram,
            phone = phone,
            stat1 = stat1,
            stat2 = stat2,
            stat3 = stat3,
            stat4 = stat4,
            stat5 = stat5,
            stat6 = stat6
        )
        
        try {
            repository.saveUserToFirestore(newUser)
        } catch (e: Exception) {}

        repository.addUser(newUser)
        _currentUserId.value = id
        _currentUserRole.value = UserRole.SCOUT
    }

    fun finishCoachWizard(name: String, license: String, team: String, city: String, photo: String, instagram: String, phone: String, stat1: Int = 85, stat2: Int = 85, stat3: Int = 85, stat4: Int = 85, stat5: Int = 85, stat6: Int = 85) {
        val id = _currentUserId.value.ifEmpty { "coach_" + UUID.randomUUID().toString().take(6) }
        val existingUser = repository.users.value.find { it.id == id }
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: existingUser?.email ?: "$id@futbolcubul.com"
        val newUser = existingUser?.copy(
            name = name,
            city = city,
            photoUrl = photo,
            role = UserRole.COACH,
            license = license,
            club = team,
            instagram = instagram,
            phone = phone,
            stat1 = stat1,
            stat2 = stat2,
            stat3 = stat3,
            stat4 = stat4,
            stat5 = stat5,
            stat6 = stat6
        ) ?: AppUser(
            id = id,
            name = name,
            email = email,
            role = UserRole.COACH,
            city = city,
            photoUrl = photo,
            isVerified = false,
            license = license,
            club = team,
            instagram = instagram,
            phone = phone,
            stat1 = stat1,
            stat2 = stat2,
            stat3 = stat3,
            stat4 = stat4,
            stat5 = stat5,
            stat6 = stat6
        )
        
        try {
            repository.saveUserToFirestore(newUser)
        } catch (e: Exception) {}

        repository.addUser(newUser)
        _currentUserId.value = id
        _currentUserRole.value = UserRole.COACH
    }

    fun finishClubWizard(
        name: String,
        foundYear: Int,
        city: String,
        district: String,
        logoUrl: String,
        phone: String,
        whatsapp: String,
        instagram: String,
        website: String,
        mapLocation: String,
        bio: String,
        activeStudents: Int,
        coachesCount: Int,
        ageGroups: String,
        hasLicense: Boolean,
        hasSummerSchool: Boolean,
        hasWinterSchool: Boolean,
        trainingFacility: String
    ) {
        val id = _currentUserId.value.ifEmpty { "club_" + UUID.randomUUID().toString().take(6) }
        val existingUser = repository.users.value.find { it.id == id }
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: existingUser?.email ?: "$id@futbolcubul.com"
        val newUser = existingUser?.copy(name = name, city = city, photoUrl = logoUrl, role = UserRole.CLUB) ?: AppUser(id, name, email, UserRole.CLUB, city, logoUrl, isVerified = false)
        
        val newClub = Club(
            id = id,
            name = name,
            city = city,
            district = district,
            foundationYear = foundYear,
            coverPhotoUrl = "",
            logoUrl = logoUrl,
            followerCount = 0,
            activeAgeGroups = ageGroups,
            aboutText = bio,
            trainingFacility = trainingFacility,
            pitchInfo = "Doğal/Suni Çim",
            trainingDays = "Haftanın her günü",
            licenseStatus = if (hasLicense) "Lisanslı Akademi" else "Lisanssız Akademi",
            acceptedAgeGroups = ageGroups,
            phoneNumber = phone,
            whatsappNumber = whatsapp,
            instagramUsername = instagram,
            websiteUrl = website,
            locationUrl = mapLocation,
            address = "$city / $district",
            coaches = emptyList(),
            players = emptyList(),
            achievements = emptyList(),
            news = emptyList(),
            activeStudentsCount = activeStudents,
            coachesCount = coachesCount,
            trophyCount = 0,
            hasLicense = hasLicense,
            hasSummerSchool = hasSummerSchool,
            hasWinterSchool = hasWinterSchool,
            ageGroups = ageGroups
        )
        
        try {
            repository.saveUserToFirestore(newUser)
            repository.saveClubToFirestore(newClub)
        } catch (e: Exception) {}

        repository.addUser(newUser)
        repository.addClub(newClub)
        
        _currentUserId.value = id
        _currentUserRole.value = UserRole.CLUB
    }

    fun finishMediaWizard(name: String, city: String, photo: String, price: String, instagram: String, whatsapp: String, portfolio: String, services: String, stat1: Int = 85, stat2: Int = 85, stat3: Int = 85, stat4: Int = 85, stat5: Int = 85, stat6: Int = 85) {
        val id = _currentUserId.value.ifEmpty { "media_" + UUID.randomUUID().toString().take(6) }
        val existingUser = repository.users.value.find { it.id == id }
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: existingUser?.email ?: "$id@futbolcubul.com"
        val newUser = existingUser?.copy(
            name = name,
            city = city,
            photoUrl = photo,
            role = UserRole.MEDIA,
            price = price,
            instagram = instagram,
            phone = whatsapp,
            achievements = portfolio,
            services = services,
            stat1 = stat1,
            stat2 = stat2,
            stat3 = stat3,
            stat4 = stat4,
            stat5 = stat5,
            stat6 = stat6
        ) ?: AppUser(
            id = id,
            name = name,
            email = email,
            role = UserRole.MEDIA,
            city = city,
            photoUrl = photo,
            isVerified = false,
            price = price,
            instagram = instagram,
            phone = whatsapp,
            achievements = portfolio,
            services = services,
            createdAt = "2026-07-13T00:30:00",
            stat1 = stat1,
            stat2 = stat2,
            stat3 = stat3,
            stat4 = stat4,
            stat5 = stat5,
            stat6 = stat6
        )
        
        try {
            repository.saveUserToFirestore(newUser)
        } catch (e: Exception) {}

        repository.addUser(newUser)
        _currentUserId.value = id
        _currentUserRole.value = UserRole.MEDIA
    }

    fun finishStoreWizard(name: String, city: String, photo: String, phone: String, instagram: String, address: String, website: String) {
        val id = _currentUserId.value.ifEmpty { "store_" + UUID.randomUUID().toString().take(6) }
        val existingUser = repository.users.value.find { it.id == id }
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: existingUser?.email ?: "$id@futbolcubul.com"
        val newUser = existingUser?.copy(
            name = name,
            city = city,
            photoUrl = photo,
            role = UserRole.STORE,
            phone = phone,
            instagram = instagram,
            bio = address,
            achievements = website,
            services = "Mağaza Hizmetleri"
        ) ?: AppUser(
            id = id,
            name = name,
            email = email,
            role = UserRole.STORE,
            city = city,
            photoUrl = photo,
            phone = phone,
            instagram = instagram,
            bio = address,
            achievements = website,
            services = "Mağaza Hizmetleri"
        )
        try {
            repository.saveUserToFirestore(newUser)
        } catch (e: Exception) {}
        repository.addUser(newUser)
        _currentUserId.value = id
        _currentUserRole.value = UserRole.STORE
    }

    fun finishPitchWizard(name: String, city: String, photo: String, phone: String, instagram: String, workingHours: String, price: String, address: String) {
        val id = _currentUserId.value.ifEmpty { "pitch_" + UUID.randomUUID().toString().take(6) }
        val existingUser = repository.users.value.find { it.id == id }
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: existingUser?.email ?: "$id@futbolcubul.com"
        val newUser = existingUser?.copy(
            name = name,
            city = city,
            photoUrl = photo,
            role = UserRole.PITCH,
            phone = phone,
            instagram = instagram,
            services = workingHours,
            price = price,
            bio = address
        ) ?: AppUser(
            id = id,
            name = name,
            email = email,
            role = UserRole.PITCH,
            city = city,
            photoUrl = photo,
            phone = phone,
            instagram = instagram,
            services = workingHours,
            price = price,
            bio = address
        )
        try {
            repository.saveUserToFirestore(newUser)
        } catch (e: Exception) {}
        repository.addUser(newUser)
        _currentUserId.value = id
        _currentUserRole.value = UserRole.PITCH
    }

    fun finishOrganizerWizard(name: String, city: String, photo: String, phone: String, instagram: String, organizerType: String, intro: String) {
        val id = _currentUserId.value.ifEmpty { "organizer_" + UUID.randomUUID().toString().take(6) }
        val existingUser = repository.users.value.find { it.id == id }
        val email = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.email ?: existingUser?.email ?: "$id@futbolcubul.com"
        val newUser = existingUser?.copy(
            name = name,
            city = city,
            photoUrl = photo,
            role = UserRole.ORGANIZER,
            phone = phone,
            instagram = instagram,
            services = organizerType,
            bio = intro
        ) ?: AppUser(
            id = id,
            name = name,
            email = email,
            role = UserRole.ORGANIZER,
            city = city,
            photoUrl = photo,
            phone = phone,
            instagram = instagram,
            services = organizerType,
            bio = intro
        )
        try {
            repository.saveUserToFirestore(newUser)
        } catch (e: Exception) {}
        repository.addUser(newUser)
        _currentUserId.value = id
        _currentUserRole.value = UserRole.ORGANIZER
    }

    // ----------------------------------------------------
    // FIREBASE AUTH & SESSION MANAGEMENT
    // ----------------------------------------------------

    fun registerUser(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        role: UserRole,
        onSuccess: (UserRole) -> Unit,
        onError: (String) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: UUID.randomUUID().toString()
                val fullName = "$firstName $lastName"
                val newUser = AppUser(
                    id = uid,
                    name = fullName,
                    email = email.trim(),
                    role = role,
                    city = "İstanbul",
                    photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=300",
                    isVerified = false,
                    isBanned = false,
                    createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )

                try {
                    repository.saveUserToFirestore(newUser)
                } catch (e: Exception) {}

                _currentUserId.value = uid
                _wizardState.value = _wizardState.value.copy(
                    firstName = firstName,
                    lastName = lastName,
                    nationality = "Türkiye" // default as requested
                )
                setRole(role)
                repository.startFirebaseSync()
                onSuccess(role)
            }
            .addOnFailureListener { e ->
                onError("Kayıt hatası: ${e.localizedMessage}")
            }
    }

    fun startFirebaseSync() {
        repository.startFirebaseSync()
    }

    fun stopFirebaseSync() {
        repository.stopFirebaseSync()
    }

    fun loginUser(
        email: String,
        password: String,
        onSuccess: (UserRole) -> Unit,
        onError: (String) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        // Test Admin Account bypass
        if (email.trim().lowercase() == "admin@futbolcubul.com") {
            if (password == "313131") {
                auth.signInWithEmailAndPassword(email.trim(), password)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: "admin_uid"
                        _currentUserId.value = uid
                        setRole(UserRole.ADMIN)
                        repository.startFirebaseSync()
                        onSuccess(UserRole.ADMIN)
                    }
                    .addOnFailureListener {
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnSuccessListener { authResult ->
                                val uid = authResult.user?.uid ?: "admin_uid"
                                val adminUser = AppUser(
                                    id = uid,
                                    name = "Sistem Yöneticisi",
                                    email = email.trim(),
                                    role = UserRole.ADMIN,
                                    city = "İstanbul",
                                    photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=300",
                                    isVerified = true,
                                    isBanned = false
                                )
                                db.collection("users").document(uid).set(adminUser)
                                _currentUserId.value = uid
                                setRole(UserRole.ADMIN)
                                repository.startFirebaseSync()
                                onSuccess(UserRole.ADMIN)
                            }
                            .addOnFailureListener {
                                // Full offline fallback bypass
                                _currentUserId.value = "admin_1"
                                setRole(UserRole.ADMIN)
                                repository.startFirebaseSync()
                                onSuccess(UserRole.ADMIN)
                            }
                    }
                return
            } else {
                onError("Hatalı şifre!")
                return
            }
        }

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val data = document.data
                            if (data != null) {
                                val user = repository.mapToAppUser(data)
                                if (user.isBanned) {
                                    onError("Bu hesap yöneticiler tarafından engellenmiştir.")
                                    auth.signOut()
                                    return@addOnSuccessListener
                                }
                                _currentUserId.value = uid
                                setRole(user.role)
                                repository.startFirebaseSync()
                                onSuccess(user.role)
                            } else {
                                onError("Kullanıcı verisi okunamadı.")
                            }
                        } else {
                            onError("Kullanıcı profili bulunamadı.")
                        }
                    }
                    .addOnFailureListener { e ->
                        onError("Profil yükleme hatası: ${e.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                onError("Giriş başarısız: E-posta veya şifre hatalı.")
            }
    }

    fun sendPasswordReset(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        if (email.isBlank()) {
            onError("Lütfen e-posta adresinizi girin.")
            return
        }
        auth.sendPasswordResetEmail(email.trim())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                onError("Sıfırlama e-postası gönderilemedi: ${e.localizedMessage}")
            }
    }

    fun authenticateWithGoogle(
        email: String,
        displayName: String,
        onExistingUser: (UserRole) -> Unit,
        onNewUser: () -> Unit,
        onError: (String) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()
        val bypassPassword = "google_auth_bypass_123"

        auth.signInWithEmailAndPassword(email.trim(), bypassPassword)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: ""
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val data = document.data
                            if (data != null) {
                                val user = repository.mapToAppUser(data)
                                if (user.isBanned) {
                                    onError("Bu hesap yöneticiler tarafından engellenmiştir.")
                                    auth.signOut()
                                    return@addOnSuccessListener
                                }
                                _currentUserId.value = uid
                                setRole(user.role)
                                repository.startFirebaseSync()
                                onExistingUser(user.role)
                            } else {
                                onError("Kullanıcı verisi okunamadı.")
                            }
                        } else {
                            _currentUserId.value = uid
                            onNewUser()
                        }
                    }
                    .addOnFailureListener { e ->
                        _currentUserId.value = uid
                        onNewUser()
                    }
            }
            .addOnFailureListener {
                auth.createUserWithEmailAndPassword(email.trim(), bypassPassword)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid ?: ""
                        _currentUserId.value = uid
                        onNewUser()
                    }
                    .addOnFailureListener { e ->
                        onError("Google entegrasyon hatası: ${e.localizedMessage}")
                    }
            }
    }

    fun completeGoogleRegistration(
        email: String,
        displayName: String,
        role: UserRole,
        onSuccess: (UserRole) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = _currentUserId.value
        if (uid.isEmpty()) {
            onError("Kullanıcı kimliği bulunamadı.")
            return
        }

        val newUser = AppUser(
            id = uid,
            name = displayName.ifEmpty { "Google Kullanıcısı" },
            email = email.trim(),
            role = role,
            city = "İstanbul",
            photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=150",
            isVerified = false,
            isBanned = false,
            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        )

        try {
            repository.saveUserToFirestore(newUser)
        } catch (e: Exception) {}

        setRole(role)
        repository.startFirebaseSync()
        onSuccess(role)
    }

    fun ensurePlayerProfileExists(userId: String) {
        if (userId.isEmpty()) return
        if (!isPlayersLoaded.value) return // Wait until Firestore loading finishes to avoid overwriting existing profiles
        val currentList = players.value
        val existing = currentList.find { it.id == userId }
        if (existing == null) {
            val user = users.value.find { it.id == userId } ?: return
            if (user.role != UserRole.PLAYER) return

            val nameParts = user.name.split(" ", limit = 2)
            val firstName = nameParts.getOrNull(0) ?: "Yeni"
            val lastName = nameParts.getOrNull(1) ?: "Oyuncu"
            
            val newPlayer = Player(
                id = userId,
                firstName = firstName,
                lastName = lastName,
                photoUrl = user.photoUrl,
                age = 20,
                birthDate = "2006-01-01",
                nationality = "Türkiye",
                city = user.city.ifEmpty { "İstanbul" },
                height = 180,
                weight = 75,
                preferredFoot = PreferredFoot.RIGHT,
                position = FootballPosition.ST,
                club = "Serbest",
                jerseyNumber = 10,
                bio = "Henüz bir biyografi eklenmedi.",
                selfRating = 0,
                scoutRating = 0,
                tournamentRating = 0,
                marketValue = "Belirtilmemiş",
                isVerified = false,
                viewsCount = 0,
                followsCount = 0,
                stats = PhysicalStats(0, 0, 0, 0, 0, 0)
            )
            repository.addPlayer(newPlayer)
            try {
                repository.savePlayerToFirestore(newPlayer)
            } catch (e: Exception) {}
        }
    }

    fun ensureClubProfileExists(userId: String) {
        if (userId.isEmpty()) return
        val currentList = clubs.value
        val existing = currentList.find { it.id == userId }
        if (existing == null) {
            val user = users.value.find { it.id == userId } ?: return
            if (user.role != UserRole.CLUB) return

            val newClub = Club(
                id = userId,
                name = user.name,
                city = user.city.ifEmpty { "İstanbul" },
                foundationYear = 2024,
                coverPhotoUrl = "",
                logoUrl = user.photoUrl,
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
                address = "${user.city.ifEmpty { "İstanbul" }} / Merkez",
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
            repository.addClub(newClub)
        }
    }

    fun resetEcosystemData(onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        repository.resetEcosystemData(
            currentUserId = _currentUserId.value,
            onSuccess = {
                logout()
                onSuccess()
            },
            onFailure = onFailure
        )
    }

    fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {}
        _currentUserId.value = ""
        repository.stopFirebaseSync()
    }
}
