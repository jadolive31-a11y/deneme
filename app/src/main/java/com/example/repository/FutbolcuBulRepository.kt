package com.example.repository

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

data class FollowItem(val followerId: String, val followedId: String)
data class LikeItem(val likerId: String, val likedId: String)

class FutbolcuBulRepository {

    companion object {
        var appContext: android.content.Context? = null
    }

    private val locallyDeletedDocIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var isDeletedIdsLoaded = false

    private fun loadDeletedIdsIfNeeded() {
        if (isDeletedIdsLoaded) return
        try {
            val context = appContext
            if (context != null) {
                val prefs = context.getSharedPreferences("futbolcubul_local_deletes", android.content.Context.MODE_PRIVATE)
                val saved = prefs.getStringSet("deleted_ids", emptySet()) ?: emptySet()
                locallyDeletedDocIds.addAll(saved)
                isDeletedIdsLoaded = true
            }
        } catch (e: Exception) {
            android.util.Log.e("FutbolcuBul", "Error loading deleted ids", e)
        }
    }

    private fun persistDeletedIds() {
        try {
            val context = appContext
            if (context != null) {
                val prefs = context.getSharedPreferences("futbolcubul_local_deletes", android.content.Context.MODE_PRIVATE)
                prefs.edit().putStringSet("deleted_ids", locallyDeletedDocIds.toSet()).apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("FutbolcuBul", "Error saving deleted ids", e)
        }
    }

    private val localPlayers = java.util.concurrent.ConcurrentHashMap<String, Player>()
    private val localUsers = java.util.concurrent.ConcurrentHashMap<String, AppUser>()
    private val localClubs = java.util.concurrent.ConcurrentHashMap<String, Club>()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _rawPlayers = MutableStateFlow<List<Player>>(emptyList())
    private val _follows = MutableStateFlow<List<FollowItem>>(emptyList())
    private val _likes = MutableStateFlow<List<LikeItem>>(emptyList())

    private val _rawClubs = MutableStateFlow<List<Club>>(emptyList())

    var currentUserIdValue: String = "p_1"
        private set

    fun setCurrentUserId(id: String) {
        currentUserIdValue = id
        updateComputedPlayers()
        updateComputedClubs()
    }

    private val _applications = MutableStateFlow<List<ClubApplication>>(emptyList())
    val applications: StateFlow<List<ClubApplication>> = _applications.asStateFlow()

    fun updateComputedClubs() {
        val raw = _rawClubs.value
        val followsList = _follows.value
        val appsList = _applications.value
        val myId = currentUserIdValue
        _clubs.value = raw.map { club ->
            val followerCount = followsList.count { it.followedId == club.id }
            val isFollowed = if (myId.isNotEmpty()) followsList.any { it.followerId == myId && it.followedId == club.id } else false
            
            // Dynamically get applied players for this club who are in PENDING status
            val appliedIds = appsList.filter { it.clubId == club.id && it.status == "PENDING" }.map { it.playerId }
            // Dynamically check if the current user has an active application to this club
            val hasApplied = if (myId.isNotEmpty()) appsList.any { it.clubId == club.id && it.playerId == myId && it.status == "PENDING" } else false

            club.copy(
                followerCount = followerCount,
                followedByMe = isFollowed,
                appliedPlayerIds = appliedIds,
                registrationApplied = hasApplied
            )
        }
    }

    fun updateComputedPlayers() {
        val raw = _rawPlayers.value
        val followsList = _follows.value
        val likesList = _likes.value
        val myId = currentUserIdValue
        _players.value = raw.map { player ->
            val followerCount = followsList.count { it.followedId == player.id }
            val followingCount = followsList.count { it.followerId == player.id }
            val likeCount = likesList.count { it.likedId == player.id }
            val isFollowed = if (myId.isNotEmpty()) followsList.any { it.followerId == myId && it.followedId == player.id } else false
            val isLiked = if (myId.isNotEmpty()) likesList.any { it.likerId == myId && it.likedId == player.id } else false
            player.copy(
                followsCount = followerCount,
                followingCount = followingCount,
                likesCount = likeCount,
                followedByMe = isFollowed,
                likedByMe = isLiked
            )
        }
    }

    private val _isPlayersLoaded = MutableStateFlow<Boolean>(false)
    val isPlayersLoaded: StateFlow<Boolean> = _isPlayersLoaded.asStateFlow()

    private val _clubs = MutableStateFlow<List<Club>>(emptyList())
    val clubs: StateFlow<List<Club>> = _clubs.asStateFlow()

    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _favoritePlayerIds = MutableStateFlow<Set<String>>(emptySet())
    val favoritePlayerIds: StateFlow<Set<String>> = _favoritePlayerIds.asStateFlow()

    private val _activityFeed = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activityFeed: StateFlow<List<ActivityItem>> = _activityFeed.asStateFlow()

    private val _users = MutableStateFlow<List<AppUser>>(emptyList())
    val users: StateFlow<List<AppUser>> = _users.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        _players.value = emptyList()
        _clubs.value = emptyList()
        localClubs.clear()
        _tournaments.value = emptyList()
        _chats.value = emptyList()
        _notifications.value = emptyList()
        _activityFeed.value = emptyList()
        _users.value = emptyList()
        _isPlayersLoaded.value = true
        if (false) {
            // Mock Scout Reports
        val reports1 = listOf(
            ScoutReport(
                id = "sr_1",
                scoutName = "Hasan Şişman",
                scoutBadge = "UEFA A Scout",
                technical = 82,
                tactical = 79,
                mental = 80,
                positioning = 85,
                comment = "Outstanding ball control and physical coverage. Quick decision-making in high-pressure defensive transitions.",
                date = "2026-06-15"
            ),
            ScoutReport(
                id = "sr_2",
                scoutName = "Matteo Moretto",
                scoutBadge = "Serie A Talent Scout",
                technical = 85,
                tactical = 82,
                mental = 75,
                positioning = 80,
                comment = "Very promising visual scanning. Needs minor improvement in defensive shielding, but overall an exceptional modern defender.",
                date = "2026-07-02"
            )
        )

        val reports2 = listOf(
            ScoutReport(
                id = "sr_3",
                scoutName = "Sir Alex",
                scoutBadge = "Pro Academy Director",
                technical = 90,
                tactical = 88,
                mental = 87,
                positioning = 89,
                comment = "Remarkable speed and finishing. Runs into half-spaces flawlessly. Highly recommended for elite level academies.",
                date = "2026-07-05"
            )
        )

        // Mock Tournament History Entries
        val history1 = listOf(
            TournamentHistoryEntry("th_1", "Istanbul Elite 1v1 Arena", "2026-05", "Winner", 92),
            TournamentHistoryEntry("th_2", "Aeon Cup U19", "2026-06", "Semi-Finals", 84)
        )

        val history2 = listOf(
            TournamentHistoryEntry("th_3", "Ankara Neon Challenge", "2026-04", "Winner", 95),
            TournamentHistoryEntry("th_4", "Golden Shootout", "2026-05", "Finalist", 88)
        )

        val mockPlayers = listOf(
            Player(
                id = "p_1",
                firstName = "Mert",
                lastName = "Yılmaz",
                photoResId = null,
                age = 19,
                birthDate = "2007-04-12",
                nationality = "Turkey",
                city = "Istanbul",
                height = 185,
                weight = 78,
                preferredFoot = PreferredFoot.RIGHT,
                position = FootballPosition.ST,
                club = "Beşiktaş JK U19",
                jerseyNumber = 9,
                bio = "Fast-paced striker with excellent positioning and sharp finishing. Hardworking and dedicated team player.",
                instagram = "@mertyilmaz9",
                youtubeUrl = "https://youtube.com/watch?v=mockST",
                selfRating = 79,
                scoutRating = 83,
                tournamentRating = 85,
                marketValue = "€450K",
                isVerified = true,
                viewsCount = 1240,
                followsCount = 89,
                followingCount = 45,
                status = "Yarın turnuvadayım, odaklandım! 🎯",
                likesCount = 120,
                likedByMe = false,
                followedByMe = false,
                badge = UserBadge.VERIFIED_PLAYER,
                profileVisits = ProfileVisits(today = 24, last7Days = 142, last30Days = 512, total = 1240),
                profileViewers = listOf(
                    ProfileViewer("pv_1", "Beşiktaş Scout", "Resmi Scout", UserBadge.SCOUT, "2 dakika önce", System.currentTimeMillis() - 120000),
                    ProfileViewer("pv_2", "Semih Yılmaz", "Doğrulanmış Oyuncu", UserBadge.VERIFIED_PLAYER, "20 dakika önce", System.currentTimeMillis() - 1200000),
                    ProfileViewer("pv_3", "Galatasaray Akademi", "Kulüp Yetkilisi", UserBadge.CLUB_REPRESENTATIVE, "1 saat önce", System.currentTimeMillis() - 3600000),
                    ProfileViewer("pv_4", "Sir Alex", "Milli Takım Scoutu", UserBadge.NATIONAL_SCOUT, "3 saat önce", System.currentTimeMillis() - 10800000)
                ),
                stats = PhysicalStats(pace = 88, shooting = 82, passing = 71, dribbling = 78, defense = 35, physical = 76),
                scoutReports = reports2,
                tournamentHistory = history1,
                communityReviews = listOf(
                    CommunityReview(
                        id = "cr_1",
                        reviewerName = "Yakup Can",
                        reviewerRole = "Futbolcu",
                        comment = "Halı sahada ve turnuvalarda her zaman fark yaratan bir golcü. Çok hızlı!",
                        tag = "Hız Canavarı",
                        date = "2026-07-01",
                        likesCount = 14,
                        likedByMe = false,
                        reviewerBadge = UserBadge.VERIFIED_PLAYER,
                        replies = listOf(
                            CommentReply("rep_1", "Mert Yılmaz", "Oyuncu", UserBadge.VERIFIED_PLAYER, "Eyvallah kardeşim, beraber daha iyiyiz!", "2026-07-01")
                        )
                    ),
                    CommunityReview(
                        id = "cr_2",
                        reviewerName = "Ahmet Y.",
                        reviewerRole = "Kulüp Temsilcisi",
                        comment = "Bitiriciliği çok üst düzey, ceza sahasında topla buluştuğunda affetmiyor.",
                        tag = "Bitirici",
                        date = "2026-07-03",
                        likesCount = 8,
                        likedByMe = false,
                        reviewerBadge = UserBadge.CLUB_REPRESENTATIVE
                    )
                )
            ),
            Player(
                id = "p_2",
                firstName = "Arda",
                lastName = "Çelik",
                photoResId = null,
                age = 20,
                birthDate = "2006-08-20",
                nationality = "Turkey",
                city = "Ankara",
                height = 176,
                weight = 68,
                preferredFoot = PreferredFoot.LEFT,
                position = FootballPosition.AM,
                club = "Gençlerbirliği",
                jerseyNumber = 10,
                bio = "Creative attacking midfielder who specializes in playmaking, key passes, and direct free-kicks.",
                instagram = "@ardacelik10",
                youtubeUrl = "https://youtube.com/watch?v=mockAM",
                selfRating = 82,
                scoutRating = 81,
                tournamentRating = 88,
                marketValue = "€650K",
                isVerified = true,
                viewsCount = 3120,
                followsCount = 245,
                followingCount = 102,
                status = "Yeni takım arıyorum. Tekliflere açığım. ⚽",
                likesCount = 380,
                likedByMe = false,
                followedByMe = false,
                badge = UserBadge.VERIFIED_PLAYER,
                profileVisits = ProfileVisits(today = 54, last7Days = 310, last30Days = 1120, total = 3120),
                profileViewers = listOf(
                    ProfileViewer("pv_5", "Fenerbahçe Scout", "Resmi Scout", UserBadge.SCOUT, "10 dakika önce", System.currentTimeMillis() - 600000),
                    ProfileViewer("pv_6", "Bursaspor Temsilcisi", "Kulüp Yetkilisi", UserBadge.CLUB_REPRESENTATIVE, "45 dakika önce", System.currentTimeMillis() - 2700000)
                ),
                stats = PhysicalStats(pace = 79, shooting = 78, passing = 86, dribbling = 85, defense = 42, physical = 65),
                scoutReports = reports1,
                tournamentHistory = history2,
                communityReviews = listOf(
                    CommunityReview(
                        id = "cr_3",
                        reviewerName = "Süleyman Demir",
                        reviewerRole = "Ziyaretçi",
                        comment = "Orta sahanın beyni bu adam, çok zeki ve teknik bir oyuncu! Pasları adrese teslim.",
                        tag = "Orta Sahanın Beyni",
                        date = "2026-07-02",
                        likesCount = 22,
                        likedByMe = false,
                        reviewerBadge = UserBadge.NONE
                    ),
                    CommunityReview(
                        id = "cr_4",
                        reviewerName = "Eren K.",
                        reviewerRole = "Futbolcu",
                        comment = "Arda ile oynamak çok rahat, nereye koşsanız topu oraya bırakıyor. Tam bir oyun kurucu.",
                        tag = "Pas Ustası",
                        date = "2026-07-04",
                        likesCount = 12,
                        likedByMe = false,
                        reviewerBadge = UserBadge.VERIFIED_PLAYER
                    )
                )
            ),
            Player(
                id = "p_3",
                firstName = "Can",
                lastName = "Kahveci",
                photoResId = null,
                age = 18,
                birthDate = "2008-01-15",
                nationality = "Turkey",
                city = "Izmir",
                height = 188,
                weight = 81,
                preferredFoot = PreferredFoot.RIGHT,
                position = FootballPosition.CB,
                club = "Altınordu Academy",
                jerseyNumber = 4,
                bio = "Strong, physically dominant center back. Composed under pressure with high aerial duel success rates.",
                instagram = "@cankahveci4",
                youtubeUrl = "",
                selfRating = 74,
                scoutRating = 78,
                tournamentRating = 75,
                marketValue = "€180K",
                isVerified = false,
                viewsCount = 540,
                followsCount = 32,
                followingCount = 12,
                status = "Antrenmandayım. Durmak yok! 🛡️",
                likesCount = 18,
                likedByMe = false,
                followedByMe = false,
                badge = UserBadge.NONE,
                profileVisits = ProfileVisits(today = 5, last7Days = 34, last30Days = 124, total = 540),
                profileViewers = listOf(
                    ProfileViewer("pv_7", "Altınordu Scout", "Resmi Scout", UserBadge.SCOUT, "1 saat önce", System.currentTimeMillis() - 3600000)
                ),
                stats = PhysicalStats(pace = 72, shooting = 40, passing = 68, dribbling = 65, defense = 81, physical = 84),
                scoutReports = emptyList(),
                tournamentHistory = emptyList()
            ),
            Player(
                id = "p_4",
                firstName = "Yusuf",
                lastName = "Kaya",
                photoResId = null,
                age = 21,
                birthDate = "2005-03-30",
                nationality = "Turkey",
                city = "Trabzon",
                height = 172,
                weight = 64,
                preferredFoot = PreferredFoot.BOTH,
                position = FootballPosition.LW,
                club = "Trabzonspor Academy",
                jerseyNumber = 11,
                bio = "Tricky and explosive winger who can play on both flanks. Loves cutting inside and taking shots.",
                instagram = "@yusufkaya11",
                youtubeUrl = "https://youtube.com/watch?v=mockLW",
                selfRating = 78,
                scoutRating = 80,
                tournamentRating = 80,
                marketValue = "€320K",
                isVerified = true,
                viewsCount = 980,
                followsCount = 67,
                followingCount = 32,
                status = "Hazırım. ⚡",
                likesCount = 84,
                likedByMe = false,
                followedByMe = false,
                badge = UserBadge.VERIFIED_PLAYER,
                profileVisits = ProfileVisits(today = 14, last7Days = 72, last30Days = 290, total = 980),
                profileViewers = listOf(
                    ProfileViewer("pv_8", "Trabzonspor Scout", "Milli Takım Scoutu", UserBadge.NATIONAL_SCOUT, "2 saat önce", System.currentTimeMillis() - 7200000)
                ),
                stats = PhysicalStats(pace = 91, shooting = 75, passing = 72, dribbling = 83, defense = 38, physical = 60),
                scoutReports = emptyList(),
                tournamentHistory = emptyList()
            ),
            Player(
                id = "p_5",
                firstName = "Burak",
                lastName = "Bulut",
                photoResId = null,
                age = 22,
                birthDate = "2004-11-05",
                nationality = "Turkey",
                city = "Bursa",
                height = 191,
                weight = 86,
                preferredFoot = PreferredFoot.RIGHT,
                position = FootballPosition.GK,
                club = "Bursaspor",
                jerseyNumber = 1,
                bio = "Vocal and agile goalkeeper. Outstanding reflex saves and strong command of the penalty area.",
                instagram = "@burakgk1",
                youtubeUrl = "",
                selfRating = 76,
                scoutRating = 0,
                tournamentRating = 78,
                marketValue = "€150K",
                isVerified = false,
                viewsCount = 420,
                followsCount = 28,
                followingCount = 18,
                status = "Kalede duvar var! 🧤",
                likesCount = 45,
                likedByMe = false,
                followedByMe = false,
                badge = UserBadge.NONE,
                profileVisits = ProfileVisits(today = 8, last7Days = 45, last30Days = 150, total = 420),
                profileViewers = listOf(
                    ProfileViewer("pv_9", "Bursaspor Scout", "Resmi Scout", UserBadge.SCOUT, "4 saat önce", System.currentTimeMillis() - 14400000)
                ),
                stats = PhysicalStats(pace = 52, shooting = 78, passing = 65, dribbling = 74, defense = 22, physical = 75), // GK customized stats inside drawing
                scoutReports = emptyList(),
                tournamentHistory = emptyList()
            )
        )

        val mockTournaments = listOf(
            Tournament(
                id = "t_1",
                title = "Istanbul Elite 1v1 Arena",
                description = "Uncover Turkey's top 1v1 football talents in the ultimate technical challenge. Played in high-performance artificial turf arenas. Pro scouts are attending live.",
                countdown = "2d 12h",
                prize = "€5,000 + Super League Trial",
                participantsCount = 76,
                maxParticipants = 128,
                difficulty = "Elite",
                isApplied = false,
                leaderboard = listOf(
                    LeaderboardUser(1, "Mert Yılmaz", 95, 24, "88%", "ST"),
                    LeaderboardUser(2, "Arda Çelik", 93, 22, "82%", "AM"),
                    LeaderboardUser(3, "Kerem Aktürkoğlu", 91, 19, "79%", "LW"),
                    LeaderboardUser(4, "Yusuf Kaya", 88, 20, "75%", "LW")
                )
            ),
            Tournament(
                id = "t_2",
                title = "Ankara Neon Challenge",
                description = "Dynamic knockout format tournament sponsored by major athletic brands. High-paced game rules, 5-minute intervals. Direct visibility multiplier.",
                countdown = "5d 08h",
                prize = "€2,500 + Nike Sponsorship",
                participantsCount = 42,
                maxParticipants = 64,
                difficulty = "Pro",
                isApplied = false,
                leaderboard = listOf(
                    LeaderboardUser(1, "Arda Çelik", 97, 18, "94%", "AM"),
                    LeaderboardUser(2, "Mert Yılmaz", 90, 16, "81%", "ST"),
                    LeaderboardUser(3, "Hakan Demir", 84, 15, "67%", "DM")
                )
            ),
            Tournament(
                id = "t_3",
                title = "Izmir Underdog Showcase",
                description = "An open tournament targeting non-academy talents. Perfect launchpad for unverified players to secure professional scout reviews.",
                countdown = "12d 18h",
                prize = "€1,500 + Full Scouting Report",
                participantsCount = 18,
                maxParticipants = 64,
                difficulty = "Amateur",
                isApplied = false,
                leaderboard = emptyList()
            )
        )

        val mockChats = listOf(
            Chat(
                id = "c_1",
                otherParticipantName = "Hasan Şişman",
                otherParticipantRole = "Scout (UEFA A)",
                messages = listOf(
                    Message("m1", "other", "Hey Mert, I saw your performance in the Istanbul 1v1 Arena.", "14:20"),
                    Message("m2", "me", "Thank you, Coach! I appreciate you watching.", "14:23"),
                    Message("m3", "other", "I am interested in inviting you to our next academy showcase. Are you free next Tuesday?", "14:25")
                ),
                lastMessage = "I am interested in inviting you to our next academy showcase. Are you free next Tuesday?",
                timestamp = "14:25",
                unreadCount = 1
            ),
            Chat(
                id = "c_2",
                otherParticipantName = "Fenerbahçe SK Academy",
                otherParticipantRole = "Club Recruiter",
                messages = listOf(
                    Message("m4", "other", "Hello, we are tracking players in the Izmir region and noticed your CB profile.", "Dün"),
                    Message("m5", "me", "Hello! Yes, I am currently playing for Altınordu Academy.", "Dün")
                ),
                lastMessage = "Hello! Yes, I am currently playing for Altınordu Academy.",
                timestamp = "Dün",
                unreadCount = 0
            )
        )

        val mockNotifications = listOf(
            Notification(
                id = "n_1",
                title = "Profil Görüntülendi",
                description = "Beşiktaş Scout (Resmi Scout) profilinizi inceledi.",
                type = NotificationType.SCOUT_VIEW,
                time = "2 dakika önce"
            ),
            Notification(
                id = "n_2",
                title = "Yeni Takipçi",
                description = "Semih Yılmaz (Doğrulanmış Oyuncu) sizi takip etmeye başladı.",
                type = NotificationType.USER_FOLLOW,
                time = "20 dakika önce"
            ),
            Notification(
                id = "n_3",
                title = "Profil Yorumlandı",
                description = "Ahmet Y. profilinize yeni bir yorum ekledi.",
                type = NotificationType.PROFILE_COMMENT,
                time = "1 saat önce"
            ),
            Notification(
                id = "n_4",
                title = "Yorum Beğenildi",
                description = "Mert Yılmaz, Yakup Can'ın yaptığı yorumu beğendi.",
                type = NotificationType.COMMENT_LIKE,
                time = "2 saat önce"
            )
        )

        val mockActivities = listOf(
            ActivityItem("act_1", "Ali Öztürk", "Oyuncu", UserBadge.NONE, "turnuvaya katıldı", "Ankara Neon Challenge", "2 dakika önce", 14, false),
            ActivityItem("act_2", "Mert Yılmaz", "Oyuncu", UserBadge.VERIFIED_PLAYER, "yeni video yükledi", "Beşiktaş Altyapı Derbisi Gollerim ⚽", "15 dakika önce", 48, false),
            ActivityItem("act_3", "Hasan Şişman", "Scout", UserBadge.SCOUT, "scout puanı verdi", "Mert Yılmaz için değerlendirme raporu yayınladı", "1 saat önce", 19, false),
            ActivityItem("act_4", "Ahmet Kara", "Kulüp Yetkilisi", UserBadge.CLUB_REPRESENTATIVE, "yeni başarı kazandı", "Altınordu Ayın Oyuncusu Ödülü 🏆", "3 saat önce", 32, false),
            ActivityItem("act_5", "Serkan Bulut", "Scout", UserBadge.NATIONAL_SCOUT, "oyuncu takibine başladı", "Arda Çelik profilini listesine ekledi", "5 saat önce", 9, false)
        )

        val mockClubs = listOf(
            Club(
                id = "c_1",
                name = "Beşiktaş JK Futbol Akademisi",
                city = "İstanbul",
                foundationYear = 1903,
                coverPhotoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=1200",
                logoUrl = "https://images.unsplash.com/photo-1614680376593-902f74fa0d41?q=80&w=300",
                followerCount = 14205,
                activeAgeGroups = "U12, U14, U15, U17, U19",
                aboutText = "Beşiktaş Jimnastik Kulübü altyapısı, Türk futboluna her yıl onlarca genç yetenek kazandıran, ülkenin en köklü ve modern altyapı akademisidir. Fulya Şan Ökten Tesisleri ve Ümraniye Nevzat Demir Tesisleri'nde elit düzeyde eğitim verilmektedir.",
                trainingFacility = "BJK Fulya Hakkı Yeten Tesisleri & BJK Nevzat Demir Tesisleri",
                pitchInfo = "2 Adet Suni Çim Saha, 1 Adet Doğal Çim Saha, Tam Donanımlı Güç ve Kondisyon Merkezi",
                trainingDays = "Hafta içi her gün (Pazartesi - Cuma) saat 16:00 - 19:00",
                licenseStatus = "Resmi UEFA Elite Akademi Lisansı & TFF Tescilli Akademi",
                acceptedAgeGroups = "10 - 18 Yaş Arası Üstün Yetenekli Sporcular",
                phoneNumber = "+90 212 259 1903",
                whatsappNumber = "+90 530 190 3190",
                instagramUsername = "@bjkaltyapi",
                websiteUrl = "www.bjk.com.tr",
                locationUrl = "https://maps.google.com/?q=Nevzat+Demir+Tesisleri",
                address = "Nevzat Demir Tesisleri Cad. No:1, Ümraniye, İstanbul",
                coaches = listOf(
                    ClubCoach("cc_1", "Ömer Gülen", "UEFA Pro", "Altyapı Teknik Sorumlusu", "https://images.unsplash.com/photo-1560250097-0b93528c311a?q=80&w=300"),
                    ClubCoach("cc_2", "Sami Şen", "UEFA A", "Kaleci Antrenörü", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=300")
                ),
                players = listOf(
                    ClubPlayer("cp_1", "Semih Kılıçsoy", "ST", 18, 84, "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?q=80&w=300"),
                    ClubPlayer("cp_2", "Mustafa Erhan H.", "ST", 17, 79, "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=300"),
                    ClubPlayer("cp_3", "Fahri Kerem Ay", "AM", 19, 78, "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=300")
                ),
                achievements = listOf(
                    ClubAchievement("Şampiyonluk", "U19 Gelişim Ligi Şampiyonu", 4, "🏆"),
                    ClubAchievement("Kupa", "Türkiye Gençlik Kupası Şampiyonu", 3, "🥇"),
                    ClubAchievement("Uluslararası Başarı", "Avrupa Gençlik Turnuvası Üçüncülüğü", 1, "🥈")
                ),
                news = listOf(
                    ClubNews("cn_1", "Nevzat Demir Tesisleri Kapılarını Açıyor", "U13 ve U15 yaş grupları için seçmelerimiz 15 Temmuz tarihinde başlayacaktır. Lisanslı ve amatör tüm genç yetenekler davetlidir.", "Bugün", "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=500"),
                    ClubNews("cn_2", "Akademimizden 3 Profesyonel Sözleşme", "U19 takımımızın genç yetenekleri bugün düzenlenen imza töreniyle profesyonel sözleşmeye imza attılar. Genç kartallarımıza başarılar dileriz.", "Dün", "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?q=80&w=500")
                )
            ),
            Club(
                id = "c_2",
                name = "Altınordu FK Gençlik Akademisi",
                city = "İzmir",
                foundationYear = 1923,
                coverPhotoUrl = "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?q=80&w=1200",
                logoUrl = "https://images.unsplash.com/photo-1543351611-58f69d7c1781?q=80&w=300",
                followerCount = 19230,
                activeAgeGroups = "U11, U13, U15, U17, U19, U21",
                aboutText = "Altınordu FK, 'İyi Birey, İyi Vatandaş, İyi Futbolcu' sloganıyla hareket eden, Türkiye'nin en büyük gençlik yetiştirme akademisine sahip kulübüdür. Torbalı Metin Oktay Yerleşkesi modern spor bilimleri teknolojileriyle donatılmıştır.",
                trainingFacility = "Torbalı Metin Oktay Yerleşkesi",
                pitchInfo = "8 Adet Hibrit ve Doğal Çim Saha, Kum Saha, Reaksiyon Alanı, Cryo-Kabin ve Atletik Performans Laboratuvarı",
                trainingDays = "Pazartesi - Cumartesi (Tam Gün Programı)",
                licenseStatus = "TFF Elite A Kategori Lisansı & Uluslararası Akademi Sertifikası",
                acceptedAgeGroups = "9 - 19 Yaş Grubu",
                phoneNumber = "+90 232 463 1923",
                whatsappNumber = "+90 535 192 3192",
                instagramUsername = "@altinordufk",
                websiteUrl = "www.altinordu.org.tr",
                locationUrl = "https://maps.google.com/?q=Metin+Oktay+Yerleskesi+Altinordu",
                address = "Torbalı Metin Oktay Yerleşkesi, İzmir",
                coaches = listOf(
                    ClubCoach("cc_3", "Cengiz Göztepe", "UEFA Pro", "Akademi Direktörü", "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?q=80&w=300"),
                    ClubCoach("cc_4", "Gökhan Saygı", "UEFA A", "Atletik Performans Hocası", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=300")
                ),
                players = listOf(
                    ClubPlayer("cp_4", "Yiğit Fidan", "CB", 19, 81, "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=300"),
                    ClubPlayer("cp_5", "Sami Satılmış", "CM", 19, 77, "https://images.unsplash.com/photo-1519345182560-3f2917c472ef?q=80&w=300"),
                    ClubPlayer("cp_6", "Furkan Yöntem", "DM", 18, 76, "https://images.unsplash.com/photo-1501196354995-cbb51c65aaea?q=80&w=300")
                ),
                achievements = listOf(
                    ClubAchievement("Şampiyonluk", "U17 Elite Ligi Şampiyonluğu", 6, "🏆"),
                    ClubAchievement("Kupa", "U19 Uluslararası İzmir Kupası", 2, "🏆"),
                    ClubAchievement("Fair Play", "TFF Yılın Örnek Akademisi Ödülü", 1, "🏅")
                ),
                news = listOf(
                    ClubNews("cn_3", "U12 İzmir Cup Heyecanı Başlıyor", "Avrupa'dan dev kulüplerin katılacağı uluslararası İzmir U12 Cup turnuvamızın resmi takvimi açıklandı. Karşılaşmalar Torbalı Metin Oktay Yerleşkesinde oynanacak.", "3 gün önce", "https://images.unsplash.com/photo-1517649763962-0c623066013b?q=80&w=500")
                )
            ),
            Club(
                id = "c_3",
                name = "Galatasaray SK Futbol Akademisi",
                city = "İstanbul",
                foundationYear = 1905,
                coverPhotoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=1200",
                logoUrl = "https://images.unsplash.com/photo-1614680376593-902f74fa0d41?q=80&w=300",
                followerCount = 18500,
                activeAgeGroups = "U12, U13, U14, U15, U16, U17, U19",
                aboutText = "Galatasaray Spor Kulübü Futbol Akademisi, Florya Metin Oktay Tesisleri'nde kurulduğu günden bu yana Türk futboluna yön veren efsane isimleri ve dünya yıldızlarını yetiştirmiştir.",
                trainingFacility = "Florya Metin Oktay Tesisleri",
                pitchInfo = "3 Adet Doğal Çim Saha, 1 Adet Suni Çim Saha, Fizyoterapi ve Rehabilitasyon Merkezi",
                trainingDays = "Salı - Pazar (Pazartesi izin günü)",
                licenseStatus = "UEFA Elite Youth Academy License & TFF A Lisansı",
                acceptedAgeGroups = "8 - 18 Yaş",
                phoneNumber = "+90 212 465 1905",
                whatsappNumber = "+90 532 190 5190",
                instagramUsername = "@gsfutbolakademi",
                websiteUrl = "www.galatasaray.org",
                locationUrl = "https://maps.google.com/?q=Florya+Metin+Oktay+Tesisleri",
                address = "Florya Metin Oktay Tesisleri, Bakırköy, İstanbul",
                coaches = listOf(
                    ClubCoach("cc_5", "Ali Yavaş", "UEFA Pro", "Akademi Genel Sorumlusu", "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?q=80&w=300")
                ),
                players = listOf(
                    ClubPlayer("cp_7", "Efe Akman", "DM", 18, 77, "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=300")
                ),
                achievements = listOf(
                    ClubAchievement("Şampiyonluk", "U19 Süper Kupa Şampiyonu", 8, "🏆")
                ),
                news = listOf(
                    ClubNews("cn_4", "Geleceğin Aslanları Florya'da Buluşuyor", "8-11 yaş grupları arası Galatasaray futbol okulları arası seçmelerimiz bu hafta sonu Florya tesislerinde organize edilecektir.", "5 gün önce", "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=500")
                ),
                clubType = "Profesyonel Kulüp"
            ),
            Club(
                id = "c_4",
                name = "Kadıköy Futbol Akademisi",
                city = "İstanbul",
                foundationYear = 2015,
                coverPhotoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=1200",
                logoUrl = "https://images.unsplash.com/photo-1543351611-58f69d7c1781?q=80&w=300",
                followerCount = 4250,
                activeAgeGroups = "U9, U11, U13, U15",
                aboutText = "İstanbul Anadolu Yakası'nın en prestijli özel futbol akademisi. Amacımız genç sporcuları profesyonel kulüplerin altyapılarına hazırlamaktır.",
                trainingFacility = "Kadıköy Arena Halı Saha Tesisleri",
                pitchInfo = "1 Adet Kapalı Suni Çim Saha, Antrenman İstasyonları",
                trainingDays = "Cumartesi - Pazar saat 09:00 - 13:00",
                licenseStatus = "TFF Tescilli Özel Spor Okulu",
                acceptedAgeGroups = "7 - 15 Yaş",
                phoneNumber = "+90 216 111 2233",
                whatsappNumber = "+90 541 111 2233",
                instagramUsername = "@kadikoyfutbol",
                websiteUrl = "www.kadikoyfutbol.com",
                locationUrl = "https://maps.google.com/?q=Kadikoy+Arena",
                address = "Caferağa Mah. Moda Cad. No:12, Kadıköy, İstanbul",
                coaches = listOf(
                    ClubCoach("cc_6", "Murat Şahin", "UEFA B", "Başantrenör", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=300")
                ),
                players = listOf(
                    ClubPlayer("cp_8", "Can Uzun", "AM", 14, 72, "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?q=80&w=300")
                ),
                achievements = listOf(
                    ClubAchievement("Kupa", "İstanbul Özel Akademiler Kupası", 1, "🏆")
                ),
                news = listOf(
                    ClubNews("cn_5", "Yaz Kampı Başvuruları Başladı", "2026 Yaz Kampımız Şile'de gerçekleştirilecektir. Sınırlı kontenjan için hemen kayıt olun.", "Bugün", "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=500")
                ),
                clubType = "Özel Akademi"
            ),
            Club(
                id = "c_5",
                name = "Karşıyaka Gençlik Gelişim Merkezi",
                city = "İzmir",
                foundationYear = 1912,
                coverPhotoUrl = "https://images.unsplash.com/photo-1529900748604-07564a03e7a6?q=80&w=1200",
                logoUrl = "https://images.unsplash.com/photo-1614680376593-902f74fa0d41?q=80&w=300",
                followerCount = 8900,
                activeAgeGroups = "U13, U15, U17, U19",
                aboutText = "Karşıyaka Spor Kulübü'nün şanlı altyapı akademisi. İzmir'in kuzeyinde yeşil-kırmızı renklerle yetenekli futbolcular yetiştirmeye devam ediyoruz.",
                trainingFacility = "Çiğli Selçuk Yaşar Tesisleri",
                pitchInfo = "2 Adet Doğal Çim Saha, Atletik Gelişim Salonu",
                trainingDays = "Hafta içi 4 gün",
                licenseStatus = "TFF Gelişim Ligleri Katılımcısı",
                acceptedAgeGroups = "11 - 19 Yaş",
                phoneNumber = "+90 232 368 1912",
                whatsappNumber = "+90 533 368 1912",
                instagramUsername = "@kskaltyapi",
                websiteUrl = "www.karsiyaka.org.tr",
                locationUrl = "https://maps.google.com/?q=Selcuk+Yasar+Tesisleri",
                address = "Yalı Mah. 6500 Sok. No:4, Karşıyaka, İzmir",
                coaches = listOf(
                    ClubCoach("cc_7", "Reşat Kartal", "UEFA A", "Altyapı Koordinatörü", "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?q=80&w=300")
                ),
                players = listOf(
                    ClubPlayer("cp_9", "Ege Demir", "CB", 16, 75, "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?q=80&w=300")
                ),
                achievements = listOf(
                    ClubAchievement("Kupa", "Ege Bölgesi Gençlik Kupası Şampiyonu", 2, "🏆")
                ),
                news = listOf(
                    ClubNews("cn_6", "Yeni Sezon Hazırlıkları Çiğli'de Başladı", "U17 ve U19 takımlarımız yeni sezon hazırlıklarına Çiğli Selçuk Yaşar tesislerimizde start verdi.", "2 gün önce", "https://images.unsplash.com/photo-1517649763962-0c623066013b?q=80&w=500")
                ),
                clubType = "Spor Kulübü"
            ),
            Club(
                id = "c_6",
                name = "Bornova Belediyesi Spor Akademisi",
                city = "İzmir",
                foundationYear = 2011,
                coverPhotoUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=1200",
                logoUrl = "https://images.unsplash.com/photo-1543351611-58f69d7c1781?q=80&w=300",
                followerCount = 3100,
                activeAgeGroups = "U11, U13, U15",
                aboutText = "Bornova Belediyesi öncülüğünde, her çocuğun spora erişimini sağlamak amacıyla kurulan belediye spor kulübü altyapısı.",
                trainingFacility = "Bornova Şehir Stadı & Yusuf Tırpancı Tesisleri",
                pitchInfo = "1 Adet Büyük Suni Çim Saha, Tribün ve Koşu Parkurları",
                trainingDays = "Hafta sonu Cumartesi - Pazar",
                licenseStatus = "TFF Amatör Lisans & Belediye Tescilli Kulüp",
                acceptedAgeGroups = "8 - 15 Yaş",
                phoneNumber = "+90 232 999 3535",
                whatsappNumber = "+90 535 999 3535",
                instagramUsername = "@bornovabldspor",
                websiteUrl = "www.bornova.bel.tr",
                locationUrl = "https://maps.google.com/?q=Yusuf+Tirpanci+Sahasi",
                address = "Kazımdirik Mah. Bornova, İzmir",
                coaches = listOf(
                    ClubCoach("cc_8", "Ahmet Yılmaz", "UEFA B", "Antrenör", "https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?q=80&w=300")
                ),
                players = emptyList(),
                achievements = emptyList(),
                news = emptyList(),
                clubType = "Belediye Kulübü"
            )
        )

        val mockUsers = listOf(
            AppUser("p_1", "Mert Yılmaz", "mert@futbolcubul.com", UserRole.PLAYER, "Istanbul", "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=300", isVerified = true, createdAt = "2026-07-12T21:00:00"),
            AppUser("p_2", "Arda Çelik", "arda@futbolcubul.com", UserRole.PLAYER, "Ankara", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=300", isVerified = true, createdAt = "2026-07-12T12:00:00"),
            AppUser("scout_hasan", "Hasan Gözlemci", "hasan@futbolcubul.com", UserRole.SCOUT, "Istanbul", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=300", isVerified = true, createdAt = "2026-07-12T23:30:00"),
            AppUser("coach_fatih", "Fatih Terim", "fatih@futbolcubul.com", UserRole.COACH, "Istanbul", "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?q=80&w=300", isVerified = true, createdAt = "2026-07-13T00:20:00", isFeatured = true, featuredAt = "2026-07-16T12:00:00"),
            AppUser("club_gs", "Galatasaray SK", "gs@futbolcubul.com", UserRole.CLUB, "Istanbul", "https://images.unsplash.com/photo-1551958219-acbc608c6377?q=80&w=300", isVerified = true, createdAt = "2026-07-13T00:28:00"),
            AppUser("media_burak", "Burak Yıldız", "burak@futbolcubul.com", UserRole.MEDIA, "Izmir", "https://images.unsplash.com/photo-1544005313-94ddf0286df2?q=80&w=300", isVerified = false, createdAt = "2026-07-13T00:15:00"),
            AppUser("media_ahmet", "Ahmet Seven", "ahmet@futbolcubul.com", UserRole.MEDIA, "Antalya", "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=300", isVerified = false, createdAt = "2026-07-13T00:05:00"),
            AppUser("admin_1", "Sistem Yöneticisi", "admin@futbolcubul.com", UserRole.ADMIN, "Istanbul", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=300", isVerified = true, createdAt = "2026-07-11T12:00:00")
        )

        _players.value = mockPlayers
        _clubs.value = mockClubs
        mockClubs.forEach { localClubs[it.id] = it }
        _tournaments.value = mockTournaments
        _chats.value = mockChats
        _notifications.value = mockNotifications
        _activityFeed.value = mockActivities
        _users.value = mockUsers
        _isPlayersLoaded.value = true
        }
    }

    // CRUD / Operations simulated offline

    fun addPlayer(player: Player) {
        localPlayers[player.id] = player
        if (isFirebaseSyncActive) {
            _rawPlayers.value = (_rawPlayers.value.filter { it.id != player.id } + player).toList()
            updateComputedPlayers()
        } else {
            _players.value = (_players.value.filter { it.id != player.id } + player).toList()
        }
    }

    fun updatePlayer(updatedPlayer: Player) {
        localPlayers[updatedPlayer.id] = updatedPlayer
        if (isFirebaseSyncActive) {
            _rawPlayers.value = _rawPlayers.value.map { if (it.id == updatedPlayer.id) updatedPlayer else it }
            updateComputedPlayers()
        } else {
            _players.value = _players.value.map { if (it.id == updatedPlayer.id) updatedPlayer else it }
        }
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("players").document(updatedPlayer.id).set(playerToMap(updatedPlayer))
                .addOnSuccessListener {
                    android.util.Log.d("FutbolcuBul", "PLAYER SAVED")
                }
        } catch (e: Exception) {
            // Ignore if offline
        }
    }

    fun getPlayerById(id: String): Player? {
        return _players.value.find { it.id == id }
    }

    fun applyToTournament(tournamentId: String) {
        _tournaments.value = _tournaments.value.map {
            if (it.id == tournamentId) it.copy(isApplied = true, participantsCount = it.participantsCount + 1) else it
        }
    }

    fun toggleFavoritePlayer(playerId: String) {
        val current = _favoritePlayerIds.value
        if (current.contains(playerId)) {
            _favoritePlayerIds.value = current - playerId
        } else {
            _favoritePlayerIds.value = current + playerId
        }

        // Also increment/decrement followsCount of player
        _players.value = _players.value.map {
            if (it.id == playerId) {
                val change = if (current.contains(playerId)) -1 else 1
                it.copy(followsCount = (it.followsCount + change).coerceAtLeast(0))
            } else it
        }
    }

    fun incrementPlayerViews(playerId: String) {
        localPlayers[playerId]?.let { p ->
            localPlayers[playerId] = p.copy(viewsCount = p.viewsCount + 1)
        }
        if (isFirebaseSyncActive) {
            val rawPlayer = _rawPlayers.value.find { it.id == playerId }
            if (rawPlayer != null) {
                val updated = rawPlayer.copy(viewsCount = rawPlayer.viewsCount + 1)
                _rawPlayers.value = _rawPlayers.value.map { if (it.id == playerId) updated else it }
                updateComputedPlayers()
                
                try {
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    db.collection("players").document(playerId).update("viewsCount", updated.viewsCount)
                        .addOnFailureListener { e ->
                            android.util.Log.e("FutbolcuBul", "Failed to update viewsCount in Firestore", e)
                        }
                } catch (e: Exception) {
                    // Ignore
                }
            } else {
                // If not found in raw list, find in computed and update there
                val compPlayer = _players.value.find { it.id == playerId }
                if (compPlayer != null) {
                    val updated = compPlayer.copy(viewsCount = compPlayer.viewsCount + 1)
                    _rawPlayers.value = (_rawPlayers.value.filter { it.id != playerId } + updated).toList()
                    updateComputedPlayers()
                    
                    try {
                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        db.collection("players").document(playerId).update("viewsCount", updated.viewsCount)
                            .addOnFailureListener { e ->
                                android.util.Log.e("FutbolcuBul", "Failed to update viewsCount in Firestore", e)
                            }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        } else {
            _players.value = _players.value.map {
                if (it.id == playerId) it.copy(viewsCount = it.viewsCount + 1) else it
            }
        }
    }

    fun sendChatMessage(chatId: String, text: String) {
        _chats.value = _chats.value.map { chat ->
            if (chat.id == chatId) {
                val newMsg = Message(UUID.randomUUID().toString(), "me", text, "Just now")
                chat.copy(
                    messages = chat.messages + newMsg,
                    lastMessage = text,
                    timestamp = "Just now",
                    unreadCount = 0
                )
            } else chat
        }
    }

    fun addScoutReport(playerId: String, scoutName: String, scoutBadge: String, technical: Int, tactical: Int, mental: Int, positioning: Int, comment: String) {
        val newReport = ScoutReport(
            id = UUID.randomUUID().toString(),
            scoutName = scoutName,
            scoutBadge = scoutBadge,
            technical = technical,
            tactical = tactical,
            mental = mental,
            positioning = positioning,
            comment = comment,
            date = "2026-07-09"
        )

        _players.value = _players.value.map { player ->
            if (player.id == playerId) {
                val updatedReports = player.scoutReports + newReport
                val avgScoutRating = updatedReports.map { it.averageRating }.average().toInt()
                player.copy(
                    scoutReports = updatedReports,
                    scoutRating = avgScoutRating
                )
            } else player
        }

        // Add a notification too
        val newNotification = Notification(
            id = UUID.randomUUID().toString(),
            title = "New Scout Evaluation",
            description = "$scoutName ($scoutBadge) added a scouting report for you.",
            type = NotificationType.SCOUT_RATE,
            time = "Just now"
        )
        _notifications.value = listOf(newNotification) + _notifications.value
    }

    fun createChatWithPlayer(playerFullName: String, playerRole: String): Chat {
        val existing = _chats.value.find { it.otherParticipantName == playerFullName }
        if (existing != null) return existing

        val newChat = Chat(
            id = UUID.randomUUID().toString(),
            otherParticipantName = playerFullName,
            otherParticipantRole = playerRole,
            messages = emptyList(),
            lastMessage = "Started conversation",
            timestamp = "Just now"
        )
        _chats.value = listOf(newChat) + _chats.value
        return newChat
    }

    fun addCommunityReview(playerId: String, reviewerName: String, reviewerRole: String, comment: String, tag: String) {
        addSocialComment(playerId, reviewerName, reviewerRole, UserBadge.NONE, comment, tag)
    }

    fun toggleFollowPlayer(playerId: String) {
        val myId = currentUserIdValue
        if (isFirebaseSyncActive && myId.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            val isFollowing = _follows.value.any { it.followerId == myId && it.followedId == playerId }
            val docId = "${myId}_${playerId}"
            
            // Optimistic Update: Update local follows list immediately to make the UI instantly responsive
            val currentFollows = _follows.value.toMutableList()
            if (isFollowing) {
                currentFollows.removeAll { it.followerId == myId && it.followedId == playerId }
                _follows.value = currentFollows
                updateComputedPlayers()
                
                db.collection("follows").document(docId).delete()
                    .addOnFailureListener { e ->
                        android.util.Log.e("FutbolcuBul", "Follow write failed, switching to local follows", e)
                        isFollowsSyncActive = false
                    }
            } else {
                currentFollows.add(FollowItem(myId, playerId))
                _follows.value = currentFollows
                updateComputedPlayers()
                
                val data = mapOf(
                    "followerId" to myId,
                    "followedId" to playerId
                )
                db.collection("follows").document(docId).set(data)
                    .addOnFailureListener { e ->
                        android.util.Log.e("FutbolcuBul", "Follow write failed, switching to local follows", e)
                        isFollowsSyncActive = false
                    }
                
                val myUser = _users.value.find { it.id == myId }
                val myName = myUser?.name ?: "Mert Yılmaz"
                val notif = Notification(
                    id = UUID.randomUUID().toString(),
                    title = "Yeni Takipçi",
                    description = "$myName sizi takip etmeye başladı.",
                    type = NotificationType.USER_FOLLOW,
                    time = "Şimdi",
                    recipientId = playerId,
                    senderId = myId
                )
                saveNotificationToFirestore(notif)

                val activity = ActivityItem(
                    id = UUID.randomUUID().toString(),
                    userName = myName,
                    userRole = if (myUser?.role == UserRole.PLAYER) "Oyuncu" else "Kullanıcı",
                    userBadge = if (myUser?.role == UserRole.PLAYER) UserBadge.VERIFIED_PLAYER else UserBadge.NONE,
                    actionText = "takip etmeye başladı",
                    detailText = "profilini takip ediyor",
                    timeAgo = "Şimdi"
                )
                db.collection("activities").document(activity.id).set(activityItemToMap(activity))
            }
        } else {
            _players.value = _players.value.map { player ->
                if (player.id == playerId) {
                    val newFollowed = !player.followedByMe
                    val diff = if (newFollowed) 1 else -1
                    val updated = player.copy(
                        followedByMe = newFollowed,
                        followsCount = (player.followsCount + diff).coerceAtLeast(0)
                    )
                    if (newFollowed) {
                        val notif = Notification(
                            id = UUID.randomUUID().toString(),
                            title = "Yeni Takipçi",
                            description = "Mert Yılmaz sizi takip etmeye başladı.",
                            type = NotificationType.USER_FOLLOW,
                            time = "Şimdi"
                        )
                        _notifications.value = listOf(notif) + _notifications.value

                        val activity = ActivityItem(
                            id = UUID.randomUUID().toString(),
                            userName = "Mert Yılmaz",
                            userRole = "Oyuncu",
                            userBadge = UserBadge.VERIFIED_PLAYER,
                            actionText = "takip etmeye başladı",
                            detailText = "${player.fullName} profilini takip ediyor",
                            timeAgo = "Şimdi"
                        )
                        _activityFeed.value = listOf(activity) + _activityFeed.value
                    }
                    updated
                } else player
            }
        }
    }

    fun toggleLikePlayer(playerId: String) {
        val myId = currentUserIdValue
        if (isFirebaseSyncActive && myId.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            val isLiked = _likes.value.any { it.likerId == myId && it.likedId == playerId }
            val docId = "${myId}_${playerId}"
            
            // Optimistic Update: Update local likes list immediately to make the UI instantly responsive
            val currentLikes = _likes.value.toMutableList()
            if (isLiked) {
                currentLikes.removeAll { it.likerId == myId && it.likedId == playerId }
                _likes.value = currentLikes
                updateComputedPlayers()
                
                db.collection("likes").document(docId).delete()
                    .addOnFailureListener { e ->
                        android.util.Log.e("FutbolcuBul", "Like write failed, switching to local likes", e)
                        isLikesSyncActive = false
                    }
            } else {
                currentLikes.add(LikeItem(myId, playerId))
                _likes.value = currentLikes
                updateComputedPlayers()
                
                val data = mapOf(
                    "likerId" to myId,
                    "likedId" to playerId
                )
                db.collection("likes").document(docId).set(data)
                    .addOnFailureListener { e ->
                        android.util.Log.e("FutbolcuBul", "Like write failed, switching to local likes", e)
                        isLikesSyncActive = false
                    }
                
                val myUser = _users.value.find { it.id == myId }
                val myName = myUser?.name ?: "Mert Yılmaz"
                val notif = Notification(
                    id = UUID.randomUUID().toString(),
                    title = "Profil Beğenildi",
                    description = "$myName profilinizi beğendi.",
                    type = NotificationType.COMMENT_LIKE,
                    time = "Şimdi",
                    recipientId = playerId,
                    senderId = myId
                )
                saveNotificationToFirestore(notif)
            }
        } else {
            _players.value = _players.value.map { player ->
                if (player.id == playerId) {
                    val newLiked = !player.likedByMe
                    val diff = if (newLiked) 1 else -1
                    val updated = player.copy(
                        likedByMe = newLiked,
                        likesCount = (player.likesCount + diff).coerceAtLeast(0)
                    )
                    if (newLiked) {
                        val notif = Notification(
                            id = UUID.randomUUID().toString(),
                            title = "Profil Beğenildi",
                            description = "Mert Yılmaz profilinizi beğendi.",
                            type = NotificationType.COMMENT_LIKE,
                            time = "Şimdi"
                        )
                        _notifications.value = listOf(notif) + _notifications.value
                    }
                    updated
                } else player
            }
        }
    }

    fun addSocialComment(playerId: String, commenterName: String, commenterRole: String, commenterBadge: UserBadge, commentText: String, tag: String) {
        val newReview = CommunityReview(
            id = UUID.randomUUID().toString(),
            reviewerName = commenterName,
            reviewerRole = commenterRole,
            reviewerBadge = commenterBadge,
            comment = commentText,
            tag = tag,
            date = "Şimdi"
        )
        _players.value = _players.value.map { player ->
            if (player.id == playerId) {
                val updatedReviews = player.communityReviews + newReview
                // Trigger notification if not commenting on own profile
                if (player.id != "p_1") {
                    val notif = Notification(
                        id = UUID.randomUUID().toString(),
                        title = "Yeni Yorum",
                        description = "$commenterName profilinize yorum yaptı.",
                        type = NotificationType.PROFILE_COMMENT,
                        time = "Şimdi"
                    )
                    _notifications.value = listOf(notif) + _notifications.value
                }
                player.copy(communityReviews = updatedReviews)
            } else player
        }
    }

    fun toggleLikeComment(playerId: String, commentId: String) {
        _players.value = _players.value.map { player ->
            if (player.id == playerId) {
                val updatedReviews = player.communityReviews.map { comment ->
                    if (comment.id == commentId) {
                        val newLiked = !comment.likedByMe
                        val diff = if (newLiked) 1 else -1
                        comment.copy(
                            likedByMe = newLiked,
                            likesCount = (comment.likesCount + diff).coerceAtLeast(0)
                        )
                    } else comment
                }
                player.copy(communityReviews = updatedReviews)
            } else player
        }
    }

    fun deleteComment(playerId: String, commentId: String) {
        _players.value = _players.value.map { player ->
            if (player.id == playerId) {
                val updatedReviews = player.communityReviews.filter { it.id != commentId }
                player.copy(communityReviews = updatedReviews)
            } else player
        }
    }

    fun reportComment(playerId: String, commentId: String) {
        _players.value = _players.value.map { player ->
            if (player.id == playerId) {
                val updatedReviews = player.communityReviews.filter { it.id != commentId }
                player.copy(communityReviews = updatedReviews)
            } else player
        }
    }

    fun replyToComment(playerId: String, commentId: String, replierName: String, replierRole: String, replierBadge: UserBadge, replyText: String) {
        val newReply = CommentReply(
            id = UUID.randomUUID().toString(),
            senderName = replierName,
            senderRole = replierRole,
            senderBadge = replierBadge,
            text = replyText,
            date = "Şimdi"
        )
        _players.value = _players.value.map { player ->
            if (player.id == playerId) {
                val updatedReviews = player.communityReviews.map { comment ->
                    if (comment.id == commentId) {
                        comment.copy(replies = comment.replies + newReply)
                    } else comment
                }
                player.copy(communityReviews = updatedReviews)
            } else player
        }
    }

    fun updatePlayerStatus(playerId: String, newStatus: String) {
        _players.value = _players.value.map { player ->
            if (player.id == playerId) {
                val updated = player.copy(status = newStatus)
                if (newStatus.isNotEmpty()) {
                    val activity = ActivityItem(
                        id = UUID.randomUUID().toString(),
                        userName = player.fullName,
                        userRole = "Oyuncu",
                        userBadge = player.badge,
                        actionText = "durumunu güncelledi",
                        detailText = "\"$newStatus\"",
                        timeAgo = "Şimdi"
                    )
                    _activityFeed.value = listOf(activity) + _activityFeed.value
                }
                updated
            } else player
        }
    }

    fun toggleActivityLike(activityId: String) {
        _activityFeed.value = _activityFeed.value.map { activity ->
            if (activity.id == activityId) {
                val newLiked = !activity.likedByMe
                val diff = if (newLiked) 1 else -1
                activity.copy(
                    likedByMe = newLiked,
                    likesCount = (activity.likesCount + diff).coerceAtLeast(0)
                )
            } else activity
        }
    }

    fun toggleFollowClub(clubId: String) {
        val myId = currentUserIdValue
        if (isFirebaseSyncActive && myId.isNotEmpty()) {
            val db = FirebaseFirestore.getInstance()
            val isFollowing = _follows.value.any { it.followerId == myId && it.followedId == clubId }
            val docId = "${myId}_${clubId}"
            
            // Optimistic Update: Update local follows list immediately to make the UI instantly responsive
            val currentFollows = _follows.value.toMutableList()
            if (isFollowing) {
                currentFollows.removeAll { it.followerId == myId && it.followedId == clubId }
                _follows.value = currentFollows
                updateComputedClubs()
                
                db.collection("follows").document(docId).delete()
                    .addOnFailureListener { e ->
                        android.util.Log.e("FutbolcuBul", "Follow write failed, switching to local follows", e)
                        isFollowsSyncActive = false
                    }
            } else {
                currentFollows.add(FollowItem(myId, clubId))
                _follows.value = currentFollows
                updateComputedClubs()
                
                val data = mapOf(
                    "followerId" to myId,
                    "followedId" to clubId
                )
                db.collection("follows").document(docId).set(data)
                    .addOnFailureListener { e ->
                        android.util.Log.e("FutbolcuBul", "Follow write failed, switching to local follows", e)
                        isFollowsSyncActive = false
                    }
                
                // Add notification if available
                val myUser = _users.value.find { it.id == myId }
                val myName = myUser?.name ?: "Mert Yılmaz"
                val notif = Notification(
                    id = java.util.UUID.randomUUID().toString(),
                    title = "Yeni Takipçi",
                    description = "$myName kulübünüzü takip etmeye başladı.",
                    type = NotificationType.USER_FOLLOW,
                    time = "Şimdi",
                    recipientId = clubId,
                    senderId = myId
                )
                saveNotificationToFirestore(notif)
            }
        } else {
            _clubs.value = _clubs.value.map { club ->
                if (club.id == clubId) {
                    val newFollowed = !club.followedByMe
                    val diff = if (newFollowed) 1 else -1
                    val updated = club.copy(
                        followedByMe = newFollowed,
                        followerCount = (club.followerCount + diff).coerceAtLeast(0)
                    )
                    localClubs[clubId] = updated
                    saveClubToFirestore(updated)
                    updated
                } else club
            }
        }
    }

    fun applyToClub(clubId: String, playerId: String) {
        if (isFirebaseSyncActive) {
            val db = FirebaseFirestore.getInstance()
            val docId = "${playerId}_${clubId}"
            val existingApp = _applications.value.find { it.clubId == clubId && it.playerId == playerId && it.status == "PENDING" }
            if (existingApp != null) {
                // Cancel application: delete the document
                db.collection("applications").document(docId).delete()
            } else {
                // Create application
                val appData = mapOf(
                    "clubId" to clubId,
                    "playerId" to playerId,
                    "status" to "PENDING"
                )
                db.collection("applications").document(docId).set(appData)
                
                // Add a notification for the club
                val player = _players.value.find { it.id == playerId }
                val playerName = if (player != null) "${player.firstName} ${player.lastName}" else "Bir Futbolcu"
                val notif = Notification(
                    id = java.util.UUID.randomUUID().toString(),
                    title = "Akademi Başvurusu",
                    description = "$playerName kulübünüze altyapı kayıt başvurusu yaptı.",
                    type = NotificationType.CLUB_PLAYER_INVITE,
                    time = "Şimdi",
                    recipientId = clubId,
                    senderId = playerId
                )
                saveNotificationToFirestore(notif)
            }
        } else {
            _clubs.value = _clubs.value.map { club ->
                if (club.id == clubId) {
                    val alreadyApplied = club.appliedPlayerIds.contains(playerId)
                    val newList = if (alreadyApplied) {
                        club.appliedPlayerIds - playerId
                    } else {
                        club.appliedPlayerIds + playerId
                    }
                    val updated = club.copy(
                        registrationApplied = !alreadyApplied,
                        appliedPlayerIds = newList
                    )
                    localClubs[clubId] = updated
                    saveClubToFirestore(updated)
                    updated
                } else club
            }
        }
    }

    fun acceptClubApplication(clubId: String, playerId: String) {
        if (isFirebaseSyncActive) {
            val db = FirebaseFirestore.getInstance()
            val docId = "${playerId}_${clubId}"
            
            // 1. Update application status in Firestore
            db.collection("applications").document(docId).update("status", "APPROVED")
            
            // 2. Add player to the club's players list in Firestore
            val clubObj = _clubs.value.find { it.id == clubId }
            val playerObj = _players.value.find { it.id == playerId }
            if (clubObj != null) {
                val alreadyInClub = clubObj.players.any { it.id == playerId }
                val newPlayers = if (alreadyInClub) {
                    clubObj.players
                } else {
                    clubObj.players + ClubPlayer(
                        id = playerId,
                        name = if (playerObj != null) "${playerObj.firstName} ${playerObj.lastName}" else "Yeni Futbolcu",
                        position = playerObj?.position?.shortName ?: "CM",
                        age = playerObj?.age ?: 17,
                        photoUrl = playerObj?.photoUrl ?: "",
                        overallRating = if (playerObj != null && playerObj.scoutRating > 0) playerObj.scoutRating else (playerObj?.selfRating ?: 75)
                    )
                }
                
                val newApplied = clubObj.appliedPlayerIds - playerId
                val updatedClub = clubObj.copy(
                    players = newPlayers,
                    appliedPlayerIds = newApplied
                )
                db.collection("clubs").document(clubId).set(clubToMap(updatedClub))
            }
            
            // 3. Update player's club in Firestore
            if (playerObj != null && clubObj != null) {
                val updatedPlayer = playerObj.copy(club = clubObj.name)
                db.collection("players").document(playerId).set(playerToMap(updatedPlayer))
            }
            
            // 4. Create notification for player
            val notif = Notification(
                id = java.util.UUID.randomUUID().toString(),
                title = "Başvuru Onaylandı",
                description = "${clubObj?.name ?: "Kulüp"} akademi başvurunuzu onayladı!",
                type = NotificationType.APP_APPROVED,
                time = "Şimdi",
                recipientId = playerId,
                senderId = clubId
            )
            saveNotificationToFirestore(notif)
            
        } else {
            val player = _players.value.find { it.id == playerId }
            _clubs.value = _clubs.value.map { club ->
                if (club.id == clubId) {
                    val newApplied = club.appliedPlayerIds - playerId
                    val alreadyInClub = club.players.any { it.id == playerId }
                    val newPlayers = if (alreadyInClub) {
                        club.players
                    } else {
                        club.players + ClubPlayer(
                            id = playerId,
                            name = if (player != null) "${player.firstName} ${player.lastName}" else "Yeni Futbolcu",
                            position = player?.position?.shortName ?: "CM",
                            age = player?.age ?: 17,
                            photoUrl = player?.photoUrl ?: "",
                            overallRating = if (player != null && player.scoutRating > 0) player.scoutRating else (player?.selfRating ?: 75)
                        )
                    }
                    
                    if (player != null) {
                        val updatedPlayer = player.copy(club = club.name)
                        localPlayers[playerId] = updatedPlayer
                        _players.value = _players.value.map { if (it.id == playerId) updatedPlayer else it }
                    }
                    
                    val updated = club.copy(
                        appliedPlayerIds = newApplied,
                        players = newPlayers
                    )
                    localClubs[clubId] = updated
                    saveClubToFirestore(updated)
                    updated
                } else club
            }
        }
    }

    fun rejectClubApplication(clubId: String, playerId: String) {
        if (isFirebaseSyncActive) {
            val db = FirebaseFirestore.getInstance()
            val docId = "${playerId}_${clubId}"
            
            // 1. Update application status to REJECTED in Firestore
            db.collection("applications").document(docId).update("status", "REJECTED")
            
            // 2. Remove from club's applied list in Firestore
            val clubObj = _clubs.value.find { it.id == clubId }
            if (clubObj != null) {
                val newApplied = clubObj.appliedPlayerIds - playerId
                val updatedClub = clubObj.copy(appliedPlayerIds = newApplied)
                db.collection("clubs").document(clubId).set(clubToMap(updatedClub))
            }
            
            // 3. Create notification for player
            val notif = Notification(
                id = java.util.UUID.randomUUID().toString(),
                title = "Başvuru Reddedildi",
                description = "${clubObj?.name ?: "Kulüp"} akademi başvurunuzu olumsuz değerlendirdi.",
                type = NotificationType.CLUB_PLAYER_INVITE,
                time = "Şimdi",
                recipientId = playerId,
                senderId = clubId
            )
            saveNotificationToFirestore(notif)
            
        } else {
            _clubs.value = _clubs.value.map { club ->
                if (club.id == clubId) {
                    val updated = club.copy(appliedPlayerIds = club.appliedPlayerIds - playerId)
                    localClubs[clubId] = updated
                    saveClubToFirestore(updated)
                    updated
                } else club
            }
        }
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
        _clubs.value = _clubs.value.map { club ->
            if (club.id == clubId) {
                val updated = club.copy(
                    logoUrl = if (logoUrl.isNotEmpty()) logoUrl else club.logoUrl,
                    phoneNumber = phone,
                    instagramUsername = instagram,
                    activeStudentsCount = activeStudents,
                    coachesCount = coachesCount,
                    trophyCount = trophyCount,
                    aboutText = aboutText,
                    trainingFacility = trainingFacility,
                    locationUrl = locationUrl,
                    websiteUrl = websiteUrl,
                    district = district,
                    city = city,
                    hasLicense = hasLicense,
                    hasSummerSchool = hasSummerSchool,
                    hasWinterSchool = hasWinterSchool,
                    ageGroups = ageGroups,
                    activeAgeGroups = ageGroups
                )
                localClubs[clubId] = updated
                saveClubToFirestore(updated)
                updated
            } else club
        }
    }

    fun addClubNews(clubId: String, title: String, content: String) {
        val newNews = ClubNews(
            id = java.util.UUID.randomUUID().toString(),
            title = title,
            content = content,
            date = "Bugün",
            imageUrl = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?q=80&w=500"
        )
        _clubs.value = _clubs.value.map { club ->
            if (club.id == clubId) {
                val updated = club.copy(news = listOf(newNews) + club.news)
                localClubs[clubId] = updated
                saveClubToFirestore(updated)
                updated
            } else club
        }
    }

    fun sendClubPlayerInvite(clubId: String, clubName: String, playerId: String) {
        val notificationId = java.util.UUID.randomUUID().toString()
        val notif = Notification(
            id = notificationId,
            title = clubName,
            description = "$clubName seni futbolcu kadrosuna davet ediyor! Katılmak ister misin?",
            type = NotificationType.CLUB_PLAYER_INVITE,
            time = "Şimdi",
            isRead = false,
            recipientId = playerId,
            senderId = clubId
        )
        addNotification(notif)
    }

    fun sendClubCoachInvite(clubId: String, clubName: String, coachId: String) {
        val notificationId = java.util.UUID.randomUUID().toString()
        val notif = Notification(
            id = notificationId,
            title = clubName,
            description = "$clubName seni antrenör kadrosuna davet ediyor! Katılmak ister misin?",
            type = NotificationType.CLUB_COACH_INVITE,
            time = "Şimdi",
            isRead = false,
            recipientId = coachId,
            senderId = clubId
        )
        addNotification(notif)
    }

    fun acceptPlayerInvite(notificationId: String, clubId: String, playerId: String) {
        val club = _clubs.value.find { it.id == clubId }
        val clubName = club?.name ?: "Kulüp"
        _players.value = _players.value.map { player ->
            if (player.id == playerId) {
                val updated = player.copy(club = clubName)
                savePlayerToFirestore(updated)
                updated
            } else player
        }
        val playerObj = _players.value.find { it.id == playerId }
        _clubs.value = _clubs.value.map { clubObj ->
            if (clubObj.id == clubId) {
                val alreadyExists = clubObj.players.any { it.id == playerId }
                val newPlayersList = if (alreadyExists) clubObj.players else {
                    clubObj.players + ClubPlayer(
                        id = playerId,
                        name = playerObj?.let { "${it.firstName} ${it.lastName}" } ?: "Yeni Futbolcu",
                        position = playerObj?.position?.shortName ?: "CM",
                        age = playerObj?.age ?: 18,
                        overallRating = playerObj?.scoutRating?.takeIf { it > 0 } ?: playerObj?.selfRating ?: 75,
                        photoUrl = playerObj?.photoUrl ?: ""
                    )
                }
                val updated = clubObj.copy(players = newPlayersList)
                localClubs[clubId] = updated
                saveClubToFirestore(updated)
                updated
            } else clubObj
        }
        markNotificationAsRead(notificationId)
    }

    fun rejectPlayerInvite(notificationId: String) {
        markNotificationAsRead(notificationId)
    }

    fun acceptCoachInvite(notificationId: String, clubId: String, coachId: String) {
        val clubObj = _clubs.value.find { it.id == clubId }
        val clubName = clubObj?.name ?: "Kulüp"
        _users.value = _users.value.map { user ->
            if (user.id == coachId) {
                val updated = user.copy(club = clubName)
                saveUserToFirestore(updated)
                updated
            } else user
        }
        _clubs.value = _clubs.value.map { c ->
            if (c.id == clubId) {
                val coachObj = _users.value.find { it.id == coachId }
                val alreadyExists = c.coaches.any { it.id == coachId }
                val newCoachesList = if (alreadyExists) c.coaches else {
                    c.coaches + ClubCoach(
                        id = coachId,
                        name = coachObj?.name ?: "Antrenör",
                        uefaLicense = "UEFA A",
                        specialization = "Teknik Sorumlu",
                        photoUrl = coachObj?.photoUrl ?: ""
                    )
                }
                val updated = c.copy(coaches = newCoachesList)
                localClubs[clubId] = updated
                saveClubToFirestore(updated)
                updated
            } else c
        }
        markNotificationAsRead(notificationId)
    }

    fun rejectCoachInvite(notificationId: String) {
        markNotificationAsRead(notificationId)
    }

    fun removePlayerFromClub(clubId: String, playerId: String) {
        _players.value = _players.value.map { player ->
            if (player.id == playerId) {
                val updated = player.copy(club = "Kulüpsüz")
                savePlayerToFirestore(updated)
                updated
            } else player
        }
        _clubs.value = _clubs.value.map { club ->
            if (club.id == clubId) {
                val updated = club.copy(players = club.players.filter { it.id != playerId })
                localClubs[clubId] = updated
                saveClubToFirestore(updated)
                updated
            } else club
        }
    }

    fun removeCoachFromClub(clubId: String, coachId: String) {
        _users.value = _users.value.map { user ->
            if (user.id == coachId) {
                val updated = user.copy(club = "")
                saveUserToFirestore(updated)
                updated
            } else user
        }
        _clubs.value = _clubs.value.map { club ->
            if (club.id == clubId) {
                val updated = club.copy(coaches = club.coaches.filter { it.id != coachId })
                localClubs[clubId] = updated
                saveClubToFirestore(updated)
                updated
            } else club
        }
    }

    fun playerLeaveClub(playerId: String) {
        val playerObj = _players.value.find { it.id == playerId } ?: return
        val currentClubName = playerObj.club
        _players.value = _players.value.map { player ->
            if (player.id == playerId) {
                val updated = player.copy(club = "Kulüpsüz")
                savePlayerToFirestore(updated)
                updated
            } else player
        }
        _clubs.value = _clubs.value.map { club ->
            if (club.name == currentClubName || club.id == currentClubName) {
                val updated = club.copy(players = club.players.filter { it.id != playerId })
                localClubs[club.id] = updated
                saveClubToFirestore(updated)
                updated
            } else club
        }
    }

    fun markNotificationAsRead(id: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == id) {
                val updated = it.copy(isRead = true)
                saveNotificationToFirestore(updated)
                updated
            } else it
        }
    }

    fun addUser(user: AppUser) {
        localUsers[user.id] = user
        _users.value = (_users.value.filter { it.id != user.id } + user).toList()
        saveUserToFirestore(user)
    }

    fun deleteUser(userId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        loadDeletedIdsIfNeeded()
        locallyDeletedDocIds.add(userId)
        persistDeletedIds()

        _users.value = _users.value.filter { it.id != userId }
        _rawPlayers.value = _rawPlayers.value.filter { it.id != userId }
        _players.value = _players.value.filter { it.id != userId }
        localPlayers.remove(userId)
        localUsers.remove(userId)

        _rawClubs.value = _rawClubs.value.filter { it.id != userId }
        _clubs.value = _clubs.value.filter { it.id != userId }
        localClubs.remove(userId)

        updateComputedPlayers()
        updateComputedClubs()

        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).delete()
            .addOnSuccessListener {
                db.collection("players").document(userId).delete()
                db.collection("clubs").document(userId).delete()
                
                db.collection("follows").document(userId).delete()
                db.collection("likes").document(userId).delete()
                db.collection("applications").document(userId).delete()
                onSuccess()
            }
            .addOnFailureListener { e ->
                // Do not restore locally, keep them deleted!
                onFailure(e)
            }
    }

    fun toggleBanUser(userId: String) {
        _users.value = _users.value.map {
            if (it.id == userId) {
                val updated = it.copy(isBanned = !it.isBanned)
                saveUserToFirestore(updated)
                updated
            } else it
        }
    }

    fun toggleVerifyUser(userId: String) {
        _users.value = _users.value.map {
            if (it.id == userId) {
                val updated = it.copy(isVerified = !it.isVerified)
                saveUserToFirestore(updated)
                updated
            } else it
        }
    }

    fun changeUserRole(userId: String, newRole: UserRole) {
        _users.value = _users.value.map {
            if (it.id == userId) {
                val updated = it.copy(role = newRole)
                saveUserToFirestore(updated)
                updated
            } else it
        }
    }

    fun addClub(club: Club) {
        localClubs[club.id] = club
        _clubs.value = (_clubs.value.filter { it.id != club.id } + club).toList()
        saveClubToFirestore(club)
    }

    fun approveClub(clubId: String) {
        _clubs.value = _clubs.value.map {
            if (it.id == clubId) {
                val updated = it.copy(licenseStatus = "Onaylandı")
                localClubs[clubId] = updated
                saveClubToFirestore(updated)
                updated
            } else it
        }
    }

    fun featureClub(clubId: String) {
        _clubs.value = _clubs.value.map {
            if (it.id == clubId) {
                val updated = it.copy(clubType = "Öne Çıkan")
                localClubs[clubId] = updated
                saveClubToFirestore(updated)
                updated
            } else it
        }
    }

    fun passivateClub(clubId: String) {
        _clubs.value = _clubs.value.map {
            if (it.id == clubId) {
                val updated = it.copy(licenseStatus = "Pasif")
                localClubs[clubId] = updated
                saveClubToFirestore(updated)
                updated
            } else it
        }
    }

    fun addTournament(tournament: Tournament) {
        _tournaments.value = _tournaments.value + tournament
        saveTournamentToFirestore(tournament)
    }

    fun editTournament(id: String, updated: Tournament) {
        _tournaments.value = _tournaments.value.map {
            if (it.id == id) {
                saveTournamentToFirestore(updated)
                updated
            } else it
        }
    }

    fun deleteTournament(id: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        loadDeletedIdsIfNeeded()
        locallyDeletedDocIds.add(id)
        persistDeletedIds()

        _tournaments.value = _tournaments.value.filter { it.id != id }
        val db = FirebaseFirestore.getInstance()
        db.collection("tournaments").document(id).delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { e ->
                // Do not restore locally, keep it deleted!
                onFailure(e)
            }
    }

    fun resetEcosystemData(currentUserId: String, onSuccess: () -> Unit, onFailure: (Exception) -> Unit) {
        loadDeletedIdsIfNeeded()
        locallyDeletedDocIds.clear()
        persistDeletedIds()

        val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("europe-west3")

        functions.getHttpsCallable("resetEcosystem")
            .call()
            .addOnSuccessListener { taskResult ->
                val resultData = taskResult.data as? Map<*, *>
                val success = resultData?.get("success") as? Boolean ?: false
                val remainingDocuments = (resultData?.get("remainingDocuments") as? Number)?.toInt() ?: -1
                val remainingAuthUsers = (resultData?.get("remainingAuthUsers") as? Number)?.toInt() ?: -1
                val failedDocuments = (resultData?.get("failedDocuments") as? Number)?.toInt() ?: -1
                val failedUsers = (resultData?.get("failedUsers") as? Number)?.toInt() ?: -1

                if (success &&
                    remainingDocuments == 0 &&
                    remainingAuthUsers == 1 &&
                    failedDocuments == 0 &&
                    failedUsers == 0
                ) {
                    // Clear UI cache only after the Cloud Function returns a successful and fully verified response
                    _rawPlayers.value = emptyList()
                    _players.value = emptyList()
                    _follows.value = emptyList()
                    _likes.value = emptyList()
                    _rawClubs.value = emptyList()
                    _clubs.value = emptyList()
                    _chats.value = emptyList()
                    _notifications.value = emptyList()
                    _activityFeed.value = emptyList()
                    _applications.value = emptyList()
                    _users.value = emptyList()
                    _tournaments.value = emptyList()
                    localPlayers.clear()
                    localUsers.clear()
                    localClubs.clear()

                    onSuccess()
                } else {
                    onFailure(
                        IllegalStateException(
                            "Sunucu temizliği tamamlanamadı. " +
                            "Kalan belge: $remainingDocuments, " +
                            "Kalan Auth hesabı: $remainingAuthUsers, " +
                            "Başarısız belge işlemi: $failedDocuments, " +
                            "Başarısız kullanıcı işlemi: $failedUsers"
                        )
                    )
                }
            }
            .addOnFailureListener { e ->
                onFailure(e)
            }
    }

    fun selectTournamentWinner(tournamentId: String, winnerName: String) {
        _tournaments.value = _tournaments.value.map {
            if (it.id == tournamentId) {
                val updated = it.copy(countdown = "Tamamlandı (Kazanan: $winnerName)")
                saveTournamentToFirestore(updated)
                updated
            } else it
        }
    }

    fun addNotification(notification: Notification) {
        _notifications.value = listOf(notification) + _notifications.value
        saveNotificationToFirestore(notification)
    }

    fun addActivityItem(item: ActivityItem) {
        _activityFeed.value = listOf(item) + _activityFeed.value
        saveActivityToFirestore(item)
    }

    // ----------------------------------------------------
    // FIREBASE REAL-TIME SYNC ENGINE
    // ----------------------------------------------------

    private val firebaseListeners = mutableListOf<ListenerRegistration>()
    private var isFirebaseSyncActive = false
    var isFollowsSyncActive = true
    var isLikesSyncActive = true

    fun startFirebaseSync() {
        loadDeletedIdsIfNeeded()
        if (isFirebaseSyncActive) return
        isFirebaseSyncActive = true
        isFollowsSyncActive = true
        isLikesSyncActive = true

        _isPlayersLoaded.value = false

        // Clear local mock data so we don't mix fake data with production data
        _players.value = emptyList()
        _rawPlayers.value = emptyList()
        _follows.value = emptyList()
        _likes.value = emptyList()
        _clubs.value = emptyList()
        _rawClubs.value = emptyList()
        _tournaments.value = emptyList()
        _chats.value = emptyList()
        _notifications.value = emptyList()
        _users.value = emptyList()
        _activityFeed.value = emptyList()
        _applications.value = emptyList()

        // Cancel previous listeners
        firebaseListeners.forEach { it.remove() }
        firebaseListeners.clear()

        val db = FirebaseFirestore.getInstance()

        // Real-time listener for Users
        firebaseListeners.add(db.collection("users").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val dbUsers = it.documents.mapNotNull { doc -> doc.data?.let { data -> mapToAppUser(data, doc.id) } }
                _users.value = dbUsers.filter { !locallyDeletedDocIds.contains(it.id) }
            }
        })

        // Real-time listener for Follows
        firebaseListeners.add(db.collection("follows").addSnapshotListener { snapshot, _ ->
            if (isFollowsSyncActive) {
                snapshot?.let {
                    _follows.value = it.documents.mapNotNull { doc ->
                        val followerId = doc.getString("followerId") ?: ""
                        val followedId = doc.getString("followedId") ?: ""
                        if (followerId.isNotEmpty() && followedId.isNotEmpty()) {
                            FollowItem(followerId, followedId)
                        } else null
                    }.filter { !locallyDeletedDocIds.contains(it.followerId) && !locallyDeletedDocIds.contains(it.followedId) }
                    updateComputedPlayers()
                    updateComputedClubs()
                }
            }
        })

        // Real-time listener for Likes
        firebaseListeners.add(db.collection("likes").addSnapshotListener { snapshot, _ ->
            if (isLikesSyncActive) {
                snapshot?.let {
                    _likes.value = it.documents.mapNotNull { doc ->
                        val likerId = doc.getString("likerId") ?: ""
                        val likedId = doc.getString("likedId") ?: ""
                        if (likerId.isNotEmpty() && likedId.isNotEmpty()) {
                            LikeItem(likerId, likedId)
                        } else null
                    }.filter { !locallyDeletedDocIds.contains(it.likerId) && !locallyDeletedDocIds.contains(it.likedId) }
                    updateComputedPlayers()
                }
            }
        })

        // Real-time listener for Applications
        firebaseListeners.add(db.collection("applications").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                _applications.value = it.documents.mapNotNull { doc ->
                    val id = doc.id
                    val clubId = doc.getString("clubId") ?: ""
                    val playerId = doc.getString("playerId") ?: ""
                    val status = doc.getString("status") ?: "PENDING"
                    if (clubId.isNotEmpty() && playerId.isNotEmpty()) {
                        ClubApplication(id, clubId, playerId, status)
                    } else null
                }.filter { !locallyDeletedDocIds.contains(it.id) && !locallyDeletedDocIds.contains(it.playerId) && !locallyDeletedDocIds.contains(it.clubId) }
                updateComputedClubs()
            }
        })

        // Real-time listener for Players
        firebaseListeners.add(db.collection("players").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val dbPlayers = it.documents.mapNotNull { doc -> doc.data?.let { data -> mapToPlayer(data, doc.id) } }
                _rawPlayers.value = dbPlayers.filter { !locallyDeletedDocIds.contains(it.id) }
                updateComputedPlayers()
                _isPlayersLoaded.value = true
                android.util.Log.d("FutbolcuBul", "PLAYER COUNT : ${dbPlayers.size}")
            }
        })

        // Real-time listener for Clubs
        firebaseListeners.add(db.collection("clubs").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val dbClubs = it.documents.mapNotNull { doc -> doc.data?.let { data -> mapToClub(data, doc.id) } }
                _rawClubs.value = dbClubs.filter { !locallyDeletedDocIds.contains(it.id) }
                updateComputedClubs()
                android.util.Log.d("FutbolcuBul", "CLUB COUNT : ${dbClubs.size}")
            }
        })

        // Real-time listener for Tournaments
        firebaseListeners.add(db.collection("tournaments").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val dbTournaments = it.documents.mapNotNull { doc -> doc.data?.let { data -> mapToTournament(data) } }
                _tournaments.value = dbTournaments.filter { !locallyDeletedDocIds.contains(it.id) }
            }
        })

        // Real-time listener for Chats
        firebaseListeners.add(db.collection("chats").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val dbChats = it.documents.mapNotNull { doc -> doc.data?.let { data -> mapToChat(data) } }
                _chats.value = dbChats.filter { !locallyDeletedDocIds.contains(it.id) }
            }
        })

        // Real-time listener for Notifications
        firebaseListeners.add(db.collection("notifications").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val dbNotifications = it.documents.mapNotNull { doc -> doc.data?.let { data -> mapToNotification(data) } }
                _notifications.value = dbNotifications.filter { !locallyDeletedDocIds.contains(it.id) }
            }
        })

        // Real-time listener for Activities
        firebaseListeners.add(db.collection("activities").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                val dbActivities = it.documents.mapNotNull { doc -> doc.data?.let { data -> mapToActivityItem(data) } }
                _activityFeed.value = dbActivities.filter { !locallyDeletedDocIds.contains(it.id) }
            }
        })
    }

    fun stopFirebaseSync() {
        isFirebaseSyncActive = false
        firebaseListeners.forEach { it.remove() }
        firebaseListeners.clear()
        _isPlayersLoaded.value = false
        
        // Reload mock data to return back to guest/offline view
        loadMockData()
    }

    // ----------------------------------------------------
    // ASYNCHRONOUS FIRESTORE WRITERS
    // ----------------------------------------------------

    fun saveUserToFirestore(user: AppUser) {
        localUsers[user.id] = user
        _users.value = (_users.value.filter { it.id != user.id } + user).toList()
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(user.id).set(appUserToMap(user))
            .addOnSuccessListener {
                when (user.role) {
                    UserRole.COACH -> android.util.Log.d("FutbolcuBul", "COACH SAVED")
                    UserRole.SCOUT -> android.util.Log.d("FutbolcuBul", "SCOUT SAVED")
                    else -> {}
                }
            }
    }

    fun savePlayerToFirestore(player: Player) {
        localPlayers[player.id] = player
        if (isFirebaseSyncActive) {
            _rawPlayers.value = (_rawPlayers.value.filter { it.id != player.id } + player).toList()
            updateComputedPlayers()
        } else {
            _players.value = (_players.value.filter { it.id != player.id } + player).toList()
        }
        val db = FirebaseFirestore.getInstance()
        db.collection("players").document(player.id).set(playerToMap(player))
            .addOnSuccessListener {
                android.util.Log.d("FutbolcuBul", "PLAYER SAVED")
            }
    }

    fun saveClubToFirestore(club: Club) {
        val db = FirebaseFirestore.getInstance()
        db.collection("clubs").document(club.id).set(clubToMap(club))
            .addOnSuccessListener {
                android.util.Log.d("FutbolcuBul", "CLUB SAVED")
            }
    }

    fun saveTournamentToFirestore(tournament: Tournament) {
        val db = FirebaseFirestore.getInstance()
        db.collection("tournaments").document(tournament.id).set(tournamentToMap(tournament))
    }

    fun saveNotificationToFirestore(notification: Notification) {
        val db = FirebaseFirestore.getInstance()
        db.collection("notifications").document(notification.id).set(notificationToMap(notification))
    }

    fun saveActivityToFirestore(activity: ActivityItem) {
        val db = FirebaseFirestore.getInstance()
        db.collection("activities").document(activity.id).set(activityItemToMap(activity))
    }

    fun saveChatToFirestore(chat: Chat) {
        val db = FirebaseFirestore.getInstance()
        db.collection("chats").document(chat.id).set(chatToMap(chat))
    }

    // ----------------------------------------------------
    // PROJECTION SERIALIZERS (Kotlin Object -> safe Firestore Map)
    // ----------------------------------------------------

    private fun appUserToMap(user: AppUser): Map<String, Any?> {
        return mapOf(
            "id" to user.id,
            "name" to user.name,
            "email" to user.email,
            "role" to user.role.name,
            "city" to user.city,
            "photoUrl" to user.photoUrl,
            "isVerified" to user.isVerified,
            "isBanned" to user.isBanned,
            "createdAt" to user.createdAt,
            "services" to user.services,
            "isPremium" to user.isPremium,
            "gallery" to user.gallery,
            "youtubeVideos" to user.youtubeVideos,
            "club" to user.club,
            "bio" to user.bio,
            "instagram" to user.instagram,
            "phone" to user.phone,
            "license" to user.license,
            "achievements" to user.achievements,
            "price" to user.price,
            "duty" to user.duty,
            "stat1" to user.stat1,
            "stat2" to user.stat2,
            "stat3" to user.stat3,
            "stat4" to user.stat4,
            "stat5" to user.stat5,
            "stat6" to user.stat6,
            "isFeatured" to user.isFeatured,
            "featuredAt" to user.featuredAt
        )
    }

    private fun playerToMap(p: Player): Map<String, Any?> {
        return mapOf(
            "id" to p.id,
            "firstName" to p.firstName,
            "lastName" to p.lastName,
            "photoResId" to p.photoResId,
            "photoUrl" to p.photoUrl,
            "age" to p.age,
            "birthDate" to p.birthDate,
            "nationality" to p.nationality,
            "city" to p.city,
            "height" to p.height,
            "weight" to p.weight,
            "preferredFoot" to p.preferredFoot.name,
            "position" to p.position.name,
            "club" to p.club,
            "jerseyNumber" to p.jerseyNumber,
            "bio" to p.bio,
            "instagram" to p.instagram,
            "youtubeUrl" to p.youtubeUrl,
            "selfRating" to p.selfRating,
            "scoutRating" to p.scoutRating,
            "tournamentRating" to p.tournamentRating,
            "marketValue" to p.marketValue,
            "isVerified" to p.isVerified,
            "viewsCount" to p.viewsCount,
            "followsCount" to p.followsCount,
            "followingCount" to p.followingCount,
            "status" to p.status,
            "likesCount" to p.likesCount,
            "likedByMe" to p.likedByMe,
            "followedByMe" to p.followedByMe,
            "badge" to p.badge.name,
            "arenaLevel" to p.arenaLevel,
            "stats" to mapOf(
                "pace" to p.stats.pace,
                "shooting" to p.stats.shooting,
                "passing" to p.stats.passing,
                "dribbling" to p.stats.dribbling,
                "defense" to p.stats.defense,
                "physical" to p.stats.physical
            ),
            "hasLicense" to p.hasLicense,
            "previousClubs" to p.previousClubs,
            "achievements" to p.achievements,
            "secondaryPositions" to p.secondaryPositions.map { it.name },
            "zoom" to p.zoom,
            "offsetX" to p.offsetX,
            "offsetY" to p.offsetY,
            "weakFoot" to p.weakFoot,
            "skillMoves" to p.skillMoves,
            "isPremium" to p.isPremium,
            "gallery" to p.gallery,
            "youtubeVideos" to p.youtubeVideos,
            "profileVisits" to mapOf(
                "today" to p.profileVisits.today,
                "last7Days" to p.profileVisits.last7Days,
                "last30Days" to p.profileVisits.last30Days,
                "total" to p.profileVisits.total
            ),
            "scoutReports" to p.scoutReports.map { r ->
                mapOf(
                    "id" to r.id,
                    "scoutName" to r.scoutName,
                    "scoutBadge" to r.scoutBadge,
                    "technical" to r.technical,
                    "tactical" to r.tactical,
                    "mental" to r.mental,
                    "positioning" to r.positioning,
                    "comment" to r.comment,
                    "date" to r.date
                )
            },
            "communityReviews" to p.communityReviews.map { cr ->
                mapOf(
                    "id" to cr.id,
                    "reviewerName" to cr.reviewerName,
                    "reviewerRole" to cr.reviewerRole,
                    "comment" to cr.comment,
                    "tag" to cr.tag,
                    "date" to cr.date,
                    "likesCount" to cr.likesCount,
                    "likedByMe" to cr.likedByMe,
                    "reviewerBadge" to cr.reviewerBadge.name,
                    "replies" to cr.replies.map { rep ->
                        mapOf(
                            "id" to rep.id,
                            "senderName" to rep.senderName,
                            "senderRole" to rep.senderRole,
                            "senderBadge" to rep.senderBadge.name,
                            "text" to rep.text,
                            "date" to rep.date
                        )
                    }
                )
            }
        )
    }

    private fun clubToMap(c: Club): Map<String, Any?> {
        return mapOf(
            "id" to c.id,
            "name" to c.name,
            "city" to c.city,
            "foundationYear" to c.foundationYear,
            "coverPhotoUrl" to c.coverPhotoUrl,
            "logoUrl" to c.logoUrl,
            "followerCount" to c.followerCount,
            "activeAgeGroups" to c.activeAgeGroups,
            "aboutText" to c.aboutText,
            "trainingFacility" to c.trainingFacility,
            "pitchInfo" to c.pitchInfo,
            "trainingDays" to c.trainingDays,
            "licenseStatus" to c.licenseStatus,
            "acceptedAgeGroups" to c.acceptedAgeGroups,
            "phoneNumber" to c.phoneNumber,
            "whatsappNumber" to c.whatsappNumber,
            "instagramUsername" to c.instagramUsername,
            "websiteUrl" to c.websiteUrl,
            "locationUrl" to c.locationUrl,
            "address" to c.address,
            "clubType" to c.clubType,
            "followedByMe" to c.followedByMe,
            "registrationApplied" to c.registrationApplied,
            "district" to c.district,
            "activeStudentsCount" to c.activeStudentsCount,
            "coachesCount" to c.coachesCount,
            "trophyCount" to c.trophyCount,
            "hasLicense" to c.hasLicense,
            "hasSummerSchool" to c.hasSummerSchool,
            "hasWinterSchool" to c.hasWinterSchool,
            "ageGroups" to c.ageGroups,
            "appliedPlayerIds" to c.appliedPlayerIds,
            "news" to c.news.map { n ->
                mapOf(
                    "id" to n.id,
                    "title" to n.title,
                    "content" to n.content,
                    "date" to n.date,
                    "imageUrl" to n.imageUrl
                )
            },
            "isPremium" to c.isPremium,
            "gallery" to c.gallery,
            "youtubeVideos" to c.youtubeVideos
        )
    }

    private fun tournamentToMap(t: Tournament): Map<String, Any?> {
        return mapOf(
            "id" to t.id,
            "title" to t.title,
            "description" to t.description,
            "countdown" to t.countdown,
            "prize" to t.prize,
            "participantsCount" to t.participantsCount,
            "maxParticipants" to t.maxParticipants,
            "difficulty" to t.difficulty,
            "isApplied" to t.isApplied
        )
    }

    private fun notificationToMap(n: Notification): Map<String, Any?> {
        return mapOf(
            "id" to n.id,
            "title" to n.title,
            "description" to n.description,
            "type" to n.type.name,
            "time" to n.time,
            "isRead" to n.isRead,
            "recipientId" to n.recipientId,
            "senderId" to n.senderId
        )
    }

    private fun activityItemToMap(a: ActivityItem): Map<String, Any?> {
        return mapOf(
            "id" to a.id,
            "userName" to a.userName,
            "userRole" to a.userRole,
            "userBadge" to a.userBadge.name,
            "actionText" to a.actionText,
            "detailText" to a.detailText,
            "timeAgo" to a.timeAgo,
            "likesCount" to a.likesCount,
            "likedByMe" to a.likedByMe
        )
    }

    private fun chatToMap(c: Chat): Map<String, Any?> {
        return mapOf(
            "id" to c.id,
            "otherParticipantName" to c.otherParticipantName,
            "otherParticipantRole" to c.otherParticipantRole,
            "otherParticipantAvatar" to c.otherParticipantAvatar,
            "lastMessage" to c.lastMessage,
            "timestamp" to c.timestamp,
            "unreadCount" to c.unreadCount,
            "messages" to c.messages.map { m ->
                mapOf(
                    "id" to m.id,
                    "senderId" to m.senderId,
                    "text" to m.text,
                    "timestamp" to m.timestamp
                )
            }
        )
    }

    // ----------------------------------------------------
    // DESERIALIZERS (safe document Map -> Kotlin Object)
    // ----------------------------------------------------

    fun mapToAppUser(map: Map<String, Any?>, docId: String = ""): AppUser {
        val finalId = map["id"] as? String ?: docId
        return AppUser(
            id = finalId,
            name = map["name"] as? String ?: map["fullName"] as? String ?: "",
            email = map["email"] as? String ?: "",
            role = try { UserRole.valueOf(map["role"] as? String ?: "PLAYER") } catch (e: Exception) { UserRole.PLAYER },
            city = map["city"] as? String ?: "",
            photoUrl = map["photoUrl"] as? String ?: "",
            isVerified = map["isVerified"] as? Boolean ?: false,
            isBanned = map["isBanned"] as? Boolean ?: false,
            createdAt = map["createdAt"] as? String ?: "2026-07-11",
            services = map["services"] as? String ?: "",
            isPremium = map["isPremium"] as? Boolean ?: false,
            gallery = (map["gallery"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            youtubeVideos = (map["youtubeVideos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            club = map["club"] as? String ?: "",
            bio = map["bio"] as? String ?: "",
            instagram = map["instagram"] as? String ?: "",
            phone = map["phone"] as? String ?: "",
            license = map["license"] as? String ?: "",
            achievements = map["achievements"] as? String ?: "",
            price = map["price"] as? String ?: "",
            duty = map["duty"] as? String ?: "",
            stat1 = (map["stat1"] as? Number)?.toInt() ?: 85,
            stat2 = (map["stat2"] as? Number)?.toInt() ?: 85,
            stat3 = (map["stat3"] as? Number)?.toInt() ?: 85,
            stat4 = (map["stat4"] as? Number)?.toInt() ?: 85,
            stat5 = (map["stat5"] as? Number)?.toInt() ?: 85,
            stat6 = (map["stat6"] as? Number)?.toInt() ?: 85,
            isFeatured = map["isFeatured"] as? Boolean ?: false,
            featuredAt = map["featuredAt"] as? String ?: ""
        )
    }

    private fun mapToPlayer(map: Map<String, Any?>, docId: String = ""): Player {
        val statsMap = map["stats"] as? Map<String, Any?>
        val physicalStats = PhysicalStats(
            pace = (statsMap?.get("pace") as? Number)?.toInt() ?: 75,
            shooting = (statsMap?.get("shooting") as? Number)?.toInt() ?: 70,
            passing = (statsMap?.get("passing") as? Number)?.toInt() ?: 72,
            dribbling = (statsMap?.get("dribbling") as? Number)?.toInt() ?: 76,
            defense = (statsMap?.get("defense") as? Number)?.toInt() ?: 50,
            physical = (statsMap?.get("physical") as? Number)?.toInt() ?: 70
        )
        
        val profileVisitsMap = map["profileVisits"] as? Map<String, Any?>
        val profileVisits = ProfileVisits(
            today = (profileVisitsMap?.get("today") as? Number)?.toInt() ?: 12,
            last7Days = (profileVisitsMap?.get("last7Days") as? Number)?.toInt() ?: 84,
            last30Days = (profileVisitsMap?.get("last30Days") as? Number)?.toInt() ?: 320,
            total = (profileVisitsMap?.get("total") as? Number)?.toInt() ?: 1240
        )

        val scoutReportsList = (map["scoutReports"] as? List<*>) ?: emptyList<Any?>()
        val scoutReports = scoutReportsList.mapNotNull { item ->
            val r = item as? Map<*, *> ?: return@mapNotNull null
            ScoutReport(
                id = r["id"] as? String ?: "",
                scoutName = r["scoutName"] as? String ?: "",
                scoutBadge = r["scoutBadge"] as? String ?: "",
                technical = (r["technical"] as? Number)?.toInt() ?: 0,
                tactical = (r["tactical"] as? Number)?.toInt() ?: 0,
                mental = (r["mental"] as? Number)?.toInt() ?: 0,
                positioning = (r["positioning"] as? Number)?.toInt() ?: 0,
                comment = r["comment"] as? String ?: "",
                date = r["date"] as? String ?: ""
            )
        }

        val communityReviewsList = (map["communityReviews"] as? List<*>) ?: emptyList<Any?>()
        val communityReviews = communityReviewsList.mapNotNull { item ->
            val cr = item as? Map<*, *> ?: return@mapNotNull null
            val repliesList = (cr["replies"] as? List<*>) ?: emptyList<Any?>()
            val replies = repliesList.mapNotNull { repItem ->
                val rep = repItem as? Map<*, *> ?: return@mapNotNull null
                CommentReply(
                    id = rep["id"] as? String ?: "",
                    senderName = rep["senderName"] as? String ?: "",
                    senderRole = rep["senderRole"] as? String ?: "",
                    senderBadge = try { UserBadge.valueOf(rep["senderBadge"] as? String ?: "NONE") } catch (e: Exception) { UserBadge.NONE },
                    text = rep["text"] as? String ?: "",
                    date = rep["date"] as? String ?: ""
                )
            }
            CommunityReview(
                id = cr["id"] as? String ?: "",
                reviewerName = cr["reviewerName"] as? String ?: "",
                reviewerRole = cr["reviewerRole"] as? String ?: "",
                comment = cr["comment"] as? String ?: "",
                tag = cr["tag"] as? String ?: "",
                date = cr["date"] as? String ?: "",
                likesCount = (cr["likesCount"] as? Number)?.toInt() ?: 0,
                likedByMe = cr["likedByMe"] as? Boolean ?: false,
                replies = replies,
                reviewerBadge = try { UserBadge.valueOf(cr["reviewerBadge"] as? String ?: "NONE") } catch (e: Exception) { UserBadge.NONE }
            )
        }

        val finalId = map["id"] as? String ?: docId
        val finalFirstName = map["firstName"] as? String ?: map["name"] as? String ?: ""
        val finalLastName = map["lastName"] as? String ?: ""

        return Player(
            id = finalId,
            firstName = finalFirstName,
            lastName = finalLastName,
            photoResId = (map["photoResId"] as? Number)?.toInt(),
            photoUrl = map["photoUrl"] as? String ?: map["photoBitmapUrl"] as? String,
            age = (map["age"] as? Number)?.toInt() ?: 19,
            birthDate = map["birthDate"] as? String ?: "",
            nationality = map["nationality"] as? String ?: "",
            city = map["city"] as? String ?: "",
            height = (map["height"] as? Number)?.toInt() ?: 180,
            weight = (map["weight"] as? Number)?.toInt() ?: 75,
            preferredFoot = try { PreferredFoot.valueOf(map["preferredFoot"] as? String ?: "RIGHT") } catch (e: Exception) { PreferredFoot.RIGHT },
            position = try { FootballPosition.valueOf(map["position"] as? String ?: "ST") } catch (e: Exception) { FootballPosition.ST },
            club = map["club"] as? String ?: "Serbest",
            jerseyNumber = (map["jerseyNumber"] as? Number)?.toInt() ?: 10,
            bio = map["bio"] as? String ?: "",
            instagram = map["instagram"] as? String ?: "",
            youtubeUrl = map["youtubeUrl"] as? String ?: "",
            selfRating = (map["selfRating"] as? Number)?.toInt() ?: 0,
            scoutRating = (map["scoutRating"] as? Number)?.toInt() ?: 0,
            tournamentRating = (map["tournamentRating"] as? Number)?.toInt() ?: 0,
            marketValue = map["marketValue"] as? String ?: "Belirtilmemiş",
            isVerified = map["isVerified"] as? Boolean ?: false,
            viewsCount = (map["viewsCount"] as? Number)?.toInt() ?: 0,
            followsCount = (map["followsCount"] as? Number)?.toInt() ?: 0,
            followingCount = (map["followingCount"] as? Number)?.toInt() ?: 0,
            status = map["status"] as? String ?: "",
            likesCount = (map["likesCount"] as? Number)?.toInt() ?: 0,
            likedByMe = map["likedByMe"] as? Boolean ?: false,
            followedByMe = map["followedByMe"] as? Boolean ?: false,
            badge = try { UserBadge.valueOf(map["badge"] as? String ?: "NONE") } catch (e: Exception) { UserBadge.NONE },
            arenaLevel = map["arenaLevel"] as? String ?: "",
            profileVisits = profileVisits,
            profileViewers = emptyList(),
            stats = physicalStats,
            scoutReports = scoutReports,
            communityReviews = communityReviews,
            hasLicense = map["hasLicense"] as? Boolean ?: false,
            previousClubs = map["previousClubs"] as? String ?: "",
            achievements = map["achievements"] as? String ?: "",
            secondaryPositions = (map["secondaryPositions"] as? List<*>)?.mapNotNull {
                try { FootballPosition.valueOf(it as String) } catch (e: Exception) { null }
            } ?: emptyList(),
            zoom = (map["zoom"] as? Number)?.toFloat() ?: 1.0f,
            offsetX = (map["offsetX"] as? Number)?.toFloat() ?: 0f,
            offsetY = (map["offsetY"] as? Number)?.toFloat() ?: 0f,
            weakFoot = (map["weakFoot"] as? Number)?.toInt() ?: 3,
            skillMoves = (map["skillMoves"] as? Number)?.toInt() ?: 3,
            isPremium = map["isPremium"] as? Boolean ?: false,
            gallery = (map["gallery"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            youtubeVideos = (map["youtubeVideos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        )
    }

    private fun mapToClub(map: Map<String, Any?>, docId: String = ""): Club {
        val newsList = map["news"] as? List<Map<String, Any?>> ?: emptyList()
        val news = newsList.map { n ->
            ClubNews(
                id = n["id"] as? String ?: "",
                title = n["title"] as? String ?: "",
                content = n["content"] as? String ?: "",
                date = n["date"] as? String ?: "",
                imageUrl = n["imageUrl"] as? String ?: ""
            )
        }
        
        val finalId = map["id"] as? String ?: map["clubId"] as? String ?: docId
        val finalName = map["name"] as? String ?: map["clubName"] as? String ?: ""
        val finalLogoUrl = map["logoUrl"] as? String ?: map["logo"] as? String ?: ""
        val finalCoachesCount = (map["coachesCount"] as? Number)?.toInt() ?: (map["coachCount"] as? Number)?.toInt() ?: 0
        val finalActiveStudentsCount = (map["activeStudentsCount"] as? Number)?.toInt() ?: (map["playerCount"] as? Number)?.toInt() ?: 0
        val finalIsVerified = map["isVerified"] as? Boolean ?: map["verified"] as? Boolean ?: false

        return Club(
            id = finalId,
            name = finalName,
            city = map["city"] as? String ?: "",
            foundationYear = (map["foundationYear"] as? Number)?.toInt() ?: 2010,
            coverPhotoUrl = map["coverPhotoUrl"] as? String ?: "",
            logoUrl = finalLogoUrl,
            followerCount = (map["followerCount"] as? Number)?.toInt() ?: 0,
            activeAgeGroups = map["activeAgeGroups"] as? String ?: "",
            aboutText = map["aboutText"] as? String ?: "",
            trainingFacility = map["trainingFacility"] as? String ?: "",
            pitchInfo = map["pitchInfo"] as? String ?: "",
            trainingDays = map["trainingDays"] as? String ?: "",
            licenseStatus = map["licenseStatus"] as? String ?: "",
            acceptedAgeGroups = map["acceptedAgeGroups"] as? String ?: "",
            phoneNumber = map["phoneNumber"] as? String ?: "",
            whatsappNumber = map["whatsappNumber"] as? String ?: "",
            instagramUsername = map["instagramUsername"] as? String ?: "",
            websiteUrl = map["websiteUrl"] as? String ?: "",
            locationUrl = map["locationUrl"] as? String ?: "",
            address = map["address"] as? String ?: "",
            coaches = emptyList(),
            players = emptyList(),
            achievements = emptyList(),
            news = news,
            clubType = map["clubType"] as? String ?: "Profesyonel Kulüp",
            followedByMe = map["followedByMe"] as? Boolean ?: false,
            registrationApplied = map["registrationApplied"] as? Boolean ?: false,
            district = map["district"] as? String ?: "",
            activeStudentsCount = finalActiveStudentsCount,
            coachesCount = finalCoachesCount,
            trophyCount = (map["trophyCount"] as? Number)?.toInt() ?: 0,
            hasLicense = map["hasLicense"] as? Boolean ?: finalIsVerified,
            hasSummerSchool = map["hasSummerSchool"] as? Boolean ?: false,
            hasWinterSchool = map["hasWinterSchool"] as? Boolean ?: false,
            ageGroups = map["ageGroups"] as? String ?: "",
            appliedPlayerIds = (map["appliedPlayerIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            isPremium = map["isPremium"] as? Boolean ?: false,
            gallery = (map["gallery"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            youtubeVideos = (map["youtubeVideos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
        )
    }

    private fun mapToTournament(map: Map<String, Any?>): Tournament {
        return Tournament(
            id = map["id"] as? String ?: "",
            title = map["title"] as? String ?: "",
            description = map["description"] as? String ?: "",
            countdown = map["countdown"] as? String ?: "",
            prize = map["prize"] as? String ?: "",
            participantsCount = (map["participantsCount"] as? Number)?.toInt() ?: 0,
            maxParticipants = (map["maxParticipants"] as? Number)?.toInt() ?: 32,
            difficulty = map["difficulty"] as? String ?: "Elite",
            isApplied = map["isApplied"] as? Boolean ?: false,
            leaderboard = emptyList()
        )
    }

    private fun mapToNotification(map: Map<String, Any?>): Notification {
        return Notification(
            id = map["id"] as? String ?: "",
            title = map["title"] as? String ?: "",
            description = map["description"] as? String ?: "",
            type = try { NotificationType.valueOf(map["type"] as? String ?: "NEW_TOURNAMENT") } catch (e: Exception) { NotificationType.NEW_TOURNAMENT },
            time = map["time"] as? String ?: "",
            isRead = map["isRead"] as? Boolean ?: false,
            recipientId = map["recipientId"] as? String ?: "",
            senderId = map["senderId"] as? String ?: ""
        )
    }

    private fun mapToActivityItem(map: Map<String, Any?>): ActivityItem {
        return ActivityItem(
            id = map["id"] as? String ?: "",
            userName = map["userName"] as? String ?: "",
            userRole = map["userRole"] as? String ?: "",
            userBadge = UserBadge.valueOf(map["userBadge"] as? String ?: "NONE"),
            actionText = map["actionText"] as? String ?: "",
            detailText = map["detailText"] as? String ?: "",
            timeAgo = map["timeAgo"] as? String ?: "",
            likesCount = (map["likesCount"] as? Number)?.toInt() ?: 0,
            likedByMe = map["likedByMe"] as? Boolean ?: false
        )
    }

    private fun mapToChat(map: Map<String, Any?>): Chat {
        val messagesList = map["messages"] as? List<Map<String, Any?>> ?: emptyList()
        val messages = messagesList.map { m ->
            Message(
                id = m["id"] as? String ?: "",
                senderId = m["senderId"] as? String ?: "",
                text = m["text"] as? String ?: "",
                timestamp = m["timestamp"] as? String ?: ""
            )
        }
        return Chat(
            id = map["id"] as? String ?: "",
            otherParticipantName = map["otherParticipantName"] as? String ?: "",
            otherParticipantRole = map["otherParticipantRole"] as? String ?: "",
            otherParticipantAvatar = (map["otherParticipantAvatar"] as? Number)?.toInt(),
            messages = messages,
            lastMessage = map["lastMessage"] as? String ?: "",
            timestamp = map["timestamp"] as? String ?: "",
            unreadCount = (map["unreadCount"] as? Number)?.toInt() ?: 0
        )
    }
}
