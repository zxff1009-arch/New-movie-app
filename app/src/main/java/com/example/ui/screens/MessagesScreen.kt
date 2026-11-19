package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ChatMessage
import com.example.data.Conversation
import com.example.data.UiState
import com.example.ui.components.AvatarBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateBanner
import com.example.ui.components.GlassCard
import com.example.ui.components.LoadingStateView
import com.example.ui.components.PrimaryVioletBlueGradient
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.FrostedCardBg
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
fun MessagesScreen(
    currentUserId: String,
    conversations: List<Conversation>,
    activeConversationId: String?,
    messagesState: UiState<List<ChatMessage>>,
    onOpenConversation: (String) -> Unit,
    onCloseConversation: () -> Unit,
    onSendMessage: (conversationId: String, body: String) -> Unit,
    onSendProposal: (conversationId: String, skill: String, date: String, time: String, note: String) -> Unit,
    onRespondProposal: (Conversation, ChatMessage, Boolean) -> Unit
) {
    val activeConversation = conversations.firstOrNull { it.id == activeConversationId }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isTwoPane = maxWidth >= 720.dp

        if (isTwoPane) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ConversationListPane(
                    conversations = conversations,
                    activeConversationId = activeConversationId,
                    onSelect = onOpenConversation,
                    modifier = Modifier
                        .width(310.dp)
                        .fillMaxHeight()
                )
                if (activeConversation != null) {
                    ChatThreadPane(
                        currentUserId = currentUserId,
                        conversation = activeConversation,
                        messagesState = messagesState,
                        showBackButton = false,
                        onBack = onCloseConversation,
                        onSendMessage = { onSendMessage(activeConversation.id, it) },
                        onSendProposal = { skill, date, time, note ->
                            onSendProposal(activeConversation.id, skill, date, time, note)
                        },
                        onRespondProposal = { msg, accept ->
                            onRespondProposal(activeConversation, msg, accept)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateView(
                            icon = Icons.Rounded.ChatBubbleOutline,
                            title = "Select a conversation",
                            subtitle = "Pick a skill partner on the left to chat or propose a 1-on-1 session."
                        )
                    }
                }
            }
        } else {
            if (activeConversation != null) {
                BackHandler { onCloseConversation() }
                ChatThreadPane(
                    currentUserId = currentUserId,
                    conversation = activeConversation,
                    messagesState = messagesState,
                    showBackButton = true,
                    onBack = onCloseConversation,
                    onSendMessage = { onSendMessage(activeConversation.id, it) },
                    onSendProposal = { skill, date, time, note ->
                        onSendProposal(activeConversation.id, skill, date, time, note)
                    },
                    onRespondProposal = { msg, accept ->
                        onRespondProposal(activeConversation, msg, accept)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                ConversationListPane(
                    conversations = conversations,
                    activeConversationId = activeConversationId,
                    onSelect = onOpenConversation,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }
        }
    }
}

@Composable
private fun ConversationListPane(
    conversations: List<Conversation>,
    activeConversationId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = "Messages", style = MaterialTheme.typography.headlineLarge)
        Text(
            text = "Coordinate skill swaps and send session proposals.",
            style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
        )

        if (conversations.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.ChatBubbleOutline,
                title = "No conversations yet",
                subtitle = "Connect with a mentor in Explore or Matches to start chatting!"
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(conversations, key = { it.id }) { conv ->
                    val isSelected = conv.id == activeConversationId
                    GlassCard(
                        highlightBorder = isSelected,
                        onClick = { onSelect(conv.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conversation_item_${conv.id}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarBadge(initials = conv.partnerInitials, colorHex = conv.partnerColorHex)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = conv.partnerName,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (conv.unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(VioletPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = conv.unreadCount.toString(),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = conv.partnerSkill,
                                    style = MaterialTheme.typography.labelSmall.copy(color = SoftLavender),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = conv.lastMessage,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatThreadPane(
    currentUserId: String,
    conversation: Conversation,
    messagesState: UiState<List<ChatMessage>>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onSendProposal: (skill: String, date: String, time: String, note: String) -> Unit,
    onRespondProposal: (ChatMessage, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageDraft by remember { mutableStateOf("") }
    var showProposalDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val messages = (messagesState as? UiState.Success)?.data.orEmpty()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = modifier) {
        // Thread Header
        Surface(
            color = FrostedCardBg,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBackButton) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to conversations")
                    }
                }
                AvatarBadge(
                    initials = conversation.partnerInitials,
                    colorHex = conversation.partnerColorHex,
                    size = 40.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(conversation.partnerName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = conversation.partnerSkill,
                        style = MaterialTheme.typography.labelSmall.copy(color = SoftLavender)
                    )
                }
                Surface(
                    onClick = { showProposalDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = CobaltBlue.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, CobaltBlue.copy(alpha = 0.45f)),
                    modifier = Modifier.testTag("propose_session_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CalendarMonth,
                            contentDescription = "Propose session",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text("Propose", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold))
                    }
                }
            }
        }

        // Messages List
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (messagesState) {
                is UiState.Loading -> LoadingStateView("Loading messages...")
                is UiState.Error -> Box(Modifier.padding(16.dp)) {
                    ErrorStateBanner(message = messagesState.message)
                }
                is UiState.Success -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isSelf = msg.senderId == currentUserId
                            if (msg.messageType == "proposal") {
                                SessionProposalBubble(
                                    message = msg,
                                    isSelf = isSelf,
                                    onAccept = { onRespondProposal(msg, true) },
                                    onDecline = { onRespondProposal(msg, false) }
                                )
                            } else {
                                TextChatBubble(message = msg, isSelf = isSelf)
                            }
                        }
                    }
                }
            }
        }

        // Input Bar
        Surface(
            color = FrostedCardStrong,
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageDraft,
                    onValueChange = { messageDraft = it },
                    placeholder = { Text("Message ${conversation.partnerName}...", color = TextMuted) },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_message_input")
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        val text = messageDraft.trim()
                        if (text.isNotEmpty()) {
                            onSendMessage(text)
                            messageDraft = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryVioletBlueGradient)
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send message",
                        tint = Color.White
                    )
                }
            }
        }
    }

    if (showProposalDialog) {
        SessionProposalDialog(
            defaultSkill = conversation.partnerSkill,
            partnerName = conversation.partnerName,
            onDismiss = { showProposalDialog = false },
            onSubmit = { skill, date, time, note ->
                showProposalDialog = false
                onSendProposal(skill, date, time, note)
            }
        )
    }
}

@Composable
private fun TextChatBubble(message: ChatMessage, isSelf: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isSelf) 18.dp else 4.dp,
                bottomEnd = if (isSelf) 4.dp else 18.dp
            ),
            color = Color.Transparent,
            border = if (isSelf) null else BorderStroke(1.dp, GlassBorder),
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(
                    brush = if (isSelf) PrimaryVioletBlueGradient else androidx.compose.ui.graphics.SolidColor(ElevatedNavy),
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isSelf) 18.dp else 4.dp,
                        bottomEnd = if (isSelf) 4.dp else 18.dp
                    )
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White)
                )
            }
        }
    }
}

@Composable
private fun SessionProposalBubble(
    message: ChatMessage,
    isSelf: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val status = message.proposalStatus ?: "pending"
    val statusColor = when (status) {
        "accepted" -> MintSuccess
        "declined" -> RoseError
        else -> SoftLavender
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isSelf) Arrangement.End else Arrangement.Start
    ) {
        GlassCard(
            highlightBorder = true,
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Session Proposal", style = MaterialTheme.typography.labelLarge.copy(color = SoftLavender))
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusColor.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(color = statusColor, fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = message.proposalSkill ?: "1-on-1 Skill Swap",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${message.proposalDate ?: "TBD"} • ${message.proposalTime ?: "TBD"}",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
            if (message.body.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(message.body, style = MaterialTheme.typography.bodySmall)
            }
            if (status == "pending") {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDecline,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Decline", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Decline", style = MaterialTheme.typography.labelMedium)
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MintSuccess, contentColor = Color.White)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Accept", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Accept", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionProposalDialog(
    defaultSkill: String,
    partnerName: String,
    onDismiss: () -> Unit,
    onSubmit: (skill: String, date: String, time: String, note: String) -> Unit
) {
    var skill by remember { mutableStateOf(defaultSkill) }
    var date by remember { mutableStateOf("2026-11-21") }
    var time by remember { mutableStateOf("18:30") }
    var note by remember { mutableStateOf("Let's do a 45-min interactive swap!") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = FrostedCardStrong,
        title = { Text("Propose Session with $partnerName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = skill,
                    onValueChange = { skill = it },
                    label = { Text("Skill Topic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
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
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Session Note") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(skill, date, time, note) }) {
                Text("Send Proposal", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
