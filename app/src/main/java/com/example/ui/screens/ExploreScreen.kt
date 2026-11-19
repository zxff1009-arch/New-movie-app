package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MentorPartner
import com.example.data.PartnerMatch
import com.example.data.SkillCatalog
import com.example.ui.components.AvatarBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassOutlinedButton
import com.example.ui.components.GradientButton
import com.example.ui.components.MatchScoreBadge
import com.example.ui.components.SkillTagChip
import com.example.ui.theme.AmberGold
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.ElevatedNavy
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletPrimary

@Composable
fun ExploreScreen(
    matches: List<PartnerMatch>,
    searchQuery: String,
    selectedCategory: String,
    selectedLevel: String,
    selectedAvailability: String,
    onSearchQueryChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onLevelChange: (String) -> Unit,
    onAvailabilityChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onSelectPartner: (PartnerMatch) -> Unit,
    onToggleConnect: (MentorPartner) -> Unit,
    onMessagePartner: (MentorPartner) -> Unit
) {
    var levelMenuOpen by remember { mutableStateOf(false) }
    var availMenuOpen by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = 1040.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Explore Skills", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "Discover verified mentors ready to swap skills 1-on-1.",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search skills, mentors, or topics...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = SoftLavender) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ElevatedNavy,
                    unfocusedContainerColor = ElevatedNavy,
                    focusedBorderColor = VioletPrimary,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("explore_search_input")
            )

            // Category Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SkillCatalog.categories) { cat ->
                    val active = selectedCategory == cat
                    FilterChip(
                        selected = active,
                        onClick = { onCategoryChange(cat) },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VioletPrimary.copy(alpha = 0.28f),
                            selectedLabelColor = TextPrimary,
                            containerColor = ElevatedNavy,
                            labelColor = TextSecondary
                        ),
                        modifier = Modifier.testTag("category_chip_${cat.lowercase()}")
                    )
                }
            }

            // Secondary Filters: Level & Availability
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Surface(
                        onClick = { levelMenuOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = ElevatedNavy,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.FilterList, contentDescription = null, tint = SoftLavender, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(selectedLevel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    DropdownMenu(expanded = levelMenuOpen, onDismissRequest = { levelMenuOpen = false }) {
                        SkillCatalog.levels.forEach { lvl ->
                            DropdownMenuItem(
                                text = { Text(lvl) },
                                onClick = {
                                    onLevelChange(lvl)
                                    levelMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Box {
                    Surface(
                        onClick = { availMenuOpen = true },
                        shape = RoundedCornerShape(12.dp),
                        color = ElevatedNavy,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Schedule, contentDescription = null, tint = CobaltBlue, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(selectedAvailability, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    DropdownMenu(expanded = availMenuOpen, onDismissRequest = { availMenuOpen = false }) {
                        SkillCatalog.availabilities.forEach { avail ->
                            DropdownMenuItem(
                                text = { Text(avail) },
                                onClick = {
                                    onAvailabilityChange(avail)
                                    availMenuOpen = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                Text(
                    text = "${matches.size} mentors",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                )
            }

            if (matches.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Rounded.Search,
                    title = "No matching skill partners",
                    subtitle = "Try broadening your search or resetting category and availability filters.",
                    actionLabel = "Reset Filters",
                    onAction = onClearFilters
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isWide = maxWidth >= 660.dp
                    if (isWide) {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(matches, key = { it.partner.id }) { item ->
                                ExploreSkillCard(
                                    match = item,
                                    onSelect = { onSelectPartner(item) },
                                    onToggleConnect = { onToggleConnect(item.partner) },
                                    onMessage = { onMessagePartner(item.partner) }
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(matches, key = { it.partner.id }) { item ->
                                ExploreSkillCard(
                                    match = item,
                                    onSelect = { onSelectPartner(item) },
                                    onToggleConnect = { onToggleConnect(item.partner) },
                                    onMessage = { onMessagePartner(item.partner) }
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
fun ExploreSkillCard(
    match: PartnerMatch,
    onSelect: () -> Unit,
    onToggleConnect: () -> Unit,
    onMessage: () -> Unit
) {
    val p = match.partner
    GlassCard(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("partner_card_${p.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarBadge(initials = p.avatarInitials, colorHex = p.avatarColorHex)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = p.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = p.headline,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MatchScoreBadge(score = match.matchScore)
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkillTagChip(
                label = "Teaches: ${p.teachSkill}",
                accentColor = VioletPrimary,
                modifier = Modifier.weight(1f, fill = false)
            )
            SkillTagChip(
                label = "Wants: ${p.wantSkill}",
                accentColor = CobaltBlue,
                modifier = Modifier.weight(1f, fill = false)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Star, contentDescription = "Rating", tint = AmberGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${p.rating} (${p.reviewCount}) • ${p.teachLevel}",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                )
            }
            Text(
                text = p.availability,
                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (p.connected) {
                GlassOutlinedButton(
                    text = "Connected",
                    leadingIcon = Icons.Rounded.Check,
                    onClick = onToggleConnect,
                    modifier = Modifier.weight(1f),
                    testTag = "connect_btn_${p.id}"
                )
                GradientButton(
                    text = "Message",
                    onClick = onMessage,
                    modifier = Modifier.weight(1f),
                    testTag = "message_btn_${p.id}"
                )
            } else {
                GradientButton(
                    text = "Connect",
                    onClick = onToggleConnect,
                    modifier = Modifier.weight(1f),
                    testTag = "connect_btn_${p.id}"
                )
                GlassOutlinedButton(
                    text = "Profile",
                    onClick = onSelect,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
