package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DesignServices
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.SkillCatalog
import com.example.data.UserProfile
import com.example.ui.components.ErrorStateBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GradientButton
import com.example.ui.components.PrimaryVioletBlueGradient
import com.example.ui.components.SkillSwapBackground
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrimary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val AUTH_TAG = "Auth"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    // Silent auto sign-in check on startup (isolated GetGoogleIdOption request)
    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { /* Stay on sign-in screen */ },
            scope = scope
        )
    }

    fun triggerGoogleSignIn() {
        isLoading = true
        errorMessage = null
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            onAuthCancelled = {
                isLoading = false
            },
            scope = scope
        )
    }

    SkillSwapBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Brand Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryVioletBlueGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "SkillSwap Logo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "SkillSwap",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MintSuccess.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, MintSuccess.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "100% Free Exchange",
                            style = MaterialTheme.typography.labelMedium.copy(color = MintSuccess),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Hero Card with Generated Artwork
                GlassCard(
                    highlightBorder = true,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(176.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_skillswap_1795068537027),
                            contentDescription = "Peer-to-peer skill exchange illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, DeepNavyBg.copy(alpha = 0.92f))
                                    )
                                )
                        )
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = VioletPrimary.copy(alpha = 0.25f),
                            border = BorderStroke(1.dp, SoftLavender.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "AI-Matched Peer Learning",
                                style = MaterialTheme.typography.labelMedium.copy(color = SoftLavender),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Your skills are someone else's superpower.",
                            style = MaterialTheme.typography.displaySmall.copy(color = TextPrimary)
                        )
                        Text(
                            text = "Teach what you already know, learn what inspires you next—with smart skill matching, 1-on-1 sessions, and Gemini learning roadmaps. No money involved.",
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary)
                        )

                        if (errorMessage != null) {
                            ErrorStateBanner(
                                message = errorMessage!!,
                                onRetry = { triggerGoogleSignIn() }
                            )
                        }

                        // Primary Google Sign-In Button
                        Button(
                            onClick = { triggerGoogleSignIn() },
                            enabled = !isLoading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("google_sign_in_button")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = VioletPrimary,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Signing in...", style = MaterialTheme.typography.titleSmall)
                            } else {
                                Text(
                                    text = "G",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CobaltBlue
                                    )
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = "Sign in with Google",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GradientButton(
                                text = "Start Learning",
                                onClick = { triggerGoogleSignIn() },
                                modifier = Modifier.weight(1f),
                                testTag = "hero_start_learning_button"
                            )
                            GlassOutlinedButton(
                                text = "Explore Skills",
                                onClick = {
                                    scope.launch {
                                        scrollState.animateScrollTo(scrollState.maxValue / 2)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "hero_explore_skills_button"
                            )
                        }
                    }
                }

                // Community Stats Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatMiniCard("18.4K+", "Skill Swaps", VioletPrimary, Modifier.weight(1f))
                    StatMiniCard("4.9 ★", "Mentor Rating", AmberGold, Modifier.weight(1f))
                    StatMiniCard("$0", "Always Free", MintSuccess, Modifier.weight(1f))
                }

                // How It Works in 3 Steps
                Text(
                    text = "How SkillSwap Works",
                    style = MaterialTheme.typography.titleLarge
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepCard(
                        step = "01",
                        title = "Share What You Know",
                        description = "List skills you can teach and skills you want to master in 30 seconds.",
                        icon = Icons.Rounded.School,
                        accent = VioletPrimary
                    )
                    StepCard(
                        step = "02",
                        title = "Match with Compatible Partners",
                        description = "Our compatibility engine pairs you with learners who want your skills and teach yours.",
                        icon = Icons.Rounded.People,
                        accent = CobaltBlue
                    )
                    StepCard(
                        step = "03",
                        title = "Swap Sessions & Track XP",
                        description = "Chat, propose 1-on-1 sessions, earn XP streaks, and follow AI-built roadmaps.",
                        icon = Icons.Rounded.CalendarMonth,
                        accent = CyanHighlight
                    )
                }

                // Featured Skill Categories
                Text(
                    text = "Featured Skill Categories",
                    style = MaterialTheme.typography.titleLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val featured = listOf(
                        "Programming" to Icons.Rounded.Code,
                        "Design" to Icons.Rounded.DesignServices,
                        "Music" to Icons.Rounded.MusicNote,
                        "Languages" to Icons.Rounded.Translate,
                        "Business" to Icons.Rounded.AutoAwesome,
                        "Photography" to Icons.Rounded.Star,
                        "Marketing" to Icons.Rounded.People,
                        "Fitness" to Icons.Rounded.Check
                    )
                    featured.forEach { (cat, icon) ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ElevatedNavy,
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(icon, contentDescription = null, tint = SoftLavender, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(cat, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                // Testimonials
                Text(
                    text = "Loved by Peer Learners",
                    style = MaterialTheme.typography.titleLarge
                )
                GlassCard {
                    Text(
                        text = "\"I traded four Jetpack Compose code reviews for Figma design-system mentoring with Maya—shipped my portfolio twice as fast without spending a dime.\"",
                        style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Daniel K. • Mobile Engineer & Guitar Learner",
                        style = MaterialTheme.typography.labelMedium.copy(color = SoftLavender)
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatMiniCard(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                color = accent,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
        )
    }
}

@Composable
private fun StepCard(
    step: String,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "STEP $step",
                    style = MaterialTheme.typography.labelSmall.copy(color = accent, fontWeight = FontWeight.Bold)
                )
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    profile: UserProfile,
    isSaving: Boolean,
    onFinishOnboarding: (
        displayName: String,
        bio: String,
        teachSkills: List<String>,
        learnSkills: List<String>,
        level: String,
        availability: String
    ) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var bio by remember(profile.bio) { mutableStateOf(profile.bio) }
    val selectedTeach = remember { mutableStateListOf<String>().apply { addAll(profile.teachSkills) } }
    val selectedLearn = remember { mutableStateListOf<String>().apply { addAll(profile.learnSkills) } }
    var customSkillInput by remember { mutableStateOf("") }
    var selectedLevel by remember(profile.skillLevel) { mutableStateOf(profile.skillLevel) }
    var selectedAvailability by remember(profile.availability) { mutableStateOf(profile.availability) }

    SkillSwapBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "STEP $step OF 2",
                    style = MaterialTheme.typography.labelLarge.copy(color = SoftLavender)
                )
                Text(
                    text = if (step == 1) "What superpowers can you teach?" else "What do you want to learn next?",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = if (step == 1) {
                        "Pick skills you're confident sharing and your weekly availability."
                    } else {
                        "Select skills to learn so SkillSwap can compute your best matches."
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary)
                )

                val activeList = if (step == 1) selectedTeach else selectedLearn
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SkillCatalog.suggestedSkills.forEach { skill ->
                        val isSelected = activeList.contains(skill)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) activeList.remove(skill) else activeList.add(skill)
                            },
                            label = { Text(skill) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VioletPrimary.copy(alpha = 0.28f),
                                selectedLabelColor = TextPrimary,
                                containerColor = ElevatedNavy,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Add custom skill input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customSkillInput,
                        onValueChange = { customSkillInput = it },
                        label = { Text("Add another skill") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VioletPrimary,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("onboarding_custom_skill_input")
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val trimmed = customSkillInput.trim()
                            if (trimmed.isNotEmpty() && !activeList.contains(trimmed)) {
                                activeList.add(trimmed)
                                customSkillInput = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(VioletPrimary.copy(alpha = 0.24f))
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add skill", tint = TextPrimary)
                    }
                }

                if (step == 1) {
                    GlassCard {
                        Text("Your Display Name & Level", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Primary Teaching Level", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Beginner", "Intermediate", "Advanced", "Expert").forEach { lvl ->
                                FilterChip(
                                    selected = selectedLevel == lvl,
                                    onClick = { selectedLevel = lvl },
                                    label = { Text(lvl) }
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Weekly Availability", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Weekday Evenings", "Weekends", "Mornings", "Flexible").forEach { avail ->
                                FilterChip(
                                    selected = selectedAvailability == avail,
                                    onClick = { selectedAvailability = avail },
                                    label = { Text(avail) }
                                )
                            }
                        }
                    }

                    GradientButton(
                        text = "Continue to Learning Goals",
                        onClick = {
                            customSkillInput = ""
                            step = 2
                        },
                        leadingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "onboarding_next_button"
                    )
                } else {
                    GlassCard {
                        Text("Short Bio", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = bio,
                            onValueChange = { bio = it },
                            label = { Text("Tell partners what excites you") },
                            minLines = 3,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassOutlinedButton(
                            text = "Back",
                            onClick = { step = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        GradientButton(
                            text = "Enter SkillSwap",
                            loading = isSaving,
                            onClick = {
                                val finalTeach = if (selectedTeach.isEmpty()) listOf("Kotlin & Compose") else selectedTeach.toList()
                                val finalLearn = if (selectedLearn.isEmpty()) listOf("UI/UX Design & Figma") else selectedLearn.toList()
                                onFinishOnboarding(
                                    displayName.ifBlank { "SkillSwap Explorer" },
                                    bio,
                                    finalTeach,
                                    finalLearn,
                                    selectedLevel,
                                    selectedAvailability
                                )
                            },
                            modifier = Modifier.weight(1.6f),
                            testTag = "onboarding_finish_button"
                        )
                    }
                }
            }
        }
    }
}

// --- CREDENTIAL MANAGER HELPERS ---

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for Google Sign-In")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned by Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(AUTH_TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e(AUTH_TAG, "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
        }
    }
}
