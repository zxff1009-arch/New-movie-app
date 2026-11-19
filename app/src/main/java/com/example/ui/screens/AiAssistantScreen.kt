package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.AiTaskState
import com.example.data.LearningRoadmap
import com.example.data.UserProfile
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
import com.example.ui.components.XpProgressBar
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.FrostedCardStrong
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrimary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiAssistantScreen(
    profile: UserProfile,
    roadmaps: List<LearningRoadmap>,
    roadmapAiState: AiTaskState<String>,
    exerciseAiState: AiTaskState<String>,
    onBack: () -> Unit,
    onGenerateRoadmap: (skillName: String, targetLevel: String) -> Unit,
    onToggleMilestone: (LearningRoadmap, Int) -> Unit,
    onDeleteRoadmap: (String) -> Unit,
    onGenerateExercise: (skillName: String, milestoneTitle: String) -> Unit,
    onClearExercise: () -> Unit
) {
    BackHandler { onBack() }

    var skillInput by remember(profile.learnSkills) {
        mutableStateOf(profile.learnSkills.firstOrNull() ?: "UI/UX Design & Figma")
    }
    var targetLevel by remember { mutableStateOf("Beginner") }
    var activeDrillTitle by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("ai_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("AI Learning Assistant", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Personalized skill roadmaps, milestones & practice drills",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
            }

            // Generator Card
            item {
                GlassCard(highlightBorder = true) {
                    Text("Build a Personalized Roadmap", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = skillInput,
                        onValueChange = { skillInput = it },
                        label = { Text("Skill you want to master") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_skill_input")
                    )
                    if (profile.learnSkills.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            profile.learnSkills.forEach { s ->
                                FilterChip(
                                    selected = skillInput == s,
                                    onClick = { skillInput = s },
                                    label = { Text(s) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Target Level", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Beginner", "Intermediate", "Advanced").forEach { lvl ->
                            FilterChip(
                                selected = targetLevel == lvl,
                                onClick = { targetLevel = lvl },
                                label = { Text(lvl) }
                            )
                        }
                    }

                    if (roadmapAiState is AiTaskState.Error) {
                        Spacer(Modifier.height(8.dp))
                        ErrorStateBanner(
                            message = roadmapAiState.message,
                            onRetry = {
                                if (skillInput.isNotBlank()) onGenerateRoadmap(skillInput, targetLevel)
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    GradientButton(
                        text = if (roadmapAiState is AiTaskState.Loading) "Crafting Milestones..." else "Generate AI Roadmap",
                        loading = roadmapAiState is AiTaskState.Loading,
                        enabled = skillInput.isNotBlank(),
                        leadingIcon = Icons.Rounded.AutoAwesome,
                        onClick = { onGenerateRoadmap(skillInput, targetLevel) },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "ai_generate_roadmap_button"
                    )
                }
            }

            item {
                Text("Your Learning Roadmaps (${roadmaps.size})", style = MaterialTheme.typography.titleLarge)
            }

            if (roadmaps.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "No roadmaps yet",
                        subtitle = "Enter any skill above to break it into beginner-friendly milestones."
                    )
                }
            } else {
                items(roadmaps, key = { it.id }) { roadmap ->
                    val pct = if (roadmap.totalCount > 0) {
                        roadmap.completedCount.toFloat() / roadmap.totalCount.toFloat()
                    } else 0f

                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(roadmap.skillName, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "${roadmap.targetLevel} • ${roadmap.completedCount}/${roadmap.totalCount} milestones done",
                                    style = MaterialTheme.typography.labelMedium.copy(color = CyanHighlight)
                                )
                            }
                            IconButton(onClick = { onDeleteRoadmap(roadmap.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete roadmap", tint = TextMuted)
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(roadmap.summary, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
                        Spacer(Modifier.height(10.dp))
                        XpProgressBar(progress = pct, height = 7.dp)
                        Spacer(Modifier.height(12.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            roadmap.milestones.forEachIndexed { idx, m ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = ElevatedNavy,
                                    border = BorderStroke(
                                        1.dp,
                                        if (m.completed) MintSuccess.copy(alpha = 0.45f) else GlassBorder
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = m.completed,
                                                onCheckedChange = { onToggleMilestone(roadmap, idx) },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = MintSuccess,
                                                    uncheckedColor = SoftLavender
                                                )
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = m.title,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        textDecoration = if (m.completed) TextDecoration.LineThrough else TextDecoration.None
                                                    )
                                                )
                                                Text(
                                                    text = m.duration,
                                                    style = MaterialTheme.typography.labelSmall.copy(color = SoftLavender)
                                                )
                                            }
                                        }
                                        if (m.description.isNotBlank()) {
                                            Text(
                                                text = m.description,
                                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                                modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                                            )
                                        }
                                        if (m.exercise.isNotBlank()) {
                                            Text(
                                                text = "Starter Drill: ${m.exercise}",
                                                style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary),
                                                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                activeDrillTitle = m.title
                                                onGenerateExercise(roadmap.skillName, m.title)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.padding(start = 12.dp)
                                        ) {
                                            Icon(Icons.Rounded.FitnessCenter, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("New AI Practice Drill", style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (activeDrillTitle != null) {
        AlertDialog(
            onDismissRequest = {
                activeDrillTitle = null
                onClearExercise()
            },
            containerColor = FrostedCardStrong,
            title = { Text("Practice Drill: $activeDrillTitle") },
            text = {
                when (exerciseAiState) {
                    is AiTaskState.Loading -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = VioletPrimary)
                            Spacer(Modifier.width(12.dp))
                            Text("Generating custom exercise with Gemini...", color = TextSecondary)
                        }
                    }
                    is AiTaskState.Success -> {
                        Text(exerciseAiState.data, style = MaterialTheme.typography.bodyMedium)
                    }
                    is AiTaskState.Error -> {
                        ErrorStateBanner(message = exerciseAiState.message)
                    }
                    AiTaskState.Idle -> {
                        Text("Preparing drill...", color = TextSecondary)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        activeDrillTitle = null
                        onClearExercise()
                    }
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
