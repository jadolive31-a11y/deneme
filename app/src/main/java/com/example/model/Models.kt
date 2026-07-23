package com.example.model

import java.io.Serializable

enum class UserRole {
    PLAYER, SCOUT, COACH, CLUB, MEDIA, ADMIN, STORE, PITCH, ORGANIZER
}

enum class PreferredFoot {
    RIGHT, LEFT, BOTH
}

enum class FootballPosition(val fullName: String, val shortName: String) {
    GK("Goalkeeper", "GK"),
    RB("Right Back", "RB"),
    LB("Left Back", "LB"),
    CB("Center Back", "CB"),
    DM("Defensive Midfielder", "DM"),
    CM("Central Midfielder", "CM"),
    AM("Attacking Midfielder", "AM"),
    RW("Right Wing", "RW"),
    LW("Left Wing", "LW"),
    SS("Second Striker", "SS"),
    ST("Striker", "ST")
}

data class PhysicalStats(
    val pace: Int,
    val shooting: Int,
    val passing: Int,
    val dribbling: Int,
    val defense: Int,
    val physical: Int
) : Serializable

enum class UserBadge(val displayName: String, val colorHex: String) {
    NONE("", ""),
    SCOUT("Resmi Scout", "#00FF66"),          // Neon Green
    VERIFIED_PLAYER("Doğrulanmış Oyuncu", "#00E5FF"), // Neon Cyan
    CLUB_REPRESENTATIVE("Kulüp Yetkilisi", "#D500F9"), // Neon Purple
    NATIONAL_SCOUT("Milli Takım Scoutu", "#FFD600")    // Neon Yellow
}

data class ProfileVisits(
    val today: Int = 12,
    val last7Days: Int = 84,
    val last30Days: Int = 320,
    val total: Int = 1240
) : Serializable

data class ProfileViewer(
    val id: String,
    val name: String,
    val role: String, // "Scout", "Oyuncu", "Kulüp Yetkilisi" etc.
    val badge: UserBadge = UserBadge.NONE,
    val timeAgo: String,
    val timestamp: Long
) : Serializable

data class Player(
    val id: String,
    val firstName: String,
    val lastName: String,
    val photoResId: Int? = null,
    val photoUrl: String? = null, // For custom wizard photo
    val age: Int,
    val birthDate: String,
    val nationality: String,
    val city: String,
    val height: Int, // in cm
    val weight: Int, // in kg
    val preferredFoot: PreferredFoot,
    val position: FootballPosition,
    val club: String,
    val jerseyNumber: Int,
    val bio: String,
    val instagram: String = "",
    val youtubeUrl: String = "",
    val selfRating: Int = 0,
    val scoutRating: Int = 0, // Calculated average of scout reviews
    val tournamentRating: Int = 0, // Automatically calculated
    val marketValue: String = "Belirtilmemiş",
    val isVerified: Boolean = false,
    val viewsCount: Int = 0,
    val followsCount: Int = 0, // represented as followers count
    val followingCount: Int = 0,
    val status: String = "", // e.g. "Hazırım.", "Yeni takım arıyorum."
    val likesCount: Int = 0,
    val likedByMe: Boolean = false,
    val followedByMe: Boolean = false,
    val badge: UserBadge = UserBadge.NONE,
    val arenaLevel: String = "",
    val profileVisits: ProfileVisits = ProfileVisits(),
    val profileViewers: List<ProfileViewer> = emptyList(),
    val stats: PhysicalStats = PhysicalStats(0, 0, 0, 0, 0, 0),
    val scoutReports: List<ScoutReport> = emptyList(),
    val tournamentHistory: List<TournamentHistoryEntry> = emptyList(),
    val hasLicense: Boolean = false,
    val previousClubs: String = "",
    val achievements: String = "",
    val communityReviews: List<CommunityReview> = emptyList(),
    val secondaryPositions: List<FootballPosition> = emptyList(),
    val zoom: Float = 1.0f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val weakFoot: Int = 0,
    val skillMoves: Int = 0,
    val isPremium: Boolean = false,
    val gallery: List<String> = emptyList(),
    val youtubeVideos: List<String> = emptyList()
) : Serializable {
    val fullName: String get() = "$firstName $lastName"
    val overallRating: Int get() {
        val ratings = mutableListOf<Int>()
        if (selfRating > 0) ratings.add(selfRating)
        if (scoutRating > 0) ratings.add(scoutRating)
        if (tournamentRating > 0) ratings.add(tournamentRating)
        return if (ratings.isEmpty()) 75 else ratings.average().toInt()
    }
}

data class CommentReply(
    val id: String,
    val senderName: String,
    val senderRole: String,
    val senderBadge: UserBadge = UserBadge.NONE,
    val text: String,
    val date: String
) : Serializable

data class CommunityReview(
    val id: String,
    val reviewerName: String,
    val reviewerRole: String, // e.g., "Gözlemci", "Kulüp", "Futbolcu", "Ziyaretçi"
    val comment: String,
    val tag: String, // e.g., "Orta Sahanın Beyni", "Hız Canavarı", "Bitirici", "Pas Ustası", "Fizik Gücü", "Gelecek Vaat Ediyor"
    val date: String,
    val likesCount: Int = 0,
    val likedByMe: Boolean = false,
    val replies: List<CommentReply> = emptyList(),
    val reviewerBadge: UserBadge = UserBadge.NONE
) : Serializable

data class ScoutReport(
    val id: String,
    val scoutName: String,
    val scoutBadge: String, // e.g. "UEFA Pro", "EPL Certified"
    val technical: Int,
    val tactical: Int,
    val mental: Int,
    val positioning: Int,
    val comment: String,
    val date: String
) : Serializable {
    val averageRating: Int get() = (technical + tactical + mental + positioning) / 4
}

data class TournamentHistoryEntry(
    val id: String,
    val tournamentTitle: String,
    val date: String,
    val result: String, // e.g. "Winner", "Finalist", "Semi-Finals"
    val matchRating: Int
) : Serializable

data class Tournament(
    val id: String,
    val title: String,
    val description: String,
    val countdown: String, // e.g. "2d 14h"
    val prize: String, // e.g. "€2,500 + Club Trial"
    val participantsCount: Int,
    val maxParticipants: Int,
    val difficulty: String, // e.g. "Pro", "Amateur", "Elite"
    val isApplied: Boolean = false,
    val leaderboard: List<LeaderboardUser> = emptyList()
) : Serializable

data class LeaderboardUser(
    val rank: Int,
    val name: String,
    val rating: Int,
    val matchesPlayed: Int,
    val winRate: String,
    val position: String
) : Serializable

data class Message(
    val id: String,
    val senderId: String, // "me" or other
    val text: String,
    val timestamp: String
) : Serializable

data class Chat(
    val id: String,
    val otherParticipantName: String,
    val otherParticipantRole: String, // Scout, Club, Player
    val otherParticipantAvatar: Int? = null,
    val messages: List<Message> = emptyList(),
    val lastMessage: String = "",
    val timestamp: String = "",
    val unreadCount: Int = 0
) : Serializable

enum class NotificationType {
    SCOUT_VIEW, SCOUT_RATE, CLUB_FOLLOW, TOURNAMENT_INVITE, APP_APPROVED,
    USER_FOLLOW, PROFILE_COMMENT, COMMENT_LIKE, NEW_MESSAGE, TOURNAMENT_START, NEW_TOURNAMENT,
    CLUB_PLAYER_INVITE, CLUB_COACH_INVITE
}

data class Notification(
    val id: String,
    val title: String,
    val description: String,
    val type: NotificationType,
    val time: String,
    val isRead: Boolean = false,
    val recipientId: String = "",
    val senderId: String = ""
) : Serializable

data class ActivityItem(
    val id: String,
    val userName: String,
    val userRole: String,
    val userBadge: UserBadge = UserBadge.NONE,
    val actionText: String, // e.g., "turnuvaya katıldı", "yeni video yükledi", "scout puanı aldı"
    val detailText: String, // e.g., "Ankara Neon Challenge", "Süper Goller videosu", "+5 Scout Puanı"
    val timeAgo: String,
    val likesCount: Int = 0,
    val likedByMe: Boolean = false
) : Serializable

data class ClubCoach(
    val id: String,
    val name: String,
    val uefaLicense: String,
    val specialization: String,
    val photoUrl: String
) : Serializable

data class ClubPlayer(
    val id: String,
    val name: String,
    val position: String,
    val age: Int,
    val overallRating: Int,
    val photoUrl: String
) : Serializable

data class ClubAchievement(
    val title: String,
    val description: String,
    val count: Int,
    val icon: String
) : Serializable

data class ClubNews(
    val id: String,
    val title: String,
    val content: String,
    val date: String,
    val imageUrl: String
) : Serializable

data class Club(
    val id: String,
    val name: String,
    val city: String,
    val foundationYear: Int,
    val coverPhotoUrl: String,
    val logoUrl: String,
    val followerCount: Int,
    val activeAgeGroups: String,
    val aboutText: String,
    val trainingFacility: String,
    val pitchInfo: String,
    val trainingDays: String,
    val licenseStatus: String,
    val acceptedAgeGroups: String,
    val phoneNumber: String,
    val whatsappNumber: String,
    val instagramUsername: String,
    val websiteUrl: String,
    val locationUrl: String,
    val address: String,
    val coaches: List<ClubCoach>,
    val players: List<ClubPlayer>,
    val achievements: List<ClubAchievement>,
    val news: List<ClubNews>,
    val clubType: String = "Profesyonel Kulüp",
    val followedByMe: Boolean = false,
    val registrationApplied: Boolean = false,
    val district: String = "",
    val activeStudentsCount: Int = 0,
    val coachesCount: Int = 0,
    val trophyCount: Int = 0,
    val hasLicense: Boolean = false,
    val hasSummerSchool: Boolean = false,
    val hasWinterSchool: Boolean = false,
    val ageGroups: String = "",
    val appliedPlayerIds: List<String> = emptyList(),
    val isPremium: Boolean = false,
    val gallery: List<String> = emptyList(),
    val youtubeVideos: List<String> = emptyList()
) : Serializable

data class AppUser(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val city: String,
    val photoUrl: String,
    val isVerified: Boolean = false,
    val isBanned: Boolean = false,
    val createdAt: String = "2026-07-11",
    val services: String = "",
    val isPremium: Boolean = false,
    val gallery: List<String> = emptyList(),
    val youtubeVideos: List<String> = emptyList(),
    val club: String = "",
    val bio: String = "",
    val instagram: String = "",
    val phone: String = "",
    val license: String = "",
    val achievements: String = "",
    val price: String = "",
    val duty: String = "",
    val stat1: Int = 85,
    val stat2: Int = 85,
    val stat3: Int = 85,
    val stat4: Int = 85,
    val stat5: Int = 85,
    val stat6: Int = 85,
    val isFeatured: Boolean = false,
    val featuredAt: String = ""
) : Serializable

data class ClubApplication(
    val id: String,
    val clubId: String,
    val playerId: String,
    val status: String = "PENDING"
) : Serializable

data class StoreItem(
    val name: String,
    val price: String,
    val imageUrl: String
) : java.io.Serializable

data class ExploreItem(
    val id: String,
    val category: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val priceOrReward: String,
    val buttonText: String,
    val rating: Double = 4.8,
    val location: String = "Kayseri",
    val phone: String = "",
    val instagram: String = "",
    val whatsapp: String = "",
    val code: String = "",
    val details: String = "",
    val itemsList: List<StoreItem> = emptyList(),
    val creatorId: String = ""
) : java.io.Serializable

data class GoalCandidate(
    val id: String,
    val playerName: String,
    val playerPosition: String,
    val playerClub: String,
    val playerPhotoUrl: String?,
    val goalDescription: String,
    val videoUrl: String, // Youtube URL or descriptive title
    val voteCount: Int,
    val weekNumber: Int = 28,
    val hasVoted: Boolean = false,
    val submitterEmail: String = ""
) : java.io.Serializable


