package com.example.ui

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.AiAssistantRepository
import com.example.data.MentorPartner
import com.example.data.PartnerMatch
import com.example.data.SkillSwapRepository
import com.example.data.UiState
import com.example.ui.components.ErrorStateBanner
import com.example.ui.components.LoadingStateView
import com.example.ui.components.SkillSwapBackground
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MatchesScreen
import com.example.ui.screens.MessagesScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PartnerDetailDialog
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SessionsScreen
import com.example.ui.theme.MidnightSurface
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.VioletPrimary
import com.example.viewmodel.SkillSwapViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("route_home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    EXPLORE("route_explore", "Explore", Icons.Rounded.Explore, Icons.Outlined.Explore),
    MATCHES("route_matches", "Matches", Icons.Rounded.SwapHoriz, Icons.Outlined.SwapHoriz),
    MESSAGES("route_messages", "Messages", Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    PROFILE("route_profile", "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
}

enum class SubScreenRoute {
    NONE,
    SESSIONS,
    AI_ASSISTANT
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = { /* AuthStateListener drives navigation cleanly */ })
    } else {
        key(user.uid) {
            AuthenticatedSkillSwapApp(
                currentUserId = user.uid,
                displayNameHint = user.displayName ?: "SkillSwap Explorer"
            )
        }
    }
}

@Composable
private fun AuthenticatedSkillSwapApp(
    currentUserId: String,
    displayNameHint: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: SkillSwapViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SkillSwapViewModel(
                    repository = SkillSwapRepository(db),
                    aiRepository = AiAssistantRepository(),
                    currentUserId = currentUserId,
                    initialDisplayName = displayNameHint
                )
            }
        }
    )

    val profileState by viewModel.userProfileState.collectAsStateWithLifecycle()
    val partnersState by viewModel.partnersState.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val sessionsState by viewModel.sessionsState.collectAsStateWithLifecycle()
    val roadmapsState by viewModel.roadmapsState.collectAsStateWithLifecycle()
    val achievementsState by viewModel.achievementsState.collectAsStateWithLifecycle()
    val reviewsState by viewModel.reviewsState.collectAsStateWithLifecycle()
    val messagesState by viewModel.activeMessagesState.collectAsStateWithLifecycle()

    val rankedMatches by viewModel.rankedMatches.collectAsStateWithLifecycle()
    val filteredMatches by viewModel.filteredMatches.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedLevel by viewModel.selectedLevel.collectAsStateWithLifecycle()
    val selectedAvailability by viewModel.selectedAvailability.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val roadmapAiState by viewModel.roadmapAiState.collectAsStateWithLifecycle()
    val exerciseAiState by viewModel.exerciseAiState.collectAsStateWithLifecycle()
    val matchInsightState by viewModel.matchInsightState.collectAsStateWithLifecycle()
    val actionLoading by viewModel.actionLoading.collectAsStateWithLifecycle()
    val seedingError by viewModel.seedingError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.toastEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    var currentDest by remember { mutableStateOf(MainDestination.HOME) }
    var subScreen by remember { mutableStateOf(SubScreenRoute.NONE) }
    var profileTabOverride by remember { mutableIntStateOf(0) }
    var selectedPartnerDialog by remember { mutableStateOf<PartnerMatch?>(null) }
    var sessionPreselectedPartner by remember { mutableStateOf<MentorPartner?>(null) }

    fun performSignOut() {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w("Auth", "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                }
            }
        }
    }

    if (seedingError != null) {
        SkillSwapBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ErrorStateBanner(
                    message = seedingError!!,
                    onRetry = { viewModel.retrySeeding(displayNameHint) }
                )
            }
        }
        return
    }

    when (val profUi = profileState) {
        is UiState.Loading -> {
            SkillSwapBackground {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingStateView("Setting up your SkillSwap workspace...")
                }
            }
        }
        is UiState.Error -> {
            SkillSwapBackground {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorStateBanner(
                        message = profUi.message,
                        onRetry = { viewModel.retrySeeding(displayNameHint) }
                    )
                }
            }
        }
        is UiState.Success -> {
            val profile = profUi.data
            if (profile == null) {
                SkillSwapBackground {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingStateView("Preparing profile...")
                    }
                }
            } else if (!profile.onboardingCompleted) {
                OnboardingScreen(
                    profile = profile,
                    isSaving = actionLoading,
                    onFinishOnboarding = { name, bio, teach, learn, level, avail ->
                        viewModel.saveProfileChanges(
                            displayName = name,
                            bio = bio,
                            teachSkills = teach,
                            learnSkills = learn,
                            skillLevel = level,
                            availability = avail,
                            onboardingCompleted = true
                        )
                    }
                )
            } else {
                val partners = (partnersState as? UiState.Success)?.data.orEmpty()
                val conversations = (conversationsState as? UiState.Success)?.data.orEmpty()
                val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
                val roadmaps = (roadmapsState as? UiState.Success)?.data.orEmpty()
                val achievements = (achievementsState as? UiState.Success)?.data.orEmpty()
                val reviews = (reviewsState as? UiState.Success)?.data.orEmpty()
                val totalUnread = conversations.sumOf { it.unreadCount }.toInt()

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val useRail = maxWidth >= 680.dp

                    Scaffold(
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets.safeDrawing,
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        bottomBar = {
                            if (!useRail && subScreen == SubScreenRoute.NONE) {
                                NavigationBar(
                                    containerColor = MidnightSurface.copy(alpha = 0.96f),
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    MainDestination.entries.forEach { dest ->
                                        val selected = currentDest == dest
                                        NavigationBarItem(
                                            selected = selected,
                                            onClick = {
                                                currentDest = dest
                                                if (dest == MainDestination.PROFILE) profileTabOverride = 0
                                            },
                                            icon = {
                                                val iconVec = if (selected) dest.selectedIcon else dest.unselectedIcon
                                                if (dest == MainDestination.MESSAGES && totalUnread > 0) {
                                                    BadgedBox(badge = { Badge { Text(totalUnread.toString()) } }) {
                                                        Icon(iconVec, contentDescription = dest.label)
                                                    }
                                                } else {
                                                    Icon(iconVec, contentDescription = dest.label)
                                                }
                                            },
                                            label = { Text(dest.label) },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = TextPrimary,
                                                selectedTextColor = SoftLavender,
                                                indicatorColor = VioletPrimary.copy(alpha = 0.28f),
                                                unselectedIconColor = TextMuted,
                                                unselectedTextColor = TextMuted
                                            ),
                                            modifier = Modifier.testTag("nav_${dest.route}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        SkillSwapBackground {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                if (useRail && subScreen == SubScreenRoute.NONE) {
                                    NavigationRail(
                                        containerColor = MidnightSurface.copy(alpha = 0.94f),
                                        modifier = Modifier.statusBarsPadding()
                                    ) {
                                        MainDestination.entries.forEach { dest ->
                                            val selected = currentDest == dest
                                            NavigationRailItem(
                                                selected = selected,
                                                onClick = {
                                                    currentDest = dest
                                                    if (dest == MainDestination.PROFILE) profileTabOverride = 0
                                                },
                                                icon = {
                                                    val iconVec = if (selected) dest.selectedIcon else dest.unselectedIcon
                                                    if (dest == MainDestination.MESSAGES && totalUnread > 0) {
                                                        BadgedBox(badge = { Badge { Text(totalUnread.toString()) } }) {
                                                            Icon(iconVec, contentDescription = dest.label)
                                                        }
                                                    } else {
                                                        Icon(iconVec, contentDescription = dest.label)
                                                    }
                                                },
                                                label = { Text(dest.label) },
                                                colors = NavigationRailItemDefaults.colors(
                                                    selectedIconColor = TextPrimary,
                                                    selectedTextColor = SoftLavender,
                                                    indicatorColor = VioletPrimary.copy(alpha = 0.28f),
                                                    unselectedIconColor = TextMuted,
                                                    unselectedTextColor = TextMuted
                                                ),
                                                modifier = Modifier.testTag("rail_${dest.route}")
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                    when (subScreen) {
                                        SubScreenRoute.SESSIONS -> {
                                            SessionsScreen(
                                                sessions = sessions,
                                                partners = partners,
                                                preselectedPartner = sessionPreselectedPartner,
                                                onBack = {
                                                    sessionPreselectedPartner = null
                                                    subScreen = SubScreenRoute.NONE
                                                },
                                                onScheduleSession = { p, topic, role, date, time, duration, notes ->
                                                    sessionPreselectedPartner = null
                                                    viewModel.scheduleSession(p, topic, role, date, time, duration, notes)
                                                },
                                                onUpdateNotes = viewModel::updateSessionNotes,
                                                onCompleteSession = viewModel::completeSession,
                                                onCancelSession = viewModel::cancelSession
                                            )
                                        }
                                        SubScreenRoute.AI_ASSISTANT -> {
                                            AiAssistantScreen(
                                                profile = profile,
                                                roadmaps = roadmaps,
                                                roadmapAiState = roadmapAiState,
                                                exerciseAiState = exerciseAiState,
                                                onBack = { subScreen = SubScreenRoute.NONE },
                                                onGenerateRoadmap = viewModel::generateRoadmap,
                                                onToggleMilestone = viewModel::toggleRoadmapMilestone,
                                                onDeleteRoadmap = viewModel::deleteRoadmap,
                                                onGenerateExercise = viewModel::generatePracticeExercise,
                                                onClearExercise = viewModel::clearExerciseState
                                            )
                                        }
                                        SubScreenRoute.NONE -> {
                                            when (currentDest) {
                                                MainDestination.HOME -> {
                                                    HomeScreen(
                                                        profile = profile,
                                                        recommendedPartners = rankedMatches,
                                                        sessions = sessions,
                                                        roadmaps = roadmaps,
                                                        onOpenAiAssistant = { subScreen = SubScreenRoute.AI_ASSISTANT },
                                                        onOpenSessions = { subScreen = SubScreenRoute.SESSIONS },
                                                        onOpenExplore = { currentDest = MainDestination.EXPLORE },
                                                        onOpenMatches = { currentDest = MainDestination.MATCHES },
                                                        onOpenLeaderboard = {
                                                            profileTabOverride = 2
                                                            currentDest = MainDestination.PROFILE
                                                        },
                                                        onSelectPartner = { selectedPartnerDialog = it },
                                                        onToggleConnect = viewModel::togglePartnerConnection
                                                    )
                                                }
                                                MainDestination.EXPLORE -> {
                                                    ExploreScreen(
                                                        matches = filteredMatches,
                                                        searchQuery = searchQuery,
                                                        selectedCategory = selectedCategory,
                                                        selectedLevel = selectedLevel,
                                                        selectedAvailability = selectedAvailability,
                                                        onSearchQueryChange = viewModel::setSearchQuery,
                                                        onCategoryChange = viewModel::setSelectedCategory,
                                                        onLevelChange = viewModel::setSelectedLevel,
                                                        onAvailabilityChange = viewModel::setSelectedAvailability,
                                                        onClearFilters = viewModel::clearFilters,
                                                        onSelectPartner = { selectedPartnerDialog = it },
                                                        onToggleConnect = viewModel::togglePartnerConnection,
                                                        onMessagePartner = { partner ->
                                                            viewModel.startChatWithPartner(partner) {
                                                                currentDest = MainDestination.MESSAGES
                                                            }
                                                        }
                                                    )
                                                }
                                                MainDestination.MATCHES -> {
                                                    MatchesScreen(
                                                        profile = profile,
                                                        matches = rankedMatches,
                                                        matchInsightState = matchInsightState,
                                                        onToggleQuickSkill = viewModel::toggleQuickSkill,
                                                        onSelectPartner = { selectedPartnerDialog = it },
                                                        onToggleConnect = viewModel::togglePartnerConnection,
                                                        onMessagePartner = { partner ->
                                                            viewModel.startChatWithPartner(partner) {
                                                                currentDest = MainDestination.MESSAGES
                                                            }
                                                        },
                                                        onRequestMatchInsight = viewModel::requestMatchInsight,
                                                        onDismissMatchInsight = viewModel::clearMatchInsight
                                                    )
                                                }
                                                MainDestination.MESSAGES -> {
                                                    MessagesScreen(
                                                        currentUserId = currentUserId,
                                                        conversations = conversations,
                                                        activeConversationId = activeConversationId,
                                                        messagesState = messagesState,
                                                        onOpenConversation = viewModel::openConversation,
                                                        onCloseConversation = viewModel::closeConversation,
                                                        onSendMessage = viewModel::sendChatMessage,
                                                        onSendProposal = viewModel::sendSessionProposal,
                                                        onRespondProposal = viewModel::respondToProposal
                                                    )
                                                }
                                                MainDestination.PROFILE -> {
                                                    ProfileScreen(
                                                        profile = profile,
                                                        achievements = achievements,
                                                        reviews = reviews,
                                                        leaderboard = leaderboard,
                                                        initialTab = profileTabOverride,
                                                        onUpdateProfile = { name, bio, teach, learn, lvl, avail ->
                                                            viewModel.saveProfileChanges(
                                                                displayName = name,
                                                                bio = bio,
                                                                teachSkills = teach,
                                                                learnSkills = learn,
                                                                skillLevel = lvl,
                                                                availability = avail,
                                                                onboardingCompleted = true
                                                            )
                                                        },
                                                        onSignOut = { performSignOut() }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedPartnerDialog != null) {
                        val match = selectedPartnerDialog!!
                        val latestPartner = partners.firstOrNull { it.id == match.partner.id } ?: match.partner
                        PartnerDetailDialog(
                            partnerMatch = match.copy(partner = latestPartner),
                            matchInsightState = matchInsightState,
                            onDismiss = {
                                selectedPartnerDialog = null
                                viewModel.clearMatchInsight()
                            },
                            onToggleConnect = viewModel::togglePartnerConnection,
                            onMessage = { partner ->
                                viewModel.startChatWithPartner(partner) {
                                    currentDest = MainDestination.MESSAGES
                                }
                            },
                            onScheduleSession = { partner ->
                                sessionPreselectedPartner = partner
                                subScreen = SubScreenRoute.SESSIONS
                            },
                            onRequestAiInsight = viewModel::requestMatchInsight
                        )
                    }
                }
            }
        }
    }
}
