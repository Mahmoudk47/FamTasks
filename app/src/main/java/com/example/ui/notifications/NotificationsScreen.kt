package com.example.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.FamTasksRepository
import com.example.localization.Strings
import com.example.model.AppNotification
import com.example.ui.theme.EmeraldSuccess
import kotlinx.coroutines.launch

@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    repository: FamTasksRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    if (notifications.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.NotificationsNone,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = Strings.get("no_notifications"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("notifications_list"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(notifications, key = { it.notificationId }) { notif ->
            NotificationCard(
                notification = notif,
                onClick = {
                    if (!notif.isRead) {
                        scope.launch {
                            repository.markNotificationRead(notif.notificationId)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun NotificationCard(
    notification: AppNotification,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notification_item_${notification.notificationId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (notification.isRead) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (notification.isRead) 1.dp else 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            notification.type.contains("approved") || notification.type.contains("completed") -> EmeraldSuccess.copy(alpha = 0.2f)
                            notification.type.contains("store") -> Color(0xFFFEF3C7)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        notification.type.contains("join") -> Icons.Default.GroupAdd
                        notification.type.contains("purchase") -> Icons.Default.ShoppingCart
                        notification.type.contains("store") -> Icons.Default.CardGiftcard
                        notification.type.contains("completed") -> Icons.Default.CheckCircle
                        else -> Icons.Default.AssignmentTurnedIn
                    },
                    contentDescription = null,
                    tint = when {
                        notification.type.contains("approved") || notification.type.contains("completed") -> EmeraldSuccess
                        notification.type.contains("store") -> Color(0xFFD97706)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (notification.isRead) FontWeight.SemiBold else FontWeight.ExtraBold
                    )
                    if (!notification.isRead) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
