package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LearningSession
import com.example.data.MentorPartner
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GlassCard
import com.example.ui.components.GradientButton
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

@Composable
fun SessionsScreen(
    sessions: List<LearningSession>,
    partners: List<MentorPartner>,
    preselectedPartner: MentorPartner? = null,
    onBack: () -> Unit,
    onScheduleSession: (
        partner: MentorPartner,
        skillTopic: String,
        role: String,
        date: String,
        time: String,
        duration: Long,
        notes: String
    ) -> Unit,
    onUpdateNotes: (sessionId: String, notes: String) -> Unit,
    onCompleteSession: (LearningSession, String) -> Unit,
    onCancelSession: (String) -> Unit
) {
    BackHandler { onBack() }

    val calendarDates = remember {
        listOf(
            "All Dates" to "All",
            "2026-11-19" to "Thu 19",
            "2026-11-20" to "Fri 20",
            "2026-11-21" to "Sat 21",
            "2026-11-22" to "Sun 22",
            "2026-11-23" to "Mon 23",
            "2026-11-24" to "Tue 24"
        )
    }

    var selectedDate by remember { mutableStateOf("All Dates") }
    var statusFilter by remember { mutableStateOf("All") }
    var showScheduleDialog by remember(preselectedPartner) {
        mutableStateOf(preselectedPartner != null)
    }
    var editingSessionNotes by remember { mutableStateOf<LearningSession?>(null) }

    val filteredSessions = sessions.filter { sess ->
        val dateOk = selectedDate == "All Dates" || sess.scheduledDate == selectedDate
        val statusOk = when (statusFilter) {
            "Scheduled" -> sess.status == "scheduled" || sess.status == "proposed"
            "Completed" -> sess.status == "completed"
            else -> true
        }
        dateOk && statusOk
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("sessions_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text("Learning Sessions", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = "Calendar, session notes & completed history",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
                GradientButton(
                    text = "Schedule",
                    leadingIcon = Icons.Rounded.Add,
                    onClick = { showScheduleDialog = true },
                    testTag = "schedule_session_button"
                )
            }

            // Interactive Calendar Date Strip
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(calendarDates) { (iso, label) ->
                    val selected = selectedDate == iso
                    val countOnDate = if (iso == "All Dates") sessions.size else sessions.count { it.scheduledDate == iso }
                    Surface(
                        onClick = { selectedDate = iso },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) VioletPrimary.copy(alpha = 0.28f) else ElevatedNavy,
                        border = BorderStroke(1.dp, if (selected) VioletPrimary else GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(label, style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = "$countOnDate ${if (countOnDate == 1) "swap" else "swaps"}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (countOnDate > 0) SoftLavender else TextMuted
                                )
                            )
                        }
                    }
                }
            }

            // Status Filter Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Scheduled", "Completed").forEach { tab ->
                    FilterChip(
                        selected = statusFilter == tab,
                        onClick = { statusFilter = tab },
                        label = { Text(tab) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CobaltBlue.copy(alpha = 0.28f),
                            selectedLabelColor = TextPrimary
                        )
                    )
                }
            }

            if (filteredSessions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Rounded.CalendarMonth,
                    title = "No sessions on this view",
                    subtitle = "Schedule a new 1-on-1 skill swap session or switch date filters.",
                    actionLabel = "Schedule Session",
                    onAction = { showScheduleDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredSessions, key = { it.id }) { sess ->
                        SessionCard(
                            session = sess,
                            onEditNotes = { editingSessionNotes = sess },
                            onComplete = { onCompleteSession(sess, sess.notes) },
                            onCancel = { onCancelSession(sess.id) }
                        )
                    }
                }
            }
        }
    }

    if (showScheduleDialog && partners.isNotEmpty()) {
        ScheduleSessionDialog(
            partners = partners,
            initialPartner = preselectedPartner ?: partners.first(),
            onDismiss = { showScheduleDialog = false },
            onConfirm = { partner, topic, role, date, time, duration, notes ->
                showScheduleDialog = false
                onScheduleSession(partner, topic, role, date, time, duration, notes)
            }
        )
    }

    if (editingSessionNotes != null) {
        val target = editingSessionNotes!!
        var noteText by remember(target.id) { mutableStateOf(target.notes) }
        AlertDialog(
            onDismissRequest = { editingSessionNotes = null },
            containerColor = FrostedCardStrong,
            title = { Text("Session Notes: ${target.skillTopic}") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Key takeaways & prep checklist") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateNotes(target.id, noteText)
                        editingSessionNotes = null
                    }
                ) { Text("Save Notes", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { editingSessionNotes = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SessionCard(
    session: LearningSession,
    onEditNotes: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val statusColor = when (session.status) {
        "completed" -> MintSuccess
        "cancelled" -> RoseError
        "proposed" -> AmberGold
        else -> CobaltBlue
    }
    val isActive = session.status == "scheduled" || session.status == "proposed"

    GlassCard(highlightBorder = isActive) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = statusColor.copy(alpha = 0.16f)
            ) {
                Text(
                    text = session.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Text(
                text = "${session.durationMinutes} min • ${session.sessionRole.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(session.skillTopic, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = SoftLavender, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                text = "with ${session.partnerName} • ${session.scheduledDate} at ${session.scheduledTime}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }

        if (session.notes.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ElevatedNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Notes: ${session.notes}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    modifier = Modifier.padding(10.dp)
                )
            }
        }

        if (isActive) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditNotes,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Notes", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onComplete,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintSuccess, contentColor = Color.White),
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("complete_session_${session.id}")
                ) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Complete +150 XP", style = MaterialTheme.typography.labelMedium)
                }
            }
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.End)) {
                Text("Cancel Session", style = MaterialTheme.typography.labelSmall.copy(color = RoseError))
            }
        }
    }
}

@Composable
private fun ScheduleSessionDialog(
    partners: List<MentorPartner>,
    initialPartner: MentorPartner,
    onDismiss: () -> Unit,
    onConfirm: (MentorPartner, String, String, String, String, Long, String) -> Unit
) {
    var selectedPartner by remember { mutableStateOf(initialPartner) }
    var partnerDropdownOpen by remember { mutableStateOf(false) }
    var topic by remember { mutableStateOf(initialPartner.teachSkill) }
    var role by remember { mutableStateOf("learning") }
    var date by remember { mutableStateOf("2026-11-21") }
    var time by remember { mutableStateOf("19:00") }
    var duration by remember { mutableLongStateOf(45L) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FrostedCardStrong,
        title = { Text("Schedule Swap Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    Surface(
                        onClick = { partnerDropdownOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = ElevatedNavy,
                        border = BorderStroke(1.dp, GlassBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Partner", style = MaterialTheme.typography.labelSmall.copy(color = TextMuted))
                            Text(selectedPartner.name, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    DropdownMenu(
                        expanded = partnerDropdownOpen,
                        onDismissRequest = { partnerDropdownOpen = false }
                    ) {
                        partners.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("${p.name} (${p.teachSkill})") },
                                onClick = {
                                    selectedPartner = p
                                    topic = p.teachSkill
                                    partnerDropdownOpen = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = topic,
                    onValueChange = { topic = it },
                    label = { Text("Skill Topic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = role == "learning",
                        onClick = { role = "learning" },
                        label = { Text("Learning") }
                    )
                    FilterChip(
                        selected = role == "teaching",
                        onClick = { role = "teaching" },
                        label = { Text("Teaching") }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time") },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30L, 45L, 60L).forEach { mins ->
                        FilterChip(
                            selected = duration == mins,
                            onClick = { duration = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Agenda / Prep Notes") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selectedPartner, topic, role, date, time, duration, notes)
                }
            ) { Text("Confirm", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
