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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LearningRoadmap
import com.example.data.LearningSession
import com.example.data.MentorPartner
import com.example.data.PartnerMatch
import com.example.data.UserProfile
import com.example.ui.components.AvatarBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.MatchScoreBadge
import com.example.ui.components.SkillTagChip
import com.example.ui.components.XpProgressBar
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    profile: UserProfile,
    recommendedPartners: List<PartnerMatch>,
    sessions: List<LearningSession>,
    roadmaps: List<LearningRoadmap>,
    onOpenAiAssistant: () -> Unit,
    onOpenSessions: () -> Unit,
    onOpenExplore: () -> Unit,
    onOpenMatches: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onSelectPartner: (PartnerMatch) -> Unit,
    onToggleConnect: (MentorPartner) -> Unit
) {
    val upcomingSessions = sessions.filter { it.status == "scheduled" || it.status == "proposed" }
    val xpIntoLevel = (profile.xp % 200L).toFloat() / 200f

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Personalized Welcome & Streak Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome back,",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                        Text(
                            text = profile.displayName,
                            style = MaterialTheme.typography.headlineLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = AmberGold.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocalFireDepartment,
                                contentDescription = "Learning streak",
                                tint = AmberGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = "${profile.streakDays}d Streak",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = AmberGold,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            // Gamification XP & Progress Card
            item {
                GlassCard(highlightBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Level ${profile.level} Skill Explorer",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${profile.xp} XP • ${profile.completedSessionsCount} swaps completed",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = VioletPrimary.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "${200 - (profile.xp % 200)} XP to Lv.${profile.level + 1}",
                                style = MaterialTheme.typography.labelMedium.copy(color = SoftLavender),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    XpProgressBar(progress = xpIntoLevel)
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickDashboardPill(
                            icon = Icons.Rounded.AutoAwesome,
                            label = "AI Coach",
                            accent = VioletPrimary,
                            onClick = onOpenAiAssistant,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_ai_assistant_button")
                        )
                        QuickDashboardPill(
                            icon = Icons.Rounded.CalendarMonth,
                            label = "Sessions",
                            accent = CobaltBlue,
                            onClick = onOpenSessions,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_sessions_button")
                        )
                        QuickDashboardPill(
                            icon = Icons.Rounded.EmojiEvents,
                            label = "Rankings",
                            accent = AmberGold,
                            onClick = onOpenLeaderboard,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_leaderboard_button")
                        )
                    }
                }
            }

            // Skills You're Learning & Progress Tracking
            item {
                SectionHeader(
                    title = "Skills You're Learning",
                    actionLabel = "AI Roadmap",
                    onAction = onOpenAiAssistant
                )
                Spacer(Modifier.height(8.dp))
                GlassCard {
                    if (profile.learnSkills.isEmpty()) {
                        Text(
                            text = "No learning skills chosen yet. Tap Matches to pick skills!",
                            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                        )
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            profile.learnSkills.forEach { skill ->
                                SkillTagChip(label = skill, accentColor = CobaltBlue)
                            }
                        }
                    }
                    if (roadmaps.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        val latest = roadmaps.first()
                        val pct = if (latest.totalCount > 0) {
                            latest.completedCount.toFloat() / latest.totalCount.toFloat()
                        } else 0f
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Active Roadmap: ${latest.skillName}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "${latest.completedCount}/${latest.totalCount} milestones",
                                style = MaterialTheme.typography.labelMedium.copy(color = CyanHighlight)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        XpProgressBar(progress = pct, height = 6.dp)
                    }
                }
            }

            // Skills You Can Teach
            item {
                SectionHeader(
                    title = "Skills You Can Teach",
                    actionLabel = "Edit",
                    onAction = onOpenMatches
                )
                Spacer(Modifier.height(8.dp))
                GlassCard {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        profile.teachSkills.forEach { skill ->
                            SkillTagChip(label = skill, accentColor = MintSuccess)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${profile.skillLevel} • Available ${profile.availability}",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }

            // Recommended Skill Partners Carousel
            item {
                SectionHeader(
                    title = "Recommended Skill Partners",
                    actionLabel = "See All",
                    onAction = onOpenExplore
                )
                Spacer(Modifier.height(8.dp))
                if (recommendedPartners.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Rounded.School,
                        title = "Finding compatible partners...",
                        subtitle = "Explore our community mentors or update your skills."
                    )
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(recommendedPartners.take(6), key = { it.partner.id }) { match ->
                            val p = match.partner
                            GlassCard(
                                modifier = Modifier.width(264.dp),
                                onClick = { onSelectPartner(match) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AvatarBadge(initials = p.avatarInitials, colorHex = p.avatarColorHex)
                                    MatchScoreBadge(score = match.matchScore)
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = p.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Teaches ${p.teachSkill}",
                                    style = MaterialTheme.typography.labelLarge.copy(color = SoftLavender),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Wants ${p.wantSkill}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Star,
                                            contentDescription = "Rating",
                                            tint = AmberGold,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${p.rating} (${p.reviewCount})",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    Surface(
                                        onClick = { onToggleConnect(p) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (p.connected) MintSuccess.copy(alpha = 0.2f) else VioletPrimary.copy(alpha = 0.24f)
                                    ) {
                                        Text(
                                            text = if (p.connected) "Connected" else "Connect",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                color = if (p.connected) MintSuccess else TextPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Upcoming Sessions Preview
            item {
                SectionHeader(
                    title = "Upcoming Sessions",
                    actionLabel = "Calendar",
                    onAction = onOpenSessions
                )
                Spacer(Modifier.height(8.dp))
                if (upcomingSessions.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "No upcoming sessions",
                        subtitle = "Schedule a 1-on-1 skill swap with any connected partner.",
                        actionLabel = "Schedule Session",
                        onAction = onOpenSessions
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        upcomingSessions.take(3).forEach { sess ->
                            GlassCard(onClick = onOpenSessions) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(CobaltBlue.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Schedule,
                                            contentDescription = null,
                                            tint = CyanHighlight
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sess.skillTopic,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "with ${sess.partnerName} • ${sess.scheduledDate} at ${sess.scheduledTime}",
                                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MintSuccess.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = sess.status.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall.copy(color = MintSuccess),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // AI Coach Callout
            item {
                GlassCard(highlightBorder = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = SoftLavender,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AI Learning Assistant", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Break any skill into beginner-friendly milestones & daily drills.",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GradientButton(
                        text = "Generate Learning Roadmap",
                        onClick = onOpenAiAssistant,
                        leadingIcon = Icons.Rounded.AutoAwesome,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun QuickDashboardPill(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(color = TextPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(color = SoftLavender)
                )
            }
        }
    }
}
