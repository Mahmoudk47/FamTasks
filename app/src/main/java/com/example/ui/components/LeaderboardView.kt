package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.localization.Strings
import com.example.model.User
import com.example.ui.theme.Bronze3rd
import com.example.ui.theme.BronzeBackground
import com.example.ui.theme.BronzeBorder
import com.example.ui.theme.Gold1st
import com.example.ui.theme.GoldBackground
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.Silver2nd
import com.example.ui.theme.SilverBackground
import com.example.ui.theme.SilverBorder

@Composable
fun LeaderboardView(
    members: List<User>,
    currentUserId: String,
    modifier: Modifier = Modifier
) {
    val sortedMembers = members.sortedByDescending { it.pointsBalance }

    if (sortedMembers.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Strings.get("no_tasks_found"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("leaderboard_list"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Podium section for Top 3
        item {
            PodiumHeader(sortedMembers = sortedMembers, currentUserId = currentUserId)
        }

        // Remaining list (ranks 4 and onwards)
        if (sortedMembers.size > 3) {
            item {
                Text(
                    text = Strings.get("all_tasks"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                )
            }

            itemsIndexed(sortedMembers.drop(3)) { index, member ->
                val rank = index + 4
                LeaderboardRow(
                    rank = rank,
                    user = member,
                    isCurrentUser = member.userId == currentUserId
                )
            }
        }
    }
}

@Composable
fun PodiumHeader(
    sortedMembers: List<User>,
    currentUserId: String
) {
    val first = sortedMembers.getOrNull(0)
    val second = sortedMembers.getOrNull(1)
    val third = sortedMembers.getOrNull(2)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd Place (Silver)
            if (second != null) {
                PodiumPillar(
                    user = second,
                    rank = 2,
                    badgeText = "2nd",
                    rankTitle = Strings.get("second_place"),
                    accentColor = Silver2nd,
                    borderColor = SilverBorder,
                    bgColor = SilverBackground,
                    heightDp = 180,
                    modifier = Modifier.weight(1f),
                    isCurrentUser = second.userId == currentUserId
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 1st Place (Gold)
            if (first != null) {
                PodiumPillar(
                    user = first,
                    rank = 1,
                    badgeText = "1st",
                    rankTitle = Strings.get("first_place"),
                    accentColor = Gold1st,
                    borderColor = GoldBorder,
                    bgColor = GoldBackground,
                    heightDp = 215,
                    modifier = Modifier.weight(1.15f),
                    isFirst = true,
                    isCurrentUser = first.userId == currentUserId
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3rd Place (Bronze)
            if (third != null) {
                PodiumPillar(
                    user = third,
                    rank = 3,
                    badgeText = "3rd",
                    rankTitle = Strings.get("third_place"),
                    accentColor = Bronze3rd,
                    borderColor = BronzeBorder,
                    bgColor = BronzeBackground,
                    heightDp = 160,
                    modifier = Modifier.weight(1f),
                    isCurrentUser = third.userId == currentUserId
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun PodiumPillar(
    user: User,
    rank: Int,
    badgeText: String,
    rankTitle: String,
    accentColor: Color,
    borderColor: Color,
    bgColor: Color,
    heightDp: Int,
    modifier: Modifier = Modifier,
    isFirst: Boolean = false,
    isCurrentUser: Boolean = false
) {
    Card(
        modifier = modifier
            .height(heightDp.dp)
            .shadow(if (isFirst) 8.dp else 4.dp, RoundedCornerShape(16.dp))
            .border(
                width = if (isFirst) 2.5.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .testTag("podium_rank_$rank"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Badge / Crown or Avatar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(if (isFirst) 48.dp else 40.dp)
                    .clip(CircleShape)
            ) {
                if (user.avatarTemplate.isNotBlank() || user.avatarUri.isNotBlank()) {
                    UserAvatar(
                        avatarTemplate = user.avatarTemplate,
                        avatarUri = user.avatarUri,
                        sizeDp = if (isFirst) 48 else 40
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(accentColor, accentColor.copy(alpha = 0.7f))
                                )
                            )
                    ) {
                        if (isFirst) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Gold Crown",
                                tint = Color(0xFF78350F),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = badgeText,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // User Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = user.displayName.ifBlank { "Member" },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                if (isCurrentUser) {
                    Text(
                        text = "(${Strings.get("assign_to_me")})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp
                    )
                }
            }

            // Points Pill
            Surface(
                color = accentColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Points",
                        tint = if (isFirst) Color(0xFFD97706) else accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${user.pointsBalance}",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardRow(
    rank: Int,
    user: User,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_$rank"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rank number
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rank",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                UserAvatar(
                    avatarTemplate = user.avatarTemplate,
                    avatarUri = user.avatarUri,
                    sizeDp = 36
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = user.displayName.ifBlank { "Member" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isCurrentUser) {
                        Text(
                            text = Strings.get("assign_to_me"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Points
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Points",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${user.pointsBalance} ${Strings.get("points")}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
