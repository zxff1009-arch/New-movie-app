package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.Achievement
import com.example.data.LeaderboardEntry
import com.example.data.Review
import com.example.data.UserProfile
import com.example.ui.components.AvatarBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.SkillTagChip
import com.example.ui.components.XpProgressBar
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.FrostedCardStrong
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    achievements: List<Achievement>,
    reviews: List<Review>,
    leaderboard: List<LeaderboardEntry>,
    initialTab: Int = 0,
    onUpdateProfile: (
        displayName: String,
        bio: String,
        teachSkills: List<String>,
        learnSkills: List<String>,
        skillLevel: String,
        availability: String
    ) -> Unit,
    onSignOut: () -> Unit
) {
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var showEditDialog by remember { mutableStateOf(false) }

    val initials = profile.displayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "SS" }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Hero Card
            item {
                GlassCard(highlightBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AvatarBadge(
                            initials = initials,
                            colorHex = profile.avatarColorHex,
                            size = 64.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.displayName, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                text = "Level ${profile.level} • ${profile.skillLevel} • ${profile.xp} XP",
                                style = MaterialTheme.typography.labelLarge.copy(color = SoftLavender)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Schedule, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = profile.availability,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit profile", tint = SoftLavender)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(profile.bio, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatBadgePill("${profile.streakDays}d Streak", Icons.Rounded.LocalFireDepartment, AmberGold)
                        StatBadgePill("${profile.completedSessionsCount} Swaps", Icons.Rounded.AutoAwesome, MintSuccess)
                        StatBadgePill("4.9 ★ Rating", Icons.Rounded.Star, VioletPrimary)
                    }
                }
            }

            // Tab Switcher: Overview / Achievements / Leaderboard
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Profile", "Badges", "Leaderboard").forEachIndexed { idx, label ->
                        FilterChip(
                            selected = activeTab == idx,
                            onClick = { activeTab = idx },
                            label = { Text(label) },
                            modifier = Modifier.testTag("profile_tab_${label.lowercase()}")
                        )
                    }
                }
            }

            when (activeTab) {
                0 -> {
                    item {
                        GlassCard {
                            Text("Skills Offered", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                profile.teachSkills.forEach { SkillTagChip(it, MintSuccess) }
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("Skills Wanted", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                profile.learnSkills.forEach { SkillTagChip(it, CobaltBlue) }
                            }
                        }
                    }

                    item {
                        Text("Ratings & Reviews (${reviews.size})", style = MaterialTheme.typography.titleLarge)
                    }
                    items(reviews.size) { idx ->
                        val rev = reviews[idx]
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AvatarBadge(initials = rev.authorInitials, colorHex = "#6366F1", size = 38.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(rev.authorName, style = MaterialTheme.typography.titleSmall)
                                        Text(rev.skillTopic, style = MaterialTheme.typography.labelSmall.copy(color = SoftLavender))
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Star, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("${rev.rating}.0", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(rev.comment, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary))
                        }
                    }
                }
                1 -> {
                    items(achievements.size) { idx ->
                        val ach = achievements[idx]
                        val ratio = if (ach.target > 0) ach.progress.toFloat() / ach.target.toFloat() else 0f
                        GlassCard(highlightBorder = ach.unlocked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (ach.unlocked) AmberGold.copy(alpha = 0.2f) else ElevatedNavy
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (ach.unlocked) Icons.Rounded.MilitaryTech else Icons.Rounded.Lock,
                                        contentDescription = null,
                                        tint = if (ach.unlocked) AmberGold else TextMuted
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(ach.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "+${ach.xpReward} XP",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (ach.unlocked) MintSuccess else SoftLavender,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Text(
                                        text = ach.description,
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    XpProgressBar(progress = ratio, height = 6.dp)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = if (ach.unlocked) "Unlocked" else "${ach.progress} / ${ach.target}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (ach.unlocked) MintSuccess else TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                else -> {
                    item {
                        Text("Community XP Leaderboard", style = MaterialTheme.typography.titleLarge)
                    }
                    items(leaderboard.size) { idx ->
                        val entry = leaderboard[idx]
                        GlassCard(highlightBorder = entry.isCurrentUser) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${entry.rank}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = if (entry.rank <= 3) AmberGold else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.width(36.dp)
                                )
                                AvatarBadge(initials = entry.initials, colorHex = entry.colorHex, size = 40.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "Lv.${entry.level} • ${entry.primarySkill}",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = AmberGold, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(3.dp))
                                        Text("${entry.xp} XP", style = MaterialTheme.typography.labelLarge)
                                    }
                                    Text(
                                        text = "${entry.streakDays}d streak",
                                        style = MaterialTheme.typography.labelSmall.copy(color = MintSuccess)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sign Out
            item {
                GlassOutlinedButton(
                    text = "Sign Out",
                    leadingIcon = Icons.AutoMirrored.Rounded.Logout,
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "sign_out_button"
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { name, bio, level, avail ->
                showEditDialog = false
                onUpdateProfile(
                    name,
                    bio,
                    profile.teachSkills,
                    profile.learnSkills,
                    level,
                    avail
                )
            }
        )
    }
}

@Composable
private fun StatBadgePill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary))
        }
    }
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, bio: String, level: String, availability: String) -> Unit
) {
    var name by remember { mutableStateOf(profile.displayName) }
    var bio by remember { mutableStateOf(profile.bio) }
    var level by remember { mutableStateOf(profile.skillLevel) }
    var availability by remember { mutableStateOf(profile.availability) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FrostedCardStrong,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Skill Level", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Beginner", "Intermediate", "Advanced", "Expert").forEach { lvl ->
                        FilterChip(
                            selected = level == lvl,
                            onClick = { level = lvl },
                            label = { Text(lvl, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = availability,
                    onValueChange = { availability = it },
                    label = { Text("Availability") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, bio, level, availability) }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
