package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data object Loading : AiTaskState<Nothing>
    data class Success<T>(val data: T) : AiTaskState<T>
    data class Error(val message: String) : AiTaskState<Nothing>
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarColorHex: String = "#7C3AED",
    val teachSkills: List<String> = emptyList(),
    val learnSkills: List<String> = emptyList(),
    val skillLevel: String = "Intermediate",
    val availability: String = "Weekday Evenings",
    val xp: Long = 250L,
    val level: Long = 2L,
    val streakDays: Long = 3L,
    val completedSessionsCount: Long = 1L,
    val onboardingCompleted: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MentorPartner(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val headline: String = "",
    val bio: String = "",
    val avatarInitials: String = "",
    val avatarColorHex: String = "#8B5CF6",
    val teachSkill: String = "",
    val teachCategory: String = "Programming",
    val teachLevel: String = "Intermediate",
    val wantSkill: String = "",
    val wantCategory: String = "Design",
    val availability: String = "Weekday Evenings",
    val rating: Double = 4.8,
    val reviewCount: Long = 0L,
    val xp: Long = 0L,
    val connected: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val partnerId: String = "",
    val partnerName: String = "",
    val partnerInitials: String = "",
    val partnerColorHex: String = "#8B5CF6",
    val partnerSkill: String = "",
    val lastMessage: String = "",
    val unreadCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val body: String = "",
    val messageType: String = "text",
    val proposalSkill: String? = null,
    val proposalDate: String? = null,
    val proposalTime: String? = null,
    val proposalStatus: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LearningSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val partnerId: String = "",
    val partnerName: String = "",
    val skillTopic: String = "",
    val sessionRole: String = "learning",
    val scheduledDate: String = "",
    val scheduledTime: String = "",
    val durationMinutes: Long = 45L,
    val status: String = "scheduled",
    val notes: String = "",
    val xpAwarded: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RoadmapMilestone(
    val title: String = "",
    val description: String = "",
    val duration: String = "",
    val exercise: String = "",
    val completed: Boolean = false
)

@Keep
@IgnoreExtraProperties
data class LearningRoadmap(
    @DocumentId val id: String = "",
    val userId: String = "",
    val skillName: String = "",
    val targetLevel: String = "Beginner",
    val summary: String = "",
    val milestones: List<RoadmapMilestone> = emptyList(),
    val completedCount: Long = 0L,
    val totalCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Achievement(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val badgeKey: String = "",
    val xpReward: Long = 100L,
    val unlocked: Boolean = false,
    val progress: Long = 0L,
    val target: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorInitials: String = "",
    val skillTopic: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class PartnerMatch(
    val partner: MentorPartner,
    val matchScore: Int,
    val mutualSwapReason: String
)

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val initials: String,
    val colorHex: String,
    val primarySkill: String,
    val xp: Long,
    val level: Long,
    val streakDays: Long,
    val isCurrentUser: Boolean = false
)

object SkillCatalog {
    val categories = listOf(
        "All",
        "Programming",
        "Design",
        "Music",
        "Languages",
        "Business",
        "Photography",
        "Marketing",
        "Fitness"
    )

    val levels = listOf("All Levels", "Beginner", "Intermediate", "Advanced", "Expert")

    val availabilities = listOf(
        "Any Time",
        "Weekday Evenings",
        "Weekends",
        "Mornings",
        "Flexible"
    )

    val suggestedSkills = listOf(
        "Kotlin & Compose",
        "Python & AI",
        "React & TypeScript",
        "UI/UX Design & Figma",
        "Motion Graphics",
        "Acoustic Guitar",
        "Music Production",
        "Spanish Conversation",
        "Japanese Basics",
        "Product Strategy",
        "Financial Modeling",
        "Portrait Photography",
        "Growth Marketing",
        "Strength & Mobility"
    )

    fun categoryForSkill(skill: String): String {
        val lower = skill.lowercase()
        return when {
            lower.contains("kotlin") || lower.contains("python") || lower.contains("react") ||
                lower.contains("code") || lower.contains("swift") || lower.contains("ai") -> "Programming"
            lower.contains("design") || lower.contains("figma") || lower.contains("ui") ||
                lower.contains("motion") || lower.contains("brand") -> "Design"
            lower.contains("guitar") || lower.contains("piano") || lower.contains("music") ||
                lower.contains("ableton") -> "Music"
            lower.contains("spanish") || lower.contains("japanese") || lower.contains("french") ||
                lower.contains("mandarin") || lower.contains("language") -> "Languages"
            lower.contains("product") || lower.contains("business") || lower.contains("finance") ||
                lower.contains("pitch") -> "Business"
            lower.contains("photo") || lower.contains("lightroom") || lower.contains("camera") -> "Photography"
            lower.contains("market") || lower.contains("seo") || lower.contains("growth") -> "Marketing"
            lower.contains("fitness") || lower.contains("yoga") || lower.contains("strength") ||
                lower.contains("mobility") -> "Fitness"
            else -> "Programming"
        }
    }
}
