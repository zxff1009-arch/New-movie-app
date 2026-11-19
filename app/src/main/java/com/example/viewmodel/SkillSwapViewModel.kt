package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Achievement
import com.example.data.AiAssistantRepository
import com.example.data.AiTaskState
import com.example.data.ChatMessage
import com.example.data.Conversation
import com.example.data.LeaderboardEntry
import com.example.data.LearningRoadmap
import com.example.data.LearningSession
import com.example.data.MentorPartner
import com.example.data.PartnerMatch
import com.example.data.Review
import com.example.data.SkillCatalog
import com.example.data.SkillSwapRepository
import com.example.data.UiState
import com.example.data.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SkillSwapVM"
private const val STOP_TIMEOUT_MILLIS = 5000L

@OptIn(ExperimentalCoroutinesApi::class)
class SkillSwapViewModel(
    private val repository: SkillSwapRepository,
    private val aiRepository: AiAssistantRepository,
    val currentUserId: String,
    initialDisplayName: String
) : ViewModel() {

    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()

    private val _seedingError = MutableStateFlow<String?>(null)
    val seedingError: StateFlow<String?> = _seedingError.asStateFlow()

    // Explore & Match Filter States
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedLevel = MutableStateFlow("All Levels")
    val selectedLevel: StateFlow<String> = _selectedLevel.asStateFlow()

    private val _selectedAvailability = MutableStateFlow("Any Time")
    val selectedAvailability: StateFlow<String> = _selectedAvailability.asStateFlow()

    // Active Chat Selection
    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    // AI States
    private val _roadmapAiState = MutableStateFlow<AiTaskState<String>>(AiTaskState.Idle)
    val roadmapAiState: StateFlow<AiTaskState<String>> = _roadmapAiState.asStateFlow()

    private val _exerciseAiState = MutableStateFlow<AiTaskState<String>>(AiTaskState.Idle)
    val exerciseAiState: StateFlow<AiTaskState<String>> = _exerciseAiState.asStateFlow()

    private val _matchInsightState = MutableStateFlow<AiTaskState<String>>(AiTaskState.Idle)
    val matchInsightState: StateFlow<AiTaskState<String>> = _matchInsightState.asStateFlow()

    private val _actionLoading = MutableStateFlow(false)
    val actionLoading: StateFlow<Boolean> = _actionLoading.asStateFlow()

    init {
        seedWorkspace(initialDisplayName)
    }

    fun retrySeeding(displayName: String) {
        _seedingError.value = null
        seedWorkspace(displayName)
    }

    private fun seedWorkspace(displayName: String) {
        viewModelScope.launch {
            val result = repository.ensureUserSeeded(displayName)
            result.onFailure { err ->
                Log.e(TAG, "Failed to seed user workspace", err)
                _seedingError.value = err.localizedMessage ?: "Failed to initialize SkillSwap workspace."
            }
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val userProfileState: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiStateFlow("profile")

    val partnersState: StateFlow<UiState<List<MentorPartner>>> =
        repository.observePartners(currentUserId).toUiStateFlow("partners")

    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations(currentUserId).toUiStateFlow("conversations")

    val sessionsState: StateFlow<UiState<List<LearningSession>>> =
        repository.observeSessions(currentUserId).toUiStateFlow("sessions")

    val roadmapsState: StateFlow<UiState<List<LearningRoadmap>>> =
        repository.observeRoadmaps(currentUserId).toUiStateFlow("roadmaps")

    val achievementsState: StateFlow<UiState<List<Achievement>>> =
        repository.observeAchievements(currentUserId).toUiStateFlow("achievements")

    val reviewsState: StateFlow<UiState<List<Review>>> =
        repository.observeReviews(currentUserId).toUiStateFlow("reviews")

    val activeMessagesState: StateFlow<UiState<List<ChatMessage>>> =
        _activeConversationId
            .flatMapLatest { convId ->
                if (convId.isNullOrBlank()) {
                    flowOf(emptyList())
                } else {
                    repository.observeMessages(currentUserId, convId)
                }
            }
            .toUiStateFlow("messages")

    // Computed Matches sorted by compatibility score
    val rankedMatches: StateFlow<List<PartnerMatch>> = combine(
        userProfileState,
        partnersState
    ) { profileUi, partnersUi ->
        val profile = (profileUi as? UiState.Success)?.data
        val partners = (partnersUi as? UiState.Success)?.data.orEmpty()
        partners.map { partner -> computePartnerMatch(profile, partner) }
            .sortedByDescending { it.matchScore }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val filteredMatches: StateFlow<List<PartnerMatch>> = combine(
        rankedMatches,
        _searchQuery,
        _selectedCategory,
        _selectedLevel,
        _selectedAvailability
    ) { matches, query, category, level, availability ->
        val q = query.trim().lowercase()
        matches.filter { item ->
            val p = item.partner
            val matchesQuery = q.isEmpty() ||
                p.name.lowercase().contains(q) ||
                p.teachSkill.lowercase().contains(q) ||
                p.wantSkill.lowercase().contains(q) ||
                p.teachCategory.lowercase().contains(q) ||
                p.headline.lowercase().contains(q)
            val matchesCat = category == "All" ||
                p.teachCategory.equals(category, ignoreCase = true) ||
                p.wantCategory.equals(category, ignoreCase = true)
            val matchesLevel = level == "All Levels" ||
                p.teachLevel.equals(level, ignoreCase = true)
            val matchesAvail = availability == "Any Time" ||
                p.availability.equals(availability, ignoreCase = true) ||
                p.availability.equals("Flexible", ignoreCase = true)
            matchesQuery && matchesCat && matchesLevel && matchesAvail
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    val leaderboard: StateFlow<List<LeaderboardEntry>> = combine(
        userProfileState,
        partnersState
    ) { profileUi, partnersUi ->
        val profile = (profileUi as? UiState.Success)?.data
        val partners = (partnersUi as? UiState.Success)?.data.orEmpty()
        val combined = mutableListOf<LeaderboardEntry>()
        if (profile != null) {
            val initials = profile.displayName
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
                .ifBlank { "YOU" }
            combined.add(
                LeaderboardEntry(
                    rank = 0,
                    name = "${profile.displayName} (You)",
                    initials = initials,
                    colorHex = profile.avatarColorHex,
                    primarySkill = profile.teachSkills.firstOrNull() ?: "Skill Explorer",
                    xp = profile.xp,
                    level = profile.level,
                    streakDays = profile.streakDays,
                    isCurrentUser = true
                )
            )
        }
        partners.forEach { p ->
            combined.add(
                LeaderboardEntry(
                    rank = 0,
                    name = p.name,
                    initials = p.avatarInitials,
                    colorHex = p.avatarColorHex,
                    primarySkill = p.teachSkill,
                    xp = p.xp,
                    level = (p.xp / 200L) + 1L,
                    streakDays = (p.reviewCount / 3L).coerceAtLeast(2L),
                    isCurrentUser = false
                )
            )
        }
        combined.sortedByDescending { it.xp }
            .mapIndexed { idx, entry -> entry.copy(rank = idx + 1) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    private fun computePartnerMatch(profile: UserProfile?, partner: MentorPartner): PartnerMatch {
        if (profile == null) {
            return PartnerMatch(
                partner = partner,
                matchScore = 78,
                mutualSwapReason = "Popular ${partner.teachCategory} mentor"
            )
        }
        var score = 56
        val reasons = mutableListOf<String>()

        val userLearnsMatch = profile.learnSkills.any { learn ->
            partner.teachSkill.contains(learn, ignoreCase = true) ||
                learn.contains(partner.teachSkill, ignoreCase = true) ||
                SkillCatalog.categoryForSkill(learn) == partner.teachCategory
        }
        if (userLearnsMatch) {
            score += 22
            reasons.add("Teaches ${partner.teachSkill} you want")
        }

        val userTeachesMatch = profile.teachSkills.any { teach ->
            partner.wantSkill.contains(teach, ignoreCase = true) ||
                teach.contains(partner.wantSkill, ignoreCase = true) ||
                SkillCatalog.categoryForSkill(teach) == partner.wantCategory
        }
        if (userTeachesMatch) {
            score += 16
            reasons.add("Wants ${partner.wantSkill} you know")
        }

        if (profile.availability.equals(partner.availability, ignoreCase = true) ||
            partner.availability.equals("Flexible", ignoreCase = true)
        ) {
            score += 4
        }

        if (partner.rating >= 4.8) score += 1
        val clamped = score.coerceIn(55, 99)
        val reason = when {
            reasons.size == 2 -> "Mutual swap: ${partner.teachSkill} <-> ${partner.wantSkill}"
            reasons.size == 1 -> reasons.first()
            else -> "${partner.teachCategory} specialist • ${partner.availability}"
        }
        return PartnerMatch(partner = partner, matchScore = clamped, mutualSwapReason = reason)
    }

    // --- FILTER MUTATIONS ---

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedLevel(level: String) {
        _selectedLevel.value = level
    }

    fun setSelectedAvailability(availability: String) {
        _selectedAvailability.value = availability
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = "All"
        _selectedLevel.value = "All Levels"
        _selectedAvailability.value = "Any Time"
    }

    // --- PROFILE & ONBOARDING ---

    fun saveProfileChanges(
        displayName: String,
        bio: String,
        teachSkills: List<String>,
        learnSkills: List<String>,
        skillLevel: String,
        availability: String,
        onboardingCompleted: Boolean = true,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _actionLoading.value = true
            val result = repository.updateProfile(
                displayName = displayName,
                bio = bio,
                teachSkills = teachSkills,
                learnSkills = learnSkills,
                skillLevel = skillLevel,
                availability = availability,
                onboardingCompleted = onboardingCompleted
            )
            _actionLoading.value = false
            result.onSuccess {
                _toastEvents.tryEmit("Profile & skills updated!")
                onComplete()
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun toggleQuickSkill(isTeach: Boolean, skill: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val currentList = if (isTeach) profile.teachSkills else profile.learnSkills
        val updated = if (currentList.contains(skill)) {
            currentList - skill
        } else {
            (currentList + skill).take(20)
        }
        viewModelScope.launch {
            repository.updateProfile(
                displayName = profile.displayName,
                bio = profile.bio,
                teachSkills = if (isTeach) updated else profile.teachSkills,
                learnSkills = if (isTeach) profile.learnSkills else updated,
                skillLevel = profile.skillLevel,
                availability = profile.availability,
                onboardingCompleted = profile.onboardingCompleted
            ).onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Could not update skills")
            }
        }
    }

    // --- CONNECTIONS & CHAT ---

    fun togglePartnerConnection(partner: MentorPartner) {
        viewModelScope.launch {
            val next = !partner.connected
            repository.setPartnerConnected(partner, next)
                .onSuccess {
                    _toastEvents.tryEmit(
                        if (next) "Connected with ${partner.name}!" else "Disconnected from ${partner.name}"
                    )
                }
                .onFailure { err ->
                    _toastEvents.tryEmit(err.localizedMessage ?: "Connection update failed")
                }
        }
    }

    fun openConversation(conversationId: String) {
        _activeConversationId.value = conversationId
        viewModelScope.launch {
            repository.markConversationRead(conversationId)
        }
    }

    fun closeConversation() {
        _activeConversationId.value = null
    }

    fun startChatWithPartner(partner: MentorPartner, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            _actionLoading.value = true
            if (!partner.connected) {
                repository.setPartnerConnected(partner, true)
            }
            val result = repository.ensureConversation(partner)
            _actionLoading.value = false
            result.onSuccess { convId ->
                _activeConversationId.value = convId
                onOpened(convId)
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Could not open chat")
            }
        }
    }

    fun sendChatMessage(conversationId: String, body: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val senderName = profile?.displayName ?: "You"
        viewModelScope.launch {
            repository.sendMessage(conversationId, senderName, body)
                .onFailure { err ->
                    _toastEvents.tryEmit(err.localizedMessage ?: "Failed to send message")
                }
        }
    }

    fun sendSessionProposal(
        conversationId: String,
        skill: String,
        date: String,
        time: String,
        note: String
    ) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val senderName = profile?.displayName ?: "You"
        viewModelScope.launch {
            repository.sendSessionProposal(conversationId, senderName, skill, date, time, note)
                .onSuccess {
                    _toastEvents.tryEmit("Session proposal sent!")
                }
                .onFailure { err ->
                    _toastEvents.tryEmit(err.localizedMessage ?: "Failed to send proposal")
                }
        }
    }

    fun respondToProposal(conversation: Conversation, message: ChatMessage, accept: Boolean) {
        viewModelScope.launch {
            repository.respondToProposal(conversation, message, accept)
                .onSuccess {
                    _toastEvents.tryEmit(
                        if (accept) "Proposal accepted & added to Sessions!" else "Proposal declined"
                    )
                }
                .onFailure { err ->
                    _toastEvents.tryEmit(err.localizedMessage ?: "Failed to update proposal")
                }
        }
    }

    // --- LEARNING SESSIONS ---

    fun scheduleSession(
        partner: MentorPartner,
        skillTopic: String,
        sessionRole: String,
        scheduledDate: String,
        scheduledTime: String,
        durationMinutes: Long,
        notes: String
    ) {
        viewModelScope.launch {
            _actionLoading.value = true
            val res = repository.scheduleSession(
                partnerId = partner.id,
                partnerName = partner.name,
                skillTopic = skillTopic,
                sessionRole = sessionRole,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                durationMinutes = durationMinutes,
                notes = notes
            )
            _actionLoading.value = false
            res.onSuccess {
                _toastEvents.tryEmit("Session scheduled with ${partner.name}!")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Failed to schedule session")
            }
        }
    }

    fun updateSessionNotes(sessionId: String, notes: String) {
        viewModelScope.launch {
            repository.updateSessionNotes(sessionId, notes)
                .onSuccess { _toastEvents.tryEmit("Session notes saved") }
                .onFailure { err -> _toastEvents.tryEmit(err.localizedMessage ?: "Failed to save notes") }
        }
    }

    fun completeSession(session: LearningSession, notes: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data ?: return
        val achievements = (achievementsState.value as? UiState.Success)?.data.orEmpty()
        viewModelScope.launch {
            _actionLoading.value = true
            val res = repository.completeSession(session, profile, notes, achievements)
            _actionLoading.value = false
            res.onSuccess { xp ->
                _toastEvents.tryEmit("Session completed! +$xp XP & streak extended!")
            }.onFailure { err ->
                _toastEvents.tryEmit(err.localizedMessage ?: "Could not complete session")
            }
        }
    }

    fun cancelSession(sessionId: String) {
        viewModelScope.launch {
            repository.cancelSession(sessionId)
                .onSuccess { _toastEvents.tryEmit("Session cancelled") }
                .onFailure { err -> _toastEvents.tryEmit(err.localizedMessage ?: "Could not cancel session") }
        }
    }

    // --- AI LEARNING ASSISTANT ---

    fun generateRoadmap(skillName: String, targetLevel: String) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        val teachSkills = profile?.teachSkills.orEmpty()
        viewModelScope.launch {
            _roadmapAiState.value = AiTaskState.Loading
            val generated = aiRepository.generateRoadmap(skillName, targetLevel, teachSkills)
            generated.onSuccess { roadmap ->
                repository.saveRoadmap(roadmap)
                    .onSuccess {
                        _roadmapAiState.value = AiTaskState.Success("Generated roadmap for $skillName!")
                        _toastEvents.tryEmit("AI Roadmap saved for $skillName!")
                    }
                    .onFailure { err ->
                        _roadmapAiState.value = AiTaskState.Error(err.localizedMessage ?: "Failed to save roadmap")
                    }
            }.onFailure { err ->
                _roadmapAiState.value = AiTaskState.Error(err.localizedMessage ?: "AI generation failed")
            }
        }
    }

    fun toggleRoadmapMilestone(roadmap: LearningRoadmap, index: Int) {
        viewModelScope.launch {
            repository.toggleRoadmapMilestone(roadmap, index)
                .onFailure { err ->
                    _toastEvents.tryEmit(err.localizedMessage ?: "Could not update milestone")
                }
        }
    }

    fun deleteRoadmap(roadmapId: String) {
        viewModelScope.launch {
            repository.deleteRoadmap(roadmapId)
                .onSuccess { _toastEvents.tryEmit("Roadmap removed") }
                .onFailure { err -> _toastEvents.tryEmit(err.localizedMessage ?: "Failed to delete roadmap") }
        }
    }

    fun generatePracticeExercise(skillName: String, milestoneTitle: String) {
        viewModelScope.launch {
            _exerciseAiState.value = AiTaskState.Loading
            aiRepository.generatePracticeExercise(skillName, milestoneTitle)
                .onSuccess { text ->
                    _exerciseAiState.value = AiTaskState.Success(text)
                }
                .onFailure { err ->
                    _exerciseAiState.value = AiTaskState.Error(err.localizedMessage ?: "Failed to create exercise")
                }
        }
    }

    fun clearExerciseState() {
        _exerciseAiState.value = AiTaskState.Idle
    }

    fun requestMatchInsight(partnerMatch: PartnerMatch) {
        val profile = (userProfileState.value as? UiState.Success)?.data
        viewModelScope.launch {
            _matchInsightState.value = AiTaskState.Loading
            aiRepository.generateMatchInsight(
                userTeachSkills = profile?.teachSkills.orEmpty(),
                userLearnSkills = profile?.learnSkills.orEmpty(),
                partner = partnerMatch.partner,
                matchScore = partnerMatch.matchScore
            ).onSuccess { insight ->
                _matchInsightState.value = AiTaskState.Success(insight)
            }.onFailure { err ->
                _matchInsightState.value = AiTaskState.Error(err.localizedMessage ?: "Could not generate insight")
            }
        }
    }

    fun clearMatchInsight() {
        _matchInsightState.value = AiTaskState.Idle
    }
}
