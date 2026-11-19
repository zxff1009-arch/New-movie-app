package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.AiTaskState
import com.example.data.MentorPartner
import com.example.data.PartnerMatch
import com.example.ui.components.AvatarBadge
import com.example.ui.components.ErrorStateBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GradientButton
import com.example.ui.components.MatchScoreBadge
import com.example.ui.components.SkillTagChip
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.FrostedCardStrong
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrimary

@Composable
fun PartnerDetailDialog(
    partnerMatch: PartnerMatch,
    matchInsightState: AiTaskState<String>,
    onDismiss: () -> Unit,
    onToggleConnect: (MentorPartner) -> Unit,
    onMessage: (MentorPartner) -> Unit,
    onScheduleSession: (MentorPartner) -> Unit,
    onRequestAiInsight: (PartnerMatch) -> Unit
) {
    val p = partnerMatch.partner
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = FrostedCardStrong,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MatchScoreBadge(score = partnerMatch.matchScore)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close profile", tint = TextSecondary)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarBadge(initials = p.avatarInitials, colorHex = p.avatarColorHex, size = 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(p.name, style = MaterialTheme.typography.headlineMedium)
                        Text(p.headline, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = AmberGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${p.rating} (${p.reviewCount} reviews)", style = MaterialTheme.typography.labelLarge)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(p.availability, style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary))
                    }
                }

                Text(p.bio, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))

                GlassCard {
                    Text("Skill Swap Profile", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkillTagChip("Offers: ${p.teachSkill} (${p.teachLevel})", VioletPrimary)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkillTagChip("Wants: ${p.wantSkill}", MintSuccess)
                    }
                }

                // AI Compatibility Insight Box
                GlassCard(highlightBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = SoftLavender, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("AI Swap Coach", style = MaterialTheme.typography.titleSmall)
                        }
                        if (matchInsightState is AiTaskState.Idle) {
                            Surface(
                                onClick = { onRequestAiInsight(partnerMatch) },
                                shape = RoundedCornerShape(10.dp),
                                color = VioletPrimary.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "Analyze Fit",
                                    style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    when (matchInsightState) {
                        is AiTaskState.Idle -> {
                            Text(
                                text = partnerMatch.mutualSwapReason,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                        is AiTaskState.Loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = VioletPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Generating personalized swap agenda...", style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                            }
                        }
                        is AiTaskState.Success -> {
                            Text(matchInsightState.data, style = MaterialTheme.typography.bodySmall)
                        }
                        is AiTaskState.Error -> {
                            ErrorStateBanner(
                                message = matchInsightState.message,
                                onRetry = { onRequestAiInsight(partnerMatch) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassOutlinedButton(
                        text = if (p.connected) "Connected" else "Connect",
                        leadingIcon = if (p.connected) Icons.Rounded.Check else null,
                        onClick = { onToggleConnect(p) },
                        modifier = Modifier.weight(1f)
                    )
                    GradientButton(
                        text = "Message",
                        leadingIcon = Icons.Rounded.ChatBubbleOutline,
                        onClick = {
                            onDismiss()
                            onMessage(p)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                GradientButton(
                    text = "Schedule 1-on-1 Session",
                    leadingIcon = Icons.Rounded.CalendarMonth,
                    onClick = {
                        onDismiss()
                        onScheduleSession(p)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
