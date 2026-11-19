package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AiTaskState
import com.example.data.MentorPartner
import com.example.data.PartnerMatch
import com.example.data.SkillCatalog
import com.example.data.UserProfile
import com.example.ui.components.AvatarBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GradientButton
import com.example.ui.components.MatchScoreBadge
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.FrostedCardStrong
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchesScreen(
    profile: UserProfile,
    matches: List<PartnerMatch>,
    matchInsightState: AiTaskState<String>,
    onToggleQuickSkill: (isTeach: Boolean, skill: String) -> Unit,
    onSelectPartner: (PartnerMatch) -> Unit,
    onToggleConnect: (MentorPartner) -> Unit,
    onMessagePartner: (MentorPartner) -> Unit,
    onRequestMatchInsight: (PartnerMatch) -> Unit,
    onDismissMatchInsight: () -> Unit
) {
    var editingTeach by remember { mutableStateOf(true) }
    var customSkill by remember { mutableStateOf("") }
    var inspectedMatch by remember { mutableStateOf<PartnerMatch?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(text = "Smart Skill Matching", style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Adjust what you teach and want to learn—compatible swap partners update in real time.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
                )
            }

            // Interactive Teach / Learn Selector Card
            item {
                GlassCard(highlightBorder = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = editingTeach,
                            onClick = { editingTeach = true },
                            label = { Text("I Can Teach (${profile.teachSkills.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MintSuccess.copy(alpha = 0.24f),
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("matches_tab_teach")
                        )
                        FilterChip(
                            selected = !editingTeach,
                            onClick = { editingTeach = false },
                            label = { Text("Want to Learn (${profile.learnSkills.size})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CobaltBlue.copy(alpha = 0.28f),
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("matches_tab_learn")
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                    val activeSkills = if (editingTeach) profile.teachSkills else profile.learnSkills
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val allOptions = (SkillCatalog.suggestedSkills + activeSkills).distinct()
                        allOptions.forEach { skill ->
                            val selected = activeSkills.contains(skill)
                            FilterChip(
                                selected = selected,
                                onClick = { onToggleQuickSkill(editingTeach, skill) },
                                label = { Text(skill) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (editingTeach) MintSuccess.copy(alpha = 0.22f) else VioletPrimary.copy(alpha = 0.28f),
                                    selectedLabelColor = TextPrimary,
                                    containerColor = ElevatedNavy,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customSkill,
                            onValueChange = { customSkill = it },
                            label = {
                                Text(if (editingTeach) "Add custom teach skill" else "Add custom learn skill")
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val trimmed = customSkill.trim()
                                if (trimmed.isNotEmpty()) {
                                    onToggleQuickSkill(editingTeach, trimmed)
                                    customSkill = ""
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add custom skill", tint = SoftLavender)
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Compatible Partners (${matches.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (matches.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.SwapHoriz,
                        title = "No compatible matches yet",
                        subtitle = "Select more skills above to discover peer swap partners."
                    )
                }
            } else {
                items(matches, key = { it.partner.id }) { match ->
                    val p = match.partner
                    GlassCard(onClick = { onSelectPartner(match) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarBadge(initials = p.avatarInitials, colorHex = p.avatarColorHex)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(p.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = "${p.teachSkill} • ${p.teachLevel}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = SoftLavender)
                                )
                            }
                            MatchScoreBadge(score = match.matchScore)
                        }

                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = VioletPrimary.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, VioletPrimary.copy(alpha = 0.28f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.SwapHoriz,
                                    contentDescription = null,
                                    tint = SoftLavender,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = match.mutualSwapReason,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlassOutlinedButton(
                                text = "AI Insight",
                                leadingIcon = Icons.Rounded.AutoAwesome,
                                onClick = {
                                    inspectedMatch = match
                                    onRequestMatchInsight(match)
                                },
                                modifier = Modifier.weight(1f),
                                testTag = "ai_insight_btn_${p.id}"
                            )
                            GradientButton(
                                text = if (p.connected) "Chat" else "Connect",
                                leadingIcon = if (p.connected) Icons.Rounded.ChatBubbleOutline else null,
                                onClick = {
                                    if (p.connected) onMessagePartner(p) else onToggleConnect(p)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }

    // AI Match Insight Dialog
    if (inspectedMatch != null) {
        val current = inspectedMatch!!
        AlertDialog(
            onDismissRequest = {
                inspectedMatch = null
                onDismissMatchInsight()
            },
            containerColor = FrostedCardStrong,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = SoftLavender)
                    Spacer(Modifier.width(8.dp))
                    Text("AI Swap Insight: ${current.partner.name}")
                }
            },
            text = {
                when (matchInsightState) {
                    is AiTaskState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = VioletPrimary)
                            Spacer(Modifier.width(12.dp))
                            Text("Analyzing swap synergy with Gemini...", color = TextSecondary)
                        }
                    }
                    is AiTaskState.Success -> {
                        Text(matchInsightState.data, style = MaterialTheme.typography.bodyMedium)
                    }
                    is AiTaskState.Error -> {
                        ErrorStateBanner(
                            message = matchInsightState.message,
                            onRetry = { onRequestMatchInsight(current) }
                        )
                    }
                    AiTaskState.Idle -> {
                        Text("Preparing compatibility analysis...", color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val p = current.partner
                        inspectedMatch = null
                        onDismissMatchInsight()
                        onMessagePartner(p)
                    }
                ) {
                    Text("Message ${current.partner.name}", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        inspectedMatch = null
                        onDismissMatchInsight()
                    }
                ) {
                    Text("Close")
                }
            }
        )
    }
}
