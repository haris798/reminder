package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class BadgeItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val emoji: String,
    val isUnlocked: Boolean,
    val progressText: String
)

@Composable
fun AchievementsCard(
    consecutiveStreakDays: Int,
    targetReachedDays7Days: Int,
    isCaffeineSafe: Boolean,
    modifier: Modifier = Modifier
) {
    val badges = listOf(
        BadgeItem(
            id = "badge_1day",
            title = "Awal Sehat",
            subtitle = "Capai 2.000 ml dalam 1 hari",
            emoji = "💧",
            isUnlocked = targetReachedDays7Days >= 1,
            progressText = if (targetReachedDays7Days >= 1) "Terbuka!" else "0/1 Hari"
        ),
        BadgeItem(
            id = "badge_3day",
            title = "Streak 3 Hari",
            subtitle = "Capai target 3 hari berturut-turut",
            emoji = "🥉",
            isUnlocked = consecutiveStreakDays >= 3,
            progressText = if (consecutiveStreakDays >= 3) "Terbuka!" else "$consecutiveStreakDays/3 Hari"
        ),
        BadgeItem(
            id = "badge_7day",
            title = "Master 7 Hari",
            subtitle = "Capai target 7 hari berturut-turut",
            emoji = "🥇",
            isUnlocked = consecutiveStreakDays >= 7,
            progressText = if (consecutiveStreakDays >= 7) "Terbuka!" else "$consecutiveStreakDays/7 Hari"
        ),
        BadgeItem(
            id = "badge_caffeine",
            title = "Kafein Bijak",
            subtitle = "Batasi kafein < 400 mg harian",
            emoji = "☕",
            isUnlocked = isCaffeineSafe,
            progressText = if (isCaffeineSafe) "Terjaga!" else "Melebihi batas"
        )
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("achievements_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB300).copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFF8F00),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Badge & Pencapaian Hidrasi 🏆",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Streak aktif: $consecutiveStreakDays hari berturut-turut",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${badges.count { it.isUnlocked }}/${badges.size} Unlocked",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Grid / Row of Badges
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                badges.chunked(2).forEach { rowBadges ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowBadges.forEach { badge ->
                            BadgeTile(
                                badge = badge,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeTile(
    badge: BadgeItem,
    modifier: Modifier = Modifier
) {
    val bgColor by animateColorAsState(
        targetValue = if (badge.isUnlocked)
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        label = "badge_bg_${badge.id}"
    )

    Surface(
        modifier = modifier.testTag("badge_item_${badge.id}"),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = if (badge.isUnlocked)
            androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(
                        if (badge.isUnlocked) Color(0xFFFFD54F).copy(alpha = 0.3f)
                        else MaterialTheme.colorScheme.surface
                    )
            ) {
                if (badge.isUnlocked) {
                    Text(text = badge.emoji, fontSize = 20.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = badge.title,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (badge.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = badge.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    maxLines = 1,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = badge.progressText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (badge.isUnlocked) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
