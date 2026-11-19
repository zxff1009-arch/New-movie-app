package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.CyanHighlight
import com.example.ui.theme.DeepNavyBg
import com.example.ui.theme.FrostedCardBg
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MintSuccess
import com.example.ui.theme.RoseError
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletDeep
import com.example.ui.theme.VioletPrimary

val PrimaryVioletBlueGradient = Brush.linearGradient(
    colors = listOf(VioletPrimary, CobaltBlue)
)

val AccentCardBorderBrush = Brush.linearGradient(
    colors = listOf(VioletPrimary.copy(alpha = 0.45f), CobaltBlue.copy(alpha = 0.25f))
)

@Composable
fun SkillSwapBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyBg)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(VioletDeep.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(size.width * 0.15f, size.height * 0.12f),
                        radius = size.maxDimension * 0.55f
                    ),
                    radius = size.maxDimension * 0.55f,
                    center = Offset(size.width * 0.15f, size.height * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(CobaltBlue.copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.72f),
                        radius = size.maxDimension * 0.5f
                    ),
                    radius = size.maxDimension * 0.5f,
                    center = Offset(size.width * 0.88f, size.height * 0.72f)
                )
            },
        content = content
    )
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    highlightBorder: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val borderStroke = if (highlightBorder) {
        BorderStroke(1.dp, AccentCardBorderBrush)
    } else {
        BorderStroke(1.dp, GlassBorder)
    }
    Surface(
        modifier = modifier
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            ),
        shape = shape,
        color = FrostedCardBg,
        border = borderStroke,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    leadingIcon: ImageVector? = null,
    testTag: String? = null
) {
    val shape = RoundedCornerShape(14.dp)
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFF283254),
            disabledContentColor = TextMuted
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .background(
                brush = if (enabled && !loading) PrimaryVioletBlueGradient else Brush.linearGradient(
                    listOf(Color(0xFF283254), Color(0xFF283254))
                ),
                shape = shape
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlassOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    testTag: String? = null
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, GlassBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0x331B264A),
            contentColor = TextPrimary
        ),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun AvatarBadge(
    initials: String,
    colorHex: String,
    size: Dp = 46.dp,
    modifier: Modifier = Modifier
) {
    val parsedColor = parseHexColor(colorHex, VioletPrimary)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(parsedColor, parsedColor.copy(alpha = 0.65f))
                )
            )
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.take(2).uppercase(),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

@Composable
fun MatchScoreBadge(score: Int, modifier: Modifier = Modifier) {
    val badgeColor = when {
        score >= 90 -> MintSuccess
        score >= 75 -> CyanHighlight
        else -> SoftLavender
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = badgeColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = badgeColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "$score% Match",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = badgeColor,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
fun SkillTagChip(
    label: String,
    accentColor: Color = VioletPrimary,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = accentColor.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(color = TextPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun XpProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(),
        label = "xp_progress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF1F2A4D))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(PrimaryVioletBlueGradient)
        )
    }
}

@Composable
fun LoadingStateView(
    message: String = "Loading SkillSwap...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = VioletPrimary)
            Spacer(Modifier.height(14.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(VioletPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = SoftLavender,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary),
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))
                GradientButton(text = actionLabel, onClick = onAction)
            }
        }
    }
}

@Composable
fun ErrorStateBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = RoseError.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, RoseError.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = "Error",
                tint = RoseError
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                Spacer(Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onRetry,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Retry", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Retry", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color((0xFF000000L or colorInt).toInt())
        } else {
            Color(colorInt.toInt())
        }
    } catch (_: Exception) {
        fallback
    }
}
